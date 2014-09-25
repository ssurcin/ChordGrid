package com.chordgrid.fileselector;

import android.app.ListActivity;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.util.Log;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;
import android.widget.Toast;

import com.chordgrid.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveApi.MetadataBufferResult;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;
import com.google.android.gms.drive.widget.DataBufferAdapter;

public class SelectGoogleDriveFileActivity extends ListActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "com.chordgrid.fileselector.SelectGoogleDriveFileActivity";

    private DataBufferAdapter<Metadata> resultsAdapter;
    private String nextPageToken;
    private boolean hasMore;
    private String fileExtension;

    private DriveId chordGridFolderId = null;

    /**
     * Request code for auto Google Play Services error resolution.
     */
    protected static final int REQUEST_CODE_RESOLUTION = 1;

    /**
     * Next available request code.
     */
    protected static final int NEXT_AVAILABLE_REQUEST_CODE = 2;

    /**
     * Google API client.
     */
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_file);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            fileExtension = extras.getString("fileExtension");
        }

        hasMore = true;

        ListView listView = getListView();
        resultsAdapter = new FileSelectorAdapter(this);
        listView.setAdapter(resultsAdapter);
        listView.setOnScrollListener(new OnScrollListener() {

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem,
                                 int visibleItemCount, int totalItemCount) {
                if (nextPageToken != null
                        && firstVisibleItem + visibleItemCount + 5 < totalItemCount) {
                    retrieveNextPage();
                }
            }
        });
    }

    /**
     * Called when activity gets visible. A connection to Drive services need to
     * be initiated as soon as the activity is visible. Registers
     * {@code ConnectionCallbacks} and {@code OnConnectionFailedListener} on the
     * activities itself.
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addApi(Drive.API).addScope(Drive.SCOPE_FILE)
                    .addScope(Drive.SCOPE_APPFOLDER)
                            // required for App Folder sample
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this).build();
        }
        googleApiClient.connect();
    }

    /**
     * Handles resolution callbacks.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            googleApiClient.connect();
        }
    }

    /**
     * Called when activity gets invisible. Connection to Drive service needs to
     * be disconnected as soon as an activity is invisible.
     */
    @Override
    protected void onPause() {
        if (googleApiClient != null) {
            googleApiClient.disconnect();
        }
        super.onPause();
    }

    /**
     * Called when {@code googleApiClient} is connected.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        if (chordGridFolderId == null) {
            Log.d(TAG, "Looking for CHORDGRID folder in Google Drive");
            DriveFolder rootFolder = Drive.DriveApi.getRootFolder(googleApiClient);
            Log.d(TAG, "Starting at root folder " + rootFolder.toString());
            rootFolder.listChildren(googleApiClient).setResultCallback(lookForChordGridFolderCallback);
        }
    }

    private final ResultCallback<MetadataBufferResult> lookForChordGridFolderCallback = new ResultCallback<DriveApi.MetadataBufferResult>() {

        @Override
        public void onResult(MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Problem while retrieving files");
                return;
            }
            MetadataBuffer buffer = result.getMetadataBuffer();
            int childCount = buffer.getCount();
            for (int i = 0; i < childCount; i++) {
                if (chordGridFolderId != null) {
                    Log.d(TAG, "CHORDGRID folder has been found, leave enumeration of folder children");
                    return;
                }
                Metadata metadata = buffer.get(i);
                if (metadata.isFolder()) {
                    if ("CHORDGRID".equalsIgnoreCase(metadata.getTitle())) {
                        Log.d(TAG, "CHORDGRID folder found!");
                        chordGridFolderId = metadata.getDriveId();
                        return;
                    }
                    Log.d(TAG, "Looking for CHORDGRID folder under " + metadata.getTitle());
                    DriveFolder folder = Drive.DriveApi.getFolder(googleApiClient, metadata.getDriveId());
                    folder.listChildren(googleApiClient).setResultCallback(lookForChordGridFolderCallback);
                }
            }
        }
    };

    /**
     * Called when {@code googleApiClient} is disconnected.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    /**
     * Called when {@code googleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution is
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
        if (!result.hasResolution()) {
            // show the localized error dialog.
            GooglePlayServicesUtil.getErrorDialog(result.getErrorCode(), this,
                    0).show();
            return;
        }
        try {
            result.startResolutionForResult(this, REQUEST_CODE_RESOLUTION);
        } catch (SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
    }

    /**
     * Shows a toast message.
     */
    public void showMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    /**
     * Getter for the {@code GoogleApiClient}.
     */
    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    /**
     * Clears the result buffer to avoid memory leaks as soon as the activity is
     * no longer visible by the user.
     */
    @Override
    protected void onStop() {
        super.onStop();
        resultsAdapter.clear();
    }

    /**
     * Retrieves results for the next page. For the first run, it retrieves
     * results for the first page.
     */
    private void retrieveNextPage() {
        // if there are no more results to retrieve,
        // return silently.
        if (!hasMore)
            return;

        // retrieve the results for the next page.
        SortOrder sortOrder = new SortOrder.Builder()
                .addSortAscending(SortableField.TITLE)
                .addSortDescending(SortableField.MODIFIED_DATE).build();
        Query query = new Query.Builder()
                .addFilter(
                        Filters.and(Filters.eq(SearchableField.MIME_TYPE,
                                "text/plain"), Filters.contains(
                                SearchableField.TITLE, fileExtension)))
                .setSortOrder(sortOrder).setPageToken(nextPageToken).build();
        Drive.DriveApi.query(getGoogleApiClient(), query).setResultCallback(
                metadataBufferCallback);
    }

    /**
     * Appends the retrieved results to the result buffer.
     */
    private final ResultCallback<MetadataBufferResult> metadataBufferCallback = new ResultCallback<MetadataBufferResult>() {
        @Override
        public void onResult(MetadataBufferResult result) {
            if (!result.getStatus().isSuccess()) {
                showMessage("Problem while retrieving files");
                return;
            }
            resultsAdapter.append(result.getMetadataBuffer());
            nextPageToken = result.getMetadataBuffer().getNextPageToken();
            hasMore = nextPageToken != null;
        }
    };
}
