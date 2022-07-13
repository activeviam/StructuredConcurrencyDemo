package com.activeviam.experiments.gameoflife.biz;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import jdk.incubator.concurrent.StructuredTaskScope;

public class Utils {

	public static <T> T parseArg(Object[] args, int idx, Class<T> clazz) {
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

	public static  <T> Future<T> forkOrDefault(StructuredTaskScope<Object> scope, Callable<T> task, T defaultValue) {
		if (task == null) {
			return CompletableFuture.completedFuture(defaultValue);
		} else {
			return scope.fork(task);
		}
	}

}
