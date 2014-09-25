package com.chordgrid.fileselector;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.chordgrid.R;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.widget.DataBufferAdapter;

public class FileSelectorAdapter extends DataBufferAdapter<Metadata> {

    public FileSelectorAdapter(Context context) {
        super(context, R.layout.selectable_list_item);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = View.inflate(getContext(), R.layout.selectable_list_item, null);
        }
        Metadata metadata = getItem(position);
        RadioButton radioButton = (RadioButton) convertView.findViewById(R.id.selectableListItemRadioButton);
        radioButton.setText(metadata.getTitle());
        return convertView;
    }

}
