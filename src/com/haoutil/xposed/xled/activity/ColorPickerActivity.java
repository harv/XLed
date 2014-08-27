package com.haoutil.xposed.xled.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import com.haoutil.xposed.xled.R;
import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.ColorPicker.OnColorChangedListener;
import com.larswerkman.holocolorpicker.OpacityBar;
import com.larswerkman.holocolorpicker.SVBar;
import com.larswerkman.holocolorpicker.SaturationBar;

public class ColorPickerActivity extends Activity implements OnTouchListener, OnColorChangedListener, OnClickListener, TextWatcher {
	private InputMethodManager imm;
	
	private ColorPicker picker;
    private SaturationBar saturationBar;
    private SVBar svBar;
	private OpacityBar opacityBar;
	private EditText tv_newColor;
	private Button bt_commit;

    private int requestCode;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.color_picker);

		imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		
		picker = (ColorPicker) findViewById(R.id.picker);
        saturationBar = (SaturationBar) findViewById(R.id.saturationBar);
        svBar = (SVBar) findViewById(R.id.svbar);
		opacityBar = (OpacityBar) findViewById(R.id.opacitybar);
		tv_newColor = (EditText) findViewById(R.id.tv_newColor);
		bt_commit = (Button) findViewById(R.id.bt_commit);
		
		picker.setOnTouchListener(this);
		picker.setOnColorChangedListener(this);
        picker.addSaturationBar(saturationBar);
        picker.addSVBar(svBar);
		picker.addOpacityBar(opacityBar);

		int originColor = getIntent().getIntExtra("originColor", Color.TRANSPARENT);
		picker.setOldCenterColor(originColor);

        requestCode = getIntent().getIntExtra("requestCode", 0);
		
		if (originColor == Color.TRANSPARENT) {
			originColor = Color.BLUE;
		}
		picker.setColor(originColor);

        saturationBar.setOnTouchListener(this);
        svBar.setOnTouchListener(this);
        opacityBar.setOnTouchListener(this);
		
		tv_newColor.setText(String.format("#%08X", (0xFFFFFFFF & originColor)));
		tv_newColor.addTextChangedListener(this);

		bt_commit.setOnClickListener(this);
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			hideSoftKeyBoard();
		}
		
		return super.onTouchEvent(event);
	}

	// OnTouchListener implementation
	@Override
	public boolean onTouch(View c, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			hideSoftKeyBoard();
		}
		
		return false;
	}

	// OnColorChangedListener implementation
	@Override
	public void onColorChanged(int color) {
		if (!tv_newColor.isFocused()) {
			tv_newColor.setText(String.format("#%08X", (0xFFFFFFFF & color)));
		}
	}

	// OnClickListener implementation
	@Override
	public void onClick(View v) {
		Intent intent = new Intent();
		intent.putExtra("color", tv_newColor.getText().toString());
		intent.putExtra("requestCode", requestCode);
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
		if (tv_newColor.isFocused()) {
			try {
				picker.setColor(Color.parseColor(tv_newColor.getText().toString()));
			} catch (Exception e) {}
		}
	}
	
	private void hideSoftKeyBoard() {
		if (tv_newColor.isFocused()) {
			tv_newColor.clearFocus();
			imm.hideSoftInputFromWindow(tv_newColor.getWindowToken(), 0);
		}
	}
}
