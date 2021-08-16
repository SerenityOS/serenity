/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package com.sun.media.sound;

/**
 * Infinite impulse response (IIR) filter class.
 *
 * The filters where implemented and adapted using algorithms from musicdsp.org
 * archive: 1-RC and C filter, Simple 2-pole LP LP and HP filter, biquad,
 * tweaked butterworth RBJ Audio-EQ-Cookbook, EQ filter kookbook
 *
 * @author Karl Helgason
 */
public final class SoftFilter {

    public static final int FILTERTYPE_LP6 = 0x00;
    public static final int FILTERTYPE_LP12 = 0x01;
    public static final int FILTERTYPE_HP12 = 0x11;
    public static final int FILTERTYPE_BP12 = 0x21;
    public static final int FILTERTYPE_NP12 = 0x31;
    public static final int FILTERTYPE_LP24 = 0x03;
    public static final int FILTERTYPE_HP24 = 0x13;

    //
    // 0x0 = 1st-order, 6 dB/oct
    // 0x1 = 2nd-order, 12 dB/oct
    // 0x2 = 3rd-order, 18 dB/oct
    // 0x3 = 4th-order, 24 db/oct
    //
    // 0x00 = LP, Low Pass Filter
    // 0x10 = HP, High Pass Filter
    // 0x20 = BP, Band Pass Filter
    // 0x30 = NP, Notch or Band Elimination Filter
    //
    private int filtertype = FILTERTYPE_LP6;
    private final float samplerate;
    private float x1;
    private float x2;
    private float y1;
    private float y2;
    private float xx1;
    private float xx2;
    private float yy1;
    private float yy2;
    private float a0;
    private float a1;
    private float a2;
    private float b1;
    private float b2;
    private float q;
    private float gain = 1;
    private float wet = 0;
    private float last_wet = 0;
    private float last_a0;
    private float last_a1;
    private float last_a2;
    private float last_b1;
    private float last_b2;
    private float last_q;
    private float last_gain;
    private boolean last_set = false;
    private double cutoff = 44100;
    private double resonancedB = 0;
    private boolean dirty = true;

    public SoftFilter(float samplerate) {
        this.samplerate = samplerate;
        dirty = true;
    }

    public void setFrequency(double cent) {
        if (cutoff == cent)
            return;
        cutoff = cent;
        dirty = true;
    }

    public void setResonance(double db) {
        if (resonancedB == db)
            return;
        resonancedB = db;
        dirty = true;
    }

    public void reset() {
        dirty = true;
        last_set = false;
        x1 = 0;
        x2 = 0;
        y1 = 0;
        y2 = 0;
        xx1 = 0;
        xx2 = 0;
        yy1 = 0;
        yy2 = 0;
        wet = 0.0f;
        gain = 1.0f;
        a0 = 0;
        a1 = 0;
        a2 = 0;
        b1 = 0;
        b2 = 0;
    }

    public void setFilterType(int filtertype) {
        this.filtertype = filtertype;
    }

    public void processAudio(SoftAudioBuffer sbuffer) {
        if (filtertype == FILTERTYPE_LP6)
            filter1(sbuffer);
        if (filtertype == FILTERTYPE_LP12)
            filter2(sbuffer);
        if (filtertype == FILTERTYPE_HP12)
            filter2(sbuffer);
        if (filtertype == FILTERTYPE_BP12)
            filter2(sbuffer);
        if (filtertype == FILTERTYPE_NP12)
            filter2(sbuffer);
        if (filtertype == FILTERTYPE_LP24)
            filter4(sbuffer);
        if (filtertype == FILTERTYPE_HP24)
            filter4(sbuffer);
    }

