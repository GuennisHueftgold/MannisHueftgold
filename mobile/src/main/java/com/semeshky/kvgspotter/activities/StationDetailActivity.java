package com.semeshky.kvgspotter.activities;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;

import com.semeshky.kvg.kvgapi.FulltextSearchResult;
import com.semeshky.kvgspotter.BR;
import com.semeshky.kvgspotter.R;
import com.semeshky.kvgspotter.database.Stop;
import com.semeshky.kvgspotter.databinding.ActivityDetailStationBinding;
import com.semeshky.kvgspotter.fragments.StationDeparturesFragment;
import com.semeshky.kvgspotter.fragments.StationDetailsFragment;
import com.semeshky.kvgspotter.viewmodel.StationDetailActivityViewModel;

import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.observers.DisposableSingleObserver;
import timber.log.Timber;

public final class StationDetailActivity extends AppCompatActivity {
    public final static String EXTRA_STATION_SHORT_NAME = StationDetailActivity.class.getName() + ".stop_short_name";
    public static final String EXTRA_STATION_NAME = StationDetailActivity.class.getName() + ".stop_name";
    private final AtomicBoolean mIsRefreshing = new AtomicBoolean(false);
    private ActivityDetailStationBinding mBinding;
    private StationDetailActivityViewModel mViewModel;
    private MenuItem mFavoriteMenuItem;

    public final static Intent createIntent(@NonNull Context context, @NonNull FulltextSearchResult fulltextSearchResult) {
        return StationDetailActivity.createIntent(context,
                fulltextSearchResult.getShortName(),
                fulltextSearchResult.getName());
    }

    public final static Intent createIntent(@NonNull Context context,
                                            @Nullable String shortName,
                                            @Nullable String name) {
        final Intent intent = new Intent(context, StationDetailActivity.class);
        intent.putExtra(EXTRA_STATION_SHORT_NAME, shortName);
        intent.putExtra(EXTRA_STATION_NAME, name);
        return intent;
    }

    public final static Intent createIntent(@NonNull Context context, @NonNull String shortName) {
        return StationDetailActivity.createIntent(context, shortName, null);
    }

    public final static Intent createIntent(@NonNull Context context) {
        return StationDetailActivity.createIntent(context, null, null);
    }

    public static Intent createIntent(Context context, Stop stop) {
        return StationDetailActivity.createIntent(context, stop.getShortName(), stop.getName());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.mBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail_station);
        this.mViewModel = ViewModelProviders.of(this)
                .get(StationDetailActivityViewModel.class);
        final Bundle extras = this.getIntent().getExtras();
        for (String key : extras.keySet()) {
            Timber.d(key + " value: " + extras.get(key));
        }
        this.mViewModel.stationName.set(extras.getString(EXTRA_STATION_NAME, getString(R.string.station_name)));
        this.mViewModel.stationShortName.set(extras.getString(EXTRA_STATION_SHORT_NAME, ""));
        this.setSupportActionBar(this.mBinding.toolbar);
        this.mBinding.setVariable(BR.viewModel, this.mViewModel);
        this.mBinding.viewPager.setAdapter(new StationDetailActivity.PagerAdapter(getSupportFragmentManager()));
        this.mBinding.tabLayout.setupWithViewPager(this.mBinding.viewPager);
        this.mViewModel.isStationFavorited()
                .observe(this,
                        new Observer<Boolean>() {
                            @Override
                            public void onChanged(@Nullable Boolean aBoolean) {
                                StationDetailActivity.this
                                        .setFavoriteDrawable(aBoolean);
                            }
                        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //this.mViewModel.updateData();
        this.mViewModel.startSyncService();
    }

    @Override
    public void onPause() {
        this.mViewModel.stopSyncService();
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.station_detail, menu);
        this.mFavoriteMenuItem = menu.findItem(R.id.action_favorize);
        /*SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        final SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);*/
        return true;
    }

    private AnimatedVectorDrawableCompat getAnimatedVectorDrawable(@DrawableRes int id) {
        return AnimatedVectorDrawableCompat.create(this, id);
    }

    private void setFavoriteDrawable(final boolean isFavorited) {
        if (isFavorited) {
            AnimatedVectorDrawableCompat drawableCompat = getAnimatedVectorDrawable(R.drawable.ic_favorite_animated_24dp);
            this.mFavoriteMenuItem.setIcon(drawableCompat);
            drawableCompat.start();
        } else {
            AnimatedVectorDrawableCompat drawableCompat = getAnimatedVectorDrawable(R.drawable.ic_unfavorite_animated_24dp);
            this.mFavoriteMenuItem.setIcon(drawableCompat);
            drawableCompat.start();
        }
    }

    private void switchFavorite() {
        this.mViewModel.toggleFavorite()
                .subscribe(new DisposableSingleObserver<Boolean>() {
                    @Override
                    public void onSuccess(Boolean aBoolean) {
                        Timber.d("Successfully liked: " + aBoolean);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }
                });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_favorize:
                this.switchFavorite();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return new StationDeparturesFragment();
                case 1:
                    return new StationDetailsFragment();
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Departures";
                case 1:
                    return "Map";
                default:
                    return null;
            }
        }
    }
}
