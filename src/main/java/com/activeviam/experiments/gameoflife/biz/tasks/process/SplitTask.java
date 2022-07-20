package com.activeviam.experiments.gameoflife.biz.tasks.process;

import com.activeviam.experiments.gameoflife.biz.board.Board;
import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.task.ATask;
import com.activeviam.experiments.gameoflife.task.Dependency;

/**
 * This task extracts a chunk that will be processed by consequent {@link ComputeTask computation tasks}.
 */
public class SplitTask extends ATask<BoardChunk> {

	@Dependency
	private ATask<Board> retrieve;
	private final int idx;
	private final int parallelism;

	/**
	 * Constructs a new task.
	 *
	 * @param retrieve    The task that returns the initial board
	 * @param idx         The chunk index
	 * @param parallelism The number of chunks
	 */
	public SplitTask(ATask<Board> retrieve, int idx, int parallelism) {
		this.retrieve = retrieve;
		this.idx = idx;
		this.parallelism = parallelism;
	}

	@Override
	protected BoardChunk compute() throws Exception {
		Board board = retrieve.call();

		int width = board.width();
		int stripeWidth = (width + parallelism - 1) / parallelism;
		int beginWidth = Math.min(width, idx * stripeWidth);
		int endWidth = Math.min(width, beginWidth + stripeWidth);

		return BoardChunk.ring(board.getChunk(beginWidth, endWidth), 2);
	}
}
