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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.Patch;

/**
 * This class is used to store information to describe instrument.
 * It contains list of regions and modulators.
 * It is stored inside a "ins " List Chunk inside DLS files.
 * In the DLS documentation a modulator is called articulator.
 *
 * @author Karl Helgason
 */
public final class DLSInstrument extends ModelInstrument {

    int preset = 0;
    int bank = 0;
    boolean druminstrument = false;
    byte[] guid = null;
    DLSInfo info = new DLSInfo();
    List<DLSRegion> regions = new ArrayList<>();
    List<DLSModulator> modulators = new ArrayList<>();

    public DLSInstrument() {
        super(null, null, null, null);
    }

    public DLSInstrument(DLSSoundbank soundbank) {
        super(soundbank, null, null, null);
    }

    public DLSInfo getInfo() {
        return info;
    }

    @Override
    public String getName() {
        return info.name;
    }

    public void setName(String name) {
        info.name = name;
    }

    @Override
    public ModelPatch getPatch() {
        return new ModelPatch(bank, preset, druminstrument);
    }

    public void setPatch(Patch patch) {
        if (patch instanceof ModelPatch && ((ModelPatch)patch).isPercussion()) {
            druminstrument = true;
            bank = patch.getBank();
            preset = patch.getProgram();
        } else {
            druminstrument = false;
            bank = patch.getBank();
            preset = patch.getProgram();
        }
    }

    @Override
    public Object getData() {
        return null;
    }

    public List<DLSRegion> getRegions() {
        return regions;
    }

    public List<DLSModulator> getModulators() {
        return modulators;
    }

    @Override
    public String toString() {
        if (druminstrument)
            return "Drumkit: " + info.name
                    + " bank #" + bank + " preset #" + preset;
        else
            return "Instrument: " + info.name
                    + " bank #" + bank + " preset #" + preset;
    }

    private ModelIdentifier convertToModelDest(int dest) {
        if (dest == DLSModulator.CONN_DST_NONE)
            return null;
        if (dest == DLSModulator.CONN_DST_GAIN)
            return ModelDestination.DESTINATION_GAIN;
        if (dest == DLSModulator.CONN_DST_PITCH)
            return ModelDestination.DESTINATION_PITCH;
        if (dest == DLSModulator.CONN_DST_PAN)
            return ModelDestination.DESTINATION_PAN;

        if (dest == DLSModulator.CONN_DST_LFO_FREQUENCY)
            return ModelDestination.DESTINATION_LFO1_FREQ;
        if (dest == DLSModulator.CONN_DST_LFO_STARTDELAY)
            return ModelDestination.DESTINATION_LFO1_DELAY;

        if (dest == DLSModulator.CONN_DST_EG1_ATTACKTIME)
            return ModelDestination.DESTINATION_EG1_ATTACK;
        if (dest == DLSModulator.CONN_DST_EG1_DECAYTIME)
            return ModelDestination.DESTINATION_EG1_DECAY;
        if (dest == DLSModulator.CONN_DST_EG1_RELEASETIME)
            return ModelDestination.DESTINATION_EG1_RELEASE;
        if (dest == DLSModulator.CONN_DST_EG1_SUSTAINLEVEL)
            return ModelDestination.DESTINATION_EG1_SUSTAIN;

        if (dest == DLSModulator.CONN_DST_EG2_ATTACKTIME)
            return ModelDestination.DESTINATION_EG2_ATTACK;
        if (dest == DLSModulator.CONN_DST_EG2_DECAYTIME)
            return ModelDestination.DESTINATION_EG2_DECAY;
        if (dest == DLSModulator.CONN_DST_EG2_RELEASETIME)
            return ModelDestination.DESTINATION_EG2_RELEASE;
        if (dest == DLSModulator.CONN_DST_EG2_SUSTAINLEVEL)
            return ModelDestination.DESTINATION_EG2_SUSTAIN;

        // DLS2 Destinations
        if (dest == DLSModulator.CONN_DST_KEYNUMBER)
            return ModelDestination.DESTINATION_KEYNUMBER;

        if (dest == DLSModulator.CONN_DST_CHORUS)
            return ModelDestination.DESTINATION_CHORUS;
        if (dest == DLSModulator.CONN_DST_REVERB)
            return ModelDestination.DESTINATION_REVERB;

        if (dest == DLSModulator.CONN_DST_VIB_FREQUENCY)
            return ModelDestination.DESTINATION_LFO2_FREQ;
        if (dest == DLSModulator.CONN_DST_VIB_STARTDELAY)
            return ModelDestination.DESTINATION_LFO2_DELAY;

        if (dest == DLSModulator.CONN_DST_EG1_DELAYTIME)
            return ModelDestination.DESTINATION_EG1_DELAY;
        if (dest == DLSModulator.CONN_DST_EG1_HOLDTIME)
            return ModelDestination.DESTINATION_EG1_HOLD;
        if (dest == DLSModulator.CONN_DST_EG1_SHUTDOWNTIME)
            return ModelDestination.DESTINATION_EG1_SHUTDOWN;

        if (dest == DLSModulator.CONN_DST_EG2_DELAYTIME)
            return ModelDestination.DESTINATION_EG2_DELAY;
        if (dest == DLSModulator.CONN_DST_EG2_HOLDTIME)
            return ModelDestination.DESTINATION_EG2_HOLD;

        if (dest == DLSModulator.CONN_DST_FILTER_CUTOFF)
            return ModelDestination.DESTINATION_FILTER_FREQ;
        if (dest == DLSModulator.CONN_DST_FILTER_Q)
            return ModelDestination.DESTINATION_FILTER_Q;

        return null;
    }

