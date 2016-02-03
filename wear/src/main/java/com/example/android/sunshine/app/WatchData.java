package com.example.android.sunshine.app;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WatchData {

    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("hh:mm");
    private DateFormat DATE_FORMAT = DateFormat.getDateInstance();
    private static final DateFormat DAY_OF_WEEK_FORMAT = new SimpleDateFormat("E", Locale.getDefault());

    String time;
    String date;

    public WatchData() {
        Date now = new Date();

        time = TIME_FORMAT.format(now);
        date = DAY_OF_WEEK_FORMAT.format(now) + " " + DATE_FORMAT.format(now);
    }
}
