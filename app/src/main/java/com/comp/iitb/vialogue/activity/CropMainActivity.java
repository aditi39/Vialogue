// "Therefore those skilled at the unorthodox
// are infinite as heaven and earth,
// inexhaustible as the great rivers.
// When they come to an end,
// they begin again,
// like the days and months;
// they die and are reborn,
// like the four seasons."
//
// - Sun Tsu,
// "The Art of War"

package com.comp.iitb.vialogue.activity;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.comp.iitb.vialogue.R;
import com.comp.iitb.vialogue.coordinators.CropImageCoordinator;
import com.comp.iitb.vialogue.coordinators.FragmentBinder;
import com.comp.iitb.vialogue.coordinators.OnFileCopyCompleted;
import com.comp.iitb.vialogue.coordinators.OnThumbnailCreated;
import com.comp.iitb.vialogue.coordinators.SharedRuntimeContent;
import com.comp.iitb.vialogue.fragments.CropMainFragment;
import com.comp.iitb.vialogue.library.Storage;
import com.comp.iitb.vialogue.listeners.FileCopyUpdateListener;
import com.comp.iitb.vialogue.models.crop.CropDemoPreset;
import com.comp.iitb.vialogue.models.crop.CropImageViewOptions;

import java.io.File;

public class CropMainActivity extends AppCompatActivity implements FragmentBinder {

    //region: Fields and Consts

    DrawerLayout mDrawerLayout;

    private ActionBarDrawerToggle mDrawerToggle;

    private Fragment mCurrentFragment;
    private CropImageCoordinator mCropImageCoordinator;
    private Storage mStorage;
    private Button mDone;
    private Uri mCropImageUri;

    private CropImageViewOptions mCropImageViewOptions = new CropImageViewOptions();
    //endregion

    public static final String IMAGE_PATH = "imagePath";
    private String mFilePath;
    private ProgressDialog mProgressDialog;


    public void setCurrentFragment(Fragment fragment) {
        mCurrentFragment = fragment;
        if (fragment instanceof CropImageCoordinator)
            mCropImageCoordinator = (CropImageCoordinator) fragment;
    }

    public void setCurrentOptions(CropImageViewOptions options) {
        mCropImageViewOptions = options;
        //updateDrawerTogglesByOptions(options);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mStorage = new Storage(getApplicationContext());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);


        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mFilePath = bundle.getString(IMAGE_PATH);
        }
        mDone = (Button) findViewById(R.id.done_button);
        mDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                done();
            }
        });

        setMainFragmentByPreset(CropDemoPreset.RECT);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
//        mDrawerToggle.syncState();
//        mCurrentFragment.updateCurrentCropViewOptions();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        */
        if (item.getItemId() == android.R.id.home) {
            done();
            finish();
            return true;
        }
        if (mCurrentFragment != null && mCurrentFragment.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setMainFragmentByPreset(CropDemoPreset demoPreset) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, CropMainFragment.newInstance(demoPreset, mFilePath))
                .commit();
    }

    private void done() {
        /*mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle(getString(R.string.processing_image));
        mProgressDialog.setMessage(getString(R.string.please_wait));
        mProgressDialog.setProgress(0);
        mProgressDialog.setMax(100);
        mProgressDialog.show();*/
        new ProcessAsync().execute();
    }

    private OnThumbnailCreated mThumbnailCreated = new OnThumbnailCreated() {
        @Override
        public void onThumbnailCreated(Bitmap thumbnail) {
            //mProgressDialog.dismiss();
            SharedRuntimeContent.imageThumbnails.add(thumbnail);

            finish();
        }
    };

    private class ProcessAsync extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Bitmap photo = mCropImageCoordinator.getCroppedImage();
            Uri imageUri = mStorage.getImageUri(photo);
            String selectedPath = mStorage.getRealPathFromURI(imageUri);
            File pickedFile = new File(selectedPath);
            mStorage.addFileToDirectory(SharedRuntimeContent.projectFolder,
                    SharedRuntimeContent.IMAGE_FOLDER_NAME,
                    SharedRuntimeContent.projectFolder.getName(),
                    pickedFile,
                    new FileCopyUpdateListener(CropMainActivity.this),
                    new OnFileCopyCompleted() {
                        @Override
                        public void done(File file, boolean isSuccessful) {
                            try {
                                SharedRuntimeContent.imagePathList.add(file.getName());
                                mFilePath = file.getAbsolutePath();
                                Toast.makeText(getApplicationContext(), "Generating Thumbnail", Toast.LENGTH_LONG).show();
                                mStorage.getImageThumbnailAsync(file.getAbsolutePath(), mThumbnailCreated);
                            } catch (Exception e) {
                                Log.d("CropMainActivity", e.getMessage());
                                Toast.makeText(getApplicationContext(), "Error Generating Thumbnail", Toast.LENGTH_LONG).show();
                            }

                        }
                    });
            return null;
        }
    }
}
