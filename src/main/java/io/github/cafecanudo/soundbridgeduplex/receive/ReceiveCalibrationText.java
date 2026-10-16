package io.github.cafecanudo.soundbridgeduplex.receive;

import com.intellij.openapi.util.text.StringUtil;
import io.github.cafecanudo.soundbridgeduplex.calibration.LinkChoice;

import java.util.ArrayList;
import java.util.List;

import static io.github.cafecanudo.soundbridgeduplex.SoundBridgeBundle.message;

final class ReceiveCalibrationText {

    private ReceiveCalibrationText() {
    }

    static List<String> lines(ReceivedCalibration calibration) {
        List<String> lines = new ArrayList<>();
        lines.add(message(calibration.oneWay() ? "receive.calibration.oneWay" : "receive.calibration.twoWay",
                calibration.timestamp()));
        lines.add(message("receive.calibration.arrival", summary(calibration.arrival())));
        if (calibration.canCopy()) {
            lines.add(message("receive.calibration.useOnSender", calibration.txFlags()));
        }
        if (!calibration.oneWay()) {
            lines.add(message("receive.calibration.departure", summary(calibration.departure())));
            lines.add(saved(calibration));
        }
        return lines;
    }

    static String html(ReceivedCalibration calibration) {
        return "<html>" + String.join("<br>", lines(calibration).stream().map(StringUtil::escapeXmlEntities).toList())
                + "</html>";
    }

    private static String summary(LinkChoice choice) {
        return choice == null ? message("receive.calibration.unknown") : choice.summary();
    }

    private static String saved(ReceivedCalibration calibration) {
        if (calibration.savedProfile() != null) {
            return message("receive.calibration.saved", calibration.savedProfile());
        }
        if (calibration.sentProfile() != null) {
            return message("receive.calibration.notSavedMissing", calibration.sentProfile());
        }
        return message("receive.calibration.notSaved");
    }
}
