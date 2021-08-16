/*
 * Copyright (c) 2007, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * AHDSR control signal envelope generator.
 *
 * @author Karl Helgason
 */
public final class SoftEnvelopeGenerator implements SoftProcess {

    public static final int EG_OFF = 0;
    public static final int EG_DELAY = 1;
    public static final int EG_ATTACK = 2;
    public static final int EG_HOLD = 3;
    public static final int EG_DECAY = 4;
    public static final int EG_SUSTAIN = 5;
    public static final int EG_RELEASE = 6;
    public static final int EG_SHUTDOWN = 7;
    public static final int EG_END = 8;
    int max_count = 10;
    int used_count = 0;
    private final int[] stage = new int[max_count];
    private final int[] stage_ix = new int[max_count];
    private final double[] stage_v = new double[max_count];
    private final int[] stage_count = new int[max_count];
    private final double[][] on = new double[max_count][1];
    private final double[][] active = new double[max_count][1];
    private final double[][] out = new double[max_count][1];
    private final double[][] delay = new double[max_count][1];
    private final double[][] attack = new double[max_count][1];
    private final double[][] hold = new double[max_count][1];
    private final double[][] decay = new double[max_count][1];
    private final double[][] sustain = new double[max_count][1];
    private final double[][] release = new double[max_count][1];
    private final double[][] shutdown = new double[max_count][1];
    private final double[][] release2 = new double[max_count][1];
    private final double[][] attack2 = new double[max_count][1];
    private final double[][] decay2 = new double[max_count][1];
    private double control_time = 0;

    @Override
    public void reset() {
        for (int i = 0; i < used_count; i++) {
            stage[i] = 0;
            on[i][0] = 0;
            out[i][0] = 0;
            delay[i][0] = 0;
            attack[i][0] = 0;
            hold[i][0] = 0;
            decay[i][0] = 0;
            sustain[i][0] = 0;
            release[i][0] = 0;
            shutdown[i][0] = 0;
            attack2[i][0] = 0;
            decay2[i][0] = 0;
            release2[i][0] = 0;
        }
        used_count = 0;
    }

    @Override
    public void init(SoftSynthesizer synth) {
        control_time = 1.0 / synth.getControlRate();
        processControlLogic();
    }

    @Override
    public double[] get(int instance, String name) {
        if (instance >= used_count)
            used_count = instance + 1;
        if (name == null)
            return out[instance];
        if (name.equals("on"))
            return on[instance];
        if (name.equals("active"))
            return active[instance];
        if (name.equals("delay"))
            return delay[instance];
        if (name.equals("attack"))
            return attack[instance];
        if (name.equals("hold"))
            return hold[instance];
        if (name.equals("decay"))
            return decay[instance];
        if (name.equals("sustain"))
            return sustain[instance];
        if (name.equals("release"))
            return release[instance];
        if (name.equals("shutdown"))
            return shutdown[instance];
        if (name.equals("attack2"))
            return attack2[instance];
        if (name.equals("decay2"))
            return decay2[instance];
        if (name.equals("release2"))
            return release2[instance];

        return null;
    }

