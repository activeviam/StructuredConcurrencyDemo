package com.activeviam.experiments.gameoflife.task;

import java.util.concurrent.Callable;
import jdk.incubator.concurrent.StructuredTaskScope;

public class TaskUtils {
	public static <V> Callable<V> withWatcher(Callable<V> mainTask, Callable<Void> watcherTask) {
		return () -> {
			try (var scope = new StructuredTaskScope.ShutdownOnSuccess<V>()) {
				scope.fork(mainTask);
				scope.fork(() -> {
					try {
						watcherTask.call();
					} catch (InterruptedException e) {
						// Do nothing, it's ok
					} catch (Throwable t) {
						t.printStackTrace();
						throw t;
					}
					return null;
				});

				scope.join();
				return scope.result();
			}
		};
	}
}
