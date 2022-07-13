package com.activeviam.experiments.gameoflife.biz.tasks;

import com.activeviam.experiments.gameoflife.task.ATask;

public class GameOfLifeWatcher extends ATask<Void> {

	@SuppressWarnings({"BusyWait", "InfiniteLoopStatement"})
	@Override
	protected Void compute() throws Exception {
		startTimer();

		try {
			while (true) {
				trace();
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			stopTimer();
			traceTimer();
			throw e;
		}
	}

	private void trace() {
		GameOfLifeContext ctx = GameOfLifeContext.getContext();

		StringBuilder sb = new StringBuilder();
		sb.append("Total iterations: ").append(ctx.getIterations()).append('\n');
		for (int i = 0; i < ctx.getParallelism(); ++i) {
			sb.append(String.format("%8d", ctx.getProgress(i)));
			if (i % 4 == 3) {
				sb.append('\n');
			}
		}

		System.out.println(sb);
	}

	@Override
	protected void dispose() {
		// Do nothing.
	}

	private long beginTime;
	private long endTime;
	private static final double NS_TO_MS = 1e-6;

	private void startTimer() {
		beginTime = System.nanoTime();
	}

	private void stopTimer() {
		endTime = System.nanoTime();
	}

	private void traceTimer() {
		System.out.println("Time elapsed: " + (endTime - beginTime) * NS_TO_MS + " ms");
	}
}
