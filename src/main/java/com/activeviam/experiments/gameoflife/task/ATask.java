package com.activeviam.experiments.gameoflife.task;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An abstract task which represents one-shot computation. The result of this computation may be acquired by
 * several dependent tasks.
 *
 * @param <V>
 */
public abstract class ATask<V> implements Callable<V> {

	private final AtomicBoolean startedFlag = new AtomicBoolean(false);
	private final CountDownLatch done = new CountDownLatch(1);
	private V result = null;
	private Exception ex = null;

	@Override
	public V call() throws Exception {
		if (tryStart()) {
			return unsafeCall();
		} else {
			return waitForResult();
		}
	}

	/**
	 * Flip the {@code startedFlag} using atomic CAS. Used for single-shot semantics when launched concurrently from
	 * several threads.
	 *
	 * @return {@code true} if the caller thread is allowed to run the task code. It is guaranteed that exactly one
	 * caller receive {@code true} result.
	 */
	boolean tryStart() {
		return startedFlag.compareAndSet(false, true);
	}

	/**
	 * Simple wrapper around user code that handles errors, saves the result and calls {@link #dispose()} method.
	 * Must not be called unless {@link #tryStart()} returned {@code true}.
	 *
	 * @return The result of the user code execution
	 * @throws Exception if user code has raised an exception
	 */
	V unsafeCall() throws Exception {
		try {
			this.result = compute();
			return this.result;
		} catch (Exception ex) {
			this.ex = ex;
			throw ex;
		} finally {
			try {
				dispose();
			} finally {
				done.countDown();
			}
		}
	}

	/**
	 * Wait for the result computed in another thread.
	 *
	 * @return The result
	 * @throws Exception if an error occurred
	 */
	V waitForResult() throws Exception {
		done.await();
		if (this.ex != null) {
			throw new RuntimeException(this.ex); // <- Maybe replace with custom exception?
		} else {
			return this.result;
		}
	}

	/**
	 * Retrieve all fields marked with {@link Dependency @Dependency} annotation.
	 * @return Task dependencies
	 * */
	protected List<ATask<?>> getDependencies() {
		return Arrays.stream(this.getClass().getDeclaredFields()).flatMap(field -> {
			if (!field.isAnnotationPresent(Dependency.class)) {
				return null;
			}

			try {
				field.setAccessible(true);
				if (ATask.class.isAssignableFrom(field.getType())) {
					return Stream.ofNullable((ATask<?>) field.get(this));
				} else if (Collection.class.isAssignableFrom(field.getType())) {
					Collection<?> collection = (Collection<?>) field.get(this);
					return collection
							.stream()
							.filter(x -> x != null && ATask.class.isAssignableFrom(x.getClass()))
							.map(x -> (ATask<?>) x);
				} else {
					throw new RuntimeException("Field " + field + " should not be annotated with @Dependency");
				}
			} catch (IllegalAccessException e) {
				throw new InternalError(e);
			}
		}).collect(Collectors.toList());
	}

	/**
	 * The user-defined computation.
	 *
	 * @return The result of the computation
	 * @throws Exception if an exception occurs while computation
	 */
	protected abstract V compute() throws Exception;

	/**
	 * Dispose links to the dependencies. Since the task holds its result, it is highly recommended to mark the tasks
	 * the current task depends on with {@link Dependency @Dependency} annotation.
	 */
	protected void dispose() {
		Arrays.stream(this.getClass().getDeclaredFields()).forEach(field -> {
			if (!field.isAnnotationPresent(Dependency.class)) {
				return;
			}

			try {
				field.setAccessible(true);
				field.set(this, null);
			} catch (IllegalAccessException e) {
				e.printStackTrace();
				throw new RuntimeException("Illegal access to the field " + field);
			}
		});
	}
}
