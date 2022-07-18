package com.activeviam.experiments.gameoflife.task;

import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * This class provides some utilities useful for creating tasks.
 */
public class TaskUtils {

	/**
	 * Run a task along with watcher tasks (e.g. loggers).
	 *
	 * @param mainTask     The main task to be executed
	 * @param watcherTasks Watcher tasks
	 * @param <V>          The return type of main tasks
	 * @return A new task that encapsulates the original main task and runs watchers in parallel
	 */
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
							// It's an expected way to terminate the watcher
							throw e;
						} catch (Throwable t) {
							t.printStackTrace();
							throw t;
						}
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

	/**
	 * Scan the dependency graph of the task and run all the tasks in correct order in parallel. May be useful if the
	 * dependency graph depth is large.
	 *
	 * @param resultTask The main task
	 * @param <T>        The return type of the main task
	 * @return A wrapper task that runs all the dependencies in the topological sort order
	 */
	public static <T> ATask<T> buildRunner(ATask<T> resultTask) {
		return new DependenciesRunner<>(resultTask);
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

			@SuppressWarnings("MissingJavadoc")
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
