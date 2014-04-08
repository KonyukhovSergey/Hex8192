package ru.serjik.hex4096;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import ru.serjik.engine.BatchDrawer;
import ru.serjik.engine.EngineView;
import ru.serjik.engine.Texture;
import ru.serjik.engine.Tile;
import ru.serjik.engine.eng;
import ru.serjik.hex4096.Cell.CellState;
import ru.serjik.utils.BitmapUtils;

public class HexField extends EngineView
{
	private static final int GEMS_PER_MOVE = 3;
	private static final Random rnd = new Random(SystemClock.elapsedRealtime());
	private static final int FIELD_SIZE = 6;

	private Cell[] cells;
	private Cell[] activeCells;
	private Cell centerCell;

	private int[] freeIndxes;

	private Tile[] gems;
	private BatchDrawer bd;

	private float centerFieldX;
	private float centerFieldY;

	private int fieldCellsWidth;
	private float cellRadius;

	private float touchDownX;
	private float touchDownY;
	private boolean isMoving = false;

	public HexField(Context context)
	{
		super(context);
		bd = new BatchDrawer(4096 * 4);
	}

	@Override
	public void onCreated(GL10 gl)
	{
		Texture atlas = new Texture(BitmapUtils.generate(512, 128, BitmapUtils.loadBitmapsFromAsset(eng.am), true));
		gems = Tile.split(atlas, atlas.width / 4, atlas.height / 4);
	}

	@Override
	public void onChanged(GL10 gl)
	{
		if (cells == null)
		{
			createCells();
			putGems(GEMS_PER_MOVE);
		}
	}

	@Override
	public void onDrawFrame(GL10 gl)
	{
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDisable(GL10.GL_DITHER);
		gl.glDisable(GL10.GL_FOG);
		gl.glDisable(GL10.GL_LIGHTING);
		gl.glDisable(GL10.GL_DEPTH_TEST);

		Texture.enable();
		Texture.filter(GL10.GL_LINEAR, GL10.GL_LINEAR);
		gl.glShadeModel(GL10.GL_SMOOTH);

		gl.glClearColor(0, 0, 0, 0);
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT);

		drawCells();

		drawGems();

		bd.flush();

		// SystemClock.sleep(250);

