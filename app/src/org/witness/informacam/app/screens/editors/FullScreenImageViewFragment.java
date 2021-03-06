package org.witness.informacam.app.screens.editors;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.witness.informacam.InformaCam;
import org.witness.informacam.app.screens.FullScreenViewFragment;
import org.witness.informacam.app.utils.Constants.EditorActivityListener;
import org.witness.informacam.app.utils.Constants.App.Editor;
import org.witness.informacam.models.media.IImage;
import org.witness.informacam.utils.Constants.Logger;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;
import android.util.Log;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class FullScreenImageViewFragment extends FullScreenViewFragment {
	Bitmap bitmap, originalBitmap, previewBitmap;
	ImageView mediaHolder_;
	IImage media_;

	// sample sized used to downsize from native photo
	int inSampleSize;

	// Image Matrix
	Matrix matrix = new Matrix();
	float[] matrixTranslate;

	// Saved Matrix for not allowing a current operation (over max zoom)
	Matrix savedMatrix = new Matrix();
	
	private final static String LOG = Editor.LOG;
	
	@Override
	public void onAttach(Activity a) {
		super.onAttach(a);
		
		media_ = new IImage(((EditorActivityListener) a).media());
	}

	@Override
	public void onDetach() {
		super.onDetach();

		try {
			bitmap.recycle();
			originalBitmap.recycle();
			previewBitmap.recycle();
		} catch(NullPointerException e) {}

	}

	@Override
	protected void initLayout() {
		super.initLayout();

		mediaHolder_ = new ImageView(getActivity());
		mediaHolder_.setLayoutParams(new LinearLayout.LayoutParams(dims[0], dims[1]));
		mediaHolder.addView(mediaHolder_);
		
		loadBitmap ();
		setBitmap();
	}
	
	 private void loadBitmap ()
	 {
		BitmapFactory.Options bfo = new BitmapFactory.Options();
		bfo.inJustDecodeBounds = true;
		bfo.inPreferredConfig = Bitmap.Config.RGB_565;

		BufferedInputStream bytes = null;
		InputStream is = null;
		
		is = InformaCam.getInstance().ioService.getStream(media_.dcimEntry.fileAsset.path, media_.dcimEntry.fileAsset.source);
		bytes = new BufferedInputStream(is);
		
		bitmap = BitmapFactory.decodeStream(bytes, null, bfo);
		
		// Ratios between the display and the image
		double widthRatio =  Math.floor(bfo.outWidth / dims[0]);
		double heightRatio = Math.floor(bfo.outHeight / dims[1]);
		Log.d(LOG, "wRatio: " + widthRatio + ", hRatio: " + heightRatio);

		// If both of the ratios are greater than 1,
		// one of the sides of the image is greater than the screen
		if (heightRatio > widthRatio) {
			// Height ratio is larger, scale according to it
			inSampleSize = (int)heightRatio;
		} else {
			// Width ratio is larger, scale according to it
			inSampleSize = (int)widthRatio;
		}

		bfo.inSampleSize = inSampleSize;
		bfo.inJustDecodeBounds = false;

		try {
			is.close();
			bytes.close();
			
			is = InformaCam.getInstance().ioService.getStream(media_.dcimEntry.fileAsset.path, media_.dcimEntry.fileAsset.source);
			bytes = new BufferedInputStream(is);
			
			bitmap = BitmapFactory.decodeStream(bytes, null, bfo);
			
			is.close();
			bytes.close();
		} catch (IOException e) {
			Logger.e(LOG, e);
		}

		if (media_.dcimEntry.exif.orientation == ExifInterface.ORIENTATION_ROTATE_90) {
			Log.d(LOG, "Rotating Bitmap 90");
			Matrix rotateMatrix = new Matrix();
			rotateMatrix.postRotate(90);
			bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),rotateMatrix,false);
		} else if (media_.dcimEntry.exif.orientation == ExifInterface.ORIENTATION_ROTATE_270) {
			Log.d(LOG,"Rotating Bitmap 270");
			Matrix rotateMatrix = new Matrix();
			rotateMatrix.postRotate(270);
			bitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),rotateMatrix,false);
		}

		originalBitmap = bitmap;
		
	}

	private void setBitmap() {
		float matrixWidthRatio = (float) dims[0] / (float) bitmap.getWidth();
		float matrixHeightRatio = (float) dims[1] / (float) bitmap.getHeight();

		// Setup the imageView and matrix for scaling
		float matrixScale = matrixHeightRatio;

		if (matrixWidthRatio < matrixHeightRatio) {
			matrixScale = matrixWidthRatio;
		} 

		mediaHolder_.setImageBitmap(bitmap);

		
		//PointF midpoint = new PointF((float)imageBitmap.getWidth()/2f, (float)imageBitmap.getHeight()/2f);
		matrix.postScale(matrixScale, matrixScale);

		// This doesn't completely center the image but it get's closer
		//int fudge = 42;
		matrixTranslate = new float[] {
				(dims[0] -bitmap.getWidth() * matrixScale)/2f,
				(dims[1] - bitmap.getHeight() * matrixScale)/2f
		};
		matrix.postTranslate(matrixTranslate[0], matrixTranslate[1]);
		Log.d(LOG, String.format("MATRIX TRANSLATE FOR IMAGE: %f , %f", matrixTranslate[0], matrixTranslate[1]));

		mediaHolder_.setImageMatrix(matrix);
		
		initRegions();
	}
	
	@Override
	public int[] getSpecs() {
		Log.d(LOG, "RECALCULATING FOR IMAGE");
		
		List<Integer> specs = new ArrayList<Integer>(Arrays.asList(ArrayUtils.toObject(super.getSpecs())));
		
		for(float i : matrixTranslate) {
			specs.add((int) i/2);
		}
		
		/*
		int[] locationInWindow = new int[2];
		mediaHolder_.getLocationInWindow(locationInWindow);
		for(int i : locationInWindow) {
			specs.add(i);
		}
		*/
		
		specs.add(mediaHolder_.getWidth());
		specs.add(mediaHolder_.getHeight());
		
		// these might not be needed
		specs.add(media_.width);
		specs.add(media_.height);
		
		return ArrayUtils.toPrimitive(specs.toArray(new Integer[specs.size()]));
	}
	
	@Override
	public RectF getImageBounds()
	{
		RectF rectImage = null;
		if (bitmap != null)
			rectImage = new RectF(0,0,bitmap.getWidth(),bitmap.getHeight());
		if (rectImage != null)
		{
			Matrix inverse = new Matrix();
			if (this.mediaHolder_.getImageMatrix().invert(inverse))
				mediaHolder_.getImageMatrix().mapRect(rectImage);
		}
		return rectImage;
	}
}