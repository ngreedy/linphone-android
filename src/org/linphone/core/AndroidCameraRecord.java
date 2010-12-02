/*
AndroidCameraRecordImpl.java
Copyright (C) 2010  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.linphone.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.hardware.Camera;
import android.hardware.Camera.ErrorCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public abstract class AndroidCameraRecord {

	protected Camera camera;
	private RecorderParams params;

	private PreviewCallback storedPreviewCallback;
	private boolean previewStarted;
	protected int orientationCode;
	protected static final String tag="Linphone";
	private List <Size> supportedVideoSizes;
	
	public AndroidCameraRecord(RecorderParams parameters) {
		this.params = parameters;
		setRotation(parameters.rotation);
	}
	
	protected List<Size> getSupportedPreviewSizes(Camera.Parameters parameters) {
		return Collections.emptyList();
	}
	
	public void startPreview() { // FIXME throws exception?
		if (previewStarted) {
			Log.w(tag, "Already started");
			throw new RuntimeException("Video recorder already started");
			// return
		}
		
		if (params.surfaceView.getVisibility() != SurfaceView.VISIBLE) {
			// Illegal state
			Log.e(tag, "Illegal state: video capture surface view is not visible");
			return;
		}
		

		camera=Camera.open();
		camera.setErrorCallback(new ErrorCallback() {
			public void onError(int error, Camera camera) {
				Log.e(tag, "Camera error : " + error);
			}
		});
		
		
		Camera.Parameters parameters=camera.getParameters();
		if (supportedVideoSizes == null) {
			supportedVideoSizes = getSupportedPreviewSizes(camera.getParameters());
		}

		parameters.set("camera-id", params.cameraId);
		if (!params.videoDimensionsInverted) {
			parameters.setPreviewSize(params.width, params.height);
		} else {
			parameters.setPreviewSize(params.height, params.width);
		}
		parameters.setPreviewFrameRate(Math.round(params.fps));


		onSettingCameraParameters(parameters);
		camera.setParameters(parameters);


		SurfaceHolder holder = params.surfaceView.getHolder();
		try {
			camera.setPreviewDisplay(holder);
		}
		catch (Throwable t) {
			Log.e(tag, "Exception in Video capture setPreviewDisplay()", t);
		}


		try {
			camera.startPreview();
			previewStarted = true;
		} catch (Throwable e) {
			Log.e(tag, "Can't start camera preview");
		}

		previewStarted = true;

		
		// Register callback to get capture buffer
		lowLevelSetPreviewCallback(camera, storedPreviewCallback);
		
		
		onPreviewStarted(camera);
	}
	



	protected void onSettingCameraParameters(Parameters parameters) {}

	/**
	 * Hook.
	 * @param camera
	 */
	public void onPreviewStarted(Camera camera) {}

	public void storePreviewCallBack(PreviewCallback cb) {
		this.storedPreviewCallback = cb;
		if (camera == null) {
			Log.w(tag, "Capture camera not ready, storing callback");
			return;
		}
		
		lowLevelSetPreviewCallback(camera, cb);
	}


	void stopPreview() {
		if (!previewStarted) return;
		lowLevelSetPreviewCallback(camera, null);
		camera.stopPreview();
		camera.release();
		camera=null;
		previewStarted = false;
	}
	
	
	public void stopCaptureCallback() {
		if (camera != null) {
			lowLevelSetPreviewCallback(camera, null);
		}
	}
	
	protected abstract void lowLevelSetPreviewCallback(Camera camera, PreviewCallback cb);

	public void setRotation(int rotation) {
		orientationCode = (4 + 1 - rotation) % 4;
	}
	
	protected int getOrientationCode() {
		return orientationCode;
	}



	
	public static class RecorderParams {
		public float fps;
		public int height;
		public int width;

		final long filterDataNativePtr;
		int cameraId;
		int rotation;
		public SurfaceView surfaceView;
		boolean videoDimensionsInverted;
		
		public RecorderParams(long ptr) {
			filterDataNativePtr = ptr;
		}
	}




	public boolean isStarted() {
		return previewStarted;
	}

	public List<Size> getSupportedVideoSizes() {
		return new ArrayList<Size>(supportedVideoSizes);
	}
}
