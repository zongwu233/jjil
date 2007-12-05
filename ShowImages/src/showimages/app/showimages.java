/* 
 * Copyright (C) 2007 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package showimages.app;

import jjil.android.CameraDeviceFile;
import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.hardware.CameraDevice;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;


// ----------------------------------------------------------------------

public class showimages extends Activity
{    
    @Override
        protected void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);

        // Hide the window title.
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        // Make sure to create a TRANSLUCENT window. This is recquired
        // for SurfaceView to work. Eventually this'll be done by
        // the system automatically.
        getWindow().setFormat(PixelFormat.TRANSLUCENT);

        // Create our Preview view and set it as the content of our
        // Activity
        mPreview = new Preview(this);
        setContentView(mPreview);
    }

    @Override
        protected boolean isFullscreenOpaque() {
        // Our main window is set to translucent, but we know that we will
        // fill it with opaque data. Tell the system that so it can perform
        // some important optimizations.
        return true;
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
        mHolder.setCallback(this);
        mHasSurface = false;

        // In this example, we hardcode the size of the preview. In a real
        // application this should be more dynamic. This guarantees that
        // the uderlying surface will never change size.
        mHolder.setFixedSize(320, 240);
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

    public boolean surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, start our main acquisition thread.
        mHasSurface = true;
        if (mPreviewThread != null) {
            mPreviewThread.start();
        }
        // Tell the system that we filled the surface in this call.
        // This is a lie to preven the system to fill the surface for us
        // automatically. 
        // THIS IS REQUIRED because other wise we'll access the Surface object
        // from 2 different threads which is not allowd (And will crash
        // currently).
        return true;
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        // Surface will be destroyed when we return. Stop the preview.
        mHasSurface = false;
        pause();
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
        // Surface size or format has changed. This should not happen in this
        // example.
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
            // We first open the CameraDevice and configure it.
        	int [] files = {R.raw.one, R.raw.two, R.raw.three, R.raw.four};
            CameraDeviceFile camera = 
            	CameraDeviceFile.open(getResources(), files);
            if (camera != null) {
                CameraDevice.CaptureParams param = new CameraDevice.CaptureParams();
                    param.type = 1; // preview
                    param.srcWidth      = 1280;
                    param.srcHeight     = 960;
                    param.leftPixel     = 0;
                    param.topPixel      = 0;
                    param.outputWidth   = 320;
                    param.outputHeight  = 240;
                    param.dataFormat    = 2; // RGB_565
                camera.setCaptureParams(param);
            }

            // This is our main acquisition thread's loop, we go until
            // asked to quit.
            SurfaceHolder holder = mHolder;
            while (!mDone) {
                // Lock the surface, this returns a Canvas that can
                // be used to render into.
                Canvas canvas = holder.lockCanvas();

                // Capture directly into the Surface
                if (camera != null) {
                    camera.capture(canvas);
                }

                // And finally unlock and post the surface.
                holder.unlockCanvasAndPost(canvas);
            }

            // Make sure to release the CameraDevice
            if (camera != null)
                camera.close();
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

            SurfaceHolder   mHolder;
    private PreviewThread   mPreviewThread;
    private boolean         mHasSurface;
}