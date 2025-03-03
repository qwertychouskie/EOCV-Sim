/*
 * Copyright (c) 2021 Sebastian Erives
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.github.serivesmejia.eocvsim.tuner.field;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.gui.component.tuner.TunableFieldPanel;
import com.github.serivesmejia.eocvsim.tuner.TunableField;
import io.github.deltacv.eocvsim.virtualreflect.VirtualField;
import org.openftc.easyopencv.OpenCvPipeline;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

abstract public class NumericField<T extends Number> extends TunableField<T> {

    protected T value;
    protected T beforeValue;

    protected volatile boolean hasChanged = false;

    public NumericField(Object instance, VirtualField reflectionField, EOCVSim eocvSim, AllowMode allowMode) throws IllegalAccessException {
        super(instance, reflectionField, eocvSim, allowMode);
    }

    @Override
    public void init() {
        setRecommendedPanelMode(TunableFieldPanel.Mode.TEXTBOXES);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void update() {
        if (value == null) return;
        value = (T) reflectionField.get();

        hasChanged = hasChanged();

        if (hasChanged) {
            updateGuiFieldValues();
        }
    }

    @Override
    public void updateGuiFieldValues() {
        SwingUtilities.invokeLater(() -> fieldPanel.setFieldValue(0, value));
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public Object getGuiFieldValue(int index) {
        return value;
    }

    @Override
    public boolean hasChanged() {
        boolean hasChanged = value != beforeValue;
        beforeValue = value;
        return hasChanged;
    }


}