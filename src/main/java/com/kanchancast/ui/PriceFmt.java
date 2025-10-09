package com.kanchancast.ui;

import java.text.NumberFormat;
import java.util.Locale;

public final class PriceFmt {
    private static final NumberFormat INR =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));

    private PriceFmt() {}

    public static String inr(double value) {
        return INR.format(value);
    }
}