    private ModelIdentifier convertToModelSrc(int src) {
        if (src == DLSModulator.CONN_SRC_NONE)
            return null;

        if (src == DLSModulator.CONN_SRC_LFO)
            return ModelSource.SOURCE_LFO1;
        if (src == DLSModulator.CONN_SRC_KEYONVELOCITY)
            return ModelSource.SOURCE_NOTEON_VELOCITY;
        if (src == DLSModulator.CONN_SRC_KEYNUMBER)
            return ModelSource.SOURCE_NOTEON_KEYNUMBER;
        if (src == DLSModulator.CONN_SRC_EG1)
            return ModelSource.SOURCE_EG1;
        if (src == DLSModulator.CONN_SRC_EG2)
            return ModelSource.SOURCE_EG2;
        if (src == DLSModulator.CONN_SRC_PITCHWHEEL)
            return ModelSource.SOURCE_MIDI_PITCH;
        if (src == DLSModulator.CONN_SRC_CC1)
            return new ModelIdentifier("midi_cc", "1", 0);
        if (src == DLSModulator.CONN_SRC_CC7)
            return new ModelIdentifier("midi_cc", "7", 0);
        if (src == DLSModulator.CONN_SRC_CC10)
            return new ModelIdentifier("midi_cc", "10", 0);
        if (src == DLSModulator.CONN_SRC_CC11)
            return new ModelIdentifier("midi_cc", "11", 0);
        if (src == DLSModulator.CONN_SRC_RPN0)
            return new ModelIdentifier("midi_rpn", "0", 0);
        if (src == DLSModulator.CONN_SRC_RPN1)
            return new ModelIdentifier("midi_rpn", "1", 0);

        if (src == DLSModulator.CONN_SRC_POLYPRESSURE)
            return ModelSource.SOURCE_MIDI_POLY_PRESSURE;
        if (src == DLSModulator.CONN_SRC_CHANNELPRESSURE)
            return ModelSource.SOURCE_MIDI_CHANNEL_PRESSURE;
        if (src == DLSModulator.CONN_SRC_VIBRATO)
            return ModelSource.SOURCE_LFO2;
        if (src == DLSModulator.CONN_SRC_MONOPRESSURE)
            return ModelSource.SOURCE_MIDI_CHANNEL_PRESSURE;

        if (src == DLSModulator.CONN_SRC_CC91)
            return new ModelIdentifier("midi_cc", "91", 0);
        if (src == DLSModulator.CONN_SRC_CC93)
            return new ModelIdentifier("midi_cc", "93", 0);

        return null;
    }

