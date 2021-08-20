package com.demo.coursemoney.pojo;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Valute {
    @SerializedName("USD")
    @Expose
    private Usd usd;
    @SerializedName("EUR")
    @Expose
    private Eur eur;

    public Usd getUsd() {
        return usd;
    }

    public void setUsd(Usd usd) {
        this.usd = usd;
    }

    public Eur getEur() {
        return eur;
    }

    public void setEur(Eur eur) {
        this.eur = eur;
    }
}
