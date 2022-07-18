package com.activeviam.experiments.gameoflife.biz.board;

import com.activeviam.experiments.gameoflife.util.IntrusiveList;

/**
 * This class represents a stripe of a {@link Board}. Several chunks may form a ring (used for memory
 * optimization).
 */
public class BoardChunk implements IntrusiveList<BoardChunk> {

	private final int width;
	private final int height;
	private final int beginWidth;
	private final int endWidth;
	private final boolean[][] data;

	/**
	 * Construct a new chunk with existing data.
	 *
	 * @param width      Field width
	 * @param height     Field height
	 * @param beginWidth First column included in this chunk
	 * @param endWidth   The column after the last one included in this chunk
	 * @param data       boolean matrix that represents the chunk
	 */
	public BoardChunk(int width, int height, int beginWidth, int endWidth, boolean[][] data) {
		this.width = width;
		this.height = height;
		this.beginWidth = beginWidth;
		this.endWidth = endWidth;
		this.data = data;
	}

	/**
	 * Construct new chunk and allocate data.
	 *
	 * @param width      Field width
	 * @param height     Field height
	 * @param beginWidth First column included in this chunk
	 * @param endWidth   The column after the last one included in this chunk
	 */
	public BoardChunk(int width, int height, int beginWidth, int endWidth) {
		this(width, height, beginWidth, endWidth, new boolean[endWidth - beginWidth][height]);
	}

	/**
	 * Constructs a ring (cycled single-linked list) of chunks. All the chunks have the same dimensions. If the chunk
	 * is already a part of a ring, new chunks are inserted between the {@code firstChunk} and its successor.
	 *
	 * @param firstChunk The initial chunk
	 * @param ringSize   Number of chunks in the ring.
	 * @return The initial chunk which is embedded into the created ring
	 */
	public static BoardChunk ring(BoardChunk firstChunk, int ringSize) {
		if (ringSize <= 0) {
			throw new IllegalArgumentException("Ring size must be positive");
		}

		BoardChunk[] chunks = new BoardChunk[ringSize];
		BoardChunk afterLast = firstChunk.next == null ? firstChunk : firstChunk.next;
		chunks[0] = firstChunk;
		for (int i = 1; i < ringSize; ++i) {
			chunks[i] = new BoardChunk(firstChunk.width, firstChunk.height, firstChunk.beginWidth, firstChunk.endWidth);
			chunks[i - 1].next = chunks[i];
		}
		chunks[ringSize - 1].next = afterLast;

		return chunks[0];
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getBeginWidth() {
		return beginWidth;
	}

	public int getEndWidth() {
		return endWidth;
	}

	public boolean[][] getData() {
		return data;
	}

	public int getStripeWidth() {
		return endWidth - beginWidth;
	}

	/**
	 * Get the state of a cell on position {@code (x, y)} relative to the stripe beginning. All the cells outside the
	 * stripe bounds are supposed to be dead.
	 *
	 * @param x Column index
	 * @param y Row index
	 * @return {@code true} if the cell is alive, {@code false} otherwise
	 */
	public boolean getAt(int x, int y) {
		if (x < 0 || x >= getStripeWidth()) {
			return false;
		}
		if (y < 0 || y >= height) {
			return false;
		}

		return data[x][y];
	}

	/**
	 * Get next chunk in the ring.
	 *
	 * @return The next chunk
	 * @throws IllegalStateException if the chunk is not a part of a ring.
	 */
	public BoardChunk nextChunk() {
		if (getNext() == null) {
			throw new IllegalStateException("Not in a ring");
		}
		return getNext();
	}

	// intrusive list
	private BoardChunk next;
	@Override
	public BoardChunk getNext() {
		return next;
	}
}