    private ModelConnectionBlock convertToModel(DLSModulator mod) {
        ModelIdentifier source = convertToModelSrc(mod.getSource());
        ModelIdentifier control = convertToModelSrc(mod.getControl());
        ModelIdentifier destination_id =
                convertToModelDest(mod.getDestination());

        int scale = mod.getScale();
        double f_scale;
        if (scale == Integer.MIN_VALUE)
            f_scale = Double.NEGATIVE_INFINITY;
        else
            f_scale = scale / 65536.0;

        if (destination_id != null) {
            ModelSource src = null;
            ModelSource ctrl = null;
            ModelConnectionBlock block = new ModelConnectionBlock();
            if (control != null) {
                ModelSource s = new ModelSource();
                if (control == ModelSource.SOURCE_MIDI_PITCH) {
                    ((ModelStandardTransform)s.getTransform()).setPolarity(
                            ModelStandardTransform.POLARITY_BIPOLAR);
                } else if (control == ModelSource.SOURCE_LFO1
                        || control == ModelSource.SOURCE_LFO2) {
                    ((ModelStandardTransform)s.getTransform()).setPolarity(
                            ModelStandardTransform.POLARITY_BIPOLAR);
                }
                s.setIdentifier(control);
                block.addSource(s);
                ctrl = s;
            }
            if (source != null) {
                ModelSource s = new ModelSource();
                if (source == ModelSource.SOURCE_MIDI_PITCH) {
                    ((ModelStandardTransform)s.getTransform()).setPolarity(
                            ModelStandardTransform.POLARITY_BIPOLAR);
                } else if (source == ModelSource.SOURCE_LFO1
                        || source == ModelSource.SOURCE_LFO2) {
                    ((ModelStandardTransform)s.getTransform()).setPolarity(
                            ModelStandardTransform.POLARITY_BIPOLAR);
                }
                s.setIdentifier(source);
                block.addSource(s);
                src = s;
            }
            ModelDestination destination = new ModelDestination();
            destination.setIdentifier(destination_id);
            block.setDestination(destination);

            if (mod.getVersion() == 1) {
                //if (mod.getTransform() ==  DLSModulator.CONN_TRN_CONCAVE) {
                //    ((ModelStandardTransform)destination.getTransform())
                //            .setTransform(
                //            ModelStandardTransform.TRANSFORM_CONCAVE);
                //}
                if (mod.getTransform() == DLSModulator.CONN_TRN_CONCAVE) {
                    if (src != null) {
                        ((ModelStandardTransform)src.getTransform())
                                .setTransform(
                                    ModelStandardTransform.TRANSFORM_CONCAVE);
                        ((ModelStandardTransform)src.getTransform())
                                .setDirection(
                                    ModelStandardTransform.DIRECTION_MAX2MIN);
                    }
                    if (ctrl != null) {
                        ((ModelStandardTransform)ctrl.getTransform())
                                .setTransform(
                                    ModelStandardTransform.TRANSFORM_CONCAVE);
                        ((ModelStandardTransform)ctrl.getTransform())
                                .setDirection(
                                    ModelStandardTransform.DIRECTION_MAX2MIN);
                    }
                }

            } else if (mod.getVersion() == 2) {
                int transform = mod.getTransform();
                int src_transform_invert = (transform >> 15) & 1;
                int src_transform_bipolar = (transform >> 14) & 1;
                int src_transform = (transform >> 10) & 8;
                int ctr_transform_invert = (transform >> 9) & 1;
                int ctr_transform_bipolar = (transform >> 8) & 1;
                int ctr_transform = (transform >> 4) & 8;


                if (src != null) {
                    int trans = ModelStandardTransform.TRANSFORM_LINEAR;
                    if (src_transform == DLSModulator.CONN_TRN_SWITCH)
                        trans = ModelStandardTransform.TRANSFORM_SWITCH;
                    if (src_transform == DLSModulator.CONN_TRN_CONCAVE)
                        trans = ModelStandardTransform.TRANSFORM_CONCAVE;
                    if (src_transform == DLSModulator.CONN_TRN_CONVEX)
                        trans = ModelStandardTransform.TRANSFORM_CONVEX;
                    ((ModelStandardTransform)src.getTransform())
                            .setTransform(trans);
                    ((ModelStandardTransform)src.getTransform())
                            .setPolarity(src_transform_bipolar == 1);
                    ((ModelStandardTransform)src.getTransform())
                            .setDirection(src_transform_invert == 1);

                }

                if (ctrl != null) {
                    int trans = ModelStandardTransform.TRANSFORM_LINEAR;
                    if (ctr_transform == DLSModulator.CONN_TRN_SWITCH)
                        trans = ModelStandardTransform.TRANSFORM_SWITCH;
                    if (ctr_transform == DLSModulator.CONN_TRN_CONCAVE)
                        trans = ModelStandardTransform.TRANSFORM_CONCAVE;
                    if (ctr_transform == DLSModulator.CONN_TRN_CONVEX)
                        trans = ModelStandardTransform.TRANSFORM_CONVEX;
                    ((ModelStandardTransform)ctrl.getTransform())
                            .setTransform(trans);
                    ((ModelStandardTransform)ctrl.getTransform())
                            .setPolarity(ctr_transform_bipolar == 1);
                    ((ModelStandardTransform)ctrl.getTransform())
                            .setDirection(ctr_transform_invert == 1);
                }

                /* No output transforms are defined the DLS Level 2
                int out_transform = transform % 8;
                int trans = ModelStandardTransform.TRANSFORM_LINEAR;
                if (out_transform == DLSModulator.CONN_TRN_SWITCH)
                    trans = ModelStandardTransform.TRANSFORM_SWITCH;
                if (out_transform == DLSModulator.CONN_TRN_CONCAVE)
                    trans = ModelStandardTransform.TRANSFORM_CONCAVE;
                if (out_transform == DLSModulator.CONN_TRN_CONVEX)
                    trans = ModelStandardTransform.TRANSFORM_CONVEX;
                if (ctrl != null) {
                    ((ModelStandardTransform)destination.getTransform())
                            .setTransform(trans);
                }
                */

            }

            block.setScale(f_scale);

            return block;
        }

        return null;
    }

