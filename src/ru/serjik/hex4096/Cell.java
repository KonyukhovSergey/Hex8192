package ru.serjik.hex4096;

import android.util.Log;
import ru.serjik.engine.BatchDrawer;
import ru.serjik.engine.Tile;

// (3 - 2 * x) * x * x

public class Cell
{
	private static final float ACCELERATION = 0.01f;

	public int gem = 0;

	public CellState state = CellState.BASE;

	private static int moveDirectionX;
	private static int moveDirectionY;

	private float actionCompletion = 0;
	private float velocity = 0;

	private int operationId = 0;

	public float screenPositionX;
	public float screenPositionY;

	public Cell[] neighborhood = new Cell[6];

	private static int indexForward = -1;
	private static int indexBackward = -1;
	private static int indexBackLeft = -1;
	private static int indexBackRight = -1;

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

		indexForward = neighborIndex(moveDirectionX, moveDirectionY);

		if (indexForward >= 0)
		{
			indexBackward = (indexForward + 6 + 3) % 6;
			indexBackLeft = (indexForward + 6 - 2) % 6;
			indexBackRight = (indexForward + 6 + 2) % 6;
		}

		return dx != 0 || dy != 0;
	}

	private Cell findStartCell()
	{
		Cell startCell = this;

		while (startCell.neighborhood[indexBackward] != null)
		{
			startCell = startCell.neighborhood[indexBackward];
		}
		return startCell;
	}

	public boolean tryToSetMoveByDirection()
	{
		Cell startCell = findStartCell();
		return startCell.callTryToSetMoveRequrcive(startCell.operationId + 1);
	}

	public boolean tryToSetMove()
	{
		if (gem > 0 && state == CellState.BASE)
		{
			Cell forwardNeighbor = neighborhood[indexForward];

			if (forwardNeighbor != null)
			{
				if (forwardNeighbor.gem == 0)
				{
					state = CellState.MOVE;
					return true;
				}

				if (forwardNeighbor.gem == gem && forwardNeighbor.state == CellState.BASE)
				{
					state = CellState.MOVE;
					forwardNeighbor.state = CellState.RECV;
					forwardNeighbor.velocity = 0;
					forwardNeighbor.actionCompletion = 0;
					velocity = 0;
					actionCompletion = 0;
					return true;
				}

				if (forwardNeighbor.state == CellState.MOVE && forwardNeighbor.actionCompletion > 0.05f)
				{
					if (velocity >= forwardNeighbor.velocity)
					{
						velocity = 0;
					}
					state = CellState.MOVE;
					return true;
				}
			}

			actionCompletion = 0;
			velocity = 0;
		}
		return false;
	}

	private boolean callTryToSetMoveRequrcive(int operationId)
	{
		if (this.operationId != operationId)
		{
			this.operationId = operationId;

			boolean result = false;

			if (tryToSetMove())
			{
				result = true;
			}

			if (indexForward >= 0)
			{
				if (neighborhood[indexBackLeft] != null)
				{
					if (neighborhood[indexBackLeft].callTryToSetMoveRequrcive(operationId))
					{
						result = true;
					}
				}

				if (neighborhood[indexBackRight] != null)
				{
					if (neighborhood[indexBackRight].callTryToSetMoveRequrcive(operationId))
					{
						result = true;
					}
				}

				if (neighborhood[indexForward] != null)
				{
					if (neighborhood[indexForward].callTryToSetMoveRequrcive(operationId))
					{
						result = true;
					}
				}
			}

			return result;
		}
		else
		{
			return false;
		}
	}

	private float screenPositionX()
	{
		return screenPositionX + (neighborhood[indexForward].screenPositionX - screenPositionX) * actionCompletion;
	}

	private float screenPositionY()
	{
		return screenPositionY + (neighborhood[indexForward].screenPositionY - screenPositionY) * actionCompletion;
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
				bd.drawCentered(gems[gem], screenPositionX(), screenPositionY());
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
			actionCompletion += velocity;
			velocity += ACCELERATION;

			if (actionCompletion >= 1 - velocity)
			{
				Cell forwardNeighbor = neighborhood[indexForward];

				if (forwardNeighbor.gem == gem)
				{
					forwardNeighbor.gem = gem + 1;
					forwardNeighbor.state = CellState.BASE;
					forwardNeighbor.velocity = 0;
					forwardNeighbor.actionCompletion = 0;
				}
				else
				{
					forwardNeighbor.gem = gem;
					forwardNeighbor.velocity = velocity;
					forwardNeighbor.actionCompletion = actionCompletion - 1;
				}
				gem = 0;
				state = CellState.BASE;
				velocity = 0;
				actionCompletion = 0;

				return false;
			}

			return true;
		}
		return false;
	}
}
