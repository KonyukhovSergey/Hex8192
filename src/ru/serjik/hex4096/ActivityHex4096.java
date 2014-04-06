package ru.serjik.hex4096;

import ru.serjik.engine.eng;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.app.Activity;

public class ActivityHex4096 extends Activity
{
	private GLSurfaceView view;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		eng.am = getAssets();

		view = new HexField(this);

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
