package com.activeviam.experiments.gameoflife.biz;

import com.activeviam.experiments.gameoflife.task.ATask;
import com.activeviam.experiments.gameoflife.task.TaskForker;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * This class contains several helper functions used in the project.
 */
public class Utils {

	/**
	 * Try to extract element {@code idx} from the array {@code args} and cast it to the class {@code clazz}.
	 *
	 * @param <T>   Result type
	 * @param args  Array of arguments
	 * @param idx   Index
	 * @param clazz Class we need to cast to
	 * @return casted element
	 * @throws IllegalArgumentException if {@code args} is null, {@code args[idx]} is null or if class cast failed
	 */
	public static <T> T parseArg(Object[] args, int idx, Class<T> clazz) {
		if (args == null) {
			throw new IllegalArgumentException("args is null");
		}
		if (args[idx] == null) {
			throw new IllegalArgumentException("args[" + idx + "] is null");
		}
		try {
			return clazz.cast(args[idx]);
		} catch (ClassCastException ex) {
			throw new IllegalArgumentException(
					"Bad args[" + idx + "] type, expected " + clazz.getName() + ", got " + args[idx].getClass().getName());
		}
	}


	/**
	 * Forks the {@code task} using {@link StructuredTaskScope#fork} method if the task is not null, otherwise return
	 * a completed {@link Future} with value {@code defaultValue}.
	 *
	 * @param <T>          Type of return value
	 * @param scope        The scope used to fork tasks
	 * @param task         The task to be forked if not null
	 * @param defaultValue The value to be returned if the task is null
	 * @return A future that either represents the forked task or is a completed future with value
	 * {@code defaultValue}
	 */
	public static <T> Future<T> forkOrDefault(StructuredTaskScope<Object> scope, ATask<T> task, T defaultValue) {
		if (task == null) {
			return CompletableFuture.completedFuture(defaultValue);
		} else {
			return scope.fork(task);
		}
	}

	/**
	 * Forks the {@code task} using {@link TaskForker#fork} method if the task is not null, otherwise return a
	 * completed {@link Future} with value {@code defaultValue}.
	 *
	 * @param <T>          Type of return value
	 * @param forker       The forker used to fork tasks
	 * @param task         The task to be forked if not null
	 * @param defaultValue The value to be returned if the task is null
	 * @return A future that either represents the forked task or is a completed future with value
	 * {@code defaultValue}
	 * @throws InterruptedException if the forker is interrupted
	 */
	public static <T> Future<T> forkOrDefault(TaskForker<Object> forker, ATask<T> task, T defaultValue)
			throws InterruptedException {
		if (task == null) {
			return CompletableFuture.completedFuture(defaultValue);
		} else {
			return forker.fork(task);
		}
	}

}
