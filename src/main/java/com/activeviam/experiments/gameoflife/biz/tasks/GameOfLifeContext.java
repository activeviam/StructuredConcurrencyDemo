package com.activeviam.experiments.gameoflife.biz.tasks;

import java.util.concurrent.atomic.AtomicIntegerArray;
import jdk.incubator.concurrent.ExtentLocal;

public class GameOfLifeContext {

	private static final ExtentLocal<GameOfLifeContext> CONTEXT = ExtentLocal.newInstance();

	public static GameOfLifeContext getContext() {
		return CONTEXT.get();
	}

	public static ExtentLocal.Carrier withContext(GameOfLifeContext context) {
		return ExtentLocal.where(CONTEXT, context);
	}

	private final int parallelism;
	private final int iterations;
	private final AtomicIntegerArray progress;

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
}
