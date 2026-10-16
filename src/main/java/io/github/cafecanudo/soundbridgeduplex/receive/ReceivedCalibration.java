package io.github.cafecanudo.soundbridgeduplex.receive;

import io.github.cafecanudo.soundbridgeduplex.calibration.LinkChoice;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public record ReceivedCalibration(String timestamp, boolean oneWay, LinkChoice arrival, LinkChoice departure,
                                  String savedProfile, String sentProfile) {

    public ReceivedCalibration at(String moment) {
        return new ReceivedCalibration(moment, oneWay, arrival, departure, savedProfile, sentProfile);
    }

    public boolean canCopy() {
        return oneWay && arrival != null && arrival.isUsable();
    }

    public String txFlags() {
        if (!canCopy()) {
            return "";
        }
        List<String> flags = new ArrayList<>();
        if (arrival.mono()) {
            flags.add("--mono");
        }
        flags.add("--band-high");
        flags.add(plain(arrival.bandHighHz()));
        flags.add("--peak");
        flags.add(plain(arrival.peak()));
        flags.add("--mcs");
        flags.add(arrival.mcs().toString());
        return String.join(" ", flags);
    }

    private static String plain(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