    @Override
    public ModelPerformer[] getPerformers() {
        List<ModelPerformer> performers = new ArrayList<>();

        Map<String, DLSModulator> modmap = new HashMap<>();
        for (DLSModulator mod: getModulators()) {
            modmap.put(mod.getSource() + "x" + mod.getControl() + "=" +
                    mod.getDestination(), mod);
        }

        Map<String, DLSModulator> insmodmap = new HashMap<>();

        for (DLSRegion zone: regions) {
            ModelPerformer performer = new ModelPerformer();
            performer.setName(zone.getSample().getName());
            performer.setSelfNonExclusive((zone.getFusoptions() &
                    DLSRegion.OPTION_SELFNONEXCLUSIVE) != 0);
            performer.setExclusiveClass(zone.getExclusiveClass());
            performer.setKeyFrom(zone.getKeyfrom());
            performer.setKeyTo(zone.getKeyto());
            performer.setVelFrom(zone.getVelfrom());
            performer.setVelTo(zone.getVelto());

            insmodmap.clear();
            insmodmap.putAll(modmap);
            for (DLSModulator mod: zone.getModulators()) {
                insmodmap.put(mod.getSource() + "x" + mod.getControl() + "=" +
                        mod.getDestination(), mod);
            }

            List<ModelConnectionBlock> blocks = performer.getConnectionBlocks();
            for (DLSModulator mod: insmodmap.values()) {
                ModelConnectionBlock p = convertToModel(mod);
                if (p != null)
                    blocks.add(p);
            }


            DLSSample sample = zone.getSample();
            DLSSampleOptions sampleopt = zone.getSampleoptions();
            if (sampleopt == null)
                sampleopt = sample.getSampleoptions();

            ModelByteBuffer buff = sample.getDataBuffer();

            float pitchcorrection = (-sampleopt.unitynote * 100) +
                    sampleopt.finetune;

            ModelByteBufferWavetable osc = new ModelByteBufferWavetable(buff,
                    sample.getFormat(), pitchcorrection);
            osc.setAttenuation(osc.getAttenuation() / 65536f);
            if (sampleopt.getLoops().size() != 0) {
                DLSSampleLoop loop = sampleopt.getLoops().get(0);
                osc.setLoopStart((int)loop.getStart());
                osc.setLoopLength((int)loop.getLength());
                if (loop.getType() == DLSSampleLoop.LOOP_TYPE_FORWARD)
                    osc.setLoopType(ModelWavetable.LOOP_TYPE_FORWARD);
                if (loop.getType() == DLSSampleLoop.LOOP_TYPE_RELEASE)
                    osc.setLoopType(ModelWavetable.LOOP_TYPE_RELEASE);
                else
                    osc.setLoopType(ModelWavetable.LOOP_TYPE_FORWARD);
            }

            performer.getConnectionBlocks().add(
                    new ModelConnectionBlock(SoftFilter.FILTERTYPE_LP12,
                        new ModelDestination(
                            new ModelIdentifier("filter", "type", 1))));

            performer.getOscillators().add(osc);

            performers.add(performer);

        }

        return performers.toArray(new ModelPerformer[performers.size()]);
    }

    public byte[] getGuid() {
        return guid == null ? null : Arrays.copyOf(guid, guid.length);
    }

    public void setGuid(byte[] guid) {
        this.guid = guid == null ? null : Arrays.copyOf(guid, guid.length);
    }
}
