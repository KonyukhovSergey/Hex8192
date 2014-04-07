package ru.serjik.hex4096;

import ru.serjik.engine.BatchDrawer;
import ru.serjik.engine.Tile;
import android.util.Log;

// (3 - 2 * x) * x * x

public class Cell
{
	private static final float DELTA_MOVE = 0.1f;

	public int gem = 0;

	private CellState state = CellState.BASE;

	private int moveDirectionX;
	private int moveDirectionY;

	private float actionCompletion;
	private int actionCounter;

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
		if (state == CellState.BASE)
		{
			state = CellState.BUMP;

			moveDirectionX = dx;
			moveDirectionY = dy;

			actionCounter = 0;
		}
	}

	// _ _ _ _ _ _
	// _ 0 _ _ _ _
	// _ 1 _ _ _ _
	// 0 2 0 _ _ _
	// 1 3 1 _ _ _
	// 2 _ 2 0 _ _
	// 3 _ 3 1 _ _
	// _ _ _ 2 0 _
	// _ _ _ 3 1 _
	// _ _ _ _ 2 0
	// _ _ _ _ 3 1
	// _ _ _ _ _ 2
	// _ _ _ _ _ 3
	// _ _ _ _ _ _
	private boolean tickSolverBump()
	{
		switch (actionCounter)
		{
		case 0:
			actionCounter = 1;
			return true;

		case 1:
			if (gem > 0)
			{
				actionCounter = 0;
				actionCompletion = 0;
				updateForwardNeighbor();
				state = CellState.MOVE;
			}
			else
			{
				bumpNeighborhood();
				actionCounter = 2;
			}
			return true;

		case 2:
			actionCounter = 3;
			return true;

		case 3:
			state = CellState.BASE;
			return false;
		}

		return false;
	}

	private boolean tickSolverMove()
	{
		switch (actionCounter)
		{
		case 0:
			if (forwardNeighbor == null)
			{
				state = CellState.BASE;
				return false;
			}
			else
			{
				if (forwardNeighbor.gem == 0)
				{
					actionCounter = 1;
					actionCompletion += DELTA_MOVE;
					return true;
				}
				if (forwardNeighbor.gem != gem)
				{
					return false;
				}
				else
				{
					if (forwardNeighbor.state == CellState.BASE || forwardNeighbor.state == CellState.BUMP)
					{
						forwardNeighbor.state = CellState.RECV;
						forwardNeighbor.actionCompletion = 0;
						actionCounter = 1;
						actionCompletion += DELTA_MOVE;
						return true;
					}
				}

			}
			break;

		case 1:
			actionCompletion += DELTA_MOVE;

			if (actionCompletion >= 1)
			{
				if (forwardNeighbor.state == CellState.RECV)
				{
					forwardNeighbor.gem = gem + 1;
				}
				else
				{
					forwardNeighbor.gem = gem;
				}
				gem = 0;
				state = CellState.BASE;
				forwardNeighbor.bump(moveDirectionX, moveDirectionY);
				return true;
			}

			break;
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

	private void bumpNeighborhood()
	{
		for (Cell neighbor : neighborhood)
		{
			if (neighbor != null)
			{
				neighbor.bump(moveDirectionX, moveDirectionY);
			}
		}
	}

	private float sdx()
	{
		return screenPositionX + (forwardNeighbor.screenPositionX - screenPositionX) * actionCompletion
				* actionCompletion;
	}

	private float sdy()
	{
		return screenPositionY + (forwardNeighbor.screenPositionY - screenPositionY) * actionCompletion
				* actionCompletion;
	}

	public boolean tick(CellState stateFilter)
	{
		if (state != stateFilter)
		{
			return false;
		}

		switch (state)
		{
		case BASE:
			break;
		case BUMP:
			return tickSolverBump();
		case MOVE:
			return tickSolverMove();
		case PUT:
			break;
		case RECV:
			break;
		default:
			break;
		}

		return false;
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
		BASE, PUT, BUMP, MOVE, RECV
	}

	public void draw(BatchDrawer bd, Tile[] gems)
	{
		if (state != CellState.MOVE)
		{
			bd.drawCentered(gems[gem], screenPositionX, screenPositionY);
		}
		else
		{
			bd.drawCentered(gems[gem], sdx(), sdy());
		}

		// TODO Auto-generated method stub

	}

}
