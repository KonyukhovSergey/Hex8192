package ru.serjik.hex4096;

public class Cell
{
	public static final float DELTA_MOVE = 0.05f;

	public int gem = 0;

	public CellState state = CellState.BASE;

	public int moveDirectionX;
	public int moveDirectionY;
	public float moveCompletion;

	public float screenPositionX;
	public float screenPositionY;

	public Cell[] neighborhood = new Cell[6];
	public Cell forwardNeighbor = null;

	public Cell(float x, float y)
	{
		screenPositionX = x;
		screenPositionY = y;
	}

	public void bump(int dx, int dy)
	{
		state = CellState.START;
		moveDirectionX = dx;
		moveDirectionY = dy;
		int forwardIndex = neighborIndex(moveDirectionX, moveDirectionY);
		forwardNeighbor = null;
		if (forwardIndex >= 0)
		{
			forwardNeighbor = neighborhood[forwardIndex];
		}

	}

	public boolean tickForStart()
	{
		boolean isMoving = false;

		if (state == CellState.START)
		{
			state = CellState.MOVE;
			moveCompletion = 0;
			isMoving = true;
		}

		return isMoving;
	}

	public float sdx()
	{
		return (forwardNeighbor.screenPositionX - screenPositionX) * moveCompletion;
	}

	public float sdy()
	{
		return (forwardNeighbor.screenPositionY - screenPositionY) * moveCompletion;
	}

	public boolean tickForMove()
	{
		boolean isMoving = false;

		if (state == CellState.MOVE)
		{
			if (gem > 0)
			{
				if (forwardNeighbor != null)
				{
					if (moveCompletion < DELTA_MOVE * 0.5f)
					{
						if (forwardNeighbor.gem == gem)
						{
							forwardNeighbor.gem = 0;
							gem++;
							moveCompletion += DELTA_MOVE;
							isMoving = true;
						}
						else if (forwardNeighbor.gem == 0)
						{
							moveCompletion += DELTA_MOVE;
							isMoving = true;
						}
					}
					else
					{
						isMoving = true;

						if (moveCompletion > 1 - DELTA_MOVE)
						{
							forwardNeighbor.gem = gem;
							gem = 0;
							state = CellState.START;
							moveCompletion = 0;
						}
						else
						{
							moveCompletion += DELTA_MOVE;
						}
					}
				}
			}
			//else
			{
				for (Cell neighbor : neighborhood)
				{
					if (neighbor != null)
					{
						if (neighbor.state == CellState.BASE)
						{
							neighbor.bump(moveDirectionX, moveDirectionY);
							isMoving = true;
						}
					}
				}
			}
		}

		return isMoving;
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
		BASE, START, MOVE
	}

}
