/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

/**
 * A standard indexed director who chooses performers
 * by there keyfrom,keyto,velfrom,velto properties.
 *
 * @author Karl Helgason
 */
public final class ModelStandardIndexedDirector implements ModelDirector {

    private final ModelPerformer[] performers;
    private final ModelDirectedPlayer player;
    private boolean noteOnUsed = false;
    private boolean noteOffUsed = false;

    // Variables needed for index
    private byte[][] trantables;
    private int[] counters;
    private int[][] mat;

    public ModelStandardIndexedDirector(final ModelPerformer[] performers,
                                        final ModelDirectedPlayer player) {
        this.performers = Arrays.copyOf(performers, performers.length);
        this.player = player;
        for (final ModelPerformer p : this.performers) {
            if (p.isReleaseTriggered()) {
                noteOffUsed = true;
            } else {
                noteOnUsed = true;
            }
        }
        buildindex();
    }

    private int[] lookupIndex(int x, int y) {
        if ((x >= 0) && (x < 128) && (y >= 0) && (y < 128)) {
            int xt = trantables[0][x];
            int yt = trantables[1][y];
            if (xt != -1 && yt != -1) {
                return mat[xt + yt * counters[0]];
            }
        }
        return null;
    }

    private int restrict(int value) {
        if(value < 0) return 0;
        if(value > 127) return 127;
        return value;
    }

    private void buildindex() {
        trantables = new byte[2][129];
        counters = new int[trantables.length];
        for (ModelPerformer performer : performers) {
            int keyFrom = performer.getKeyFrom();
            int keyTo = performer.getKeyTo();
            int velFrom = performer.getVelFrom();
            int velTo = performer.getVelTo();
            if (keyFrom > keyTo) continue;
            if (velFrom > velTo) continue;
            keyFrom = restrict(keyFrom);
            keyTo = restrict(keyTo);
            velFrom = restrict(velFrom);
            velTo = restrict(velTo);
            trantables[0][keyFrom] = 1;
            trantables[0][keyTo + 1] = 1;
            trantables[1][velFrom] = 1;
            trantables[1][velTo + 1] = 1;
        }
        for (int d = 0; d < trantables.length; d++) {
            byte[] trantable = trantables[d];
            int transize = trantable.length;
            for (int i = transize - 1; i >= 0; i--) {
                if (trantable[i] == 1) {
                    trantable[i] = -1;
                    break;
                }
                trantable[i] = -1;
            }
            int counter = -1;
            for (int i = 0; i < transize; i++) {
                if (trantable[i] != 0) {
                    counter++;
                    if (trantable[i] == -1)
                        break;
                }
                trantable[i] = (byte) counter;
            }
            counters[d] = counter;
        }
        mat = new int[counters[0] * counters[1]][];
        int ix = 0;
        for (ModelPerformer performer : performers) {
            int keyFrom = performer.getKeyFrom();
            int keyTo = performer.getKeyTo();
            int velFrom = performer.getVelFrom();
            int velTo = performer.getVelTo();
            if (keyFrom > keyTo) continue;
            if (velFrom > velTo) continue;
            keyFrom = restrict(keyFrom);
            keyTo = restrict(keyTo);
            velFrom = restrict(velFrom);
            velTo = restrict(velTo);
            int x_from = trantables[0][keyFrom];
            int x_to = trantables[0][keyTo + 1];
            int y_from = trantables[1][velFrom];
            int y_to = trantables[1][velTo + 1];
            if (x_to == -1)
                x_to = counters[0];
            if (y_to == -1)
                y_to = counters[1];
            for (int y = y_from; y < y_to; y++) {
                int i = x_from + y * counters[0];
                for (int x = x_from; x < x_to; x++) {
                    int[] mprev = mat[i];
                    if (mprev == null) {
                        mat[i] = new int[] { ix };
                    } else {
                        int[] mnew = new int[mprev.length + 1];
                        mnew[mnew.length - 1] = ix;
                        for (int k = 0; k < mprev.length; k++)
                            mnew[k] = mprev[k];
                        mat[i] = mnew;
                    }
                    i++;
                }
            }
            ix++;
        }
    }

    @Override
    public void close() {
    }

    @Override
    public void noteOff(int noteNumber, int velocity) {
        if (!noteOffUsed)
            return;
        int[] plist = lookupIndex(noteNumber, velocity);
        if(plist == null) return;
        for (int i : plist) {
            ModelPerformer p = performers[i];
            if (p.isReleaseTriggered()) {
                player.play(i, null);
            }
        }
    }

    @Override
    public void noteOn(int noteNumber, int velocity) {
        if (!noteOnUsed)
            return;
        int[] plist = lookupIndex(noteNumber, velocity);
        if(plist == null) return;
        for (int i : plist) {
            ModelPerformer p = performers[i];
            if (!p.isReleaseTriggered()) {
                player.play(i, null);
            }
        }
    }
}
