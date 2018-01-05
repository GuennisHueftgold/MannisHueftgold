package com.semeshky.kvgspotter.adapter;

import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.github.guennishueftgold.trapezeapi.Departure;
import com.github.guennishueftgold.trapezeapi.DepartureStatus;
import com.semeshky.kvgspotter.BR;
import com.semeshky.kvgspotter.R;
import com.semeshky.kvgspotter.databinding.VhStationDepartureBinding;

import org.joda.time.LocalTime;
import org.joda.time.Minutes;
import org.joda.time.format.DateTimeFormat;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class DepartureAdapter extends AbstractDataboundAdapter<Departure, VhStationDepartureBinding> {


    final static int FIVE_MINUTES_IN_SECONDS = 300;
    private final Presenter mPresenter;

    public DepartureAdapter(@NonNull Presenter presenter) {
        this.mPresenter = presenter;
        this.setHasStableIds(true);
    }

    public long getItemId(int position) {
        return Long.parseLong(this.getItem(position).getTripId());
    }

    @Override
    protected VhStationDepartureBinding createBinding(ViewGroup parent, int type) {
        final VhStationDepartureBinding binding = DataBindingUtil
                .inflate(LayoutInflater.from(parent.getContext()), R.layout.vh_station_departure,
                        parent, false);
        binding.setPresenter(this.mPresenter);
        return binding;
    }

    @Override
    protected void bind(VhStationDepartureBinding binding, Departure item) {
        binding.setVariable(BR.departure, item);
        final Resources resources = binding.getRoot().getContext().getResources();
        if ((item.getActualRelativeTime() < 0 || item.getStatus() == DepartureStatus.STATUS_DEPARTED)
                && item.getStatus() != DepartureStatus.STATUS_STOPPING) {
            // IF Bus has left
            binding.setActive(false);
            binding.setSecondaryText(resources.getString(R.string.departed));
            binding.setSecondaryTextAlert(false);
            binding.setSecondaryTextVisible(true);

        } else {
            binding.setActive(true);
            if (item.getStatus() == DepartureStatus.STATUS_STOPPING) {
                // if bus is stopping dont display time just potential delay
                binding.setSecondaryText(resources.getString(R.string.stopping));
                binding.setSecondaryTextAlert(false);
                binding.setSecondaryTextVisible(true);
            } else {
                int delta = 0;
                if (item.getActualTime() != null && item.getPlannedTime() != null) {
                    delta = Minutes.minutesBetween(item.getPlannedTime(), item.getActualTime()).getMinutes();
                }
                binding.setSecondaryTextVisible(delta > 0);
                if (delta > 0) {
                    binding.setSecondaryText(resources.getQuantityString(R.plurals.minutes_delayed, delta, delta));
                    binding.setSecondaryTextAlert(true);
                }
            }
        }
        if (item.getActualRelativeTime() > 0 && item.getActualRelativeTime() < FIVE_MINUTES_IN_SECONDS) {
            final int delay = item.getActualRelativeTime() / 60;
            binding.setDepartureTime(resources.getQuantityString(R.plurals.minutes, delay, delay));
        } else {
            LocalTime localTime = (item.getActualTime() == null ? item.getPlannedTime() : item.getActualTime());
            if (localTime == null) {
                binding.setDepartureTime("--:--");
            } else {
                binding.setDepartureTime(localTime.toString(DateTimeFormat.shortTime()));
            }
        }
    }

    @Override
    protected boolean areItemsTheSame(Departure oldItem, Departure newItem) {
        return oldItem.getTripId().equals(newItem.getTripId()) &&
                oldItem.getRouteId().equals(newItem.getRouteId());
    }

    @Override
    protected boolean areContentsTheSame(Departure oldItem, Departure newItem) {
        return oldItem.getTripId().equals(newItem.getTripId()) &&
                oldItem.getRouteId().equals(newItem.getRouteId()) &&
                compareLocalTime(oldItem.getActualTime(), newItem.getActualTime()) &&
                compareLocalTime(oldItem.getPlannedTime(), newItem.getPlannedTime());
    }

    private final boolean compareLocalTime(LocalTime a, LocalTime b) {
        if (a == null && b == null) {
            return true;
        } else if (a == null) {
            return false;
        } else if (b == null) {
            return false;
        } else {
            return a.equals(b);
        }
    }

    @Override
    public void setItems(List<Departure> departures) {
        Collections.sort(departures,
                new Comparator<Departure>() {
                    @Override
                    public int compare(Departure d1, Departure d2) {
                        LocalTime t1 = d1.getActualTime();
                        LocalTime t2 = d2.getActualTime();
                        if (t1 == null)
                            t1 = d1.getPlannedTime();
                        if (t2 == null)
                            t2 = d2.getPlannedTime();
                        if (t1 == null || t2 == null)
                            return d1.getActualRelativeTime() - d2.getActualRelativeTime();
                        return Minutes.minutesBetween(t2, t1).getMinutes();
                    }
                });
        super.setItems(departures);
    }

    public interface Presenter {
        void onOpenClick(Departure departure);
    }
}