    public void filter4(SoftAudioBuffer sbuffer) {

        float[] buffer = sbuffer.array();

        if (dirty) {
            filter2calc();
            dirty = false;
        }
        if (!last_set) {
            last_a0 = a0;
            last_a1 = a1;
            last_a2 = a2;
            last_b1 = b1;
            last_b2 = b2;
            last_gain = gain;
            last_wet = wet;
            last_set = true;
        }

        if (wet > 0 || last_wet > 0) {

            int len = buffer.length;
            float a0 = this.last_a0;
            float a1 = this.last_a1;
            float a2 = this.last_a2;
            float b1 = this.last_b1;
            float b2 = this.last_b2;
            float gain = this.last_gain;
            float wet = this.last_wet;
            float a0_delta = (this.a0 - this.last_a0) / len;
            float a1_delta = (this.a1 - this.last_a1) / len;
            float a2_delta = (this.a2 - this.last_a2) / len;
            float b1_delta = (this.b1 - this.last_b1) / len;
            float b2_delta = (this.b2 - this.last_b2) / len;
            float gain_delta = (this.gain - this.last_gain) / len;
            float wet_delta = (this.wet - this.last_wet) / len;
            float x1 = this.x1;
            float x2 = this.x2;
            float y1 = this.y1;
            float y2 = this.y2;
            float xx1 = this.xx1;
            float xx2 = this.xx2;
            float yy1 = this.yy1;
            float yy2 = this.yy2;

            if (wet_delta != 0) {
                for (int i = 0; i < len; i++) {
                    a0 += a0_delta;
                    a1 += a1_delta;
                    a2 += a2_delta;
                    b1 += b1_delta;
                    b2 += b2_delta;
                    gain += gain_delta;
                    wet += wet_delta;
                    float x = buffer[i];
                    float y = (a0*x + a1*x1 + a2*x2 - b1*y1 - b2*y2);
                    float xx = (y * gain) * wet + (x) * (1 - wet);
                    x2 = x1;
                    x1 = x;
                    y2 = y1;
                    y1 = y;
                    float yy = (a0*xx + a1*xx1 + a2*xx2 - b1*yy1 - b2*yy2);
                    buffer[i] = (yy * gain) * wet + (xx) * (1 - wet);
                    xx2 = xx1;
                    xx1 = xx;
                    yy2 = yy1;
                    yy1 = yy;
                }
            } else if (a0_delta == 0 && a1_delta == 0 && a2_delta == 0
                    && b1_delta == 0 && b2_delta == 0) {
                for (int i = 0; i < len; i++) {
                    float x = buffer[i];
                    float y = (a0*x + a1*x1 + a2*x2 - b1*y1 - b2*y2);
                    float xx = (y * gain) * wet + (x) * (1 - wet);
                    x2 = x1;
                    x1 = x;
                    y2 = y1;
                    y1 = y;
                    float yy = (a0*xx + a1*xx1 + a2*xx2 - b1*yy1 - b2*yy2);
                    buffer[i] = (yy * gain) * wet + (xx) * (1 - wet);
                    xx2 = xx1;
                    xx1 = xx;
                    yy2 = yy1;
                    yy1 = yy;
                }
            } else {
                for (int i = 0; i < len; i++) {
                    a0 += a0_delta;
                    a1 += a1_delta;
                    a2 += a2_delta;
                    b1 += b1_delta;
                    b2 += b2_delta;
                    gain += gain_delta;
                    float x = buffer[i];
                    float y = (a0*x + a1*x1 + a2*x2 - b1*y1 - b2*y2);
                    float xx = (y * gain) * wet + (x) * (1 - wet);
                    x2 = x1;
                    x1 = x;
                    y2 = y1;
                    y1 = y;
                    float yy = (a0*xx + a1*xx1 + a2*xx2 - b1*yy1 - b2*yy2);
                    buffer[i] = (yy * gain) * wet + (xx) * (1 - wet);
                    xx2 = xx1;
                    xx1 = xx;
                    yy2 = yy1;
                    yy1 = yy;
                }
            }

            if (Math.abs(x1) < 1.0E-8)
                x1 = 0;
            if (Math.abs(x2) < 1.0E-8)
                x2 = 0;
            if (Math.abs(y1) < 1.0E-8)
                y1 = 0;
            if (Math.abs(y2) < 1.0E-8)
                y2 = 0;
            this.x1 = x1;
            this.x2 = x2;
            this.y1 = y1;
            this.y2 = y2;
            this.xx1 = xx1;
            this.xx2 = xx2;
            this.yy1 = yy1;
            this.yy2 = yy2;
        }

        this.last_a0 = this.a0;
        this.last_a1 = this.a1;
        this.last_a2 = this.a2;
        this.last_b1 = this.b1;
        this.last_b2 = this.b2;
        this.last_gain = this.gain;
        this.last_wet = this.wet;

    }

