package readbarj.app;

import jjil.algorithm.RgbSelect2Gray;
import jjil.android.CameraDeviceFile;
import jjil.android.RgbImageAndroid;
import jjil.core.PipelineStage;
import jjil.core.RgbImage;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.hardware.CameraDevice;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import barcode.DetectBarcode;
import barcode.ReadBarcode;

//----------------------------------------------------------------------

public class readbarj extends Activity
{    
    @Override
        protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        PipelineStage g8t = new RgbSelect2Gray(RgbSelect2Gray.GREEN);
        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Create our Preview view and set it as the content of our
        // Activity
        mPreview = new Preview(this);
        setContentView(mPreview);
    }

    @Override
        protected void onResume()
    {
        // Because the CameraDevice object is not a shared resource,
        // it's very important to release it when the activity is paused.
        super.onResume();
        mPreview.resume();
    }

    @Override
        protected void onPause()
    {
        // Start Preview again when we resume.
        super.onPause();
        mPreview.pause();
    }

   private Preview mPreview;
}

// ----------------------------------------------------------------------

class Preview extends SurfaceView implements SurfaceHolder.Callback
{
    Preview(Context context) {
        super(context);

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHasSurface = false;

        // In this example, we hardcode the size of the preview. In a real
        // application this should be more dynamic. This guarantees that
        // the underlying surface will never change size.
        mHolder.setFixedSize(320, 240);
        
        // get key events
        setFocusable(true);
    }

    public void resume() {
        // We do the actual acquisition in a separate thread. Create it now.
        if (mPreviewThread == null) {
            mPreviewThread = new PreviewThread();
            // If we already have a surface, just start the thread now too.
            if (mHasSurface == true) {
                mPreviewThread.start();
            }
        }
    }

    public void pause() {
        // Stop Preview.
        if (mPreviewThread != null) {
            mPreviewThread.requestExitAndWait();
            mPreviewThread = null;
        }
    }
    
    public void surfaceCreated(SurfaceHolder holder) {
        // We first open the CameraDevice and configure it.
    	int [] files = {R.raw.barcode};
       	this.mCamera = CameraDeviceFile.open(getResources(), files);
        if (mCamera != null) {
            CameraDevice.CaptureParams param = new CameraDevice.CaptureParams();
                param.type = 1; // preview
                param.srcWidth      = 1280;
                param.srcHeight     = 960;
                param.leftPixel     = 0;
                param.topPixel      = 0;
                param.outputWidth   = 1280;
                param.outputHeight  = 960;
                param.dataFormat    = 2; // RGB_565
            mCamera.setCaptureParams(param);
        }
        
        // The Surface has been created, start our main acquisition thread.
        mHasSurface = true;
        if (mPreviewThread != null) {
            mPreviewThread.start();
        }
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return. Stop the preview.
        mHasSurface = false;
        pause();
        
        // Make sure to release the CameraDevice
        if (mCamera != null) {
            mCamera.close();
            mCamera = null;
        }
   }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Surface size or format has changed. This should not happen in this
        // example.
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent msg) {
    	boolean handled = false;
    	
    	if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
    		this.mPreviewThread.interrupt();
    		Bitmap bmp = Bitmap.createBitmap(1280, 1024, false);
    		Canvas c = new Canvas(bmp);
    		this.mCamera.capture(c);
    		RgbImage inimg = RgbImageAndroid.toRgbImage(bmp);
    		DetectBarcode db = new DetectBarcode(20000);
    		if (!db.Push(inimg)) {
    			/**
    			 * Couldn't find the barcode. Tell the user.
    			 */
    			System.out.println("Couldn't find barcode"); //$NON-NLS-1$
    		} else {
    			ReadBarcode rb = new ReadBarcode();
    			rb.setRect(db.getRect());
    			rb.Push(inimg);
    			if (!rb.getSuccessful()) {
    				/**
    				 * Couldn't read the barcode.
    				 */
    				System.out.println("Couldn't read barcode"); //$NON-NLS-1$
    			} else {
    				/**
    				 * Read the barcode. Tell the user.
    				 */
    				System.out.println("Read barcode: " + rb.getCode()); //$NON-NLS-1$
    			}
    		}
    	}
    	return handled;
    }

    // ----------------------------------------------------------------------

    class PreviewThread extends Thread
    {
        PreviewThread() {
            super();
            mDone = false;
        }

        @Override
                public void run() {

            // This is our main acquisition thread's loop, we go until
            // asked to quit.
            SurfaceHolder holder = mHolder;
            while (!mDone) {
                // Lock the surface, this returns a Canvas that can
                // be used to render into.
                Canvas canvas = holder.lockCanvas();

                // Capture directly into the Surface
                if (mCamera != null) {
                    //mCamera.capture(canvas);
                }

                // And finally unlock and post the surface.
                holder.unlockCanvasAndPost(canvas);
                
                if (interrupted()) {
                	yield();
                }
            }

        }

        public void requestExitAndWait() {
            // don't call this from PreviewThread thread or it a guaranteed
            // deadlock!
            mDone = true;
            try {
                join();
            } catch (InterruptedException ex) { }
        }
        private boolean mDone;
    }

    private CameraDeviceFile mCamera;
    	    SurfaceHolder    mHolder;
    private PreviewThread    mPreviewThread;
    private boolean          mHasSurface;
}