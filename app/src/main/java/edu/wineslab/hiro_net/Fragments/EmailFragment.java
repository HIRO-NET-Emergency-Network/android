package edu.wineslab.hiro_net.Fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import edu.wineslab.hiro_net.R;

public class EmailFragment extends Fragment {

    public static EmailFragment newInstance() {
        EmailFragment fragment = new EmailFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    public EmailFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_email, container, false);
        return rootView;
    }
}