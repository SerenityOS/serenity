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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.Patch;

/**
 * Soundfont instrument.
 *
 * @author Karl Helgason
 */
public final class SF2Instrument extends ModelInstrument {

    String name = "";
    int preset = 0;
    int bank = 0;
    long library = 0;
    long genre = 0;
    long morphology = 0;
    SF2GlobalRegion globalregion = null;
    List<SF2InstrumentRegion> regions = new ArrayList<>();

    public SF2Instrument() {
        super(null, null, null, null);
    }

    public SF2Instrument(SF2Soundbank soundbank) {
        super(soundbank, null, null, null);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Patch getPatch() {
        if (bank == 128)
            return new ModelPatch(0, preset, true);
        else
            return new ModelPatch(bank << 7, preset, false);
    }

    public void setPatch(Patch patch) {
        if (patch instanceof ModelPatch && ((ModelPatch) patch).isPercussion()) {
            bank = 128;
            preset = patch.getProgram();
        } else {
            bank = patch.getBank() >> 7;
            preset = patch.getProgram();
        }
    }

    @Override
    public Object getData() {
        return null;
    }

    public long getGenre() {
        return genre;
    }

    public void setGenre(long genre) {
        this.genre = genre;
    }

    public long getLibrary() {
        return library;
    }

    public void setLibrary(long library) {
        this.library = library;
    }

    public long getMorphology() {
        return morphology;
    }

    public void setMorphology(long morphology) {
        this.morphology = morphology;
    }

    public List<SF2InstrumentRegion> getRegions() {
        return regions;
    }

    public SF2GlobalRegion getGlobalRegion() {
        return globalregion;
    }

    public void setGlobalZone(SF2GlobalRegion zone) {
        globalregion = zone;
    }

    @Override
    public String toString() {
        if (bank == 128)
            return "Drumkit: " + name + " preset #" + preset;
        else
            return "Instrument: " + name + " bank #" + bank
                    + " preset #" + preset;
    }

    @Override
    public ModelPerformer[] getPerformers() {
        int performercount = 0;
        for (SF2InstrumentRegion presetzone : regions)
            performercount += presetzone.getLayer().getRegions().size();
        ModelPerformer[] performers = new ModelPerformer[performercount];
        int pi = 0;

        SF2GlobalRegion presetglobal = globalregion;
        for (SF2InstrumentRegion presetzone : regions) {
            Map<Integer, Short> pgenerators = new HashMap<>();
            pgenerators.putAll(presetzone.getGenerators());
            if (presetglobal != null)
                pgenerators.putAll(presetglobal.getGenerators());

            SF2Layer layer = presetzone.getLayer();
            SF2GlobalRegion layerglobal = layer.getGlobalRegion();
            for (SF2LayerRegion layerzone : layer.getRegions()) {
                ModelPerformer performer = new ModelPerformer();
                if (layerzone.getSample() != null)
                    performer.setName(layerzone.getSample().getName());
                else
                    performer.setName(layer.getName());

                performers[pi++] = performer;

                int keyfrom = 0;
                int keyto = 127;
                int velfrom = 0;
                int velto = 127;

                if (layerzone.contains(SF2Region.GENERATOR_EXCLUSIVECLASS)) {
                    performer.setExclusiveClass(layerzone.getInteger(
                            SF2Region.GENERATOR_EXCLUSIVECLASS));
                }
                if (layerzone.contains(SF2Region.GENERATOR_KEYRANGE)) {
                    byte[] bytes = layerzone.getBytes(
                            SF2Region.GENERATOR_KEYRANGE);
                    if (bytes[0] >= 0)
                        if (bytes[0] > keyfrom)
                            keyfrom = bytes[0];
                    if (bytes[1] >= 0)
                        if (bytes[1] < keyto)
                            keyto = bytes[1];
                }
                if (layerzone.contains(SF2Region.GENERATOR_VELRANGE)) {
                    byte[] bytes = layerzone.getBytes(
                            SF2Region.GENERATOR_VELRANGE);
                    if (bytes[0] >= 0)
                        if (bytes[0] > velfrom)
                            velfrom = bytes[0];
                    if (bytes[1] >= 0)
                        if (bytes[1] < velto)
                            velto = bytes[1];
                }
                if (presetzone.contains(SF2Region.GENERATOR_KEYRANGE)) {
                    byte[] bytes = presetzone.getBytes(
                            SF2Region.GENERATOR_KEYRANGE);
                    if (bytes[0] > keyfrom)
                        keyfrom = bytes[0];
                    if (bytes[1] < keyto)
                        keyto = bytes[1];
                }
                if (presetzone.contains(SF2Region.GENERATOR_VELRANGE)) {
                    byte[] bytes = presetzone.getBytes(
                            SF2Region.GENERATOR_VELRANGE);
                    if (bytes[0] > velfrom)
                        velfrom = bytes[0];
                    if (bytes[1] < velto)
                        velto = bytes[1];
                }
                performer.setKeyFrom(keyfrom);
                performer.setKeyTo(keyto);
                performer.setVelFrom(velfrom);
                performer.setVelTo(velto);

                int startAddrsOffset = layerzone.getShort(
                        SF2Region.GENERATOR_STARTADDRSOFFSET);
                int endAddrsOffset = layerzone.getShort(
                        SF2Region.GENERATOR_ENDADDRSOFFSET);
                int startloopAddrsOffset = layerzone.getShort(
                        SF2Region.GENERATOR_STARTLOOPADDRSOFFSET);
                int endloopAddrsOffset = layerzone.getShort(
                        SF2Region.GENERATOR_ENDLOOPADDRSOFFSET);

                startAddrsOffset += layerzone.getShort(
                        SF2Region.GENERATOR_STARTADDRSCOARSEOFFSET) * 32768;
                endAddrsOffset += layerzone.getShort(
                        SF2Region.GENERATOR_ENDADDRSCOARSEOFFSET) * 32768;
                startloopAddrsOffset += layerzone.getShort(
                        SF2Region.GENERATOR_STARTLOOPADDRSCOARSEOFFSET) * 32768;
                endloopAddrsOffset += layerzone.getShort(
                        SF2Region.GENERATOR_ENDLOOPADDRSCOARSEOFFSET) * 32768;
                startloopAddrsOffset -= startAddrsOffset;
                endloopAddrsOffset -= startAddrsOffset;

                SF2Sample sample = layerzone.getSample();
                int rootkey = sample.originalPitch;
                if (layerzone.getShort(SF2Region.GENERATOR_OVERRIDINGROOTKEY) != -1) {
                    rootkey = layerzone.getShort(
                            SF2Region.GENERATOR_OVERRIDINGROOTKEY);
                }
                float pitchcorrection = (-rootkey * 100) + sample.pitchCorrection;
                ModelByteBuffer buff = sample.getDataBuffer();
                ModelByteBuffer buff24 = sample.getData24Buffer();

                if (startAddrsOffset != 0 || endAddrsOffset != 0) {
                    buff = buff.subbuffer(startAddrsOffset * 2,
                            buff.capacity() + endAddrsOffset * 2);
                    if (buff24 != null) {
                        buff24 = buff24.subbuffer(startAddrsOffset,
                                buff24.capacity() + endAddrsOffset);
                    }

                    /*
                    if (startAddrsOffset < 0)
                        startAddrsOffset = 0;
                    if (endAddrsOffset > (buff.capacity()/2-startAddrsOffset))
                        startAddrsOffset = (int)buff.capacity()/2-startAddrsOffset;
                    byte[] data = buff.array();
                    int off = (int)buff.arrayOffset() + startAddrsOffset*2;
                    int len = (int)buff.capacity() + endAddrsOffset*2;
                    if (off+len > data.length)
                        len = data.length - off;
                    buff = new ModelByteBuffer(data, off, len);
                    if(buff24 != null) {
                        data = buff.array();
                        off = (int)buff.arrayOffset() + startAddrsOffset;
                        len = (int)buff.capacity() + endAddrsOffset;
                        buff24 = new ModelByteBuffer(data, off, len);
                    }
                    */
                }

                ModelByteBufferWavetable osc = new ModelByteBufferWavetable(
                        buff, sample.getFormat(), pitchcorrection);
                if (buff24 != null)
                    osc.set8BitExtensionBuffer(buff24);

                Map<Integer, Short> generators = new HashMap<>();
                if (layerglobal != null)
                    generators.putAll(layerglobal.getGenerators());
                generators.putAll(layerzone.getGenerators());
                for (Map.Entry<Integer, Short> gen : pgenerators.entrySet()) {
                    short val;
                    if (!generators.containsKey(gen.getKey()))
                        val = layerzone.getShort(gen.getKey());
                    else
                        val = generators.get(gen.getKey());
                    val += gen.getValue();
                    generators.put(gen.getKey(), val);
                }

                // SampleMode:
                // 0 indicates a sound reproduced with no loop
                // 1 indicates a sound which loops continuously
                // 2 is unused but should be interpreted as indicating no loop
                // 3 indicates a sound which loops for the duration of key
                //   depression then proceeds to play the remainder of the sample.
                int sampleMode = getGeneratorValue(generators,
                        SF2Region.GENERATOR_SAMPLEMODES);
                if ((sampleMode == 1) || (sampleMode == 3)) {
                    if (sample.startLoop >= 0 && sample.endLoop > 0) {
                        osc.setLoopStart((int)(sample.startLoop
                                + startloopAddrsOffset));
                        osc.setLoopLength((int)(sample.endLoop - sample.startLoop
                                + endloopAddrsOffset - startloopAddrsOffset));
                        if (sampleMode == 1)
                            osc.setLoopType(ModelWavetable.LOOP_TYPE_FORWARD);
                        if (sampleMode == 3)
                            osc.setLoopType(ModelWavetable.LOOP_TYPE_RELEASE);
                    }
                }
                performer.getOscillators().add(osc);


                short volDelay = getGeneratorValue(generators,
                        SF2Region.GENERATOR_DELAYVOLENV);
                short volAttack = getGeneratorValue(generators,
                        SF2Region.GENERATOR_ATTACKVOLENV);
                short volHold = getGeneratorValue(generators,
                        SF2Region.GENERATOR_HOLDVOLENV);
                short volDecay = getGeneratorValue(generators,
                        SF2Region.GENERATOR_DECAYVOLENV);
                short volSustain = getGeneratorValue(generators,
                        SF2Region.GENERATOR_SUSTAINVOLENV);
                short volRelease = getGeneratorValue(generators,
                        SF2Region.GENERATOR_RELEASEVOLENV);

                if (volHold != -12000) {
                    short volKeyNumToHold = getGeneratorValue(generators,
                            SF2Region.GENERATOR_KEYNUMTOVOLENVHOLD);
                    volHold += 60 * volKeyNumToHold;
                    float fvalue = -volKeyNumToHold * 128;
                    ModelIdentifier src = ModelSource.SOURCE_NOTEON_KEYNUMBER;
                    ModelIdentifier dest = ModelDestination.DESTINATION_EG1_HOLD;
                    performer.getConnectionBlocks().add(
                        new ModelConnectionBlock(new ModelSource(src), fvalue,
                            new ModelDestination(dest)));
                }
                if (volDecay != -12000) {
                    short volKeyNumToDecay = getGeneratorValue(generators,
                            SF2Region.GENERATOR_KEYNUMTOVOLENVDECAY);
                    volDecay += 60 * volKeyNumToDecay;
                    float fvalue = -volKeyNumToDecay * 128;
                    ModelIdentifier src = ModelSource.SOURCE_NOTEON_KEYNUMBER;
                    ModelIdentifier dest = ModelDestination.DESTINATION_EG1_DECAY;
                    performer.getConnectionBlocks().add(
                        new ModelConnectionBlock(new ModelSource(src), fvalue,
                            new ModelDestination(dest)));
                }

                addTimecentValue(performer,
                        ModelDestination.DESTINATION_EG1_DELAY, volDelay);
                addTimecentValue(performer,
                        ModelDestination.DESTINATION_EG1_ATTACK, volAttack);
                addTimecentValue(performer,
                        ModelDestination.DESTINATION_EG1_HOLD, volHold);
                addTimecentValue(performer,
                        ModelDestination.DESTINATION_EG1_DECAY, volDecay);
                //float fvolsustain = (960-volSustain)*(1000.0f/960.0f);

                volSustain = (short)(1000 - volSustain);
                if (volSustain < 0)
                    volSustain = 0;
                if (volSustain > 1000)
                    volSustain = 1000;

                addValue(performer,
                        ModelDestination.DESTINATION_EG1_SUSTAIN, volSustain);
                addTimecentValue(performer,
                        ModelDestination.DESTINATION_EG1_RELEASE, volRelease);

                if (getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODENVTOFILTERFC) != 0
                        || getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODENVTOPITCH) != 0) {
                    short modDelay = getGeneratorValue(generators,
                            SF2Region.GENERATOR_DELAYMODENV);
                    short modAttack = getGeneratorValue(generators,
                            SF2Region.GENERATOR_ATTACKMODENV);
                    short modHold = getGeneratorValue(generators,
                            SF2Region.GENERATOR_HOLDMODENV);
                    short modDecay = getGeneratorValue(generators,
                            SF2Region.GENERATOR_DECAYMODENV);
                    short modSustain = getGeneratorValue(generators,
                            SF2Region.GENERATOR_SUSTAINMODENV);
                    short modRelease = getGeneratorValue(generators,
                            SF2Region.GENERATOR_RELEASEMODENV);


                    if (modHold != -12000) {
                        short modKeyNumToHold = getGeneratorValue(generators,
                                SF2Region.GENERATOR_KEYNUMTOMODENVHOLD);
                        modHold += 60 * modKeyNumToHold;
                        float fvalue = -modKeyNumToHold * 128;
                        ModelIdentifier src = ModelSource.SOURCE_NOTEON_KEYNUMBER;
                        ModelIdentifier dest = ModelDestination.DESTINATION_EG2_HOLD;
                        performer.getConnectionBlocks().add(
                            new ModelConnectionBlock(new ModelSource(src),
                                fvalue, new ModelDestination(dest)));
                    }
                    if (modDecay != -12000) {
                        short modKeyNumToDecay = getGeneratorValue(generators,
                                SF2Region.GENERATOR_KEYNUMTOMODENVDECAY);
                        modDecay += 60 * modKeyNumToDecay;
                        float fvalue = -modKeyNumToDecay * 128;
                        ModelIdentifier src = ModelSource.SOURCE_NOTEON_KEYNUMBER;
                        ModelIdentifier dest = ModelDestination.DESTINATION_EG2_DECAY;
                        performer.getConnectionBlocks().add(
                            new ModelConnectionBlock(new ModelSource(src),
                                fvalue, new ModelDestination(dest)));
                    }

                    addTimecentValue(performer,
                            ModelDestination.DESTINATION_EG2_DELAY, modDelay);
                    addTimecentValue(performer,
                            ModelDestination.DESTINATION_EG2_ATTACK, modAttack);
                    addTimecentValue(performer,
                            ModelDestination.DESTINATION_EG2_HOLD, modHold);
                    addTimecentValue(performer,
                            ModelDestination.DESTINATION_EG2_DECAY, modDecay);
                    if (modSustain < 0)
                        modSustain = 0;
                    if (modSustain > 1000)
                        modSustain = 1000;
                    addValue(performer, ModelDestination.DESTINATION_EG2_SUSTAIN,
                            1000 - modSustain);
                    addTimecentValue(performer,
                            ModelDestination.DESTINATION_EG2_RELEASE, modRelease);

                    if (getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODENVTOFILTERFC) != 0) {
                        double fvalue = getGeneratorValue(generators,
                                SF2Region.GENERATOR_MODENVTOFILTERFC);
                        ModelIdentifier src = ModelSource.SOURCE_EG2;
                        ModelIdentifier dest
                                = ModelDestination.DESTINATION_FILTER_FREQ;
                        performer.getConnectionBlocks().add(
                            new ModelConnectionBlock(new ModelSource(src),
                                fvalue, new ModelDestination(dest)));
                    }

                    if (getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODENVTOPITCH) != 0) {
                        double fvalue = getGeneratorValue(generators,
                                SF2Region.GENERATOR_MODENVTOPITCH);
                        ModelIdentifier src = ModelSource.SOURCE_EG2;
                        ModelIdentifier dest = ModelDestination.DESTINATION_PITCH;
                        performer.getConnectionBlocks().add(
                            new ModelConnectionBlock(new ModelSource(src),
                                fvalue, new ModelDestination(dest)));
                    }

                }

