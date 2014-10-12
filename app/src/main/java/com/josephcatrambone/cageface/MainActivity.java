package com.josephcatrambone.cageface;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.media.FaceDetector;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.logging.Logger;


public class MainActivity extends Activity {
    public static final float MIN_FACE_CONFIDENCE = 0.4f;
	private static final String IMAGE_PREFIX = "CAGEFACE_TEMP";
	private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 100;
	private File lastFile = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
				.add(R.id.container, new PlaceholderFragment())
				.commit();
		}
	}

	public void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}

	public File allocatePicture() {
		try {
			File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
			File image = File.createTempFile(
				IMAGE_PREFIX,  /* prefix */
				".jpg",         /* suffix */
				storageDir      /* directory */
			);

			// Save a file: path for use with ACTION_VIEW intents
			return image;
		} catch(IOException ioe) {
			Logger.getAnonymousLogger().info("Unable to allocate image: " + ioe);
			return null;
		}
	}

	public void takePicture(View view) {
		Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

		File imgFile = allocatePicture();
		if(imgFile != null) {
			lastFile = imgFile;
			Uri imgUri = Uri.fromFile(imgFile);
			takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri); // EXTRA_OUTPUT means intent will be null in response.
			takePictureIntent.putExtra("return-data", true);

			if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
				startActivityForResult(takePictureIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
			} else {
				showMessage("Unable to resolve camera activity. :(");
			}
		} else {
			showMessage("Problem making temporary image file. :(");
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Logger.getAnonymousLogger().info("Result");
		if (requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
			if (resultCode == RESULT_OK) {
				ImageView imageView = (ImageView)findViewById(R.id.image_area);

				// Assign preview to image view
				//Bundle extras = data.getExtras();
				//Bitmap imageBitmap = (Bitmap) extras.get("data");
				Bitmap imageBitmap = BitmapFactory.decodeFile(lastFile.getPath());
				imageView.setImageBitmap(imageBitmap);
				new BitmapWorkerTask(imageView).execute(imageBitmap);

				// Image captured and saved to fileUri specified in the Intent
				//showMessage("Image saved to:\n" + data.getData());
			} else if (resultCode == RESULT_CANCELED) {
				// User cancelled the image capture
			} else {
				// Image capture failed, advise user
			}
		}
	}

	public void resizePicture(ImageView imageView, String filename) {
		// Get the dimensions of the View
		int targetW = imageView.getWidth();
		int targetH = imageView.getHeight();

		// Get the dimensions of the bitmap
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(filename, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;

		// Determine how much to scale down the image
		int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

		// Decode the image file into a Bitmap sized to fill the View
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		Bitmap bitmap = BitmapFactory.decodeFile(filename, bmOptions);
		imageView.setImageBitmap(bitmap);
	}

	public void savePicture(View view) {
		Logger.getAnonymousLogger().info("savePicture");
		//Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		//File f = new File(mCurrentPhotoPath);
		//Uri contentUri = Uri.fromFile(f);
		//mediaScanIntent.setData(contentUri);
		//this.sendBroadcast(mediaScanIntent);
	}


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container, false);
			return rootView;
		}
	}

	class BitmapWorkerTask extends AsyncTask<Bitmap, Integer, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private final int MAX_FACES = 10;

		public BitmapWorkerTask(ImageView imageView) {
			// Use a WeakReference to ensure the ImageView can be garbage collected
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		// Decode image in background.
		@Override
		protected Bitmap doInBackground(Bitmap... params) {
			// Load image and find faces
			//Bitmap imageBitmap = BitmapFactory.decodeFile(lastFile.getPath());
			Bitmap sourceBitmap = params[0];
			Bitmap imageBitmap = Bitmap.createBitmap(
				sourceBitmap.getWidth(), sourceBitmap.getHeight(), Bitmap.Config.RGB_565 // Have to use 565 for face detection
			);
			Canvas canvas = new Canvas(imageBitmap);
			Paint paint = new Paint();
			canvas.drawBitmap(sourceBitmap, 0, 0, paint);

			// Load cageface
			Bitmap cageface = BitmapFactory.decodeResource(getResources(), R.drawable.cageface_0);
			float scaleFactor = 1.0f/500;

			// Find faces
			// Face detection only works on bitmaps in 565 form.
			FaceDetector faceDetector = new FaceDetector(imageBitmap.getWidth(), imageBitmap.getHeight(), MAX_FACES);
			FaceDetector.Face[] faces = new FaceDetector.Face[MAX_FACES];
			int facesFound = faceDetector.findFaces(imageBitmap, faces);
			Logger.getAnonymousLogger().info("Found " + facesFound + " faces.");

			// NICHOLAS CAGE to each face.
			for(int i=0; i < facesFound; i++) {
				FaceDetector.Face f = faces[i];
				PointF midpoint = new PointF();
				float eyeDistance = 0.0f; // We normalize all our face images to the 0/1 range, so this can be a constant multiplier.
				float angleX, angleY, angleZ;

				if(f.confidence() > MIN_FACE_CONFIDENCE) {
					// Fill face values
					f.getMidPoint(midpoint);
					eyeDistance = f.eyesDistance();
					angleX = f.pose(FaceDetector.Face.EULER_X);
					angleY = f.pose(FaceDetector.Face.EULER_Y);
					angleZ = f.pose(FaceDetector.Face.EULER_Z);

					// Look up cage face from information
					Logger.getAnonymousLogger().info("Detected face with " +
						"confidence: " +	f.confidence() +
						" eyeDist: " + eyeDistance +
						" position: " + midpoint.x + "," + midpoint.y +
						" angle: " + angleX + "," + angleY + "," + angleZ);

					Logger.getAnonymousLogger().info("Cage face left:" + ((cageface.getWidth()*scaleFactor*eyeDistance/2) + midpoint.x));

					// Write cage-face to bitmap.
					Rect source = new Rect(0, 0, cageface.getWidth(), cageface.getHeight());
					Rect destination = new Rect(
						(int)((-cageface.getWidth()*scaleFactor*eyeDistance) + midpoint.x),
						(int)((-cageface.getHeight()*scaleFactor*eyeDistance) + midpoint.y),
						(int)((cageface.getWidth()*scaleFactor*eyeDistance) + midpoint.x),
						(int)((cageface.getHeight()*scaleFactor*eyeDistance) + midpoint.y)
					);
					canvas.drawBitmap(cageface, source, destination, paint);
				}

			}
			
			return imageBitmap;
			//return decodeSampledBitmapFromResource(getResources(), data, 100, 100));
		}

		@Override
		protected void onProgressUpdate(Integer... progress) {
			// Do update
		}

		// Once complete, see if ImageView is still around and set bitmap.
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			Logger.getAnonymousLogger().info("Finished processing cage faces.");
			if (imageViewReference != null && bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}
	}
}
