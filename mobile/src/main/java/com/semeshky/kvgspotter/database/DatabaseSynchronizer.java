package com.semeshky.kvgspotter.database;

import android.content.Context;

import com.github.guennishueftgold.trapezeapi.StationLocations;
import com.github.guennishueftgold.trapezeapi.StopPoints;
import com.semeshky.kvgspotter.api.KvgApiClient;

import java.io.IOException;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiFunction;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Response;

public class DatabaseSynchronizer {

    public static Single<Integer> synchronizeStops() {
        return synchronizeStops(null);
    }
    public static Single<Integer> synchronizeStops(Context context) {
        if (context != null) {
            KvgApiClient.init(context);
            AppDatabase.init(context);
        }
        return Single.create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> emitter) throws Exception {
                try {
                    final Response<StationLocations> response = KvgApiClient
                            .getService()
                            .getStationLocations()
                            .execute();
                    if (response.code() != 200) {
                        emitter.onError(new Exception("Couldnt reach server with error:" + response.code()));
                        return;
                    }
                    AppDatabase.getInstance()
                            .stopDao()
                            .insertAll(Stop.create(response.body().getStops()));
                    emitter.onSuccess(response.body().getStops().size());
                } catch (RuntimeException | IOException ex) {
                    emitter.onError(ex);
                }
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    /**
     * Checks if database entries are present
     *
     * @return
     */
    public static Single<Boolean> isDataSynchronized() {
        final Single<Integer> stops = Single.create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> emitter) throws Exception {
                try {
                    final StopDao stopDao = AppDatabase.getInstance()
                            .stopDao();
                    emitter.onSuccess(stopDao.countStops());
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        });
        final Single<Integer> stopPoints = Single.create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> emitter) throws Exception {
                try {
                    final StopPointDao stopDao = AppDatabase.getInstance()
                            .stopPointDao();
                    emitter.onSuccess(stopDao.countStops());
                } catch (Exception e) {
                    emitter.onError(e);
                }
            }
        });
        return Single.zip(stops,
                stopPoints,
                new BiFunction<Integer, Integer, Boolean>() {
                    @Override
                    public Boolean apply(Integer integer, Integer integer2) throws Exception {
                        return integer > 0 && integer2 > 0;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread());

    }

    public static Single<Integer> synchronizeStopPoints(Context context) {
        if (context != null) {
            KvgApiClient.init(context);
            AppDatabase.init(context);
        }
        return Single.create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> emitter) throws Exception {
                try {
                    final Response<StopPoints> response = KvgApiClient
                            .getService()
                            .getStopPoints()
                            .execute();
                    if (response.code() != 200) {
                        emitter.onError(new Exception("Couldnt reach server with error:" + response.code()));
                        return;
                    }
                    AppDatabase
                                .getInstance()
                                .stopPointDao()
                            .insertAll(StopPoint.create(response.body().getStopPoints()));
                    emitter.onSuccess(response.body().getStopPoints().size());
                } catch (RuntimeException | IOException ex) {
                    emitter.onError(ex);
                }
            }
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread());
    }

    public static Single<Integer> synchronizeStopPoints() {
        return synchronizeStopPoints(null);
    }
}