    private double sinh(double x) {
        return (Math.exp(x) - Math.exp(-x)) * 0.5;
    }

    public void filter2calc() {

        double resonancedB = this.resonancedB;
        if (resonancedB < 0)
            resonancedB = 0;    // Negative dB are illegal.
        if (resonancedB > 30)
            resonancedB = 30;   // At least 22.5 dB is needed.
        if (filtertype == FILTERTYPE_LP24 || filtertype == FILTERTYPE_HP24)
            resonancedB *= 0.6;

        if (filtertype == FILTERTYPE_BP12) {
            wet = 1;
            double r = (cutoff / samplerate);
            if (r > 0.45)
                r = 0.45;

            double bandwidth = Math.PI * Math.pow(10.0, -(resonancedB / 20));

            double omega = 2 * Math.PI * r;
            double cs = Math.cos(omega);
            double sn = Math.sin(omega);
            double alpha = sn * sinh((Math.log(2)*bandwidth*omega) / (sn * 2));

            double b0 = alpha;
            double b1 = 0;
            double b2 = -alpha;
            double a0 = 1 + alpha;
            double a1 = -2 * cs;
            double a2 = 1 - alpha;

            double cf = 1.0 / a0;
            this.b1 = (float) (a1 * cf);
            this.b2 = (float) (a2 * cf);
            this.a0 = (float) (b0 * cf);
            this.a1 = (float) (b1 * cf);
            this.a2 = (float) (b2 * cf);
        }

        if (filtertype == FILTERTYPE_NP12) {
            wet = 1;
            double r = (cutoff / samplerate);
            if (r > 0.45)
                r = 0.45;

            double bandwidth = Math.PI * Math.pow(10.0, -(resonancedB / 20));

            double omega = 2 * Math.PI * r;
            double cs = Math.cos(omega);
            double sn = Math.sin(omega);
            double alpha = sn * sinh((Math.log(2)*bandwidth*omega) / (sn*2));

            double b0 = 1;
            double b1 = -2 * cs;
            double b2 = 1;
            double a0 = 1 + alpha;
            double a1 = -2 * cs;
            double a2 = 1 - alpha;

            double cf = 1.0 / a0;
            this.b1 = (float)(a1 * cf);
            this.b2 = (float)(a2 * cf);
            this.a0 = (float)(b0 * cf);
            this.a1 = (float)(b1 * cf);
            this.a2 = (float)(b2 * cf);
        }

        if (filtertype == FILTERTYPE_LP12 || filtertype == FILTERTYPE_LP24) {
            double r = (cutoff / samplerate);
            if (r > 0.45) {
                if (wet == 0) {
                    if (resonancedB < 0.00001)
                        wet = 0.0f;
                    else
                        wet = 1.0f;
                }
                r = 0.45;
            } else
                wet = 1.0f;

            double c = 1.0 / (Math.tan(Math.PI * r));
            double csq = c * c;
            double resonance = Math.pow(10.0, -(resonancedB / 20));
            double q = Math.sqrt(2.0f) * resonance;
            double a0 = 1.0 / (1.0 + (q * c) + (csq));
            double a1 = 2.0 * a0;
            double a2 = a0;
            double b1 = (2.0 * a0) * (1.0 - csq);
            double b2 = a0 * (1.0 - (q * c) + csq);

            this.a0 = (float)a0;
            this.a1 = (float)a1;
            this.a2 = (float)a2;
            this.b1 = (float)b1;
            this.b2 = (float)b2;

        }

        if (filtertype == FILTERTYPE_HP12 || filtertype == FILTERTYPE_HP24) {
            double r = (cutoff / samplerate);
            if (r > 0.45)
                r = 0.45;
            if (r < 0.0001)
                r = 0.0001;
            wet = 1.0f;
            double c = (Math.tan(Math.PI * (r)));
            double csq = c * c;
            double resonance = Math.pow(10.0, -(resonancedB / 20));
            double q = Math.sqrt(2.0f) * resonance;
            double a0 = 1.0 / (1.0 + (q * c) + (csq));
            double a1 = -2.0 * a0;
            double a2 = a0;
            double b1 = (2.0 * a0) * (csq - 1.0);
            double b2 = a0 * (1.0 - (q * c) + csq);

            this.a0 = (float)a0;
            this.a1 = (float)a1;
            this.a2 = (float)a2;
            this.b1 = (float)b1;
            this.b2 = (float)b2;

        }

    }

