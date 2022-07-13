package com.activeviam.experiments.gameoflife.biz.board;

import java.util.Arrays;

public record Board(int width, int height, boolean[][] cells) {


	public BoardChunk getChunk(int beginWidth, int endWidth) {
		boolean[][] data = Arrays.copyOfRange(cells, beginWidth, endWidth);
		return new BoardChunk(width, height, beginWidth, endWidth, data);
	}

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
