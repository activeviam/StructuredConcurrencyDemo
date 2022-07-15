package com.activeviam.experiments.gameoflife.biz.tasks.process;

import com.activeviam.experiments.gameoflife.biz.Utils;
import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext;
import com.activeviam.experiments.gameoflife.task.ATask;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.incubator.concurrent.StructuredTaskScope;

public class ComputeTask extends ATask<BoardChunk> {

	private ATask<BoardChunk> prevTask;
	private ATask<BoardChunk> sameTask;
	private ATask<BoardChunk> nextTask;
	private final int idx;

	public ComputeTask(ATask<BoardChunk> prevTask, ATask<BoardChunk> sameTask, ATask<BoardChunk> nextTask,
			int idx) {
		this.prevTask = prevTask;
		this.sameTask = sameTask;
		this.nextTask = nextTask;
		this.idx = idx;
	}

	@Override
	protected List<ATask<?>> getDependencies() {
		return Stream.of(prevTask, sameTask, nextTask).filter(Objects::nonNull).collect(Collectors.toList());
	}

	@Override
	protected BoardChunk compute() throws Exception {
		BoardChunk prevChunk;
		BoardChunk sameChunk;
		BoardChunk nextChunk;

		try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
			Future<BoardChunk> prevFuture = Utils.forkOrDefault(scope, prevTask, null);
			Future<BoardChunk> sameFuture = scope.fork(sameTask);
			Future<BoardChunk> nextFuture = Utils.forkOrDefault(scope, nextTask, null);

			scope.join().throwIfFailed();

			prevChunk = prevFuture.resultNow();
			sameChunk = sameFuture.resultNow();
			nextChunk = nextFuture.resultNow();
		}

		BoardChunk result = sameChunk.nextChunk();
		boolean[][] data = result.data();
		fill(data, prevChunk, sameChunk, nextChunk);

		GameOfLifeContext.getContext().incProgress(idx);

		return result;
	}

	private void fill(boolean[][] data, BoardChunk prevChunk, BoardChunk sameChunk, BoardChunk nextChunk) {
		int stripeWidth = sameChunk.getStripeWidth();
		int height = sameChunk.height();

		for (int i = 0; i < stripeWidth; ++i) {
			for (int j = 0; j < height; ++j) {
				boolean isAlive = sameChunk.getAt(i, j);
				int neighbours = 0;

				for (int di = -1; di <= 1; ++di) {
					for (int dj = -1; dj <= 1; ++dj) {
						if (di == 0 && dj == 0) {
							continue;
						}

						int ni = i + di;
						int nj = j + dj;

						boolean isNeighbourAlive;
						if (ni < 0) {
							isNeighbourAlive = prevChunk != null && prevChunk.getAt(prevChunk.getStripeWidth() - 1, nj);
						} else if (ni >= stripeWidth) {
							isNeighbourAlive = nextChunk != null && nextChunk.getAt(0, nj);
						} else {
							isNeighbourAlive = sameChunk.getAt(ni, nj);
						}

						if (isNeighbourAlive) {
							++neighbours;
						}
					}
				}

				data[i][j] = decide(isAlive, neighbours);
			}
		}
	}

	private boolean decide(boolean isAlive, int neighbours) {
		if (isAlive) {
			return neighbours == 2 || neighbours == 3;
		} else {
			return neighbours == 3;
		}
	}

	@Override
	protected void dispose() {
		prevTask = null;
		sameTask = null;
		nextTask = null;
	}
}
