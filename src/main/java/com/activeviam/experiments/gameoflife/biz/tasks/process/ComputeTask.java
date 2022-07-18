package com.activeviam.experiments.gameoflife.biz.tasks.process;

import com.activeviam.experiments.gameoflife.biz.Utils;
import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.biz.tasks.GameOfLifeContext;
import com.activeviam.experiments.gameoflife.task.ATask;
import com.activeviam.experiments.gameoflife.task.TaskForker;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.incubator.concurrent.StructuredTaskScope;

/**
 * This class represents the Game Of Life computation task.
 */
public class ComputeTask extends ATask<BoardChunk> {

	private ATask<BoardChunk> prevTask;
	private ATask<BoardChunk> sameTask;
	private ATask<BoardChunk> nextTask;
	private final int idx;

	/**
	 * Constructs a new computation tasks.
	 *
	 * @param prevTask The task that returns the left-neighbour chunk of the previous generation (may be null)
	 * @param sameTask The task that returns the same chunk of the previous generation
	 * @param nextTask The task that returns the right-neighbour chunk of the previous generation (may be null)
	 * @param idx      The chunk index
	 */
	@SuppressWarnings("GrazieInspection")
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
			var forker = new TaskForker<>(scope);
			Future<BoardChunk> prevFuture = Utils.forkOrDefault(forker, prevTask, null);
			Future<BoardChunk> sameFuture = forker.fork(sameTask);
			Future<BoardChunk> nextFuture = Utils.forkOrDefault(forker, nextTask, null);

			forker.done();
			scope.join().throwIfFailed();

			prevChunk = prevFuture.resultNow();
			sameChunk = sameFuture.resultNow();
			nextChunk = nextFuture.resultNow();
		}

		BoardChunk result = sameChunk.nextChunk();
		boolean[][] data = result.getData();
		fill(data, prevChunk, sameChunk, nextChunk);

		GameOfLifeContext.getContext().incProgress(idx);

		return result;
	}

	private void fill(boolean[][] data, BoardChunk prevChunk, BoardChunk sameChunk, BoardChunk nextChunk) {
		int stripeWidth = sameChunk.getStripeWidth();
		int height = sameChunk.getHeight();

		for (int x = 0; x < stripeWidth; ++x) {
			for (int y = 0; y < height; ++y) {
				boolean isAlive = sameChunk.getAt(x, y);
				int neighbours = 0;

				for (int dx = -1; dx <= 1; ++dx) {
					for (int dy = -1; dy <= 1; ++dy) {
						if (dx == 0 && dy == 0) {
							continue;
						}

						int nx = x + dx;
						int ny = y + dy;

						boolean isNeighbourAlive;
						if (nx < 0) {
							isNeighbourAlive = prevChunk != null && prevChunk.getAt(prevChunk.getStripeWidth() - 1, ny);
						} else if (nx >= stripeWidth) {
							isNeighbourAlive = nextChunk != null && nextChunk.getAt(0, ny);
						} else {
							isNeighbourAlive = sameChunk.getAt(nx, ny);
						}

						if (isNeighbourAlive) {
							++neighbours;
						}
					}
				}

				data[x][y] = decide(isAlive, neighbours);
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
