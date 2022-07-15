package com.activeviam.experiments.gameoflife.biz.tasks;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerArray;
import jdk.incubator.concurrent.ExtentLocal;

public class GameOfLifeContext {

	private static final ExtentLocal<GameOfLifeContext> CONTEXT = ExtentLocal.newInstance();

	public enum ExecutionStage {
		RETRIEVING,
		COMPUTING,
		EXPORTING,
		DONE
	}

	public static GameOfLifeContext getContext() {
		return CONTEXT.get();
	}

	public static ExtentLocal.Carrier withContext(GameOfLifeContext context) {
		return ExtentLocal.where(CONTEXT, context);
	}

	private final int parallelism;
	private final int iterations;
	private final AtomicIntegerArray progress;
	private final Map<ExecutionStage, Long> timestamps = new HashMap<>();

	public GameOfLifeContext(int parallelism, int iterations) {
		this.parallelism = parallelism;
		this.iterations = iterations;
		this.progress = new AtomicIntegerArray(parallelism);
	}

	public int getParallelism() {
		return parallelism;
	}

	public int getIterations() {
		return iterations;
	}

	public int getProgress(int idx) {
		return progress.get(idx);
	}

	public void incProgress(int idx) {
		progress.addAndGet(idx, 1);
	}

	public void addTimestamp(ExecutionStage stage) {
		timestamps.put(stage, System.nanoTime());
	}

	public Map<ExecutionStage, Long> getDurations() {
		List<Entry<ExecutionStage, Long>> events =
				timestamps
				.entrySet()
				.stream()
				.sorted(
						Comparator.comparingInt(entry -> entry.getKey().ordinal())
				).toList();

		HashMap<ExecutionStage, Long> durations = new HashMap<>();

		for (int i = 1; i < events.size(); ++i) {
			ExecutionStage stage = events.get(i - 1).getKey();
			long duration = events.get(i).getValue() - events.get(i - 1).getValue();
			durations.put(stage, duration);
		}

		return durations;
	}

	@Override
	public String toString() {
		return "GameOfLifeContext{" +
				"parallelism=" + parallelism +
				", iterations=" + iterations +
				", progress=" + progress +
				", timestamps=" + timestamps +
				'}';
	}
}
