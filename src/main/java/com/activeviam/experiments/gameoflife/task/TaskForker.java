package com.activeviam.experiments.gameoflife.task;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * This class is a wrapper for {@link StructuredTaskScope} that is used to reduce the number of virtual threads
 * while waiting task results.
 * @param <T>
 */
public class TaskForker<T> {
	private final StructuredTaskScope<T> scope;

	private record TaskContext<T>(ATask<T> task, CompletableFuture<T> future) {

		public void process() {
			try {
				T result = task.waitForResult();
				future.complete(result);
			} catch (Exception e) {
				future.completeExceptionally(e);
			}
		}
	}

	private static final TaskContext<?> POISON_CTX = new TaskContext<>(null, null);
	private final LinkedBlockingQueue<TaskContext<?>> queue = new LinkedBlockingQueue<>();
	private boolean closed = false;

	/**
	 * Construct a new {@link TaskForker}.
	 * @param scope Current scope
	 */
	public TaskForker(StructuredTaskScope<T> scope) {
		this.scope = scope;
		scope.fork(() -> {
			processQueue();
			return null;
		});
	}

	private void processQueue() throws InterruptedException {
		while (true) {
			TaskContext<?> ctx = queue.take();
			if (ctx == POISON_CTX) {
				// No more tasks, exiting
				return;
			}

			ctx.process();
		}
	}

	/**
	 * Try to fork a task. If it is already started, add it to the joining queue.
	 * @param task The task to be forked
	 * @param <U> Task result type
	 * @return A future that represents the task result
	 * @throws InterruptedException if the current thread is interrupted while fork
	 */
	public <U extends T> Future<U> fork(ATask<U> task) throws InterruptedException {
		if (closed) {
			throw new IllegalStateException("Cannot call fork() after done()");
		}
		if (task.tryStart()) {
			return scope.fork(task::unsafeCall);
		} else {
			return enqueue(task);
		}
	}

	private <U> Future<U> enqueue(ATask<U> task) throws InterruptedException {
		CompletableFuture<U> future = new CompletableFuture<>();
		TaskContext<U> ctx = new TaskContext<>(task, future);
		queue.put(ctx);
		return future;
	}

	/**
	 * Send a signal tho the joiner thread to terminate
	 * Must be called right before {@link StructuredTaskScope#join() scope.join()}.
	 */
	public void done() {
		closed = true;
		queue.add(POISON_CTX);
	}
}
