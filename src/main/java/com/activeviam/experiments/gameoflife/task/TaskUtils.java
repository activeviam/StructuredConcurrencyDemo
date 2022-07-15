package com.activeviam.experiments.gameoflife.task;

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import jdk.incubator.concurrent.StructuredTaskScope;

public class TaskUtils {

	public static <V> ATask<V> withWatchers(ATask<V> mainTask, ATask<?>... watcherTasks) {
		return new TaskWithWatchers<>(mainTask, watcherTasks);
	}

	private static class TaskWithWatchers<V> extends ATask<V> {

		private ATask<V> mainTask;
		private List<ATask<?>> watcherTasks;

		public TaskWithWatchers(ATask<V> mainTask, ATask<?>... watcherTasks) {
			this.mainTask = mainTask;
			this.watcherTasks = List.of(watcherTasks);
		}

		@Override
		protected List<ATask<?>> getDependencies() {
			return List.of();
		}

		@Override
		protected V compute() throws Exception {
			try (var scope = new StructuredTaskScope.ShutdownOnSuccess<V>()) {
				scope.fork(mainTask);

				for (var watcherTask : watcherTasks) {
					scope.fork(() -> {
						try {
							watcherTask.call();
							throw new IllegalStateException("Watcher must not terminate normally");
						} catch (InterruptedException e) {
							// Do nothing, it's ok
						} catch (Throwable t) {
							t.printStackTrace();
							throw t;
						}
						return null;
					});
				}

				scope.join();
				return scope.result();
			}
		}

		@Override
		protected void dispose() {
			mainTask = null;
			watcherTasks = null;
		}
	}

	public static <T> ATask<T> buildRunner(ATask<T> resultTask) {
		return new DependenciesRunner(resultTask);
	}

	private static class DependenciesRunner<V> extends ATask<V> {

		private ATask<V> resultTask;
		private List<ATask<?>> dependencies = new LinkedList<>();

		public DependenciesRunner(ATask<V> resultTask) {
			this.resultTask = resultTask;
			scanDependencies();
		}

		private void scanDependencies() {
			Set<ATask<?>> visited = new HashSet<>();

			record StackEntry(ATask<?> node, Iterator<ATask<?>> edges) {
				public StackEntry(ATask<?> node) {
					this(node, node.getDependencies().iterator());
				}
			}
			Deque<StackEntry> stack = new LinkedList<>();

			visited.add(resultTask);
			stack.push(new StackEntry(resultTask));

			while (!stack.isEmpty()) {
				StackEntry entry = stack.peek();

				if (entry.edges.hasNext()) {
					ATask<?> next = entry.edges.next();
					if (visited.contains(next)) {
						continue;
					}

					visited.add(next);
					stack.push(new StackEntry(next));
				} else {
					stack.pop();
					dependencies.add(entry.node);
				}
			}

			// remove resultTask from the list of dependencies
			dependencies.remove(dependencies.size() - 1);
		}

		@Override
		protected List<ATask<?>> getDependencies() {
			return List.of();
		}

		@Override
		protected V compute() throws Exception {
			try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
				for (ATask<?> dependency : dependencies) {
					scope.fork(dependency);
				}

				Future<V> f = scope.fork(resultTask);
				scope.join().throwIfFailed();
				return f.resultNow();
			}
		}

		@Override
		protected void dispose() {
			resultTask = null;
			dependencies = null;
		}
	}
}
