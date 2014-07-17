package com.haoutil.xposed.xled.activity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.NumberKeyListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;

import com.haoutil.xposed.xled.R;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.ColorPicker.OnColorChangedListener;
import com.larswerkman.holocolorpicker.OpacityBar;

public class ColorPickerActivity extends Activity implements OnTouchListener, OnColorChangedListener, OnClickListener, TextWatcher {
	private ColorPicker picker;
	private OpacityBar opacityBar;
	private EditText tv_newColor;
	private Button bt_commit;
	
	boolean colorSelecting = false;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.color_picker);

		picker = (ColorPicker) findViewById(R.id.picker);
		opacityBar = (OpacityBar) findViewById(R.id.opacitybar);
		tv_newColor = (EditText) findViewById(R.id.tv_newColor);
		bt_commit = (Button) findViewById(R.id.bt_commit);

		picker.setOnTouchListener(this);
		picker.setOnColorChangedListener(this);
		picker.addOpacityBar(opacityBar);

		int originColor = getIntent().getIntExtra("originColor", Color.TRANSPARENT);
		picker.setOldCenterColor(originColor);
		
		if (originColor == Color.TRANSPARENT) {
			originColor = Color.BLUE;
		}
		picker.setColor(originColor);
		
		opacityBar.setOnTouchListener(this);
		
		tv_newColor.setText(String.format("#%08X", (0xFFFFFFFF & originColor)));
		tv_newColor.setKeyListener(new ColorKeyListener());
		tv_newColor.addTextChangedListener(this);

		bt_commit.setOnClickListener(this);
	}

	// OnTouchListener implementation
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch (event.getAction()) {
			case MotionEvent.ACTION_DOWN :
				colorSelecting = true;
				break;
			case MotionEvent.ACTION_UP :
				colorSelecting = false;
				break;
		}
	
		return false;
	}

	// OnColorChangedListener implementation
	@Override
	public void onColorChanged(int color) {
		if (colorSelecting) {
			tv_newColor.setText(String.format("#%08X", (0xFFFFFFFF & color)));
		}
	}

	// OnClickListener implementation
	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.putExtra("color", tv_newColor.getText().toString());
		ColorPickerActivity.this.setResult(RESULT_OK, intent);
		
		ColorPickerActivity.this.finish();
	}

	// TextWatcher implementation
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}

	@Override
	public void afterTextChanged(Editable s) {
		if (!colorSelecting) {
			try {
				picker.setColor(Color.parseColor(tv_newColor.getText().toString()));
			} catch (Exception e) {}
		}
	}
}

class ColorKeyListener extends NumberKeyListener {
	@Override
	public int getInputType() {
		return InputType.TYPE_CLASS_TEXT;
	}
	
	@Override
	protected char[] getAcceptedChars() {
		return new char[] {'#', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f', 'A', 'B', 'C', 'D', 'E', 'F'};
	}
}
