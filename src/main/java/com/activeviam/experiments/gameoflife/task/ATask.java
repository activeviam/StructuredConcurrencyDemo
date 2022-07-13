package com.activeviam.experiments.gameoflife.task;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class ATask<V> implements Callable<V> {

	private final AtomicBoolean startedFlag = new AtomicBoolean(false);
	private final CountDownLatch done = new CountDownLatch(1);
	private V result = null;
	private Exception ex = null;

	@Override
	public V call() throws Exception {
		if (startedFlag.compareAndSet(false, true)) {
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
		} else {
			done.await();
			if (this.ex != null) {
				throw new RuntimeException(this.ex);
			} else {
				return this.result;
			}
		}
	}

	protected abstract V compute() throws Exception;
	protected abstract void dispose();
}
