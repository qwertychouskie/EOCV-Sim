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

package com.github.serivesmejia.eocvsim.tuner.field.numeric;

import com.github.serivesmejia.eocvsim.EOCVSim;
import com.github.serivesmejia.eocvsim.tuner.field.NumericField;
import com.github.serivesmejia.eocvsim.tuner.scanner.RegisterTunableField;
import io.github.deltacv.eocvsim.virtualreflect.VirtualField;
import org.openftc.easyopencv.OpenCvPipeline;

import java.lang.reflect.Field;

@RegisterTunableField
public class DoubleField extends NumericField<Double> {

    public DoubleField(Object instance, VirtualField reflectionField, EOCVSim eocvSim) throws IllegalAccessException {
        super(instance, reflectionField, eocvSim, AllowMode.ONLY_NUMBERS_DECIMAL);
        value = (double) initialFieldValue;
    }

    @Override
    public void setFieldValueFromGui(int index, String newValue) throws IllegalAccessException {
        try {
            value = Double.valueOf(newValue);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Parameter should be a valid numeric String");
        }

        setFieldValue(index, value);
        beforeValue = value;
    }

    @Override
    public void setFieldValue(int index, Object value) throws IllegalAccessException {
        if(value instanceof Number) {
            this.value = ((Number) value).doubleValue();
        } else {
            this.value = (double)value;
        }
        setPipelineFieldValue(this.value);
    }

}