package edu.wineslab.hiro_net;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import edu.wineslab.hiro_net.Fragments.ChatFragment;
import edu.wineslab.hiro_net.Fragments.EmailFragment;
import edu.wineslab.hiro_net.Fragments.TweetFragment;

/**
 * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class SectionsPagerAdapter extends FragmentPagerAdapter {
    private Context mContext;

    private SectionsPagerAdapter(FragmentManager fm, Context context) {
        super(fm);
        mContext = context;
    }

    @Override
    public Fragment getItem(int position) {
        // getItem is called to instantiate the fragment for the given page.
        if (position == 0) {
            return EmailFragment.newInstance();
        } else if (position == 1) {
            return ChatFragment.newInstance();
        } else {
            return TweetFragment.newInstance();
        }
    }

    @Override
    public int getCount() {
        // Show 3 total pages.
        return 3;
    }

    // This determines the title for each tab
    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        switch (position) {
            case 0:
                return mContext.getString(R.string.mail_tab);
            case 1:
                return mContext.getString(R.string.chat_tab);
            case 2:
                return mContext.getString(R.string.tweet_tab);
            default:
                return null;
        }
    }
}
