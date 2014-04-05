package ru.serjik.hex4096;

import ru.serjik.engine.eng;
import android.os.Bundle;
import android.app.Activity;

public class ActivityHex4096 extends Activity
{
	private GameField view;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		eng.am = getAssets();

		view = new GameField(this);

		setContentView(view);
	}

	@Override
	protected void onResume()
	{
		view.onResume();
		super.onResume();
	}

	@Override
	protected void onPause()
	{
		view.onPause();
		super.onPause();
	}

}
