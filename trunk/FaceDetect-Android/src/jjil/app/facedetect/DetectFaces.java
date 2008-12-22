package jjil.app.facedetect;

import java.util.Enumeration;

import jjil.android.RgbImageAndroid;
import jjil.core.RgbImage;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.WindowManager;

public class DetectFaces extends Activity {
	private DetectHaarParam mDhp = new DetectHaarParam();
	private static byte[] mJpegData = null;
	private FaceView mFaceView;
	RgbImage mRgbCurrent = null;
	
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.faces);
        mFaceView = (FaceView) findViewById(R.id.detectedFaces);
		if (mJpegData != null) {
        	runOnUiThread(new Runnable() {
        		public void run() {
        			mFaceView.resetFaces();
        			mFaceView.resetShowX();
		        	BitmapFactory.Options opt = new BitmapFactory.Options();
		        	opt.inSampleSize = 4;
		        	final Bitmap bmp = BitmapFactory.decodeByteArray(mJpegData, 0, mJpegData.length, opt);
					mFaceView.setImageBitmap(bmp);
        		}
        	});
        	
			try {
				Thread t = new Thread() {
					public void run() {
						for (int nSample = 16; nSample >= 8 && !mFaceView.hasFaces(); nSample /= 2) {
				        	BitmapFactory.Options opt = new BitmapFactory.Options();
				        	opt.inSampleSize = nSample;
				        	final Bitmap bmp = BitmapFactory.decodeByteArray(mJpegData, 0, mJpegData.length, opt);
							mRgbCurrent = RgbImageAndroid.toRgbImage(bmp);
				        	mDhp.push(mRgbCurrent);
		        	    	for (Enumeration<jjil.core.Rect> e = mDhp.getResult(); e.hasMoreElements();) {
			            		jjil.core.Rect r = e.nextElement();
			            		int nTop = (r.getTop() * mFaceView.getHeight()) / mRgbCurrent.getHeight();
			            		int nBottom = (r.getBottom() * mFaceView.getHeight()) / mRgbCurrent.getHeight();
			            		int nLeft = (r.getLeft() * mFaceView.getWidth()) / mRgbCurrent.getWidth();
			            		int nRight = (r.getRight() * mFaceView.getWidth()) / mRgbCurrent.getWidth();
			            		final Rect rDisplay = new Rect(
			            						nLeft, 
			            						nTop, 
			            						nRight, 
			            						nBottom);
			            		runOnUiThread(new Runnable() {
				            			public void run() {
						            		mFaceView.addRect(rDisplay);
				            			}
			            			}
			            		);
		        	    	};
		        	    	mRgbCurrent = null;
						};
 						runOnUiThread(new Runnable() {
		        			public void run() {
		        				mFaceView.setShowX();
		        				mFaceView.invalidate();
							}
						});
					}
				};
				t.start();
			} catch (Exception ex) {
				
			}
		}
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_BACK:
                startActivity(new Intent("jjil.app.facedetect.preview"));
                finish();
                return true;
       }
       return super.onKeyDown(keyCode, event);
    }

    public static void setJpegData(byte[] jpegData) {
		mJpegData = jpegData;
	}
}