		tick();
	}

	private boolean doMove()
	{
		boolean result = false;

		for (Cell cell : cells)
		{
			if (cell != null)
			{
				if (cell.move())
				{
					result = true;
				}
			}
		}
		return result;
	}

	private void onMoveEnd()
	{
		for (Cell cell : activeCells)
		{
			cell.state = CellState.BASE;
		}

	}

	private boolean tryToSetMove()
	{
		return centerCell.tryToSetMoveByDirection();
//		boolean result = false;
//
//		for (Cell cell : activeCells)
//		{
//			if (cell.tryToSetMove())
//			{
//				result = true;
//			}
//		}
//		return result;
	}

	private void tick()
	{
		if (isMoving)
		{
			boolean hasAction = false;

			if (tryToSetMove())
			{
				hasAction = true;
			}

			if (doMove())
			{
				hasAction = true;
			}

			if (tryToSetMove())
			{
				hasAction = true;
			}

			if (hasAction)
			{
				return;
			}

			Log.v("move", "onMoveEnd!!!");
			putGems(GEMS_PER_MOVE);
			isMoving = false;
			onMoveEnd();
		}

	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		if (isMoving == false)
		{
			int action = event.getAction();
			float x = event.getX();
			float y = event.getY();

			switch (action)
			{
			case MotionEvent.ACTION_DOWN:
				touchDownX = x;
				touchDownY = y;
				return true;

			case MotionEvent.ACTION_MOVE:
			{
				int dx = (int) (x - touchDownX);
				int dy = (int) (y - touchDownY);

				if (dx * dx + dy * dy > 4 * cellRadius * cellRadius)
				{
					isMoving = Cell.bump(dx, dy);
				}
			}
				break;
			}
		}
		else
		{
			touchDownX = event.getX();
			touchDownY = event.getY();
		}
		return true;
	}

	public boolean putGems(int count)
	{
		while (count > 0)
		{
			int freeCount = fillFreeIndexes();

			if (freeCount < 1)
			{
				return false;
			}

			cells[freeIndxes[rnd.nextInt(freeCount)]].gem = 1;

			count--;
		}

		return true;
	}

	private void drawCells()
	{
		for (Cell cell : activeCells)
		{
			bd.drawCentered(gems[0], cell.screenPositionX, cell.screenPositionY);
		}
	}

	private void drawGems()
	{
		for (Cell cell : activeCells)
		{
			cell.draw(bd, gems);
		}
	}

	private int fillFreeIndexes()
	{
		int count = 0;

		for (int i = 0; i < cells.length; i++)
		{
			if (cells[i] != null)
			{
				if (cells[i].gem == 0)
				{
					freeIndxes[count++] = i;
				}
			}

		}
		return count;
	}

	private void createCells()
	{
		cells = new Cell[(FIELD_SIZE * 2 - 1) * (FIELD_SIZE * 2 - 1)];
		freeIndxes = new int[cells.length];

		fillWallAndCell(FIELD_SIZE - 1, FIELD_SIZE - 1, FIELD_SIZE - 1);

		setNieghborhood();
	}

	private int getIndex(float x, float y)
	{
		for (int i = 0; i < cells.length; i++)
		{
			if (cells[i] != null)
			{
				float dx = x - cells[i].screenPositionX;
				float dy = y - cells[i].screenPositionY;
				if (dx * dx + dy * dy < cellRadius * cellRadius)
				{
					return i;
				}
			}
		}
		return -1;
	}

	private void fillWallAndCell(int cr, int cq, int dist)
	{
		fieldCellsWidth = FIELD_SIZE * 2 - 1;

		float kw = (float) (Math.sqrt(3) / 2);

		cellRadius = kw * width() / 10;

		centerFieldX = width() / 2;
		centerFieldY = height() - width() / 2;

		float cw = cellRadius * kw;
		float ch = cellRadius * 0.75f;

		List<Cell> activeCellList = new ArrayList<Cell>();

		for (int i = 0; i < cells.length; i++)
		{
			int ix = i % fieldCellsWidth;
			int iy = i / fieldCellsWidth;

			int hexDistance = HexUtils.distance(ix, iy, cr, cq);

			if (hexDistance <= dist)
			{
				int r = ix - (FIELD_SIZE - 1);
				int q = iy - (FIELD_SIZE - 1);

				float x = r * cw * 2 + q * cw + centerFieldX;
				float y = q * ch * 2 + centerFieldY;

				if (x >= cellRadius && x < width() - cellRadius && y >= cellRadius + (height() - width())
						&& y < height() - cellRadius)
				{
					cells[i] = new Cell(x, y);
					activeCellList.add(cells[i]);
					if (hexDistance == 0)
					{
						centerCell = cells[i];
					}
				}
			}
		}

		activeCells = new Cell[activeCellList.size()];
		activeCellList.toArray(activeCells);
	}

	private void setNieghborhood()
	{
		for (int i = 0; i < cells.length; i++)
		{
			if (cells[i] != null)
			{
				int ri = i % fieldCellsWidth;
				int qi = i / fieldCellsWidth;

				Cell cell = cells[i];

				cell.neighborhood[0] = cells[(ri + 1) + (qi - 1) * fieldCellsWidth];
				cell.neighborhood[1] = cells[(ri + 1) + (qi + 0) * fieldCellsWidth];
				cell.neighborhood[2] = cells[(ri + 0) + (qi + 1) * fieldCellsWidth];
				cell.neighborhood[3] = cells[(ri - 1) + (qi + 1) * fieldCellsWidth];
				cell.neighborhood[4] = cells[(ri - 1) + (qi + 0) * fieldCellsWidth];
				cell.neighborhood[5] = cells[(ri + 0) + (qi - 1) * fieldCellsWidth];
			}
		}
	}
}
