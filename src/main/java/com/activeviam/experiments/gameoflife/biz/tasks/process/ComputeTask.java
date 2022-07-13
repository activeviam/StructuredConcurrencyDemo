package com.activeviam.experiments.gameoflife.biz.tasks.process;

import com.activeviam.experiments.gameoflife.biz.Utils;
import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext;
import com.activeviam.experiments.gameoflife.task.ATask;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import jdk.incubator.concurrent.StructuredTaskScope;

public class ComputeTask extends ATask<BoardChunk> {

	private Callable<BoardChunk> prevTask;
	private Callable<BoardChunk> sameTask;
	private Callable<BoardChunk> nextTask;
	private final int idx;

	public ComputeTask(Callable<BoardChunk> prevTask, Callable<BoardChunk> sameTask, Callable<BoardChunk> nextTask,
			int idx) {
		this.prevTask = prevTask;
		this.sameTask = sameTask;
		this.nextTask = nextTask;
		this.idx = idx;
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

		boolean[][] data = new boolean[sameChunk.getStripeWidth()][sameChunk.height()];
		fill(data, prevChunk, sameChunk, nextChunk);

		GameOfLifeContext.getContext().incProgress(idx);

		return new BoardChunk(
				sameChunk.width(),
				sameChunk.height(),
				sameChunk.beginWidth(),
				sameChunk.endWidth(),
				data
		);
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