    @Override
    @SuppressWarnings("fallthrough")
    public void processControlLogic() {
        for (int i = 0; i < used_count; i++) {

            if (stage[i] == EG_END)
                continue;

            if ((stage[i] > EG_OFF) && (stage[i] < EG_RELEASE)) {
                if (on[i][0] < 0.5) {
                    if (on[i][0] < -0.5) {
                        stage_count[i] = (int)(Math.pow(2,
                                this.shutdown[i][0] / 1200.0) / control_time);
                        if (stage_count[i] < 0)
                            stage_count[i] = 0;
                        stage_v[i] = out[i][0];
                        stage_ix[i] = 0;
                        stage[i] = EG_SHUTDOWN;
                    } else {
                        if ((release2[i][0] < 0.000001) && release[i][0] < 0
                                && Double.isInfinite(release[i][0])) {
                            out[i][0] = 0;
                            active[i][0] = 0;
                            stage[i] = EG_END;
                            continue;
                        }

                        stage_count[i] = (int)(Math.pow(2,
                                this.release[i][0] / 1200.0) / control_time);
                        stage_count[i]
                                += (int)(this.release2[i][0]/(control_time * 1000));
                        if (stage_count[i] < 0)
                            stage_count[i] = 0;
                        // stage_v[i] = out[i][0];
                        stage_ix[i] = 0;

                        double m = 1 - out[i][0];
                        stage_ix[i] = (int)(stage_count[i] * m);

                        stage[i] = EG_RELEASE;
                    }
                }
            }

            switch (stage[i]) {
            case EG_OFF:
                active[i][0] = 1;
                if (on[i][0] < 0.5)
                    break;
                stage[i] = EG_DELAY;
                stage_ix[i] = (int)(Math.pow(2,
                        this.delay[i][0] / 1200.0) / control_time);
                if (stage_ix[i] < 0)
                    stage_ix[i] = 0;
                // Fallthrough
            case EG_DELAY:
                if (stage_ix[i] == 0) {
                    double attack = this.attack[i][0];
                    double attack2 = this.attack2[i][0];

                    if (attack2 < 0.000001
                            && (attack < 0 && Double.isInfinite(attack))) {
                        out[i][0] = 1;
                        stage[i] = EG_HOLD;
                        stage_count[i] = (int)(Math.pow(2,
                                this.hold[i][0] / 1200.0) / control_time);
                        stage_ix[i] = 0;
                    } else {
                        stage[i] = EG_ATTACK;
                        stage_count[i] = (int)(Math.pow(2,
                                attack / 1200.0) / control_time);
                        stage_count[i] += (int)(attack2 / (control_time * 1000));
                        if (stage_count[i] < 0)
                            stage_count[i] = 0;
                        stage_ix[i] = 0;
                    }
                } else
                    stage_ix[i]--;
                break;
            case EG_ATTACK:
                stage_ix[i]++;
                if (stage_ix[i] >= stage_count[i]) {
                    out[i][0] = 1;
                    stage[i] = EG_HOLD;
                } else {
                    // CONVEX attack
                    double a = ((double)stage_ix[i]) / ((double)stage_count[i]);
                    a = 1 + ((40.0 / 96.0) / Math.log(10)) * Math.log(a);
                    if (a < 0)
                        a = 0;
                    else if (a > 1)
                        a = 1;
                    out[i][0] = a;
                }
                break;
            case EG_HOLD:
                stage_ix[i]++;
                if (stage_ix[i] >= stage_count[i]) {
                    stage[i] = EG_DECAY;
                    stage_count[i] = (int)(Math.pow(2,
                            this.decay[i][0] / 1200.0) / control_time);
                    stage_count[i] += (int)(this.decay2[i][0]/(control_time*1000));
                    if (stage_count[i] < 0)
                        stage_count[i] = 0;
                    stage_ix[i] = 0;
                }
                break;
            case EG_DECAY:
                stage_ix[i]++;
                double sustain = this.sustain[i][0] * (1.0 / 1000.0);
                if (stage_ix[i] >= stage_count[i]) {
                    out[i][0] = sustain;
                    stage[i] = EG_SUSTAIN;
                    if (sustain < 0.001) {
                        out[i][0] = 0;
                        active[i][0] = 0;
                        stage[i] = EG_END;
                    }
                } else {
                    double m = ((double)stage_ix[i]) / ((double)stage_count[i]);
                    out[i][0] = (1 - m) + sustain * m;
                }
                break;
            case EG_SUSTAIN:
                break;
            case EG_RELEASE:
                stage_ix[i]++;
                if (stage_ix[i] >= stage_count[i]) {
                    out[i][0] = 0;
                    active[i][0] = 0;
                    stage[i] = EG_END;
                } else {
                    double m = ((double)stage_ix[i]) / ((double)stage_count[i]);
                    out[i][0] = (1 - m); // *stage_v[i];

                    if (on[i][0] < -0.5) {
                        stage_count[i] = (int)(Math.pow(2,
                                this.shutdown[i][0] / 1200.0) / control_time);
                        if (stage_count[i] < 0)
                            stage_count[i] = 0;
                        stage_v[i] = out[i][0];
                        stage_ix[i] = 0;
                        stage[i] = EG_SHUTDOWN;
                    }

                    // re-damping
                    if (on[i][0] > 0.5) {
                        sustain = this.sustain[i][0] * (1.0 / 1000.0);
                        if (out[i][0] > sustain) {
                            stage[i] = EG_DECAY;
                            stage_count[i] = (int)(Math.pow(2,
                                    this.decay[i][0] / 1200.0) / control_time);
                            stage_count[i] +=
                                    (int)(this.decay2[i][0]/(control_time*1000));
                            if (stage_count[i] < 0)
                                stage_count[i] = 0;
                            m = (out[i][0] - 1) / (sustain - 1);
                            stage_ix[i] = (int) (stage_count[i] * m);
                        }
                    }

                }
                break;
            case EG_SHUTDOWN:
                stage_ix[i]++;
                if (stage_ix[i] >= stage_count[i]) {
                    out[i][0] = 0;
                    active[i][0] = 0;
                    stage[i] = EG_END;
                } else {
                    double m = ((double)stage_ix[i]) / ((double)stage_count[i]);
                    out[i][0] = (1 - m) * stage_v[i];
                }
                break;
            default:
                break;
            }
        }
    }
}