    public void filter2(SoftAudioBuffer sbuffer) {

        float[] buffer = sbuffer.array();

        if (dirty) {
            filter2calc();
            dirty = false;
        }
        if (!last_set) {
            last_a0 = a0;
            last_a1 = a1;
            last_a2 = a2;
            last_b1 = b1;
            last_b2 = b2;
            last_q = q;
            last_gain = gain;
            last_wet = wet;
            last_set = true;
        }

        if (wet > 0 || last_wet > 0) {

            int len = buffer.length;
            float a0 = this.last_a0;
            float a1 = this.last_a1;
            float a2 = this.last_a2;
            float b1 = this.last_b1;
            float b2 = this.last_b2;
            float gain = this.last_gain;
            float wet = this.last_wet;
            float a0_delta = (this.a0 - this.last_a0) / len;
            float a1_delta = (this.a1 - this.last_a1) / len;
            float a2_delta = (this.a2 - this.last_a2) / len;
            float b1_delta = (this.b1 - this.last_b1) / len;
            float b2_delta = (this.b2 - this.last_b2) / len;
            float gain_delta = (this.gain - this.last_gain) / len;
            float wet_delta = (this.wet - this.last_wet) / len;
            float x1 = this.x1;
            float x2 = this.x2;
            float y1 = this.y1;
            float y2 = this.y2;

            if (wet_delta != 0) {
                for (int i = 0; i < len; i++) {
                    a0 += a0_delta;
                    a1 += a1_delta;
                    a2 += a2_delta;
                    b1 += b1_delta;
                    b2 += b2_delta;
                    gain += gain_delta;
                    wet += wet_delta;
                    float x = buffer[i];
                    float y = (a0*x + a1*x1 + a2*x2 - b1*y1 - b2*y2);
                    buffer[i] = (y * gain) * wet + (x) * (1 - wet);
                    x2 = x1;
                    x1 = x;
                    y2 = y1;
                    y1 = y;
                }
            } else if (a0_delta == 0 && a1_delta == 0 && a2_delta == 0
                    && b1_delta == 0 && b2_delta == 0) {
                for (int i = 0; i < len; i++) {
                    float x = buffer[i];
                    float y = (a0*x + a1*x1 + a2*x2 - b1*y1 - b2*y2);
                    buffer[i] = y * gain;
                    x2 = x1;
                    x1 = x;
                    y2 = y1;
                    y1 = y;
                }
            } else {
                for (int i = 0; i < len; i++) {
                    a0 += a0_delta;
                    a1 += a1_delta;
                    a2 += a2_delta;
                    b1 += b1_delta;
                    b2 += b2_delta;
                    gain += gain_delta;
                    float x = buffer[i];
                    float y = (a0*x + a1*x1 + a2*x2 - b1*y1 - b2*y2);
                    buffer[i] = y * gain;
                    x2 = x1;
                    x1 = x;
                    y2 = y1;
                    y1 = y;
                }
            }

            if (Math.abs(x1) < 1.0E-8)
                x1 = 0;
            if (Math.abs(x2) < 1.0E-8)
                x2 = 0;
            if (Math.abs(y1) < 1.0E-8)
                y1 = 0;
            if (Math.abs(y2) < 1.0E-8)
                y2 = 0;
            this.x1 = x1;
            this.x2 = x2;
            this.y1 = y1;
            this.y2 = y2;
        }

        this.last_a0 = this.a0;
        this.last_a1 = this.a1;
        this.last_a2 = this.a2;
        this.last_b1 = this.b1;
        this.last_b2 = this.b2;
        this.last_q = this.q;
        this.last_gain = this.gain;
        this.last_wet = this.wet;

    }

