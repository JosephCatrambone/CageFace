package com.josephcatrambone.cageface;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
			lastFile = null;
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

	public void cagePicture(View view) {
		Logger.getAnonymousLogger().info("cagePicture");
		//android.media.FaceDetector
	}

	public void savePicture(View view) {
		Logger.getAnonymousLogger().info("savePicture");
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

	class BitmapWorkerTask extends AsyncTask<Integer, Void, Bitmap> {
		private final WeakReference<ImageView> imageViewReference;
		private final int MAX_FACES = 10;

		public BitmapWorkerTask(ImageView imageView) {
			// Use a WeakReference to ensure the ImageView can be garbage collected
			imageViewReference = new WeakReference<ImageView>(imageView);
		}

		// Decode image in background.
		@Override
		protected Bitmap doInBackground(Integer... params) {
			// Load image and find faces
			Bitmap imageBitmap = BitmapFactory.decodeFile(lastFile.getPath());
			FaceDetector faceDetector = new FaceDetector(imageBitmap.getWidth(), imageBitmap.getHeight(), MAX_FACES);
			FaceDetector.Face[] faces = new FaceDetector.Face[MAX_FACES];
			int facesFound = faceDetector.findFaces(imageBitmap, faces);

			// NICHOLAS CAGE to each face.

			// Draw to bitmap
			
			return imageBitmap;
			//return decodeSampledBitmapFromResource(getResources(), data, 100, 100));
		}

		// Once complete, see if ImageView is still around and set bitmap.
		@Override
		protected void onPostExecute(Bitmap bitmap) {
			if (imageViewReference != null && bitmap != null) {
				final ImageView imageView = imageViewReference.get();
				if (imageView != null) {
					imageView.setImageBitmap(bitmap);
				}
			}
		}
	}
}
