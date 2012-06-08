package com.madgag.agit;

import static com.madgag.agit.GitIntents.REPO_STATE_CHANGED_BROADCAST;
import static com.madgag.agit.GitIntents.actionWithSuffix;
import static com.madgag.agit.R.layout.repo_list_item;
import static com.madgag.agit.RepositoryViewerActivity.manageRepoIntent;
import static com.madgag.android.listviews.ReflectiveHolderFactory.reflectiveFactoryFor;
import static com.madgag.android.listviews.ViewInflator.viewInflatorFor;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import com.madgag.android.listviews.ViewHoldingListAdapter;

import java.util.List;

public class RepoListFragment extends ListLoadingFragment<RepoSummary> {

    private static final String TAG = "RepoListFragment";

    BroadcastReceiver repoStateChangeReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "repoStateChangeReceiver got broadcast : " + intent);
            refresh();
        }
    };

    @Override
    protected ViewHoldingListAdapter<RepoSummary> adapterFor(List<RepoSummary> items) {
        return new ViewHoldingListAdapter<RepoSummary>(items, viewInflatorFor(getActivity(), repo_list_item),
                        reflectiveFactoryFor(RepositoryViewHolder.class));
    }

    @Override
    public Loader<List<RepoSummary>> onCreateLoader(int id, Bundle args) {
        return new AsyncLoader<List<RepoSummary>>(getActivity()) {
            public List<RepoSummary> loadInBackground() {
                return RepoSummary.getAllReposOrderChronologically();
            }
        };
    }

    @Override
    public void onListItemClick(ListView list, View view, int position, long id) {
        RepoSummary repo = (RepoSummary) list.getItemAtPosition(position);
        startActivity(manageRepoIntent(repo.getRepo().getDirectory()));
    }

    public void onResume() {
        super.onResume();
        refresh();
        getActivity().registerReceiver(repoStateChangeReceiver,
                new IntentFilter(actionWithSuffix(REPO_STATE_CHANGED_BROADCAST)));
    }

    public void onPause() {
        super.onPause();
        getActivity().unregisterReceiver(repoStateChangeReceiver);
    }
}
