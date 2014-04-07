package ru.serjik.hex4096;

import ru.serjik.engine.BatchDrawer;
import ru.serjik.engine.Tile;

// (3 - 2 * x) * x * x

public class Cell
{
	private static final float DELTA_MOVE = 0.05f;

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

	public void bump(int dx, int dy)
	{
		moveDirectionX = dx;
		moveDirectionY = dy;
	}

	private boolean findMove(int operationId)
	{
		this.operationId = operationId;

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

				if (forwardNeighbor.gem == gem)
				{
					state = CellState.MOVE;
					forwardNeighbor.state = CellState.RECV;
					return true;
				}
			}
		}

		for (int i = 0; i < 6; i++)
		{
			if (neighborhood[i] != null)
			{
				if (neighborhood[i].operationId != operationId)
				{
					if (neighborhood[i].findMove(operationId))
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	public boolean tick()
	{
		if (findMove(operationId + 1))
		{
			return true;
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
		return (-x * x + x)*0.5f + 1;
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
