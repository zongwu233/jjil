package jjil.app.facedetect;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import jjil.android.RgbImageAndroid;
import jjil.core.RgbImage;
import android.R.color;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.OrientationListener;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AbsoluteLayout;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;

public class FaceDetect extends Activity
	implements SurfaceHolder.Callback, View.OnClickListener
{
	final int RESTART_PREVIEW = 1;
	final int PROGRESS = 2;
	final int RESTART_PREVIEW2 = 3;
	
	private boolean boolCaptureOnFocus = false;
	private boolean boolFocusButtonPressed = false;
	private boolean boolFocused = false;
	private boolean boolFocusing = false;
	private boolean boolPreviewing = false;
	private android.hardware.Camera camera = null;
    private final FaceDetect me = this;
    private int nPreviewWidth, nPreviewHeight;
    private SurfaceView preview = null;
    private SurfaceHolder surfaceHolder = null;
    
    
    private final class AutoFocusCallback implements android.hardware.Camera.AutoFocusCallback {
        public void onAutoFocus(boolean focused, android.hardware.Camera camera) {
            boolFocusing = false;
            boolFocused = focused;
            if (focused) {
                if (boolCaptureOnFocus) {
                    android.hardware.Camera.Parameters parameters = camera.getParameters();
                    parameters.set("jpeg-quality", 85);
                    parameters.setPictureSize(768, 512);
                    camera.setParameters(parameters);
                    camera.takePicture(null, null, new JpegPictureCallback());
                    clearFocus();
                }
                boolCaptureOnFocus = false;
            }
        }
    };
    

    private final class JpegPictureCallback implements PictureCallback {
        public void onPictureTaken(byte [] jpegData, android.hardware.Camera camera) {
        	DetectFaces.setJpegData(jpegData);
        	startActivity(new Intent("jjil.app.facedetect.detectfaces"));
        	stopPreview();
        	//finish();
        }
    };
    
    private void done() {
//    		//selectButton.setBackgroundColor(color.background_light);
//    	}

    }

    private void autoFocus() {
        if (!this.boolFocusing) {
            if (this.camera != null) {
            	this.boolFocusing = true;
                this.boolFocused = false;
                this.camera.autoFocus(new AutoFocusCallback());
            }
        }

    }
    
    private void clearFocus() {
    	this.boolFocusButtonPressed = false;
    	this.boolFocused = false;
    	this.boolFocusing = false;
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

		setContentView(R.layout.main);
        preview = (SurfaceView) findViewById(R.id.Preview);
        SurfaceHolder s = preview.getHolder();
        s.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        s.addCallback(this);
    }
        
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_DPAD_CENTER:
                if (event.getRepeatCount() == 0) {
                    if (this.boolFocused || !this.boolPreviewing) {
                        clearFocus();
                    } else {
                        this.boolCaptureOnFocus = true;
                    }
                    if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER && !this.boolFocusButtonPressed) {
                        autoFocus();
                    }
                }
                return true;
            case KeyEvent.KEYCODE_FOCUS:
                this.boolFocusButtonPressed = true;
                if (event.getRepeatCount() == 0) {
                    if (this.boolPreviewing) {
                        autoFocus();
                    }
                }
                return true;
       }
        return super.onKeyDown(keyCode, event);
    }

    private void startPreview(int nWidth, int nHeight) {
    	this.nPreviewWidth = nWidth;
    	this.nPreviewHeight = nHeight;
    	if (this.boolPreviewing) {
    		return;
    	}
    	if (this.camera == null) {
    		this.camera = android.hardware.Camera.open();
    	}
    	if (this.camera != null && this.surfaceHolder != null) {
    		Camera.Parameters parm = this.camera.getParameters();
    		parm.setPreviewSize(nWidth, nHeight);
    		this.camera.setParameters(parm);
    		this.camera.setPreviewDisplay(this.surfaceHolder);
    		this.camera.startPreview();
    		this.boolPreviewing = true;
    	}
    }
    
    private void stopPreview() {
    	if (this.camera != null) {
    		this.camera.stopPreview();
    		this.camera.release();
    		this.camera = null;
    		this.boolPreviewing = false;
    	}
    }
 
    public void onClick(View v) {
		// TODO Auto-generated method stub
		
	}

	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		if (holder.isCreating()) {
			startPreview(width, height);
		}
	}

	public void surfaceCreated(SurfaceHolder holder) {
		this.surfaceHolder = holder;
	}

	public void surfaceDestroyed(SurfaceHolder holder) {
		stopPreview();
		this.surfaceHolder = null;
	}

}