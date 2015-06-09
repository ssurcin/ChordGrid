package com.chordgrid;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.chordgrid.drive.DriveClientManager;
import com.chordgrid.model.Rhythm;
import com.chordgrid.model.Tune;
import com.chordgrid.model.TuneBook;
import com.chordgrid.model.TuneSet;
import com.chordgrid.settings.UserSettingsActivity;
import com.chordgrid.tunes.DisplayTuneGridActivity;
import com.chordgrid.tunes.ExpandableTunesListFragment;
import com.chordgrid.tunes.TuneMetadataDialogFragment;
import com.chordgrid.tunesets.TuneSetAdapter;
import com.chordgrid.util.FileUtils;
import com.chordgrid.util.LogUtils;
import com.chordgrid.util.StorageUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class MainActivity extends FragmentActivity implements TabListener {

    public static final String TAG = "MainActivity";

    /**
     * VIEW Intent action.
     */
    private static final String ACTION_VIEW = "android.intent.action.VIEW";
    /**
     * DRIVE_OPEN Intent action.
     */
    private static final String ACTION_DRIVE_OPEN = "com.google.android.apps.drive.DRIVE_OPEN";
    /**
     * Drive file ID key.
     */
    private static final String EXTRA_FILE_ID = "resourceId";

    /**
     * Drive file ID.
     */
    private String fileId;

    //private Uri localFileUri;

    private String mLocalFilePath;

    /**
     * A delegate object for Drive API.
     */
    private DriveClientManager driveClientManager;

    /**
     * The current tunebook.
     */
    private TuneBook tunebook;

    /**
     * The default tunebook's file name for serialization.
     */
    private String tunebookFileName = "myTunebook.txt";

    private ViewPager viewPager;
    private TunesAndSetsTabPagerAdapter adapter;
    private ActionBar actionBar;

    /**
     * Hold a reference on a tune being edited by the DisplayTuneGridActivity.
     */
    private Tune mEditedTune;

    private static final int ACTIONBAR_TUNELIST_INDEX = 0;
    private static final int ACTIONBAR_SETLIST_INDEX = 1;

    /**
     * Shows a toast message.
     */
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "MainActivity onCreate");

        setContentView(R.layout.activity_main);

        // Read the preferences to initialize the Known Rhythms cache
        Rhythm.initializeKnownRhythms(getApplicationContext());

        String contentPath = getIntentContentPath();
        if (!TextUtils.isEmpty(contentPath))
            openFromFileView(contentPath);
        /*
        // Get the action that triggered the intent filter for this Activity
        final Intent intent = getIntent();
        final String action = intent.getAction();

        Log.d(TAG, "Action = " + action);

        // Make sure the Action is DRIVE_OPEN.
        if (ACTION_DRIVE_OPEN.equals(action)) {
            onOpenFromGoogleDrive(intent);
        } else if (ACTION_VIEW.equals(action)) {
            onOpenFromFileView(intent);
        }
        */
        else {
            try {
                setTunebook(loadLocalTunebook());
                setUseMyTunebook();
                //loadResourceTunebook();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
                finish();
            }
        }

        Log.d(TAG, String.format("%d tunes in tunebook", tunebook.countTunes()));
        Log.d(TAG,
                String.format("%d sets in tunebook", tunebook.countTuneSets()));

        viewPager = (ViewPager) findViewById(R.id.pager);
        actionBar = getActionBar();
        adapter = new TunesAndSetsTabPagerAdapter(getSupportFragmentManager(),
                tunebook);

        viewPager.setAdapter(adapter);
        actionBar.setHomeButtonEnabled(false);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        // Adding Tabs
        actionBar.addTab(actionBar.newTab().setText(R.string.tunes)
                .setTabListener(this));
        actionBar.addTab(actionBar.newTab().setText(R.string.sets)
                .setTabListener(this));

        /**
         * on swiping the viewpager make respective tab selected
         * */
        viewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageSelected(int position) {
                // on changing the page
                // make respected tab selected
                actionBar.setSelectedNavigationItem(position);
            }

            @Override
            public void onPageScrolled(int arg0, float arg1, int arg2) {
            }

            @Override
            public void onPageScrollStateChanged(int arg0) {
            }
        });

    }

    @Override
    protected void onRestart() {
        super.onRestart();

        String contentPath = getIntentContentPath();
        if (contentPath != null) {
            openFromFileView(contentPath);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "Application stopping");

        Rhythm.saveKnownRhyhms(getApplicationContext());
        saveTuneBook();

        super.onStop();
    }

    /**
     * Called when the activity becomes visible.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (driveClientManager != null)
            driveClientManager.onActivityResume();
    }

    /**
     * Called when activity gets invisible.
     */
    @Override
    protected void onPause() {
        if (driveClientManager != null)
            driveClientManager.onActivityPause();

        Rhythm.saveKnownRhyhms(getApplicationContext());
        saveTuneBook();

        super.onPause();
    }

    private String getIntentContentPath() {
        Intent intent = getIntent();
        InputStream is = null;
        FileOutputStream os = null;
        String fullPath = null;

        try {
            String action = intent.getAction();
            if (!Intent.ACTION_VIEW.equals(action))
                return null;

            Uri uri = intent.getData();
            String scheme = uri.getScheme();
            String name = null;

            if (scheme.equals("file")) {
                List<String> pathSegments = uri.getPathSegments();
                if (pathSegments.size() > 0)
                    name = pathSegments.get(pathSegments.size() - 1);
            } else if (scheme.equals("content")) {
                Cursor cursor = getContentResolver().query(
                        uri,
                        new String[]{MediaStore.MediaColumns.DISPLAY_NAME},
                        null,
                        null,
                        null);
                cursor.moveToFirst();

                int nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME);
                if (nameIndex >= 0)
                    name = cursor.getString(nameIndex);
            } else
                return null;

            if (name == null)
                return null;

            int n = name.lastIndexOf(".");
            String fileName, fileExt;

            if (n == -1)
                return null;
            else {
                fileName = name.substring(0, n);
                fileExt = name.substring(n);
            }

            File outputDir = getCacheDir();
            File outputFile = File.createTempFile("tunebook", ".txt", outputDir);
            fullPath = outputFile.getAbsolutePath();

            is = getContentResolver().openInputStream(uri);
            os = new FileOutputStream(fullPath);

            byte[] buffer = new byte[4096];
            int count;
            while ((count = is.read(buffer)) > 0)
                os.write(buffer, 0, count);

            os.close();
            is.close();
        } catch (Exception e) {
            if (is != null) {
                try {
                    is.close();
                } catch (Exception e1) {
                    Log.e(LogUtils.getTag(), "Cannot close input stream", e1);
                }
            }
            if (os != null) {
                try {
                    os.close();
                } catch (Exception e1) {
                    Log.e(LogUtils.getTag(), "Cannot close output stream", e1);
                }
            }
            if (fullPath != null) {
                File f = new File(fullPath);
                f.delete();
                fullPath = null;
            }
        }

        return fullPath;
    }

    /**
     * Saves a tunebook to a given file path.
     *
     * @param aTunebook The tunebook to save.
     * @param path      The path where to save it.
     */
    private void saveTuneBook(TuneBook aTunebook, String path) {
        Log.d(TAG, "Saving tunebook to " + path);
        FileOutputStream fileOutput;
        try {
            fileOutput = openFileOutput(path, Context.MODE_PRIVATE);
            if (path.endsWith(".xml"))
                aTunebook.xmlSerialize(fileOutput);
            else
                aTunebook.serialize(fileOutput);
            fileOutput.close();
        } catch (Exception e) {
            Log.e(TAG, "Cannot serialize tunebook!", e);
        }
    }

    private void saveTuneBook() {
        saveTuneBook(tunebook, getCurrentTunebookFilename());
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Activity results
    //////////////////////////////////////////////////////////////////////////////////////////////

    private static final int ACTIVITY_REQUEST_ADD_TUNE = 1000;

    /**
     * Handles resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (driveClientManager != null)
            driveClientManager.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case TuneSetAdapter.ACTIVITY_REQUEST_CODE_REORDER:
                if (resultCode == Activity.RESULT_OK) {
                    TuneSet tuneSet = data.getParcelableExtra("tuneset");
                    adapter.updateTuneSet(tuneSet);
                }
                break;

            case ACTIVITY_REQUEST_ADD_TUNE:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(TAG, "Tune edition activity returned OK");
                    Tune tune = data.getParcelableExtra(DisplayTuneGridActivity.BUNDLE_KEY_TUNE);
                    if (data.getBooleanExtra(DisplayTuneGridActivity.BUNDLE_KEY_NEW, false)) {
                        tunebook.add(tune);
                    } else {
                        tunebook.replaceTune(mEditedTune, tune);
                        mEditedTune = null;
                    }
                }
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Hide the Merge button if we are already displaying My Tunebook
        if (usesMyTunebook()) {
            MenuItem mergeItem = menu.findItem(R.id.action_merge);
            if (mergeItem == null)
                Log.w(TAG, "Merge menu item is null!");
            else {
                Log.v(TAG, "Hiding Merge menu item");
                mergeItem.setVisible(false);
            }
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case R.id.action_add_tune:
                Log.d(TAG, "Action ADD TUNE");
                onAddTune();
                break;

            case R.id.action_add_set:
                Log.d(TAG, "Action ADD SET");
                onAddSet();
                break;

            case R.id.action_settings:
                Log.d(TAG, "Action SETTINGS");
                startActivity(new Intent(getApplicationContext(), UserSettingsActivity.class));
                break;

            case R.id.action_save:
                Log.d(TAG, "Action SAVE");
                onSave();
                break;

            case R.id.action_discard:
                Log.d(TAG, "Action DISCARD");
                onDiscard();
                break;

            case R.id.action_merge:
                Log.d(TAG, "Action MERGE");
                onMerge();
                break;

            default:
                Log.d(TAG, String.format("Action item not covered %X", id));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void onOpenFromGoogleDrive(final Intent intent) {
        Log.d(TAG, "Opening a file from Google Drive");
        showMessage("Opening a file from Google Drive");

        driveClientManager = new DriveClientManager(this);
        Log.d(TAG, "Created Drive Client Manager");

        return;
        /*
         * // Get the Drive file ID. fileId =
		 * intent.getStringExtra(EXTRA_FILE_ID);
		 * 
		 * // Avoid multiple reading of this property final GoogleApiClient
		 * client = driveClientManager.getGoogleApiClient();
		 * 
		 * // Get the file handle final DriveFile driveFile =
		 * Drive.DriveApi.getFile(client, DriveId.decodeFromString(fileId));
		 * showMessage("Got drive file");
		 * 
		 * // Get a copy of the file contents driveFile.openContents(client,
		 * DriveFile.MODE_READ_ONLY, null).setResultCallback(new
		 * ResultCallback<DriveApi.ContentsResult>() {
		 * 
		 * @Override public void onResult(ContentsResult result) { if
		 * (!result.getStatus().isSuccess()) {
		 * showMessage(getResources().getString(R.string.error_drive_read));
		 * return; } Contents contents = result.getContents(); BufferedReader
		 * reader = new BufferedReader(new
		 * InputStreamReader(contents.getInputStream())); StringBuilder builder
		 * = new StringBuilder(); String line; try { while ((line =
		 * reader.readLine()) != null) { builder.append(line); } }
		 * catch(IOException ex) { Log.e(TAG,
		 * "Cannot read next line from Drive file! " + ex.getMessage()); }
		 * String contentsAsString = builder.toString(); tuneBook =
		 * parseXmlChordGridString(contentsAsString);
		 * 
		 * // Close file driveFile.commitAndCloseContents(client, contents); }
		 * });
		 */
    }

    private void openFromFileView(final String filePath) {
        Log.d(TAG, "Opening file " + filePath + " with VIEW action");

        setUseOtherTunebook(filePath);

        try {
            String fileContents = StorageUtil.getStringFromFile(filePath);
            int lastDot = filePath.lastIndexOf('.');
            if (lastDot >= 0) {
                String extension = filePath.substring(lastDot + 1);
                if ("txt".equalsIgnoreCase(extension))
                    tunebook = new TuneBook(fileContents);
                else {
                    showMessage(String.format(
                            "Unexpected file extension '.%s'!", extension));
                    finish();
                }
            } else {
                showMessage(String.format("Unexpected file nature %s!",
                        filePath));
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot read file " + filePath, e);
            showMessage(getString(R.string.error_drive_read));
        }
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // Properties
    //////////////////////////////////////////////////////////////////////////////////////////////

    private void setTunebook(TuneBook tunebook) {
        this.tunebook = tunebook;
    }

    /**
     * Returns true iff we are currently displaying My Tunebook (and not
     * another file).
     */
    public boolean usesMyTunebook() {
        return TextUtils.isEmpty(mLocalFilePath);
    }

    private void setUseMyTunebook() {
        mLocalFilePath = null;
        invalidateOptionsMenu();
    }

    private void setUseOtherTunebook(String filePath) {
        mLocalFilePath = filePath;
        invalidateOptionsMenu();
    }

    private String getCurrentTunebookFilename() {
        if (!TextUtils.isEmpty(mLocalFilePath))
            return mLocalFilePath;
        return tunebookFileName;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // abListener implementation
    //////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        viewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    /**
     * ***********************************************************************
     * Tunebook storage
     * ************************************************************************
     */

    private TuneBook loadLocalTunebook() throws Exception {
        try {
            Log.d(TAG, "Looking for a local tunebook file: " + tunebookFileName);
            FileInputStream inputStream = openFileInput(tunebookFileName);
            return new TuneBook(StorageUtil.convertStreamToString(inputStream));
        } catch (FileNotFoundException e) {
            Log.i(TAG, String.format("File %s not found, use resource instead",
                    tunebookFileName));
            return loadResourceTunebook();
        }
    }

    private TuneBook loadResourceTunebook() throws Exception {
        Log.i(TAG, String.format("File %s not found, use resource instead",
                tunebookFileName));
        String testText = StorageUtil.convertStreamToString(getResources()
                .openRawResource(R.raw.tunebook1));
        return new TuneBook(testText);
    }

    /**
     * ***********************************************************************
     * Actions
     * ************************************************************************
     */

    private void onAddTune() {
        Log.d(TAG, "Selected action is onAddTune");

        TuneMetadataDialogFragment dialog = TuneMetadataDialogFragment.newInstance();
        dialog.setResultHandler(new TuneMetadataDialogFragment.TuneDialogResultHandler() {
            @Override
            public void onTuneDialogOk(TuneMetadataDialogFragment dialogFragment) {
                Log.d(TAG, "Start editing the tune");
                Tune newTune = new Tune(dialogFragment.getTuneTitle(), dialogFragment.getRhythm(), dialogFragment.getKey());
                displayTuneGridEdit(newTune, dialogFragment.getBarsPerLine());
            }

            @Override
            public void onTuneDialogCancel(TuneMetadataDialogFragment dialogFragment) {
                Log.d(TAG, "Cancelled Tune edition dialog");
            }
        });
        dialog.show(getFragmentManager(), getString(R.string.tune));
    }

    private void onAddSet() {
        Log.d(TAG, "Selected action is onAddSet");
        int currentTabIndex = actionBar.getSelectedNavigationIndex();
        if (currentTabIndex != ACTIONBAR_TUNELIST_INDEX) {
            actionBar.setSelectedNavigationItem(ACTIONBAR_TUNELIST_INDEX);
            Log.d(LogUtils.getTag(), "Switched to tunes list before adding a set");
        }
        ExpandableTunesListFragment fragment = (ExpandableTunesListFragment) adapter
                .getItem(ACTIONBAR_TUNELIST_INDEX);
        fragment.enterNewSet();
    }

    private void onDiscard() {
        Log.d(TAG, "Selected action is onDiscard");
        int currentTabIndex = actionBar.getSelectedNavigationIndex();
        EditableExpandableListFragment fragment = (EditableExpandableListFragment) adapter
                .getItem(currentTabIndex);
        fragment.enterDiscardMode();
    }

    private void onSave() {
        Log.d(TAG, "Selected action is onSave");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        final MainActivity currentContext = this;

        alertDialogBuilder
                .setCancelable(false)
                .setTitle(R.string.email_send_tunebook_dialog_title)
                .setPositiveButton("Submit", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //send email to admin with dialog box values
                        Intent emailIntent = new Intent(Intent.ACTION_SEND);
                        emailIntent.setType("text/plain");
                        //emailIntent.setType("message/rfc822");

                        //set up the recipient address
                        //emailActivity.putExtra(Intent.EXTRA_EMAIL, new String[] { to });

                        //set up the email subject
                        emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.tunebook));

                        //you can specify cc addresses as well
                        // email.putExtra(Intent.EXTRA_CC, new String[]{ ...});
                        // email.putExtra(Intent.EXTRA_BCC, new String[]{ ... });

                        //set up the message body
                        emailIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.email_send_tunebook_message));

                        File file = getFileStreamPath(tunebookFileName);
                        if (!file.exists() || !file.canRead()) {
                            Toast.makeText(currentContext, R.string.email_attachment_error, Toast.LENGTH_SHORT).show();
                            return;
                        }

                        File tempFile = new File(getExternalCacheDir(), tunebookFileName);
                        try {
                            FileUtils.copy(file, tempFile);
                        } catch (IOException e) {
                            Log.e(TAG, "Cannot copy tunebook file to " + tempFile);
                            Log.e(TAG, e.getMessage());
                            return;
                        }

                        Uri uri = Uri.fromFile(tempFile);
                        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);

                        startActivity(Intent.createChooser(emailIntent, getString(R.string.email_select_provider)));
                    }
                });

        alertDialogBuilder.create().show();
    }

    private void onMerge() {
        Log.d(TAG, "Selected action is onMerge");

        // Loading My Tunebook in memory
        TuneBook myTunebook;
        try {
            myTunebook = loadLocalTunebook();
        } catch (Exception e) {
            Log.e(TAG, "Cannot load local tunebook! Skip merge.");
            return;
        }

        // Do merge with current tunebook
        myTunebook.addObserver(new Observer() {
            @Override
            public void update(Observable observable, Object data) {
                if (data instanceof TuneBook.ChangedStatus) {
                    TuneBook.ChangedStatus status = (TuneBook.ChangedStatus) data;
                    if (data == TuneBook.ChangedStatus.MergeComplete) {
                        saveTuneBook((TuneBook) observable, tunebookFileName);
                    }
                }
            }
        });
        myTunebook.mergeAsync(this, tunebook);
    }

    /**
     * Starts a DisplayTuneGridActivity over the current activity for a given tune.
     *
     * @param tune A tune to display.
     */
    public void displayTuneGrid(Tune tune) {
        Intent intent = new Intent(this, DisplayTuneGridActivity.class);
        intent.putExtra(DisplayTuneGridActivity.BUNDLE_KEY_TUNE, tune);
        startActivity(intent);
    }

    /**
     * Starts a DisplayTuneGridActivity over the current activity for edition of a given tune.
     *
     * @param tune A tune to edit.
     */
    public void displayTuneGridEdit(Tune tune) {
        mEditedTune = tune;
        Intent intent = new Intent(this, DisplayTuneGridActivity.class);
        intent.putExtra(DisplayTuneGridActivity.BUNDLE_KEY_TUNE, tune);
        intent.putExtra(DisplayTuneGridActivity.BUNDLE_KEY_EDIT, true);
        startActivityForResult(intent, ACTIVITY_REQUEST_ADD_TUNE);
    }

    /**
     * Starts a DisplayTuneGridActivity over the current activity for edition of a given tune.
     *
     * @param tune        A tune to edit.
     * @param barsPerLine The default number of bars per line.
     */
    public void displayTuneGridEdit(Tune tune, int barsPerLine) {
        Intent intent = new Intent(this, DisplayTuneGridActivity.class);
        intent.putExtra(DisplayTuneGridActivity.BUNDLE_KEY_TUNE, tune);
        intent.putExtra(DisplayTuneGridActivity.BUNDLE_KEY_EDIT, true);
        intent.putExtra(DisplayTuneGridActivity.BUNDLE_KEY_NEW, true);
        intent.putExtra(DisplayTuneGridActivity.BUNDLE_KEY_BARSPERLINE, barsPerLine);
        startActivityForResult(intent, ACTIVITY_REQUEST_ADD_TUNE);
    }
}
