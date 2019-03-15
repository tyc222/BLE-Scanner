package com.example.ble_scanner_test_app;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.ArrayList;


/**
 * A simple {@link Fragment} subclass.
 */
public class MessageWithDevice extends Fragment {

    /**
     Variable Storage for the App
     */

    // Edit text view from the user
    static EditText inputText;

    // Define a string adapter which will handle the data of the response listview
    static ArrayAdapter<String> listViewAdapterResponse;

    public MessageWithDevice() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_message_with_device, container, false);

        // List of Array Strings which will serve as response list items
        ArrayList<String> listItemResponses = new ArrayList<String>();

        // set up an adapter for response listview
        listViewAdapterResponse = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, listItemResponses);

        // set the data to our response listview
        ListView responseList = (ListView) rootView.findViewById(R.id.list_view_ble_device_response);
        responseList.setAdapter(listViewAdapterResponse);

        // Find the edit text box for inputText
        inputText = (EditText) rootView.findViewById(R.id.inputEditText);

        /**
         Buttons
         */

        // OnClick for the send message button, which sends cmd to BLE deices
        Button sendButton = rootView.findViewById(R.id.sendButton);
        sendButton.setOnClickListener(v -> ScanDevice.write(getActivity()));
        // OnClick for the disconnect button, which restarts the activity
        Button disconnectButton = rootView.findViewById(R.id.disconnectButton);
        disconnectButton.setOnClickListener(v -> {
            ScanDevice.close();
            getActivity().recreate();
        });

        return rootView;
    }
}
