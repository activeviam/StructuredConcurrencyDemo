package com.activeviam.experiments.gameoflife.biz.tasks.process;

import com.activeviam.experiments.gameoflife.biz.board.Board;
import com.activeviam.experiments.gameoflife.biz.board.BoardChunk;
import com.activeviam.experiments.gameoflife.task.ATask;
import java.util.List;

public class SplitTask extends ATask<BoardChunk> {

	private ATask<Board> retrieve;
	private final int idx;
	private final int parallelism;

	public SplitTask(ATask<Board> retrieve, int idx, int parallelism) {
		this.retrieve = retrieve;
		this.idx = idx;
		this.parallelism = parallelism;
	}

	@Override
	protected List<ATask<?>> getDependencies() {
		return List.of(retrieve);
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

	@Override
	protected void dispose() {
		retrieve = null;
	}
}
