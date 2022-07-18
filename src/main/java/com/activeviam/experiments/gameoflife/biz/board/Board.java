package com.activeviam.experiments.gameoflife.biz.board;

import java.util.Arrays;

/**
 * Board class represents the Game Of Life field.
 *
 * @param width  Field width
 * @param height Field height
 * @param cells  boolean matrix; {@code cells[i][j]} is {@code true} if the cell is alive and {@code false}
 *               otherwise
 */
public record Board(int width, int height, boolean[][] cells) {


	/**
	 * Extract a vertical stripe {@code [beginWidth; endWidth)} of the board.
	 *
	 * @param beginWidth Begin of the range of columns to be extracted (inclusive)
	 * @param endWidth   End of the range of columns to be extracted (exclusive)
	 * @return A {@link BoardChunk} that holds selected columns
	 */
	public BoardChunk getChunk(int beginWidth, int endWidth) {
		boolean[][] data = Arrays.copyOfRange(cells, beginWidth, endWidth);
		return new BoardChunk(width, height, beginWidth, endWidth, data);
	}

	/**
	 * Get the state of a cell on position {@code (x, y)}. All the cells outside the board bounds are supposed to be
	 * dead.
	 *
	 * @param x Column index
	 * @param y Row index
	 * @return {@code true} if the cell is alive, {@code false} otherwise
	 */
	public boolean getAt(int x, int y) {
		if (x < 0 || x >= width) {
			return false;
		}
		if (y < 0 || y >= height) {
			return false;
		}
		return cells[x][y];
	}
}
