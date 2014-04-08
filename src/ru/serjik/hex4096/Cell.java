package ru.serjik.hex4096;

import android.util.Log;
import ru.serjik.engine.BatchDrawer;
import ru.serjik.engine.Tile;

// (3 - 2 * x) * x * x

public class Cell
{
	private static final float DELTA_MOVE = 0.07f;

	public int gem = 0;

	public CellState state = CellState.BASE;

	private static int moveDirectionX;
	private static int moveDirectionY;

	private float actionCompletion;

	private int operationId = 0;

	public float screenPositionX;
	public float screenPositionY;

	public Cell[] neighborhood = new Cell[6];

	private Cell forwardNeighbor = null;

	public Cell(float x, float y)
	{
		screenPositionX = x;
		screenPositionY = y;
	}

	public static boolean bump(int dx, int dy)
	{
		if (Math.abs(dy) < Math.abs(dx) * 0.5f)
		{
			dy = 0;
			dx = dx < 0 ? -1 : 1;
		}
		else
		{
			dx = dy < 0 ? (dx < 0 ? 0 : 1) : (dx < 0 ? -1 : 0);
			dy = dy < 0 ? -1 : 1;
		}

		Log.v("bump", "dx = " + dx + " dy = " + dy);

		moveDirectionX = dx;
		moveDirectionY = dy;

		return dx != 0 || dy != 0;
	}

	public boolean tryToSetMove()
	{
		if (gem > 0 && state == CellState.BASE)
		{
			updateForwardNeighbor();

			if (forwardNeighbor != null)
			{
				actionCompletion = 0;

				if (forwardNeighbor.gem == 0)
				{
					state = CellState.MOVE;
					return true;
				}

				if (forwardNeighbor.gem == gem && forwardNeighbor.state == CellState.BASE)
				{
					state = CellState.MOVE;
					forwardNeighbor.state = CellState.RECV;
					return true;
				}

				if (forwardNeighbor.state == CellState.MOVE && forwardNeighbor.actionCompletion > 0.1f)
				{
					state = CellState.MOVE;
					return true;
				}
			}
		}
		return false;
	}

	private void updateForwardNeighbor()
	{
		int forwardIndex = neighborIndex(moveDirectionX, moveDirectionY);

		forwardNeighbor = null;

		if (forwardIndex >= 0)
		{
			forwardNeighbor = neighborhood[forwardIndex];
		}
	}

	private static float xx(float x)
	{
		// return (3 - 2 * x) * x * x;
		return x * x;
	}

	private float sdx()
	{
		return screenPositionX + (forwardNeighbor.screenPositionX - screenPositionX) * xx(actionCompletion);
	}

	private float sdy()
	{
		return screenPositionY + (forwardNeighbor.screenPositionY - screenPositionY) * xx(actionCompletion);
	}

	public final static int neighborIndex(int dx, int dy)
	{
		if (dy == 0)
		{
			if (dx > 0)
			{
				return 1;
			}
			if (dx < 0)
			{
				return 4;
			}
		}
		if (dy < 0)
		{
			if (dx == 0)
			{
				return 5;
			}
			if (dx > 0)
			{
				return 0;
			}
		}
		if (dy > 0)
		{
			if (dx == 0)
			{
				return 2;
			}
			if (dx < 0)
			{
				return 3;
			}

		}
		return -1;
	}

	public enum CellState
	{
		BASE, MOVE, RECV
	}

	private float fScale(float x)
	{
		return (-x * x + x) * 0.5f + 1;
	}

	public void draw(BatchDrawer bd, Tile[] gems)
	{
		if (gem > 0)
		{
			switch (state)
			{
			case BASE:
				bd.drawCentered(gems[gem], screenPositionX, screenPositionY);
				break;
			case MOVE:
				bd.drawScaledCentered(gems[gem], fScale(actionCompletion), sdx(), sdy());
				break;
			case RECV:
				bd.drawScaledCentered(gems[gem], 0.9f, screenPositionX, screenPositionY);
				break;
			}
		}
	}

	public boolean move()
	{
		if (state == CellState.MOVE)
		{
			actionCompletion += DELTA_MOVE;

			if (actionCompletion >= 1 - DELTA_MOVE)
			{
				if (forwardNeighbor.gem == gem)
				{
					forwardNeighbor.gem = gem + 1;
					forwardNeighbor.state = CellState.BASE;
				}
				else
				{
					forwardNeighbor.gem = gem;
				}
				gem = 0;
				state = CellState.BASE;

				return false;
			}

			return true;
		}
		return false;
	}
}