    public void filter1calc() {
        if (cutoff < 120)
            cutoff = 120;
        double c = (7.0 / 6.0) * Math.PI * 2 * cutoff / samplerate;
        if (c > 1)
            c = 1;
        a0 = (float)(Math.sqrt(1 - Math.cos(c)) * Math.sqrt(0.5 * Math.PI));
        if (resonancedB < 0)
            resonancedB = 0;
        if (resonancedB > 20)
            resonancedB = 20;
        q = (float)(Math.sqrt(0.5) * Math.pow(10.0, -(resonancedB / 20)));
        gain = (float)Math.pow(10, -((resonancedB)) / 40.0);
        if (wet == 0.0f)
            if (resonancedB > 0.00001 || c < 0.9999999)
                wet = 1.0f;
    }

    public void filter1(SoftAudioBuffer sbuffer) {

        if (dirty) {
            filter1calc();
            dirty = false;
        }
        if (!last_set) {
            last_a0 = a0;
            last_q = q;
            last_gain = gain;
            last_wet = wet;
            last_set = true;
        }

        if (wet > 0 || last_wet > 0) {

            float[] buffer = sbuffer.array();
            int len = buffer.length;
            float a0 = this.last_a0;
            float q = this.last_q;
            float gain = this.last_gain;
            float wet = this.last_wet;
            float a0_delta = (this.a0 - this.last_a0) / len;
            float q_delta = (this.q - this.last_q) / len;
            float gain_delta = (this.gain - this.last_gain) / len;
            float wet_delta = (this.wet - this.last_wet) / len;
            float y2 = this.y2;
            float y1 = this.y1;

            if (wet_delta != 0) {
                for (int i = 0; i < len; i++) {
                    a0 += a0_delta;
                    q += q_delta;
                    gain += gain_delta;
                    wet += wet_delta;
                    float ga0 = (1 - q * a0);
                    y1 = ga0 * y1 + (a0) * (buffer[i] - y2);
                    y2 = ga0 * y2 + (a0) * y1;
                    buffer[i] = y2 * gain * wet + buffer[i] * (1 - wet);
                }
            } else if (a0_delta == 0 && q_delta == 0) {
                float ga0 = (1 - q * a0);
                for (int i = 0; i < len; i++) {
                    y1 = ga0 * y1 + (a0) * (buffer[i] - y2);
                    y2 = ga0 * y2 + (a0) * y1;
                    buffer[i] = y2 * gain;
                }
            } else {
                for (int i = 0; i < len; i++) {
                    a0 += a0_delta;
                    q += q_delta;
                    gain += gain_delta;
                    float ga0 = (1 - q * a0);
                    y1 = ga0 * y1 + (a0) * (buffer[i] - y2);
                    y2 = ga0 * y2 + (a0) * y1;
                    buffer[i] = y2 * gain;
                }
            }

            if (Math.abs(y2) < 1.0E-8)
                y2 = 0;
            if (Math.abs(y1) < 1.0E-8)
                y1 = 0;
            this.y2 = y2;
            this.y1 = y1;
        }

        this.last_a0 = this.a0;
        this.last_q = this.q;
        this.last_gain = this.gain;
        this.last_wet = this.wet;
    }
}
