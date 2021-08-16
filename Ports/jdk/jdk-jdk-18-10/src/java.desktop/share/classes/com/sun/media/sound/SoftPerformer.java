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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class decodes information from ModelPeformer for use in SoftVoice.
 * It also adds default connections if they where missing in ModelPerformer.
 *
 * @author Karl Helgason
 */
public final class SoftPerformer {

    static ModelConnectionBlock[] defaultconnections
            = new ModelConnectionBlock[42];

    static {
        int o = 0;
        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("noteon", "on", 0),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            1, new ModelDestination(new ModelIdentifier("eg", "on", 0)));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("noteon", "on", 0),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            1, new ModelDestination(new ModelIdentifier("eg", "on", 1)));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("eg", "active", 0),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            1, new ModelDestination(new ModelIdentifier("mixer", "active", 0)));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("eg", 0),
                ModelStandardTransform.DIRECTION_MAX2MIN,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            -960, new ModelDestination(new ModelIdentifier("mixer", "gain")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("noteon", "velocity"),
                ModelStandardTransform.DIRECTION_MAX2MIN,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_CONCAVE),
            -960, new ModelDestination(new ModelIdentifier("mixer", "gain")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi", "pitch"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            new ModelSource(new ModelIdentifier("midi_rpn", "0"),
                new ModelTransform() {
                    @Override
                    public double transform(double value) {
                        int v = (int) (value * 16384.0);
                        int msb = v >> 7;
                        int lsb = v & 127;
                        return msb * 100 + lsb;
                    }
                }),
            new ModelDestination(new ModelIdentifier("osc", "pitch")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("noteon", "keynumber"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            12800, new ModelDestination(new ModelIdentifier("osc", "pitch")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "7"),
                ModelStandardTransform.DIRECTION_MAX2MIN,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_CONCAVE),
            -960, new ModelDestination(new ModelIdentifier("mixer", "gain")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "8"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            1000, new ModelDestination(new ModelIdentifier("mixer", "balance")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "10"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            1000, new ModelDestination(new ModelIdentifier("mixer", "pan")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "11"),
                ModelStandardTransform.DIRECTION_MAX2MIN,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_CONCAVE),
            -960, new ModelDestination(new ModelIdentifier("mixer", "gain")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "91"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            1000, new ModelDestination(new ModelIdentifier("mixer", "reverb")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "93"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            1000, new ModelDestination(new ModelIdentifier("mixer", "chorus")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "71"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            200, new ModelDestination(new ModelIdentifier("filter", "q")));
        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "74"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            9600, new ModelDestination(new ModelIdentifier("filter", "freq")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "72"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            6000, new ModelDestination(new ModelIdentifier("eg", "release2")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "73"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            2000, new ModelDestination(new ModelIdentifier("eg", "attack2")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "75"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            6000, new ModelDestination(new ModelIdentifier("eg", "decay2")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "67"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_SWITCH),
            -50, new ModelDestination(ModelDestination.DESTINATION_GAIN));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_cc", "67"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_UNIPOLAR,
                ModelStandardTransform.TRANSFORM_SWITCH),
            -2400, new ModelDestination(ModelDestination.DESTINATION_FILTER_FREQ));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_rpn", "1"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            100, new ModelDestination(new ModelIdentifier("osc", "pitch")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("midi_rpn", "2"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            12800, new ModelDestination(new ModelIdentifier("osc", "pitch")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("master", "fine_tuning"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            100, new ModelDestination(new ModelIdentifier("osc", "pitch")));

        defaultconnections[o++] = new ModelConnectionBlock(
            new ModelSource(
                new ModelIdentifier("master", "coarse_tuning"),
                ModelStandardTransform.DIRECTION_MIN2MAX,
                ModelStandardTransform.POLARITY_BIPOLAR,
                ModelStandardTransform.TRANSFORM_LINEAR),
            12800, new ModelDestination(new ModelIdentifier("osc", "pitch")));

        defaultconnections[o++] = new ModelConnectionBlock(13500,
                new ModelDestination(new ModelIdentifier("filter", "freq", 0)));

        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "delay", 0)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "attack", 0)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "hold", 0)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "decay", 0)));
        defaultconnections[o++] = new ModelConnectionBlock(1000,
                new ModelDestination(new ModelIdentifier("eg", "sustain", 0)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "release", 0)));
        defaultconnections[o++] = new ModelConnectionBlock(1200.0
                * Math.log(0.015) / Math.log(2), new ModelDestination(
                new ModelIdentifier("eg", "shutdown", 0))); // 15 msec default

        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "delay", 1)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "attack", 1)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "hold", 1)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "decay", 1)));
        defaultconnections[o++] = new ModelConnectionBlock(1000,
                new ModelDestination(new ModelIdentifier("eg", "sustain", 1)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("eg", "release", 1)));

        defaultconnections[o++] = new ModelConnectionBlock(-8.51318,
                new ModelDestination(new ModelIdentifier("lfo", "freq", 0)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("lfo", "delay", 0)));
        defaultconnections[o++] = new ModelConnectionBlock(-8.51318,
                new ModelDestination(new ModelIdentifier("lfo", "freq", 1)));
        defaultconnections[o++] = new ModelConnectionBlock(
                Float.NEGATIVE_INFINITY, new ModelDestination(
                new ModelIdentifier("lfo", "delay", 1)));

    }
    public int keyFrom = 0;
    public int keyTo = 127;
    public int velFrom = 0;
    public int velTo = 127;
    public int exclusiveClass = 0;
    public boolean selfNonExclusive = false;
    public boolean forcedVelocity = false;
    public boolean forcedKeynumber = false;
    public ModelPerformer performer;
    public ModelConnectionBlock[] connections;
    public ModelOscillator[] oscillators;
    public Map<Integer, int[]> midi_rpn_connections = new HashMap<>();
    public Map<Integer, int[]> midi_nrpn_connections = new HashMap<>();
    public int[][] midi_ctrl_connections;
    public int[][] midi_connections;
    public int[] ctrl_connections;
    private final List<Integer> ctrl_connections_list = new ArrayList<>();

    private static class KeySortComparator implements Comparator<ModelSource> {

        @Override
        public int compare(ModelSource o1, ModelSource o2) {
            return o1.getIdentifier().toString().compareTo(
                    o2.getIdentifier().toString());
        }
    }
    private static final KeySortComparator keySortComparator = new KeySortComparator();

    private String extractKeys(ModelConnectionBlock conn) {
        StringBuilder sb = new StringBuilder();
        if (conn.getSources() != null) {
            sb.append("[");
            ModelSource[] srcs = conn.getSources();
            ModelSource[] srcs2 = new ModelSource[srcs.length];
            for (int i = 0; i < srcs.length; i++)
                srcs2[i] = srcs[i];
            Arrays.sort(srcs2, keySortComparator);
            for (int i = 0; i < srcs.length; i++) {
                sb.append(srcs[i].getIdentifier());
                sb.append(";");
            }
            sb.append("]");
        }
        sb.append(";");
        if (conn.getDestination() != null) {
            sb.append(conn.getDestination().getIdentifier());
        }
        sb.append(";");
        return sb.toString();
    }

    private void processSource(ModelSource src, int ix) {
        ModelIdentifier id = src.getIdentifier();
        String o = id.getObject();
        if (o.equals("midi_cc"))
            processMidiControlSource(src, ix);
        else if (o.equals("midi_rpn"))
            processMidiRpnSource(src, ix);
        else if (o.equals("midi_nrpn"))
            processMidiNrpnSource(src, ix);
        else if (o.equals("midi"))
            processMidiSource(src, ix);
        else if (o.equals("noteon"))
            processNoteOnSource(src, ix);
        else if (o.equals("osc"))
            return;
        else if (o.equals("mixer"))
            return;
        else
            ctrl_connections_list.add(ix);
    }

    private void processMidiControlSource(ModelSource src, int ix) {
        String v = src.getIdentifier().getVariable();
        if (v == null)
            return;
        int c = Integer.parseInt(v);
        if (midi_ctrl_connections[c] == null)
            midi_ctrl_connections[c] = new int[]{ix};
        else {
            int[] olda = midi_ctrl_connections[c];
            int[] newa = new int[olda.length + 1];
            for (int i = 0; i < olda.length; i++)
                newa[i] = olda[i];
            newa[newa.length - 1] = ix;
            midi_ctrl_connections[c] = newa;
        }
    }

    private void processNoteOnSource(ModelSource src, int ix) {
        String v = src.getIdentifier().getVariable();
        int c = -1;
        if (v.equals("on"))
            c = 3;
        if (v.equals("keynumber"))
            c = 4;
        if (c == -1)
            return;
        if (midi_connections[c] == null)
            midi_connections[c] = new int[]{ix};
        else {
            int[] olda = midi_connections[c];
            int[] newa = new int[olda.length + 1];
            for (int i = 0; i < olda.length; i++)
                newa[i] = olda[i];
            newa[newa.length - 1] = ix;
            midi_connections[c] = newa;
        }
    }

    private void processMidiSource(ModelSource src, int ix) {
        String v = src.getIdentifier().getVariable();
        int c = -1;
        if (v.equals("pitch"))
            c = 0;
        if (v.equals("channel_pressure"))
            c = 1;
        if (v.equals("poly_pressure"))
            c = 2;
        if (c == -1)
            return;
        if (midi_connections[c] == null)
            midi_connections[c] = new int[]{ix};
        else {
            int[] olda = midi_connections[c];
            int[] newa = new int[olda.length + 1];
            for (int i = 0; i < olda.length; i++)
                newa[i] = olda[i];
            newa[newa.length - 1] = ix;
            midi_connections[c] = newa;
        }
    }

    private void processMidiRpnSource(ModelSource src, int ix) {
        String v = src.getIdentifier().getVariable();
        if (v == null)
            return;
        int c = Integer.parseInt(v);
        if (midi_rpn_connections.get(c) == null)
            midi_rpn_connections.put(c, new int[]{ix});
        else {
            int[] olda = midi_rpn_connections.get(c);
            int[] newa = new int[olda.length + 1];
            for (int i = 0; i < olda.length; i++)
                newa[i] = olda[i];
            newa[newa.length - 1] = ix;
            midi_rpn_connections.put(c, newa);
        }
    }

    private void processMidiNrpnSource(ModelSource src, int ix) {
        String v = src.getIdentifier().getVariable();
        if (v == null)
            return;
        int c = Integer.parseInt(v);
        if (midi_nrpn_connections.get(c) == null)
            midi_nrpn_connections.put(c, new int[]{ix});
        else {
            int[] olda = midi_nrpn_connections.get(c);
            int[] newa = new int[olda.length + 1];
            for (int i = 0; i < olda.length; i++)
                newa[i] = olda[i];
            newa[newa.length - 1] = ix;
            midi_nrpn_connections.put(c, newa);
        }
    }

    public SoftPerformer(ModelPerformer performer) {
        this.performer = performer;

        keyFrom = performer.getKeyFrom();
        keyTo = performer.getKeyTo();
        velFrom = performer.getVelFrom();
        velTo = performer.getVelTo();
        exclusiveClass = performer.getExclusiveClass();
        selfNonExclusive = performer.isSelfNonExclusive();

        Map<String, ModelConnectionBlock> connmap = new HashMap<>();

        List<ModelConnectionBlock> performer_connections = new ArrayList<>();
        performer_connections.addAll(performer.getConnectionBlocks());

        if (performer.isDefaultConnectionsEnabled()) {

            // Add modulation depth range (RPN 5) to the modulation wheel (cc#1)

            boolean isModulationWheelConectionFound = false;
            for (int j = 0; j < performer_connections.size(); j++) {
                ModelConnectionBlock connection = performer_connections.get(j);
                ModelSource[] sources = connection.getSources();
                ModelDestination dest = connection.getDestination();
                boolean isModulationWheelConection = false;
                if (dest != null && sources != null && sources.length > 1) {
                    for (int i = 0; i < sources.length; i++) {
                        // check if connection block has the source "modulation
                        // wheel cc#1"
                        if (sources[i].getIdentifier().getObject().equals(
                                "midi_cc")) {
                            if (sources[i].getIdentifier().getVariable()
                                    .equals("1")) {
                                isModulationWheelConection = true;
                                isModulationWheelConectionFound = true;
                                break;
                            }
                        }
                    }
                }
                if (isModulationWheelConection) {

                    ModelConnectionBlock newconnection = new ModelConnectionBlock();
                    newconnection.setSources(connection.getSources());
                    newconnection.setDestination(connection.getDestination());
                    newconnection.addSource(new ModelSource(
                            new ModelIdentifier("midi_rpn", "5")));
                    newconnection.setScale(connection.getScale() * 256.0);
                    performer_connections.set(j, newconnection);
                }
            }

            if (!isModulationWheelConectionFound) {
                ModelConnectionBlock conn = new ModelConnectionBlock(
                        new ModelSource(ModelSource.SOURCE_LFO1,
                        ModelStandardTransform.DIRECTION_MIN2MAX,
                        ModelStandardTransform.POLARITY_BIPOLAR,
                        ModelStandardTransform.TRANSFORM_LINEAR),
                        new ModelSource(new ModelIdentifier("midi_cc", "1", 0),
                        ModelStandardTransform.DIRECTION_MIN2MAX,
                        ModelStandardTransform.POLARITY_UNIPOLAR,
                        ModelStandardTransform.TRANSFORM_LINEAR),
                        50,
                        new ModelDestination(ModelDestination.DESTINATION_PITCH));
                conn.addSource(new ModelSource(new ModelIdentifier("midi_rpn",
                        "5")));
                conn.setScale(conn.getScale() * 256.0);
                performer_connections.add(conn);

            }

            // Let Aftertouch to behave just like modulation wheel (cc#1)
            boolean channel_pressure_set = false;
            boolean poly_pressure = false;
            ModelConnectionBlock mod_cc_1_connection = null;
            int mod_cc_1_connection_src_ix = 0;

            for (ModelConnectionBlock connection : performer_connections) {
                ModelSource[] sources = connection.getSources();
                ModelDestination dest = connection.getDestination();
                // if(dest != null && sources != null)
                if (dest != null && sources != null) {
                    for (int i = 0; i < sources.length; i++) {
                        ModelIdentifier srcid = sources[i].getIdentifier();
                        // check if connection block has the source "modulation
                        // wheel cc#1"
                        if (srcid.getObject().equals("midi_cc")) {
                            if (srcid.getVariable().equals("1")) {
                                mod_cc_1_connection = connection;
                                mod_cc_1_connection_src_ix = i;
                            }
                        }
                        // check if channel or poly pressure are already
                        // connected
                        if (srcid.getObject().equals("midi")) {
                            if (srcid.getVariable().equals("channel_pressure"))
                                channel_pressure_set = true;
                            if (srcid.getVariable().equals("poly_pressure"))
                                poly_pressure = true;
                        }
                    }
                }

            }

            if (mod_cc_1_connection != null) {
                if (!channel_pressure_set) {
                    ModelConnectionBlock mc = new ModelConnectionBlock();
                    mc.setDestination(mod_cc_1_connection.getDestination());
                    mc.setScale(mod_cc_1_connection.getScale());
                    ModelSource[] src_list = mod_cc_1_connection.getSources();
                    ModelSource[] src_list_new = new ModelSource[src_list.length];
                    for (int i = 0; i < src_list_new.length; i++)
                        src_list_new[i] = src_list[i];
                    src_list_new[mod_cc_1_connection_src_ix] = new ModelSource(
                            new ModelIdentifier("midi", "channel_pressure"));
                    mc.setSources(src_list_new);
                    connmap.put(extractKeys(mc), mc);
                }
                if (!poly_pressure) {
                    ModelConnectionBlock mc = new ModelConnectionBlock();
                    mc.setDestination(mod_cc_1_connection.getDestination());
                    mc.setScale(mod_cc_1_connection.getScale());
                    ModelSource[] src_list = mod_cc_1_connection.getSources();
                    ModelSource[] src_list_new = new ModelSource[src_list.length];
                    for (int i = 0; i < src_list_new.length; i++)
                        src_list_new[i] = src_list[i];
                    src_list_new[mod_cc_1_connection_src_ix] = new ModelSource(
                            new ModelIdentifier("midi", "poly_pressure"));
                    mc.setSources(src_list_new);
                    connmap.put(extractKeys(mc), mc);
                }
            }

            // Enable Vibration Sound Controllers : 76, 77, 78
            ModelConnectionBlock found_vib_connection = null;
            for (ModelConnectionBlock connection : performer_connections) {
                ModelSource[] sources = connection.getSources();
                if (sources.length != 0
                        && sources[0].getIdentifier().getObject().equals("lfo")) {
                    if (connection.getDestination().getIdentifier().equals(
                            ModelDestination.DESTINATION_PITCH)) {
                        if (found_vib_connection == null)
                            found_vib_connection = connection;
                        else {
                            if (found_vib_connection.getSources().length > sources.length)
                                found_vib_connection = connection;
                            else if (found_vib_connection.getSources()[0]
                                    .getIdentifier().getInstance() < 1) {
                                if (found_vib_connection.getSources()[0]
                                        .getIdentifier().getInstance() >
                                        sources[0].getIdentifier().getInstance()) {
                                    found_vib_connection = connection;
                                }
                            }
                        }

                    }
                }
            }

            int instance = 1;

            if (found_vib_connection != null) {
                instance = found_vib_connection.getSources()[0].getIdentifier()
                        .getInstance();
            }
            ModelConnectionBlock connection;

            connection = new ModelConnectionBlock(
                new ModelSource(new ModelIdentifier("midi_cc", "78"),
                    ModelStandardTransform.DIRECTION_MIN2MAX,
                    ModelStandardTransform.POLARITY_BIPOLAR,
                    ModelStandardTransform.TRANSFORM_LINEAR),
                2000, new ModelDestination(
                    new ModelIdentifier("lfo", "delay2", instance)));
            connmap.put(extractKeys(connection), connection);

            final double scale = found_vib_connection == null ? 0
                    : found_vib_connection.getScale();
            connection = new ModelConnectionBlock(
                new ModelSource(new ModelIdentifier("lfo", instance)),
                new ModelSource(new ModelIdentifier("midi_cc", "77"),
                    new ModelTransform() {
                        double s = scale;
                        @Override
                        public double transform(double value) {
                            value = value * 2 - 1;
                            value *= 600;
                            if (s == 0) {
                                return value;
                            } else if (s > 0) {
                                if (value < -s)
                                    value = -s;
                                return value;
                            } else {
                                if (value < s)
                                    value = -s;
                                return -value;
                            }
                        }
                    }), new ModelDestination(ModelDestination.DESTINATION_PITCH));
            connmap.put(extractKeys(connection), connection);

            connection = new ModelConnectionBlock(
                new ModelSource(new ModelIdentifier("midi_cc", "76"),
                    ModelStandardTransform.DIRECTION_MIN2MAX,
                    ModelStandardTransform.POLARITY_BIPOLAR,
                    ModelStandardTransform.TRANSFORM_LINEAR),
                2400, new ModelDestination(
                    new ModelIdentifier("lfo", "freq", instance)));
            connmap.put(extractKeys(connection), connection);

        }

        // Add default connection blocks
        if (performer.isDefaultConnectionsEnabled())
            for (ModelConnectionBlock connection : defaultconnections)
                connmap.put(extractKeys(connection), connection);
        // Add connection blocks from modelperformer
        for (ModelConnectionBlock connection : performer_connections)
            connmap.put(extractKeys(connection), connection);
        // seperate connection blocks : Init time, Midi Time, Midi/Control Time,
        // Control Time
        List<ModelConnectionBlock> connections = new ArrayList<>();

        midi_ctrl_connections = new int[128][];
        for (int i = 0; i < midi_ctrl_connections.length; i++) {
            midi_ctrl_connections[i] = null;
        }
        midi_connections = new int[5][];
        for (int i = 0; i < midi_connections.length; i++) {
            midi_connections[i] = null;
        }

        int ix = 0;
        boolean mustBeOnTop = false;

        for (ModelConnectionBlock connection : connmap.values()) {
            if (connection.getDestination() != null) {
                ModelDestination dest = connection.getDestination();
                ModelIdentifier id = dest.getIdentifier();
                if (id.getObject().equals("noteon")) {
                    mustBeOnTop = true;
                    if (id.getVariable().equals("keynumber"))
                        forcedKeynumber = true;
                    if (id.getVariable().equals("velocity"))
                        forcedVelocity = true;
                }
            }
            if (mustBeOnTop) {
                connections.add(0, connection);
                mustBeOnTop = false;
            } else
                connections.add(connection);
        }

        for (ModelConnectionBlock connection : connections) {
            if (connection.getSources() != null) {
                ModelSource[] srcs = connection.getSources();
                for (int i = 0; i < srcs.length; i++) {
                    processSource(srcs[i], ix);
                }
            }
            ix++;
        }

        this.connections = new ModelConnectionBlock[connections.size()];
        connections.toArray(this.connections);

        this.ctrl_connections = new int[ctrl_connections_list.size()];

        for (int i = 0; i < this.ctrl_connections.length; i++)
            this.ctrl_connections[i] = ctrl_connections_list.get(i);

        oscillators = new ModelOscillator[performer.getOscillators().size()];
        performer.getOscillators().toArray(oscillators);

        for (ModelConnectionBlock conn : connections) {
            if (conn.getDestination() != null) {
                if (isUnnecessaryTransform(conn.getDestination().getTransform())) {
                    conn.getDestination().setTransform(null);
                }
            }
            if (conn.getSources() != null) {
                for (ModelSource src : conn.getSources()) {
                    if (isUnnecessaryTransform(src.getTransform())) {
                        src.setTransform(null);
                    }
                }
            }
        }

    }

    private static boolean isUnnecessaryTransform(ModelTransform transform) {
        if (transform == null)
            return false;
        if (!(transform instanceof ModelStandardTransform))
            return false;
        ModelStandardTransform stransform = (ModelStandardTransform)transform;
        if (stransform.getDirection() != ModelStandardTransform.DIRECTION_MIN2MAX)
            return false;
        if (stransform.getPolarity() != ModelStandardTransform.POLARITY_UNIPOLAR)
            return false;
        if (stransform.getTransform() != ModelStandardTransform.TRANSFORM_LINEAR)
            return false;
        return false;
    }
}
