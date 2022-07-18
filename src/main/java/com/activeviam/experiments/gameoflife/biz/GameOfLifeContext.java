package com.activeviam.experiments.gameoflife.biz;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicIntegerArray;
import jdk.incubator.concurrent.ExtentLocal;

/**
 * This class represents the context of computation of the Game Of Life.
 */
public class GameOfLifeContext {

	private static final ExtentLocal<GameOfLifeContext> CONTEXT = ExtentLocal.newInstance();

	/**
	 * This enum is used to mark timestamps.
	 */
	public enum ExecutionStage {
		/**
		 * Start retrieving
		 */
		RETRIEVING,
		/**
		 * Stop retrieving and start computing
		 */
		COMPUTING,
		/**
		 * Computing is done, start exporting
		 */
		EXPORTING,
		/**
		 * Exporting is done, terminating
		 */
		DONE
	}

	public static GameOfLifeContext getContext() {
		return CONTEXT.get();
	}

	/**
	 * Create a scope with an instance of the context.
	 *
	 * @param context The instance of context to be used within this scope
	 * @return A new scope
	 */
	public static ExtentLocal.Carrier withContext(GameOfLifeContext context) {
		return ExtentLocal.where(CONTEXT, context);
	}

	private final int parallelism;
	private final int iterations;
	private final AtomicIntegerArray progress;
	private final Map<ExecutionStage, Long> timestamps = new HashMap<>();

	/**
	 * Construct a new context instance.
	 *
	 * @param parallelism Number of parallel computation flows
	 * @param iterations  Number of iterations to be computed
	 */
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

	/**
	 * Get the iteration of the {@code idx}'th computation flow.
	 *
	 * @param idx The index of computation flow
	 * @return Number of iterations done in this flow
	 */
	public int getProgress(int idx) {
		return progress.get(idx);
	}

	/**
	 * Notify the context that one more iteration is done.
	 *
	 * @param idx The index of computation flow
	 */
	public void incProgress(int idx) {
		progress.addAndGet(idx, 1);
	}

	/**
	 * Notify the context that the computation has started a new stage.
	 *
	 * @param stage The stage that was started
	 */
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
