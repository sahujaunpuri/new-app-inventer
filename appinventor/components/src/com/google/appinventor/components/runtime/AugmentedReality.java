package com.google.appinventor.components.runtime;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesFeatures;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.FileUtil;

/**
 * @author: Tomislav Tomsic
 * @email: tomsict@gmail.com Duplication of logic in methods is for forward
 *         compatibility reasons i.e. it allows us more future flexiblity imo.
 */

@DesignerComponent(version = YaVersion.AUGMENTED_REALITY_COMPONENT_VERSION, description = "A component to use the device's camera and "
		+ "subsequently manipulate its camera preview.", category = ComponentCategory.MEDIA, nonVisible = true, iconName = "images/abstractreality.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE,"
		+ "android.permission.CAMERA")
@UsesFeatures(featureNames = "android.hardware.camera")
public class AugmentedReality extends AndroidNonvisibleComponent implements
		TextureView.SurfaceTextureListener {

	private final Activity context;
	private final Form form;

	private RelativeLayout containerLayout;
	private TextureView myTextureView;

	private RelativeLayout.LayoutParams camLp;
	private RelativeLayout.LayoutParams cameraOverlayParams;

	private android.hardware.Camera myCamera;
	private boolean useFront;
	private Camera.Size myPreviewSize;

	// For saving pictures and perspectives
	private Bitmap cameraBitmap;
	private Bitmap overlayBitmap;
	private Bitmap resultBitmap;

	// For the precise placment of the camera overlay
	int horizontalAlignment;
	int verticalAlignment;

	private AndroidViewComponent overlay;

	public AugmentedReality(ComponentContainer container) {
		super(container.$form());
		context = container.$context();
		form = container.$form();
		// Default property values
		UseFront(false);
		overlayBitmap = null;
		cameraBitmap = null;
		resultBitmap = null;
		horizontalAlignment = form.AlignHorizontal();
		verticalAlignment = form.AlignVertical();

		/*
		 * Crucial bit of code.TextureView needs a hardware acceleration,or it
		 * would never call a camera.
		 */

		container
				.$form()
				.getWindow()
				.setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
						WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);

		myTextureView = new TextureView(this.context);
		myTextureView.setSurfaceTextureListener(this);

		android.provider.Settings.System.putInt(context.getContentResolver(),
				Settings.System.ACCELEROMETER_ROTATION, 0);
	}

	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
			int height) {

		android.hardware.Camera.CameraInfo cameraInfo = new android.hardware.Camera.CameraInfo();

		if (UseFront()) {
			int numberOfCameras = 0;

			numberOfCameras = android.hardware.Camera.getNumberOfCameras();

			for (int i = 0; i < numberOfCameras; i++) {
				Camera.getCameraInfo(i, cameraInfo);
				if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
					try {
						myCamera = Camera.open(i);
					} catch (RuntimeException e) {
						Log.e(" Augmented Reality Component ",
								"Front camera failed to open: "
										+ e.getLocalizedMessage());
					}
				}
			}

		} else {
			myCamera = android.hardware.Camera.open();
		}

		try {

			myCamera.setPreviewTexture(surface);
			myCamera.startPreview();
		} catch (Throwable t) {
			Log.e("onSurfaceTextureAvailable in AR Component",
					"Exception in setPreviewTexture() or with starting Preview",
					t);
		}

		Camera.Parameters parameters = myCamera.getParameters();
		myPreviewSize = parameters.getSupportedPreviewSizes().get(0);

		if (myPreviewSize != null) {
			parameters
					.setPreviewSize(myPreviewSize.width, myPreviewSize.height);
		}

		myCamera.setParameters(parameters);

		this.myCamera.setDisplayOrientation(getAppropriateCameraOrientation());

	}

	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		if (myCamera != null) {
			myCamera.stopPreview();
			myCamera.release();
			myCamera = null;
		}
		return true;
	}

	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
			int height) {

	}

	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {

		if (surface == null) {
			// Something occured and surface doesn't exist any more
			return;
		}

		try {
			Camera.Parameters parameters = myCamera.getParameters();

			// Set the auto-focus mode to "continuous"
			parameters
					.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

			// Preview size must exist.
			if (myPreviewSize != null) {
				Camera.Size previewSize = myPreviewSize;
				parameters
						.setPreviewSize(previewSize.width, previewSize.height);
			}

			myCamera.setParameters(parameters);
			myCamera.startPreview();
		} catch (Exception e) {
			Log.e(" AugmentedReality Component ",
					"Surface texture wasn't updated correctly: "
							+ e.getLocalizedMessage());
			e.printStackTrace();
		}

	}

	/**
	 * Returns true if the front-facing camera is to be used (when available)
	 *
	 * @return {@code true} indicates front-facing is to be used, {@code false}
	 *         will open default
	 */
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public boolean UseFront() {
		return useFront;
	}

	/**
	 * Specifies whether the camera preview camera should be transparent.
	 */
	@DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN, defaultValue = "False")
	@SimpleProperty(description = "Specifies whether the camera preview camera should be transparent.")
	public void UseFront(boolean front) {
		useFront = front;
	}

	/**
	 * Here is where we add components, further capabilities are to be expected
	 * in future versions. Caution, for some reason, if component is 100% in
	 * either width or height, it will not be shown on top of a CameraPreview.
	 * 
	 * @param component
	 */
	@SimpleFunction(description = "Adds a visible component on a camera preview.")
	public void AddPerspective(AndroidViewComponent component) {

		// overlay = component;
		containerLayout = new RelativeLayout(context);
		containerLayout.setId(10);

		camLp = new RelativeLayout.LayoutParams(
				RelativeLayout.LayoutParams.MATCH_PARENT,
				RelativeLayout.LayoutParams.MATCH_PARENT);
		camLp.addRule(RelativeLayout.CENTER_IN_PARENT, 10);

		myTextureView.setLayoutParams(camLp);
		myTextureView.setId(11);
		containerLayout.addView(myTextureView, 0, camLp);

		/*
		 * Before we detach component from its previous View hierarchy, in order
		 * to add it to new one, we first have to take care of properties that
		 * we have a use for. Please note, all future such logic should be
		 * placed in this place, for the same reasons. tt
		 */

		cameraOverlayParams = new RelativeLayout.LayoutParams(component
				.getView().getLayoutParams().width, component.getView()
				.getLayoutParams().height);

		/*
		 * Since at the moment only LinearLayout and TableLayout are available
		 * through App Inv design, this check may seem redundant, but in my
		 * opinion, if one is able to perceive possible future needs, one should
		 * be advised to do so. tt
		 */

		if (component instanceof HVArrangement
				|| component instanceof TableArrangement) {

			if (horizontalAlignment == 1 && verticalAlignment == 1) {
				// Default case, putting it here for the code's
				// logical completness
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			}

			else if (horizontalAlignment == 1 && verticalAlignment == 2) {
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				cameraOverlayParams.addRule(RelativeLayout.CENTER_VERTICAL);
			}

			else if (horizontalAlignment == 1 && verticalAlignment == 3) {
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			}
			/*
			 * This is where the weird things start to happen, and the reason
			 * is, as far as I am able to discern, a difference in positioning
			 * of child elements with Frame Layout (the one form uses) and
			 * Relative Layout. Meaning, Center Horizontal and Center Vertical
			 * would end up as Right Horizontal and Center Vertical, and vice
			 * versa. Consequently, CB would be RB, and CT would be RT and vice
			 * versa. So I had to swap logic to work around different ways those
			 * two work with the horizontal alignment. Further testing and
			 * debugging needed! tt
			 */
			else if (horizontalAlignment == 2 && verticalAlignment == 1) {
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			}

			else if (horizontalAlignment == 2 && verticalAlignment == 2) {
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				cameraOverlayParams.addRule(RelativeLayout.CENTER_VERTICAL);
			}

			else if (horizontalAlignment == 2 && verticalAlignment == 3) {
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			}

			else if (horizontalAlignment == 3 && verticalAlignment == 1) {
				cameraOverlayParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
			}

			else if (horizontalAlignment == 3 && verticalAlignment == 2) {
				cameraOverlayParams.addRule(RelativeLayout.CENTER_IN_PARENT);
			}

			else if (horizontalAlignment == 3 && verticalAlignment == 3) {
				cameraOverlayParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
				cameraOverlayParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
			}
		}

		// Finally we are detaching component from its previous View hierarchy
		final ViewGroup parent = (ViewGroup) component.getView().getParent();

		if (parent != null) {
			parent.removeView(component.getView());
		}

		overlay = component;
		overlay.getView().setLayoutParams(cameraOverlayParams);
		overlay.getView().setId(13);
		containerLayout.addView(overlay.getView(), 1, cameraOverlayParams);

		context.setContentView(containerLayout);
	}

	@SimpleFunction(description = "Removes visible component from a camera preview.")
	public void RemovePerspective(AndroidViewComponent component) {
		overlay = component;
		containerLayout.removeView(overlay.getView());
		getView().invalidate();

	}

	public View getView() {
		return containerLayout;
	}

	/**
	 * Code inspired by Zoltán's answer on;
	 * http://stackoverflow.com/questions/10380989
	 * /how-do-i-get-the-current-orientation
	 * -activityinfo-screen-orientation-of-an-a
	 * 
	 * @return degrees
	 */
	private int getAppropriateCameraOrientation() {
		int rotation = context.getWindowManager().getDefaultDisplay()
				.getRotation();
		int degrees = 0;
		DisplayMetrics dm = new DisplayMetrics();
		context.getWindowManager().getDefaultDisplay().getMetrics(dm);
		int width = dm.widthPixels;
		int height = dm.heightPixels;
		int orientation;
		// if the device's natural orientation is portrait:
		if ((rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180)
				&& height > width
				|| (rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270)
				&& width > height) {
			switch (rotation) {
			case Surface.ROTATION_0:
				orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
				degrees = 90;
				break;
			case Surface.ROTATION_90:
				orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				degrees = 0;
				break;
			case Surface.ROTATION_180:
				orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
				degrees = 270;
				break;
			case Surface.ROTATION_270:
				orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
				degrees = 180;
				break;
			default:
				Log.e("Default", "Unknown screen orientation. Defaulting to "
						+ "portrait.");
				orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
				degrees = 90;
				break;
			}
		}
		// if the device's natural orientation is landscape.
		else {
			switch (rotation) {
			case Surface.ROTATION_0:
				orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				degrees = 0;
				break;
			case Surface.ROTATION_90:
				orientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
				degrees = 90;
				break;
			case Surface.ROTATION_180:
				orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
				degrees = 180;
				break;
			case Surface.ROTATION_270:
				orientation = ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
				degrees = 270;
				break;
			default:
				Log.e("Default", "Unknown screen orientation. Defaulting to "
						+ "landscape.");
				orientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
				degrees = 0;
				break;
			}
		}
		return degrees;
	}

	// Re-used code from Canvas component

	/**
	 * Saves a picture of Camera's preview to the device's external storage in
	 * the file named fileName. fileName must end with one of ".jpg", ".jpeg",
	 * or ".png" (which determines the file type: JPEG, or PNG).
	 *
	 * @return the full path name of the saved file, or the empty string if the
	 *         save failed
	 */
	@SimpleFunction(description = "Saves a picture of Camera's preview to the device's "
			+ "external storage in the file "
			+ "named fileName. fileName must end with one of .jpg, .jpeg, or .png, "
			+ "which determines the file type.")
	public String SavePictureAs(String fileName) {
		// Figure out desired file format
		if (cameraBitmap != null) {
			cameraBitmap.recycle();
		}
		Bitmap.CompressFormat format;
		cameraBitmap = myTextureView.getBitmap();
		if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			format = Bitmap.CompressFormat.JPEG;
		} else if (fileName.endsWith(".png")) {
			format = Bitmap.CompressFormat.PNG;
		} else if (!fileName.contains(".")) { // make PNG the default to match
												// Save behavior
			fileName = fileName + ".png";
			format = Bitmap.CompressFormat.PNG;
		} else {
			form.dispatchErrorOccurredEvent(this, "SavePictureAs",
					ErrorMessages.ERROR_MEDIA_IMAGE_FILE_FORMAT);
			return "";
		}
		try {
			File file = FileUtil.getExternalFile(fileName);
			return saveFile(file, format, "SavePictureAs", cameraBitmap);
		} catch (IOException e) {
			form.dispatchErrorOccurredEvent(this, "SavePictureAs",
					ErrorMessages.ERROR_MEDIA_FILE_ERROR, e.getMessage());
		} catch (FileUtil.FileException e) {
			form.dispatchErrorOccurredEvent(this, "SavePictureAs",
					e.getErrorMessageNumber());
		}
		return "";
	}

	/**
	 * Saves a perspective of Camera's preview to the device's external storage
	 * in the file named fileName. fileName must end with one of ".jpg",
	 * ".jpeg", or ".png" (which determines the file type: JPEG, or PNG).
	 *
	 * @return the full path name of the saved file, or the empty string if the
	 *         save failed
	 */
	@SimpleFunction(description = "Saves a perspective of Camera's to the device's "
			+ "external storage in the file "
			+ "named fileName. fileName must end with one of .jpg, .jpeg, or .png, "
			+ "which determines the file type.")
	public String SavePerspectiveAs(String fileName) {

		if (overlayBitmap != null || cameraBitmap != null) {
			overlay.getView().setDrawingCacheEnabled(false);
			overlay.getView().destroyDrawingCache();
			overlayBitmap.recycle();
			cameraBitmap.recycle();
		}

		Bitmap.CompressFormat format;
		cameraBitmap = myTextureView.getBitmap();

		overlay.getView().setDrawingCacheEnabled(true);
		overlay.getView().buildDrawingCache();
		overlayBitmap = overlay.getView().getDrawingCache();

		if (resultBitmap != null) {
			resultBitmap.recycle();
		}

		resultBitmap = combineBitmaps(cameraBitmap, overlayBitmap);

		if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
			format = Bitmap.CompressFormat.JPEG;
		} else if (fileName.endsWith(".png")) {
			format = Bitmap.CompressFormat.PNG;
		} else if (!fileName.contains(".")) { // make PNG the default to match
												// Save behavior
			fileName = fileName + ".png";
			format = Bitmap.CompressFormat.PNG;
		} else {
			form.dispatchErrorOccurredEvent(this, "SavePerspectiveAs",
					ErrorMessages.ERROR_MEDIA_IMAGE_FILE_FORMAT);
			return "";
		}
		try {
			File file = FileUtil.getExternalFile(fileName);
			return saveFile(file, format, "SavePerspectiveAs", resultBitmap);
		} catch (IOException e) {
			form.dispatchErrorOccurredEvent(this, "SavePerspectiveAs",
					ErrorMessages.ERROR_MEDIA_FILE_ERROR, e.getMessage());
		} catch (FileUtil.FileException e) {
			form.dispatchErrorOccurredEvent(this, "SavePerspectiveAs",
					e.getErrorMessageNumber());
		}
		return "";
	}

	public Bitmap combineBitmaps(Bitmap camBmp, Bitmap overBmp) {

		Bitmap resultingBmp = Bitmap.createBitmap(camBmp.getWidth(),
				camBmp.getHeight(), Bitmap.Config.ARGB_8888);

		android.graphics.Canvas canvas = new android.graphics.Canvas(
				resultingBmp);
		Paint paint = new Paint();
		paint.setAntiAlias(true);
		paint.setFilterBitmap(true);
		paint.setDither(true);
		canvas.drawBitmap(camBmp, new Matrix(), paint);

		float marginLeft = 0;
		float marginTop = 0;

		float camWidth = camBmp.getWidth();
		float camHeight = camBmp.getHeight();

		float overlayWidth = overBmp.getWidth();
		float overlayHeight = overBmp.getHeight();

		// Btw, in case anyone wonders, I simply prefer how
		// continuous if staments look like than the else if ones.
		if (overlayWidth == camWidth && overlayHeight == camHeight) {
			canvas.drawBitmap(overBmp, 0, 0, paint);
		} else {
			if (horizontalAlignment == 1 && verticalAlignment == 1) {
				canvas.drawBitmap(overBmp, marginLeft, marginTop, paint);
			} else if (horizontalAlignment == 1 && verticalAlignment == 2) {
				marginTop = (camHeight - overlayHeight) / 2;
				canvas.drawBitmap(overBmp, marginLeft, marginTop, paint);
			} else if (horizontalAlignment == 1 && verticalAlignment == 3) {
				marginTop = camHeight - overlayHeight;
				canvas.drawBitmap(overBmp, marginLeft, marginTop, paint);
			}
			/*
			 * Again, I have to change logic probably because of the different
			 * standards between Frame and Relative Layout regarding horizonatal
			 * alignment.
			 */
			else if (horizontalAlignment == 2 && verticalAlignment == 1) {
				marginLeft = (camWidth - overlayWidth);
				canvas.drawBitmap(overBmp, marginLeft, marginTop, paint);
			} else if (horizontalAlignment == 2 && verticalAlignment == 2) {
				marginLeft = (camWidth - overlayWidth);
				marginTop = (camHeight - overlayHeight) / 2;
				canvas.drawBitmap(overBmp, marginLeft, marginTop, paint);
			} else if (horizontalAlignment == 2 && verticalAlignment == 3) {
				marginLeft = (camWidth - overlayWidth);
				marginTop = (camHeight - overlayHeight);
				canvas.drawBitmap(overBmp, marginLeft, marginTop, paint);
			} else if (horizontalAlignment == 3 && verticalAlignment == 1) {
				marginLeft = (camWidth - overlayWidth) / 2;
				canvas.drawBitmap(overBmp, marginLeft, marginTop, paint);
			} else if (horizontalAlignment == 3 && verticalAlignment == 2) {
				marginLeft = (camWidth - overlayWidth) / 2;
				marginTop = (camHeight - overlayHeight) / 2;
				canvas.drawBitmap(overBmp, marginLeft, marginTop, paint);
			} else if (horizontalAlignment == 3 && verticalAlignment == 3) {
				marginLeft = (camWidth / 2 - overlayWidth / 2);
				marginTop = (camHeight - overlayHeight);
				canvas.drawBitmap(overBmp, marginLeft, marginTop, paint);
			}
		}
		return resultingBmp;
	}

	// Helper method for SavePictureAs
	private String saveFile(File file, Bitmap.CompressFormat format,
			String method, Bitmap picture) {
		try {
			boolean success = false;
			FileOutputStream fos = new FileOutputStream(file);
			try {
				success = picture.compress(format, 100, // quality: ignored for
														// png
						fos);
			} finally {
				fos.close();
			}
			if (success) {
				return file.getAbsolutePath();
			} else {
				form.dispatchErrorOccurredEvent(this, method,
						ErrorMessages.ERROR_CANVAS_BITMAP_ERROR);
			}
		} catch (FileNotFoundException e) {
			form.dispatchErrorOccurredEvent(this, method,
					ErrorMessages.ERROR_MEDIA_CANNOT_OPEN,
					file.getAbsolutePath());
		} catch (IOException e) {
			form.dispatchErrorOccurredEvent(this, method,
					ErrorMessages.ERROR_MEDIA_FILE_ERROR, e.getMessage());
		}
		return "";
	}
}