                if (getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODLFOTOFILTERFC) != 0
                        || getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODLFOTOPITCH) != 0
                        || getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODLFOTOVOLUME) != 0) {
                    short lfo_freq = getGeneratorValue(generators,
                            SF2Region.GENERATOR_FREQMODLFO);
                    short lfo_delay = getGeneratorValue(generators,
                            SF2Region.GENERATOR_DELAYMODLFO);
                    addTimecentValue(performer,
                            ModelDestination.DESTINATION_LFO1_DELAY, lfo_delay);
                    addValue(performer,
                            ModelDestination.DESTINATION_LFO1_FREQ, lfo_freq);
                }

                short vib_freq = getGeneratorValue(generators,
                        SF2Region.GENERATOR_FREQVIBLFO);
                short vib_delay = getGeneratorValue(generators,
                        SF2Region.GENERATOR_DELAYVIBLFO);
                addTimecentValue(performer,
                        ModelDestination.DESTINATION_LFO2_DELAY, vib_delay);
                addValue(performer,
                        ModelDestination.DESTINATION_LFO2_FREQ, vib_freq);


                if (getGeneratorValue(generators,
                        SF2Region.GENERATOR_VIBLFOTOPITCH) != 0) {
                    double fvalue = getGeneratorValue(generators,
                            SF2Region.GENERATOR_VIBLFOTOPITCH);
                    ModelIdentifier src = ModelSource.SOURCE_LFO2;
                    ModelIdentifier dest = ModelDestination.DESTINATION_PITCH;
                    performer.getConnectionBlocks().add(
                        new ModelConnectionBlock(
                            new ModelSource(src,
                                ModelStandardTransform.DIRECTION_MIN2MAX,
                                ModelStandardTransform.POLARITY_BIPOLAR),
                            fvalue, new ModelDestination(dest)));
                }

                if (getGeneratorValue(generators,
                        SF2Region.GENERATOR_MODLFOTOFILTERFC) != 0) {
                    double fvalue = getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODLFOTOFILTERFC);
                    ModelIdentifier src = ModelSource.SOURCE_LFO1;
                    ModelIdentifier dest = ModelDestination.DESTINATION_FILTER_FREQ;
                    performer.getConnectionBlocks().add(
                        new ModelConnectionBlock(
                            new ModelSource(src,
                                ModelStandardTransform.DIRECTION_MIN2MAX,
                                ModelStandardTransform.POLARITY_BIPOLAR),
                            fvalue, new ModelDestination(dest)));
                }

                if (getGeneratorValue(generators,
                        SF2Region.GENERATOR_MODLFOTOPITCH) != 0) {
                    double fvalue = getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODLFOTOPITCH);
                    ModelIdentifier src = ModelSource.SOURCE_LFO1;
                    ModelIdentifier dest = ModelDestination.DESTINATION_PITCH;
                    performer.getConnectionBlocks().add(
                        new ModelConnectionBlock(
                            new ModelSource(src,
                                ModelStandardTransform.DIRECTION_MIN2MAX,
                                ModelStandardTransform.POLARITY_BIPOLAR),
                            fvalue, new ModelDestination(dest)));
                }

                if (getGeneratorValue(generators,
                        SF2Region.GENERATOR_MODLFOTOVOLUME) != 0) {
                    double fvalue = getGeneratorValue(generators,
                            SF2Region.GENERATOR_MODLFOTOVOLUME);
                    ModelIdentifier src = ModelSource.SOURCE_LFO1;
                    ModelIdentifier dest = ModelDestination.DESTINATION_GAIN;
                    performer.getConnectionBlocks().add(
                        new ModelConnectionBlock(
                            new ModelSource(src,
                                ModelStandardTransform.DIRECTION_MIN2MAX,
                                ModelStandardTransform.POLARITY_BIPOLAR),
                            fvalue, new ModelDestination(dest)));
                }

                if (layerzone.getShort(SF2Region.GENERATOR_KEYNUM) != -1) {
                    double val = layerzone.getShort(SF2Region.GENERATOR_KEYNUM)/128.0;
                    addValue(performer, ModelDestination.DESTINATION_KEYNUMBER, val);
                }

                if (layerzone.getShort(SF2Region.GENERATOR_VELOCITY) != -1) {
                    double val = layerzone.getShort(SF2Region.GENERATOR_VELOCITY)
                                 / 128.0;
                    addValue(performer, ModelDestination.DESTINATION_VELOCITY, val);
                }

                if (getGeneratorValue(generators,
                        SF2Region.GENERATOR_INITIALFILTERFC) < 13500) {
                    short filter_freq = getGeneratorValue(generators,
                            SF2Region.GENERATOR_INITIALFILTERFC);
                    short filter_q = getGeneratorValue(generators,
                            SF2Region.GENERATOR_INITIALFILTERQ);
                    addValue(performer,
                            ModelDestination.DESTINATION_FILTER_FREQ, filter_freq);
                    addValue(performer,
                            ModelDestination.DESTINATION_FILTER_Q, filter_q);
                }

                int tune = 100 * getGeneratorValue(generators,
                        SF2Region.GENERATOR_COARSETUNE);
                tune += getGeneratorValue(generators,
                        SF2Region.GENERATOR_FINETUNE);
                if (tune != 0) {
                    addValue(performer,
                            ModelDestination.DESTINATION_PITCH, (short) tune);
                }
                if (getGeneratorValue(generators, SF2Region.GENERATOR_PAN) != 0) {
                    short val = getGeneratorValue(generators,
                            SF2Region.GENERATOR_PAN);
                    addValue(performer, ModelDestination.DESTINATION_PAN, val);
                }
                if (getGeneratorValue(generators, SF2Region.GENERATOR_INITIALATTENUATION) != 0) {
                    short val = getGeneratorValue(generators,
                            SF2Region.GENERATOR_INITIALATTENUATION);
                    addValue(performer,
                            ModelDestination.DESTINATION_GAIN, -0.376287f * val);
                }
                if (getGeneratorValue(generators,
                        SF2Region.GENERATOR_CHORUSEFFECTSSEND) != 0) {
                    short val = getGeneratorValue(generators,
                            SF2Region.GENERATOR_CHORUSEFFECTSSEND);
                    addValue(performer, ModelDestination.DESTINATION_CHORUS, val);
                }
                if (getGeneratorValue(generators,
                        SF2Region.GENERATOR_REVERBEFFECTSSEND) != 0) {
                    short val = getGeneratorValue(generators,
                            SF2Region.GENERATOR_REVERBEFFECTSSEND);
                    addValue(performer, ModelDestination.DESTINATION_REVERB, val);
                }
                if (getGeneratorValue(generators,
                        SF2Region.GENERATOR_SCALETUNING) != 100) {
                    short fvalue = getGeneratorValue(generators,
                            SF2Region.GENERATOR_SCALETUNING);
                    if (fvalue == 0) {
                        ModelIdentifier dest = ModelDestination.DESTINATION_PITCH;
                        performer.getConnectionBlocks().add(
                            new ModelConnectionBlock(null, rootkey * 100,
                                new ModelDestination(dest)));
                    } else {
                        ModelIdentifier dest = ModelDestination.DESTINATION_PITCH;
                        performer.getConnectionBlocks().add(
                            new ModelConnectionBlock(null, rootkey * (100 - fvalue),
                                new ModelDestination(dest)));
                    }

                    ModelIdentifier src = ModelSource.SOURCE_NOTEON_KEYNUMBER;
                    ModelIdentifier dest = ModelDestination.DESTINATION_PITCH;
                    performer.getConnectionBlocks().add(
                        new ModelConnectionBlock(new ModelSource(src),
                            128 * fvalue, new ModelDestination(dest)));

                }

                performer.getConnectionBlocks().add(
                    new ModelConnectionBlock(
                        new ModelSource(ModelSource.SOURCE_NOTEON_VELOCITY,
                            new ModelTransform() {
                                @Override
                                public double transform(double value) {
                                    if (value < 0.5)
                                        return 1 - value * 2;
                                    else
                                        return 0;
                                }
                            }),
                        -2400,
                        new ModelDestination(
                            ModelDestination.DESTINATION_FILTER_FREQ)));


                performer.getConnectionBlocks().add(
                    new ModelConnectionBlock(
                        new ModelSource(ModelSource.SOURCE_LFO2,
                            ModelStandardTransform.DIRECTION_MIN2MAX,
                            ModelStandardTransform.POLARITY_BIPOLAR,
                            ModelStandardTransform.TRANSFORM_LINEAR),
                        new ModelSource(new ModelIdentifier("midi_cc", "1", 0),
                            ModelStandardTransform.DIRECTION_MIN2MAX,
                            ModelStandardTransform.POLARITY_UNIPOLAR,
                            ModelStandardTransform.TRANSFORM_LINEAR),
                        50, new ModelDestination(
                            ModelDestination.DESTINATION_PITCH)));

                if (layer.getGlobalRegion() != null) {
                    for (SF2Modulator modulator
                            : layer.getGlobalRegion().getModulators()) {
                        convertModulator(performer, modulator);
                    }
                }
                for (SF2Modulator modulator : layerzone.getModulators())
                    convertModulator(performer, modulator);

                if (presetglobal != null) {
                    for (SF2Modulator modulator : presetglobal.getModulators())
                        convertModulator(performer, modulator);
                }
                for (SF2Modulator modulator : presetzone.getModulators())
                    convertModulator(performer, modulator);

            }
        }
        return performers;
    }

    private void convertModulator(ModelPerformer performer,
            SF2Modulator modulator) {
        ModelSource src1 = convertSource(modulator.getSourceOperator());
        ModelSource src2 = convertSource(modulator.getAmountSourceOperator());
        if (src1 == null && modulator.getSourceOperator() != 0)
            return;
        if (src2 == null && modulator.getAmountSourceOperator() != 0)
            return;
        double amount = modulator.getAmount();
        double[] amountcorrection = new double[1];
        ModelSource[] extrasrc = new ModelSource[1];
        amountcorrection[0] = 1;
        ModelDestination dst = convertDestination(
                modulator.getDestinationOperator(), amountcorrection, extrasrc);
        amount *= amountcorrection[0];
        if (dst == null)
            return;
        if (modulator.getTransportOperator() == SF2Modulator.TRANSFORM_ABSOLUTE) {
            ((ModelStandardTransform)dst.getTransform()).setTransform(
                    ModelStandardTransform.TRANSFORM_ABSOLUTE);
        }
        ModelConnectionBlock conn = new ModelConnectionBlock(src1, src2, amount, dst);
        if (extrasrc[0] != null)
            conn.addSource(extrasrc[0]);
        performer.getConnectionBlocks().add(conn);

    }

    private static ModelSource convertSource(int src) {
        if (src == 0)
            return null;
        ModelIdentifier id = null;
        int idsrc = src & 0x7F;
        if ((src & SF2Modulator.SOURCE_MIDI_CONTROL) != 0) {
            id = new ModelIdentifier("midi_cc", Integer.toString(idsrc));
        } else {
            if (idsrc == SF2Modulator.SOURCE_NOTE_ON_VELOCITY)
                id = ModelSource.SOURCE_NOTEON_VELOCITY;
            if (idsrc == SF2Modulator.SOURCE_NOTE_ON_KEYNUMBER)
                id = ModelSource.SOURCE_NOTEON_KEYNUMBER;
            if (idsrc == SF2Modulator.SOURCE_POLY_PRESSURE)
                id = ModelSource.SOURCE_MIDI_POLY_PRESSURE;
            if (idsrc == SF2Modulator.SOURCE_CHANNEL_PRESSURE)
                id = ModelSource.SOURCE_MIDI_CHANNEL_PRESSURE;
            if (idsrc == SF2Modulator.SOURCE_PITCH_WHEEL)
                id = ModelSource.SOURCE_MIDI_PITCH;
            if (idsrc == SF2Modulator.SOURCE_PITCH_SENSITIVITY)
                id = new ModelIdentifier("midi_rpn", "0");
        }
        if (id == null)
            return null;

        ModelSource msrc = new ModelSource(id);
        ModelStandardTransform transform
                = (ModelStandardTransform) msrc.getTransform();

        if ((SF2Modulator.SOURCE_DIRECTION_MAX_MIN & src) != 0)
            transform.setDirection(ModelStandardTransform.DIRECTION_MAX2MIN);
        else
            transform.setDirection(ModelStandardTransform.DIRECTION_MIN2MAX);

        if ((SF2Modulator.SOURCE_POLARITY_BIPOLAR & src) != 0)
            transform.setPolarity(ModelStandardTransform.POLARITY_BIPOLAR);
        else
            transform.setPolarity(ModelStandardTransform.POLARITY_UNIPOLAR);

        if ((SF2Modulator.SOURCE_TYPE_CONCAVE & src) != 0)
            transform.setTransform(ModelStandardTransform.TRANSFORM_CONCAVE);
        if ((SF2Modulator.SOURCE_TYPE_CONVEX & src) != 0)
            transform.setTransform(ModelStandardTransform.TRANSFORM_CONVEX);
        if ((SF2Modulator.SOURCE_TYPE_SWITCH & src) != 0)
            transform.setTransform(ModelStandardTransform.TRANSFORM_SWITCH);

        return msrc;
    }

    static ModelDestination convertDestination(int dst,
            double[] amountcorrection, ModelSource[] extrasrc) {
        ModelIdentifier id = null;
        switch (dst) {
            case SF2Region.GENERATOR_INITIALFILTERFC:
                id = ModelDestination.DESTINATION_FILTER_FREQ;
                break;
            case SF2Region.GENERATOR_INITIALFILTERQ:
                id = ModelDestination.DESTINATION_FILTER_Q;
                break;
            case SF2Region.GENERATOR_CHORUSEFFECTSSEND:
                id = ModelDestination.DESTINATION_CHORUS;
                break;
            case SF2Region.GENERATOR_REVERBEFFECTSSEND:
                id = ModelDestination.DESTINATION_REVERB;
                break;
            case SF2Region.GENERATOR_PAN:
                id = ModelDestination.DESTINATION_PAN;
                break;
            case SF2Region.GENERATOR_DELAYMODLFO:
                id = ModelDestination.DESTINATION_LFO1_DELAY;
                break;
            case SF2Region.GENERATOR_FREQMODLFO:
                id = ModelDestination.DESTINATION_LFO1_FREQ;
                break;
            case SF2Region.GENERATOR_DELAYVIBLFO:
                id = ModelDestination.DESTINATION_LFO2_DELAY;
                break;
            case SF2Region.GENERATOR_FREQVIBLFO:
                id = ModelDestination.DESTINATION_LFO2_FREQ;
                break;

            case SF2Region.GENERATOR_DELAYMODENV:
                id = ModelDestination.DESTINATION_EG2_DELAY;
                break;
            case SF2Region.GENERATOR_ATTACKMODENV:
                id = ModelDestination.DESTINATION_EG2_ATTACK;
                break;
            case SF2Region.GENERATOR_HOLDMODENV:
                id = ModelDestination.DESTINATION_EG2_HOLD;
                break;
            case SF2Region.GENERATOR_DECAYMODENV:
                id = ModelDestination.DESTINATION_EG2_DECAY;
                break;
            case SF2Region.GENERATOR_SUSTAINMODENV:
                id = ModelDestination.DESTINATION_EG2_SUSTAIN;
                amountcorrection[0] = -1;
                break;
            case SF2Region.GENERATOR_RELEASEMODENV:
                id = ModelDestination.DESTINATION_EG2_RELEASE;
                break;
            case SF2Region.GENERATOR_DELAYVOLENV:
                id = ModelDestination.DESTINATION_EG1_DELAY;
                break;
            case SF2Region.GENERATOR_ATTACKVOLENV:
                id = ModelDestination.DESTINATION_EG1_ATTACK;
                break;
            case SF2Region.GENERATOR_HOLDVOLENV:
                id = ModelDestination.DESTINATION_EG1_HOLD;
                break;
            case SF2Region.GENERATOR_DECAYVOLENV:
                id = ModelDestination.DESTINATION_EG1_DECAY;
                break;
            case SF2Region.GENERATOR_SUSTAINVOLENV:
                id = ModelDestination.DESTINATION_EG1_SUSTAIN;
                amountcorrection[0] = -1;
                break;
            case SF2Region.GENERATOR_RELEASEVOLENV:
                id = ModelDestination.DESTINATION_EG1_RELEASE;
                break;
            case SF2Region.GENERATOR_KEYNUM:
                id = ModelDestination.DESTINATION_KEYNUMBER;
                break;
            case SF2Region.GENERATOR_VELOCITY:
                id = ModelDestination.DESTINATION_VELOCITY;
                break;

            case SF2Region.GENERATOR_COARSETUNE:
                amountcorrection[0] = 100;
                id = ModelDestination.DESTINATION_PITCH;
                break;

            case SF2Region.GENERATOR_FINETUNE:
                id = ModelDestination.DESTINATION_PITCH;
                break;

            case SF2Region.GENERATOR_INITIALATTENUATION:
                id = ModelDestination.DESTINATION_GAIN;
                amountcorrection[0] = -0.376287f;
                break;

            case SF2Region.GENERATOR_VIBLFOTOPITCH:
                id = ModelDestination.DESTINATION_PITCH;
                extrasrc[0] = new ModelSource(
                        ModelSource.SOURCE_LFO2,
                        ModelStandardTransform.DIRECTION_MIN2MAX,
                        ModelStandardTransform.POLARITY_BIPOLAR);
                break;

            case SF2Region.GENERATOR_MODLFOTOPITCH:
                id = ModelDestination.DESTINATION_PITCH;
                extrasrc[0] = new ModelSource(
                        ModelSource.SOURCE_LFO1,
                        ModelStandardTransform.DIRECTION_MIN2MAX,
                        ModelStandardTransform.POLARITY_BIPOLAR);
                break;

            case SF2Region.GENERATOR_MODLFOTOFILTERFC:
                id = ModelDestination.DESTINATION_FILTER_FREQ;
                extrasrc[0] = new ModelSource(
                        ModelSource.SOURCE_LFO1,
                        ModelStandardTransform.DIRECTION_MIN2MAX,
                        ModelStandardTransform.POLARITY_BIPOLAR);
                break;

            case SF2Region.GENERATOR_MODLFOTOVOLUME:
                id = ModelDestination.DESTINATION_GAIN;
                amountcorrection[0] = -0.376287f;
                extrasrc[0] = new ModelSource(
                        ModelSource.SOURCE_LFO1,
                        ModelStandardTransform.DIRECTION_MIN2MAX,
                        ModelStandardTransform.POLARITY_BIPOLAR);
                break;

            case SF2Region.GENERATOR_MODENVTOPITCH:
                id = ModelDestination.DESTINATION_PITCH;
                extrasrc[0] = new ModelSource(
                        ModelSource.SOURCE_EG2,
                        ModelStandardTransform.DIRECTION_MIN2MAX,
                        ModelStandardTransform.POLARITY_BIPOLAR);
                break;

            case SF2Region.GENERATOR_MODENVTOFILTERFC:
                id = ModelDestination.DESTINATION_FILTER_FREQ;
                extrasrc[0] = new ModelSource(
                        ModelSource.SOURCE_EG2,
                        ModelStandardTransform.DIRECTION_MIN2MAX,
                        ModelStandardTransform.POLARITY_BIPOLAR);
                break;

            default:
                break;
        }
        if (id != null)
            return new ModelDestination(id);
        return null;
    }

    private void addTimecentValue(ModelPerformer performer,
            ModelIdentifier dest, short value) {
        double fvalue;
        if (value == -12000)
            fvalue = Double.NEGATIVE_INFINITY;
        else
            fvalue = value;
        performer.getConnectionBlocks().add(
                new ModelConnectionBlock(fvalue, new ModelDestination(dest)));
    }

    private void addValue(ModelPerformer performer,
            ModelIdentifier dest, short value) {
        double fvalue = value;
        performer.getConnectionBlocks().add(
                new ModelConnectionBlock(fvalue, new ModelDestination(dest)));
    }

    private void addValue(ModelPerformer performer,
            ModelIdentifier dest, double value) {
        double fvalue = value;
        performer.getConnectionBlocks().add(
                new ModelConnectionBlock(fvalue, new ModelDestination(dest)));
    }

    private short getGeneratorValue(Map<Integer, Short> generators, int gen) {
        if (generators.containsKey(gen))
            return generators.get(gen);
        return SF2Region.getDefaultValue(gen);
    }
}
