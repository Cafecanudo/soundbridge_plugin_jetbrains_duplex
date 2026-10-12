package io.github.cafecanudo.soundbridgeduplex.settings;

import com.intellij.openapi.ui.ComboBox;
import io.github.cafecanudo.soundbridgeduplex.client.AudioDevice;

import javax.swing.DefaultComboBoxModel;
import java.util.List;

final class DeviceComboBox extends ComboBox<Object> {

    private final DefaultComboBoxModel<Object> model = new DefaultComboBoxModel<>();

    DeviceComboBox() {
        setModel(model);
        setEditable(true);
    }

    void show(List<AudioDevice> devices, String value) {
        model.removeAllElements();
        devices.forEach(model::addElement);
        AudioDevice match = devices.stream().filter(d -> d.id().equals(value)).findFirst().orElse(null);
        setSelectedItem(match != null ? match : value);
    }

    void select(String value) {
        for (int i = 0; i < model.getSize(); i++) {
            if (model.getElementAt(i) instanceof AudioDevice device && device.id().equals(value)) {
                setSelectedItem(device);
                return;
            }
        }
        setSelectedItem(value);
    }

    String spec() {
        Object item = getEditor().getItem();
        if (item instanceof AudioDevice device) {
            return device.id();
        }
        String text = item == null ? "" : item.toString();
        int separator = text.indexOf(AudioDevice.LABEL_SEPARATOR);
        return (separator >= 0 ? text.substring(0, separator) : text).strip();
    }
}
