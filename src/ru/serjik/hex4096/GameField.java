package ru.serjik.hex4096;

import java.util.Arrays;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import ru.serjik.engine.BatchDrawer;
import ru.serjik.engine.ColorTools;
import ru.serjik.engine.EngineView;
import ru.serjik.engine.Texture;
import ru.serjik.engine.Tile;
import ru.serjik.engine.eng;
import ru.serjik.utils.BitmapUtils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class GameField extends EngineView
{
	private static final int FIELD_SIZE = 6;
	private static final float kw = (float) (Math.sqrt(3) / 2);
	private static final Random rnd = new Random(SystemClock.elapsedRealtime());

	private int size;
	private int[] values;
	private int[] tempValues;
	private int tempCount;

	public State state = State.WAIT;
	private int dx;
	private int dy;

	float moving_pshase = -1;

	private int gemIndex = 1;

	private Tile[] diamonds;
	private BatchDrawer bd;

	public GameField(Context context)
	{
		super(context);
		init(FIELD_SIZE);
		bd = new BatchDrawer(4096);
	}

	public void init(int size)
	{
		this.size = size;
		values = new int[(size * 2 - 1) * (size * 2 - 1)];
		tempValues = new int[values.length];
		fill(size - 1, size - 1, size - 1);
		state = State.INIT;
	}

	private boolean move(int dx, int dy)
	{
		int width = size * 2 - 1;

		int count = 0;
		tempCount = 0;

		for (int i = 0; i < values.length; i++)
		{
			int ri = i % width;
			int qi = i / width;

			int targetIndex = (ri + dx) + (qi + dy) * width;

			if (values[i] > 1)
			{
				if (values[targetIndex] == 0)
				{
					tempValues[count] = i;
					count++;
				}
				else if (values[targetIndex] == values[i])
				{
					values[targetIndex] *= 2;
					values[i] = 0;
					tempCount = 1;
					tempValues[0] = targetIndex;
					return true;
				}
			}
		}

		for (int i = 0; i < count; i++)
		{
			int index = tempValues[i];

			int ri = index % width;
			int qi = index / width;

			int targetIndex = (ri + dx) + (qi + dy) * width;

			values[targetIndex] = values[index];
			values[index] = 0;
			tempValues[i] = targetIndex;
		}

		Arrays.sort(tempValues, 0, count);
		tempCount = count;

		return count != 0;
	}

	public boolean touch(float x, float y)
	{
		if (state == State.WAIT)
		{
			dx = (int) (x - getWidth() / 2);
			dy = (int) (y - (getHeight() - getWidth() / 2));

			int dr = (int) Math.sqrt(dx * dx + dy * dy);

			if (dr < getWidth() / 8)
			{
				return false;
			}

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
			state = State.MOVE;
			return true;
		}
		return false;
	}

	// private void draw(Canvas canvas, String text, float x, float y)
	// {
	// Rect bounds = new Rect();
	// paint.getTextBounds(text, 0, text.length(), bounds);
	// canvas.drawText(text, x - bounds.centerX(), y - bounds.centerY(), paint);
	// }

	private void fill(int cr, int cq, int dist)
	{
		int width = size * 2 - 1;

		for (int i = 0; i < values.length; i++)
		{
			values[i] = HexUtils.distance(i % width, i / width, cr, cq) > dist ? 1 : 0;
		}
	}

	public int getFreePositions()
	{
		int count = 0;
		for (int i = 0; i < values.length; i++)
		{
			if (values[i] == 0)
			{
				tempValues[count] = i;
				count++;
			}
		}

		return count;
	}

	public boolean putGems(int count)
	{
		while (count > 0)
		{
			int freeCount = getFreePositions();

			if (freeCount < 1)
			{
				return false;
			}

			values[tempValues[rnd.nextInt(freeCount)]] = 1 << gemIndex;

			// gemIndex++; if(gemIndex>14) { gemIndex = 1; }

			count--;
		}

		return true;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event)
	{
		touch(event.getX(), event.getY());

		// TODO Auto-generated method stub
		return super.onTouchEvent(event);
	}

	public enum State
	{
		INIT, WAIT, MOVE, MOVING,
	}

	@Override
	public void onCreated(GL10 gl)
	{
		Texture atlas = new Texture(BitmapUtils.generate(512, 128, BitmapUtils.loadBitmapsFromAsset(eng.am), true));
		diamonds = Tile.split(atlas, atlas.width / 4, atlas.height / 4);

	}

	@Override
	public void onChanged(GL10 gl)
	{
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

		float cellRadius = kw * getWidth() / 10;

		float cx = getWidth() / 2;
		float cy = getHeight() - getWidth() / 2;

		float cw = cellRadius * kw;
		float ch = cellRadius * 0.75f;

		int width = size * 2 - 1;

		float hw = cw;
		float hh = cellRadius;

		int score = 0;

		drawBack(cellRadius, cx, cy, cw, ch, width);
		score = drawDiamonds(cellRadius, cx, cy, cw, ch, width, score);

		// paint.setColor(Color.WHITE);
		// draw(canvas, String.format("score = %d", score), getWidth() / 2,
		// getHeight() / 5);

		switch (state)
		{
		case INIT:
			putGems(3);
			state = State.WAIT;
			break;

		case MOVING:
			moving_pshase += 0.1f;

			if (moving_pshase >= 0 || tempCount == 0)
			{
				state = State.MOVE;
			}
			break;

		case MOVE:
		{
			if (move(dx, dy) == false)
			{
				state = State.WAIT;

				if (putGems(3) == false)
				{
					Toast.makeText(this.getContext(), "GAME OVER", Toast.LENGTH_LONG).show();
					init(FIELD_SIZE);
				}
			}
			else
			{
				state = State.MOVING;
				moving_pshase = -1;
			}
		}
			break;
		}

		bd.flush();
	}

	private int drawDiamonds(float cellRadius, float cx, float cy, float cw, float ch, int width, int score)
	{
		for (int i = 0; i < values.length; i++)
		{
			int r = (i % width) - (size - 1);
			int q = (i / width) - (size - 1);

			float x = r * cw * 2 + q * cw + cx;
			float y = q * ch * 2 + cy;

			if (values[i] > 1 && x >= cw && x < getWidth() - cw && y >= cellRadius + (getHeight() - getWidth())
					&& y < getHeight() - cellRadius)
			{
				int diamondIndex = (int) (Math.log(values[i]) / Math.log(2)) - 1;

				score += (values[i] >> 1) * (diamondIndex + 1);

				if (state == State.MOVING && Arrays.binarySearch(tempValues, 0, tempCount, i) >= 0)
				{
					float mx = moving_pshase * (dx * cw * 2 + dy * cw);
					float my = moving_pshase * (dy * ch * 2);
					bd.draw(diamonds[diamondIndex + 1], x - 64 + mx, y - 64 + my);
				}
				else
				{
					bd.draw(diamonds[diamondIndex + 1], x - 64, y - 64);
				}
			}
		}
		return score;
	}

	private void drawBack(float cellRadius, float cx, float cy, float cw, float ch, int width)
	{
		for (int i = 0; i < values.length; i++)
		{
			int r = (i % width) - (size - 1);
			int q = (i / width) - (size - 1);

			float x = r * cw * 2 + q * cw + cx;
			float y = q * ch * 2 + cy;

			if (values[i] != 1 && x >= cw && x < getWidth() - cw && y >= cellRadius + (getHeight() - getWidth())
					&& y < getHeight() - cellRadius)
			{
				bd.draw(diamonds[0], x - 64, y - 64);
			}
			else
			{
				values[i] = 1;
			}
		}
	}
}
