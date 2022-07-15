package com.activeviam.experiments.gameoflife.biz.board;

import com.activeviam.experiments.gameoflife.util.IntrusiveList;

public class BoardChunk extends IntrusiveList<BoardChunk> {

	private final int width;
	private final int height;
	private final int beginWidth;
	private final int endWidth;
	private final boolean[][] data;

	public BoardChunk(int width, int height, int beginWidth, int endWidth, boolean[][] data) {
		this.width = width;
		this.height = height;
		this.beginWidth = beginWidth;
		this.endWidth = endWidth;
		this.data = data;
	}

	public BoardChunk(int width, int height, int beginWidth, int endWidth) {
		this(width, height, beginWidth, endWidth, new boolean[endWidth - beginWidth][height]);
	}

	public static BoardChunk ring(int width, int height, int beginWidth, int endWidth, boolean[][] initialData,
			int ringSize) {
		if (ringSize <= 0) {
			throw new IllegalArgumentException("Ring size must be positive");
		}

		return ring(new BoardChunk(width, height, beginWidth, endWidth, initialData), ringSize);
	}

	public static BoardChunk ring(BoardChunk firstChunk, int ringSize) {
		if (ringSize <= 0) {
			throw new IllegalArgumentException("Ring size must be positive");
		}

		BoardChunk[] chunks = new BoardChunk[ringSize];
		chunks[0] = firstChunk;
		for (int i = 1; i < ringSize; ++i) {
			chunks[i] = new BoardChunk(firstChunk.width, firstChunk.height, firstChunk.beginWidth, firstChunk.endWidth);
			chunks[i - 1].next = chunks[i];
		}
		chunks[ringSize - 1].next = chunks[0];

		return chunks[0];
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int beginWidth() {
		return beginWidth;
	}

	public int endWidth() {
		return endWidth;
	}

	public boolean[][] data() {
		return data;
	}

	public int getStripeWidth() {
		return endWidth - beginWidth;
	}

	public boolean getAt(int i, int j) {
		if (i < 0 || i >= getStripeWidth()) {
			return false;
		}
		if (j < 0 || j >= height) {
			return false;
		}

		return data[i][j];
	}

	public BoardChunk nextChunk() {
		return getNext();
	}
}
