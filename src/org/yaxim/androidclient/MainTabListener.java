package org.yaxim.androidclient;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;

public class MainTabListener<T extends Fragment> implements ActionBar.TabListener {
	private Fragment mFragment;
	private final Activity mActivity;
	private final String mTag;
	private final Class<T> mClass;

	public MainTabListener(Activity activity, String tag, Class<T> clz) {
		mActivity = activity;
		mTag = tag;
		mClass = clz;
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
	    FragmentManager fragMgr = ((FragmentActivity)mActivity).getSupportFragmentManager();
	    ft = fragMgr.beginTransaction();
	    // Check if the fragment is already initialized
		if (mFragment == null) {
			// If not, instantiate and add it to the activity
			mFragment = Fragment.instantiate(mActivity,	mClass.getName());
			// mFragment.setProviderId(mTag); // id for event provider
			ft.add(android.R.id.content, mFragment, mTag);
		} else {
			// If it exists, simply attach it in order to show it
			ft.attach(mFragment);
		}
		ft.commit();
		fragMgr.executePendingTransactions();
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
	    FragmentManager fragMgr = ((FragmentActivity)mActivity).getSupportFragmentManager();
	    ft = fragMgr.beginTransaction();
		if (mFragment != null) {
			// Detach the fragment, because another one is being attached
			ft.detach(mFragment);
		}
		ft.commit();
		fragMgr.executePendingTransactions();
	}

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
		// TODO Auto-generated method stub
	}
}