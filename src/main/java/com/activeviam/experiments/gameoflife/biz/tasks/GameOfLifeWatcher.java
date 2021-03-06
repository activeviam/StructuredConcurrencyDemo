package com.activeviam.experiments.gameoflife.biz.tasks;

import com.activeviam.experiments.gameoflife.biz.GameOfLifeContext;
import com.activeviam.experiments.gameoflife.biz.GameOfLifeContext.ExecutionStage;
import com.activeviam.experiments.gameoflife.task.ATask;
import java.util.Arrays;

/**
 * This task watches for the computation progress. This task never stops unless interrupted.
 */
public class GameOfLifeWatcher extends ATask<Void> {

	@SuppressWarnings({"BusyWait", "InfiniteLoopStatement"})
	@Override
	protected Void compute() throws Exception {
		try {
			while (true) {
				traceProgress();
				Thread.sleep(1000);
			}
		} catch (InterruptedException e) {
			traceSummary();
			throw e;
		}
	}

	private void traceProgress() {
		GameOfLifeContext ctx = GameOfLifeContext.getContext();

		StringBuilder sb = new StringBuilder();
		sb.append("Total iterations: ").append(ctx.getIterations());
		for (int i = 0; i < ctx.getParallelism(); ++i) {
			if (i % 4 == 0) {
				sb.append('\n');
			}
			sb.append(String.format("%8d", ctx.getProgress(i)));
		}

		System.err.println(sb);
	}
	private static final double NS_TO_MS = 1e-6;

	private void traceSummary() {
		traceContext();
		traceTimer();
	}

	private void traceContext() {
		System.out.println("Context: ");
		System.out.println(GameOfLifeContext.getContext());
	}

	private void traceTimer() {
		StringBuilder sb = new StringBuilder();
		sb.append("Stages duration:\n");

		int titleLength =
				Arrays.stream(ExecutionStage.values())
						.map(stage -> stage.name().length())
						.reduce(0, Math::max);

		var durations = GameOfLifeContext.getContext().getDurations();
		for (ExecutionStage stage : ExecutionStage.values()) {
			if (!durations.containsKey(stage)) {
				continue;
			}

			sb.append(stage).append(": ")
					.append(" ".repeat(titleLength - stage.name().length()))
					.append(durations.get(stage) * NS_TO_MS).append(" ms\n");
		}

		System.out.println(sb);
	}
}
