package com.chordgrid;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.chordgrid.drive.DriveClientManager;
import com.chordgrid.model.TuneBook;
import com.chordgrid.model.TuneSet;
import com.chordgrid.settings.UserSettingsActivity;
import com.chordgrid.tunes.ExpandableTunesListFragment;
import com.chordgrid.tunesets.TuneSetAdapter;
import com.chordgrid.util.StorageUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.StringReader;
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

    private Uri localFileUri;

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

        // Get the action that triggered the intent filter for this Activity
        final Intent intent = getIntent();
        final String action = intent.getAction();

        Log.d(TAG, "Action = " + action);

        // Make sure the Action is DRIVE_OPEN.
        if (ACTION_DRIVE_OPEN.equals(action)) {
            onOpenFromGoogleDrive(intent);
        } else if (ACTION_VIEW.equals(action)) {
            onOpenFromFileView(intent);
        } else {
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
    protected void onStart() {
        super.onStart();
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

        saveTuneBook();

        super.onPause();
    }

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

    /**
     * Handles resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (driveClientManager != null)
            driveClientManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == TuneSetAdapter.ACTIVITY_REQUEST_CODE_REORDER && resultCode == Activity.RESULT_OK) {
            TuneSet tuneSet = (TuneSet) data.getParcelableExtra("tuneset");
            adapter.updateTuneSet(tuneSet);
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
        if (id == R.id.action_add_set) {
            Log.d(TAG, "Action ADD SET");
            onAddSet();
        } else if (id == R.id.action_settings) {
            Log.d(TAG, "Action SETTINGS");
            Intent intent = new Intent(getApplicationContext(),
                    UserSettingsActivity.class);
            startActivity(intent);
        } else if (id == R.id.action_discard) {
            Log.d(TAG, "Action DISCARD");
            onDiscard();
        } else if (id == R.id.action_merge) {
            Log.d(TAG, "Action MERGE");
            onMerge();
        } else {
            Log.d(TAG, String.format("Action item not covered %X", id));
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

    /** Open a file from local file VIEW action. */

    /**
     * Open a file from local file VIEW action. <br/>
     * Expected file types are:
     * <ul>
     * <li>.cgx: XML representation</li>
     * <li>.txt: text representation</li>
     * </ul>
     *
     * @param intent The intent with the data to open.
     */
    private void onOpenFromFileView(final Intent intent) {
        Log.d(TAG, "Opening a file with VIEW action");

        setUseOtherTunebook(intent.getData());
        Log.d(TAG, "URI = " + localFileUri.toString());

        try {
            String path = localFileUri.getPath();
            String fileContents = StorageUtil.getStringFromFile(path);
            int lastDot = path.lastIndexOf('.');
            if (lastDot >= 0) {
                String extension = path.substring(lastDot + 1);
                if ("xml".equalsIgnoreCase(extension))
                    tunebook = parseXmlChordGridString(fileContents);
                else if ("txt".equalsIgnoreCase(extension))
                    tunebook = new TuneBook(fileContents);
                else {
                    showMessage(String.format(
                            "Unexpected file extension '.%s'!", extension));
                    finish();
                }
            } else {
                showMessage(String.format("Unexpected file nature %s!",
                        localFileUri));
                finish();
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot read file " + localFileUri);
            showMessage(getString(R.string.error_drive_read));
        }
    }

    private TuneBook parseXmlChordGridString(final String xml) {
        Log.d(TAG, "Parsing an XML definition");
        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
            XmlPullParser xpp = factory.newPullParser();
            xpp.setInput(new StringReader(xml));
            xpp.next();
            return new TuneBook(xpp);
        } catch (Exception ex) {
            Log.e(TAG, "Cannot parse basic tune book XML file", ex);
            return null;
        }
    }

    /**
     * ***********************************************************************
     * Properties
     * ************************************************************************
     */

    private void setTunebook(TuneBook tunebook) {
        this.tunebook = tunebook;
    }

    /**
     * Returns true iff we are currently displaying My Tunebook (and not
     * another file).
     */
    public boolean usesMyTunebook() {
        return localFileUri == null;
    }

    private void setUseMyTunebook() {
        localFileUri = null;
        invalidateOptionsMenu();
    }

    private void setUseOtherTunebook(Uri uri) {
        localFileUri = uri;
        invalidateOptionsMenu();
    }

    private String getCurrentTunebookFilename() {
        if (localFileUri != null)
            return localFileUri.getPath();
        return tunebookFileName;
    }

    /**
     * ***********************************************************************
     * TabListener implementation
     * ************************************************************************
     */

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
            if (tunebookFileName.endsWith(".xml")) {
                XmlPullParserFactory factory = XmlPullParserFactory
                        .newInstance();
                XmlPullParser parser = factory.newPullParser();
                String xmlString = StorageUtil
                        .convertStreamToString(inputStream);
                parser.setInput(new StringReader(xmlString));
                parser.next();
                return new TuneBook(parser);
            } else {
                return new TuneBook(StorageUtil.convertStreamToString(inputStream));
            }
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

    private void onAddSet() {
        Log.d(TAG, "Selected action is onAddSet");
        int currentTabIndex = actionBar.getSelectedNavigationIndex();
        if (currentTabIndex == 0) {
            ExpandableTunesListFragment fragment = (ExpandableTunesListFragment) adapter
                    .getItem(currentTabIndex);
            fragment.enterNewSet();
        }
    }

    private void onDiscard() {
        Log.d(TAG, "Selected action is onDiscard");
        int currentTabIndex = actionBar.getSelectedNavigationIndex();
        EditableExpandableListFragment fragment = (EditableExpandableListFragment) adapter
                .getItem(currentTabIndex);
        fragment.enterDiscardMode();
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
}
