package com.google.appinventor.components.runtime;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.TextureView;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
//import com.google.appinventor.components.annotations.UsesFeatures;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;

/**
 * @author: Tomislav Tomsic
 * @email: tomsict@gmail.com Duplication of logic in methods is for forward
 *                          compatibility reasons i.e. it allows us more
 *                          future flexiblity imo.
 */

@DesignerComponent(version = YaVersion.ABSTRACT_REALITY_COMPONENT_VERSION, description = "A component to take a picture using the device's camera. "
		+ "After the picture is taken, the name of the file on the phone "
		+ "containing the picture is available as an argument to the "
		+ "AfterPicture event. The file name can be used, for example, to set "
		+ "the Picture property of an Image component.", category = ComponentCategory.MEDIA, nonVisible = true, iconName = "images/camera.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE,"
		+ "android.permission.READ_EXTERNAL_STORAGE,"
		+ "android.permission.CAMERA")
// @UsesFeatures(featureNames="android.hardware.camera.autofocus")
public class AbstractReality extends AndroidNonvisibleComponent implements
		TextureView.SurfaceTextureListener {

	private TextureView myTextureView = null;
	private android.hardware.Camera myCamera = null;
	private FrameLayout myRootLayout;
	private final Handler handler;
	private final Activity context;
	private final Form form;
	private int transparency = ComponentConstants.TRANSPARENCY;
	private int rotation = ComponentConstants.ROTATION;
	private int width;
	private int height;
	private boolean useFrontCamera;
	private static final String LOG_TAG = "AbstractRealityComponent";

	public AbstractReality(ComponentContainer container) {
		super(container.$form());
		// time saving shortcuts
		context = container.$context();
		this.form = container.$form();

		// Default property values.
		UseFrontCamera(false);
		/*
		 * initializing Camera preview to desired values,in case none are set.
		 */
		width = myRootLayout.getWidth();
		height = myRootLayout.getHeight();

		handler = new Handler();
		/*
		 * Crucial bit of code.TextureView needs a hardware acceleration,or it
		 * would never call a camera.
		 */

		container
				.$form()
				.getWindow()
				.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
						WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
			int height) {

		if (UseFrontCamera()) {
			myCamera = openFrontFacingCamera();
		} else
			myCamera = android.hardware.Camera.open();
		// Camera.Size previewSize = myCamera.getParameters().getPreviewSize();
		myTextureView.setLayoutParams(new FrameLayout.LayoutParams(width,
				height, Gravity.CENTER));

		try {
			myCamera.setPreviewTexture(surface);
			myCamera.startPreview();

		} catch (IOException ioe) {
			Toast.makeText(form.$context(), "Failed to get the camera", 5)
					.show();
			// Something bad happened
		}

	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		myCamera.stopPreview();
		myCamera.release();
		return true;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
			int height) {
		// Ignored, Camera does all the work for us

	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// Invoked every time there's a new Camera preview frame

	}

	@SimpleFunction(description = "Begins a Camera Preview in a TextureView, which allows us to treat preview as"
			+ "yet another View, though hardware accelerated one.")
	public void StartAbstractReality() {

		myTextureView = new TextureView(context);
		myTextureView.setSurfaceTextureListener(this);

		myRootLayout = new FrameLayout(context);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		myRootLayout.setLayoutParams(layoutParams);

		myRootLayout.addView(myTextureView);
		context.setContentView(myRootLayout);

	}

	@SimpleFunction(description = "Add a component, or a whole scene on a camera preview.")
	public void AddArtefact(final AndroidViewComponent component) {

		myTextureView = new TextureView(context);
		myTextureView.setSurfaceTextureListener(this);

		myRootLayout = new FrameLayout(context);
		FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT,
				ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER);

		myRootLayout.setLayoutParams(layoutParams);

		ViewGroup parent = (ViewGroup) component.getView().getParent();
		parent.removeView(component.getView());
		handler.post(new Runnable() {
			public void run() {

				myRootLayout.addView(myTextureView);
				myRootLayout.addView(component.getView(), component.getView()
						.getLayoutParams());

			}
		});

		context.setContentView(myRootLayout);
	}

	// traversing the view group and removing a particular component
	@SimpleFunction(description = "Remove a component, or a whole scene from a camera preview.")
	public void RemoveArtefact(final AndroidViewComponent component) {

		myRootLayout.removeView(component.getView());

	}

	@SimpleFunction(description = "Set color, component, animation or a scene, behind camera preview.")
	public void SetARBackground(final AndroidViewComponent component) {

		myTextureView = new TextureView(context);
		myTextureView.setSurfaceTextureListener(this);

		myRootLayout = new FrameLayout(context);

		ViewGroup parent = (ViewGroup) component.getView().getParent();
		parent.removeView(component.getView());
		handler.post(new Runnable() {
			public void run() {

				myRootLayout.addView(component.getView(), component.getView()
						.getLayoutParams());

				myRootLayout.addView(myTextureView);
			}
		});

		context.setContentView(myRootLayout);

	}

	@SimpleProperty(category = PropertyCategory.APPEARANCE)
	public int SetTransparency() {
		return transparency;
	}

	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "100")
	@SimpleProperty(description = "Specifies whether the camera preview should be transparent on the screen. "
			+ "Zero is for completely transparent and 100 for opaque.")
	public void SetTransparency(int transparency) {
		this.transparency = transparency;

		if (transparency > 100) {
			transparency = 100;
		} else if (transparency < 0) {
			transparency = 0;
		}
		//Alpha values are between 0 and 1, which is why we have to 
		//divide input by 100
		float alpha = transparency / 100;
		myTextureView.setAlpha(alpha);

	}

	@SimpleProperty(category = PropertyCategory.APPEARANCE)
	public int CameraRotation() {
		return rotation;
	}

	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_INTEGER, defaultValue = "90")
	@SimpleProperty(description = "Specifies rotation of the camera from 0 to 360 degrees. "
			+ "90 as in 90 degrees  is default value.")
	public void CameraRotation(int rotation) {
		this.rotation = rotation;

		// I am simply not in the mood for
		if (rotation > 360) {
			rotation = 0;
		} else if (rotation < 0) {
			rotation = 0;
		}

		myTextureView.setRotation(rotation);
	}

	/**
	 * Returns the camera's preview horizontal width, measured in pixels.
	 *
	 * @return width in pixels
	 */
	@SimpleProperty
	public int Width() {
		return width;
	}

	/**
	 * Specifies the camera's preview horizontal width, measured in pixels.
	 *
	 * @param width
	 *            in pixels
	 */
	@SimpleProperty
	public void Width(int width) {
		this.width = width;
	}

	/**
	 * Returns the component's horizontal height, measured in pixels.
	 *
	 * @return height in pixels
	 */
	@SimpleProperty
	public int Height() {
		return height;
	}

	/**
	 * Specifies the component's horizontal height, measured in pixels.
	 *
	 * @param height
	 *            in pixels
	 */
	@SimpleProperty
	public void Height(int height) {
		this.height = height;
	}

	private Camera openFrontFacingCamera() {
		int cameraCount = 0;
		Camera frontCamera = null;
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		cameraCount = Camera.getNumberOfCameras();
		for (int camNumber = 0; camNumber < cameraCount; camNumber++) {
			Camera.getCameraInfo(camNumber, cameraInfo);
			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
				try {
					frontCamera = Camera.open(camNumber);
				} catch (RuntimeException e) {
					Log.e(LOG_TAG, "AbstractReality Component, Front Camera failed to open: "
									+ e.getLocalizedMessage());
				}
			}
		}
		return frontCamera;
	}

	/**
	 * Returns true if the front-facing camera is to be used (when available)
	 *
	 * @return {@code true} indicates front-facing is to be used, {@code false}
	 *         will open default
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public boolean UseFrontCamera() {
		return useFrontCamera;
	}

	/**
	 * Specifies whether the front-facing camera should be used (when available)
	 *
	 * @param front
	 *            {@code true} for front-facing camera, {@code false} for
	 *            default
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
	@SimpleProperty(description = "Specifies whether the front-facing camera should be used (when available). "
			+ "If the device does not have a front-facing camera, this option will be ignored "
			+ "and the camera will open normally.")
	public void UseFrontCamera(boolean front) {
		useFrontCamera = front;
	}

	/**
	 * Takes a picture, then raises the AfterPicture event. If useFront is true,
	 * adds an extra to the intent that requests the front-facing camera.
	 */
	@SimpleFunction
	public void TakePicture() {

		myCamera.takePicture(null, null, new ARHandlingAndMergingPictures(
				context));

	}

	public class ARHandlingAndMergingPictures implements PictureCallback {

		private final Context context;
		private Bitmap bitmap;
		private Long topPhotoLong;
		private static final String LOG_TAG = "ARHandlingAndMergingPictures";
		public ARHandlingAndMergingPictures(Context context) {
			this.context = context;
		}

		/**
		 * Following code was inspired by jiahaoliuliu/RealMadridvsBorussia
		 * project on github, of which important parts were used verbatim
		 */
		@SuppressWarnings("deprecation")
		@Override
		public void onPictureTaken(byte[] data, Camera myCamera) {
			// TODO Auto-generated method stub
			java.io.File sdDir = Environment
					.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
			java.io.File pictureFileDir = new java.io.File(sdDir,
					"/ARArtefacts");

			if (!pictureFileDir.exists() && !pictureFileDir.mkdirs()) {

				Log.d(LOG_TAG, "Directory for saving image can not be created.");
				Toast.makeText(context,
						"Can't create directory to save image.",
						Toast.LENGTH_LONG).show();
				return;
			}

			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyymmddhhmmss");
			String date = dateFormat.format(new Date());
			String photoFile = "Picture_" + date + ".jpg";

			String filename = pictureFileDir.getPath() + java.io.File.separator
					+ photoFile;

			java.io.File pictureFile = new java.io.File(filename);
			try {
				FileOutputStream fos = new FileOutputStream(pictureFile);
				fos.write(data);
				fos.close();
				Toast.makeText(context, "New Image saved:" + photoFile,
						Toast.LENGTH_LONG).show();

				// TODO: Merge the photo
				try {
					Bitmap bottomImage = BitmapFactory.decodeFile(pictureFile
							.getAbsolutePath()); // blue

					bitmap = Bitmap.createBitmap(bottomImage.getWidth(),
							bottomImage.getHeight(), Bitmap.Config.ARGB_8888);
					android.graphics.Canvas c = new android.graphics.Canvas(
							bitmap);
					Resources res = context.getResources();

					Bitmap topImage = BitmapFactory.decodeResource(res,
							topPhotoLong.intValue()); // green
					@SuppressWarnings("deprecation")
					Drawable drawable1 = new BitmapDrawable(bottomImage);
					Drawable drawable2 = new BitmapDrawable(topImage);

					drawable1.setBounds(0, 0, bottomImage.getWidth(),
							bottomImage.getHeight());
					drawable2.setBounds(0, 0, bottomImage.getWidth(),
							bottomImage.getHeight());
					drawable1.draw(c);
					drawable2.draw(c);

				} catch (Exception e) {
				}
				// To write the file out to the SDCard:
				OutputStream os = null;
				try {
					os = new FileOutputStream(filename);
					bitmap.compress(Bitmap.CompressFormat.PNG, 50, os);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} catch (Exception error) {
				Log.d(LOG_TAG,
						"File" + filename + "not saved: " + error.getMessage());
				Toast.makeText(context, "Image could not be saved.",
						Toast.LENGTH_LONG).show();
			}
		}

	}

}
