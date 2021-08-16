/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * A SoundFont 2.04 soundbank reader.
 *
 * Based on SoundFont 2.04 specification from:
 * <p>  http://developer.creative.com <br>
 *      http://www.soundfont.com/ ;
 *
 * @author Karl Helgason
 */
public final class SF2Soundbank implements Soundbank {

    // version of the Sound Font RIFF file
    int major = 2;
    int minor = 1;
    // target Sound Engine
    String targetEngine = "EMU8000";
    // Sound Font Bank Name
    String name = "untitled";
    // Sound ROM Name
    String romName = null;
    // Sound ROM Version
    int romVersionMajor = -1;
    int romVersionMinor = -1;
    // Date of Creation of the Bank
    String creationDate = null;
    // Sound Designers and Engineers for the Bank
    String engineers = null;
    // Product for which the Bank was intended
    String product = null;
    // Copyright message
    String copyright = null;
    // Comments
    String comments = null;
    // The SoundFont tools used to create and alter the bank
    String tools = null;
    // The Sample Data loaded from the SoundFont
    private ModelByteBuffer sampleData = null;
    private ModelByteBuffer sampleData24 = null;
    private File sampleFile = null;
    private boolean largeFormat = false;
    private final List<SF2Instrument> instruments = new ArrayList<>();
    private final List<SF2Layer> layers = new ArrayList<>();
    private final List<SF2Sample> samples = new ArrayList<>();

    public SF2Soundbank() {
    }

    public SF2Soundbank(URL url) throws IOException {

        InputStream is = url.openStream();
        try {
            readSoundbank(is);
        } finally {
            is.close();
        }
    }

    public SF2Soundbank(File file) throws IOException {
        largeFormat = true;
        sampleFile = file;
        InputStream is = new FileInputStream(file);
        try {
            readSoundbank(is);
        } finally {
            is.close();
        }
    }

    public SF2Soundbank(InputStream inputstream) throws IOException {
        readSoundbank(inputstream);
    }

    private void readSoundbank(InputStream inputstream) throws IOException {
        RIFFReader riff = new RIFFReader(inputstream);
        if (!riff.getFormat().equals("RIFF")) {
            throw new RIFFInvalidFormatException(
                    "Input stream is not a valid RIFF stream!");
        }
        if (!riff.getType().equals("sfbk")) {
            throw new RIFFInvalidFormatException(
                    "Input stream is not a valid SoundFont!");
        }
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            if (chunk.getFormat().equals("LIST")) {
                if (chunk.getType().equals("INFO"))
                    readInfoChunk(chunk);
                if (chunk.getType().equals("sdta"))
                    readSdtaChunk(chunk);
                if (chunk.getType().equals("pdta"))
                    readPdtaChunk(chunk);
            }
        }
    }

    private void readInfoChunk(RIFFReader riff) throws IOException {
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            String format = chunk.getFormat();
            if (format.equals("ifil")) {
                major = chunk.readUnsignedShort();
                minor = chunk.readUnsignedShort();
            } else if (format.equals("isng")) {
                this.targetEngine = chunk.readString(chunk.available());
            } else if (format.equals("INAM")) {
                this.name = chunk.readString(chunk.available());
            } else if (format.equals("irom")) {
                this.romName = chunk.readString(chunk.available());
            } else if (format.equals("iver")) {
                romVersionMajor = chunk.readUnsignedShort();
                romVersionMinor = chunk.readUnsignedShort();
            } else if (format.equals("ICRD")) {
                this.creationDate = chunk.readString(chunk.available());
            } else if (format.equals("IENG")) {
                this.engineers = chunk.readString(chunk.available());
            } else if (format.equals("IPRD")) {
                this.product = chunk.readString(chunk.available());
            } else if (format.equals("ICOP")) {
                this.copyright = chunk.readString(chunk.available());
            } else if (format.equals("ICMT")) {
                this.comments = chunk.readString(chunk.available());
            } else if (format.equals("ISFT")) {
                this.tools = chunk.readString(chunk.available());
            }

        }
    }

    private void readSdtaChunk(RIFFReader riff) throws IOException {
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            if (chunk.getFormat().equals("smpl")) {
                if (!largeFormat) {
                    byte[] sampleData = new byte[chunk.available()];

                    int read = 0;
                    int avail = chunk.available();
                    while (read != avail) {
                        if (avail - read > 65536) {
                            chunk.readFully(sampleData, read, 65536);
                            read += 65536;
                        } else {
                            chunk.readFully(sampleData, read, avail - read);
                            read = avail;
                        }

                    }
                    this.sampleData = new ModelByteBuffer(sampleData);
                    //chunk.read(sampleData);
                } else {
                    this.sampleData = new ModelByteBuffer(sampleFile,
                            chunk.getFilePointer(), chunk.available());
                }
            }
            if (chunk.getFormat().equals("sm24")) {
                if (!largeFormat) {
                    byte[] sampleData24 = new byte[chunk.available()];
                    //chunk.read(sampleData24);

                    int read = 0;
                    int avail = chunk.available();
                    while (read != avail) {
                        if (avail - read > 65536) {
                            chunk.readFully(sampleData24, read, 65536);
                            read += 65536;
                        } else {
                            chunk.readFully(sampleData24, read, avail - read);
                            read = avail;
                        }

                    }
                    this.sampleData24 = new ModelByteBuffer(sampleData24);
                } else {
                    this.sampleData24 = new ModelByteBuffer(sampleFile,
                            chunk.getFilePointer(), chunk.available());
                }

            }
        }
    }

    private void readPdtaChunk(RIFFReader riff) throws IOException {

        List<SF2Instrument> presets = new ArrayList<>();
        List<Integer> presets_bagNdx = new ArrayList<>();
        List<SF2InstrumentRegion> presets_splits_gen = new ArrayList<>();
        List<SF2InstrumentRegion> presets_splits_mod = new ArrayList<>();

        List<SF2Layer> instruments = new ArrayList<>();
        List<Integer> instruments_bagNdx = new ArrayList<>();
        List<SF2LayerRegion> instruments_splits_gen = new ArrayList<>();
        List<SF2LayerRegion> instruments_splits_mod = new ArrayList<>();

        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            String format = chunk.getFormat();
            if (format.equals("phdr")) {
                // Preset Header / Instrument
                if (chunk.available() % 38 != 0)
                    throw new RIFFInvalidDataException();
                int count = chunk.available() / 38;
                for (int i = 0; i < count; i++) {
                    SF2Instrument preset = new SF2Instrument(this);
                    preset.name = chunk.readString(20);
                    preset.preset = chunk.readUnsignedShort();
                    preset.bank = chunk.readUnsignedShort();
                    presets_bagNdx.add(chunk.readUnsignedShort());
                    preset.library = chunk.readUnsignedInt();
                    preset.genre = chunk.readUnsignedInt();
                    preset.morphology = chunk.readUnsignedInt();
                    presets.add(preset);
                    if (i != count - 1)
                        this.instruments.add(preset);
                }
            } else if (format.equals("pbag")) {
                // Preset Zones / Instruments splits
                if (chunk.available() % 4 != 0)
                    throw new RIFFInvalidDataException();
                int count = chunk.available() / 4;

                // Skip first record
                {
                    int gencount = chunk.readUnsignedShort();
                    int modcount = chunk.readUnsignedShort();
                    while (presets_splits_gen.size() < gencount)
                        presets_splits_gen.add(null);
                    while (presets_splits_mod.size() < modcount)
                        presets_splits_mod.add(null);
                    count--;
                }

                if (presets_bagNdx.isEmpty()) {
                    throw new RIFFInvalidDataException();
                }
                int offset = presets_bagNdx.get(0);
                // Offset should be 0 (but just case)
                for (int i = 0; i < offset; i++) {
                    if (count == 0)
                        throw new RIFFInvalidDataException();
                    int gencount = chunk.readUnsignedShort();
                    int modcount = chunk.readUnsignedShort();
                    while (presets_splits_gen.size() < gencount)
                        presets_splits_gen.add(null);
                    while (presets_splits_mod.size() < modcount)
                        presets_splits_mod.add(null);
                    count--;
                }

                for (int i = 0; i < presets_bagNdx.size() - 1; i++) {
                    int zone_count = presets_bagNdx.get(i + 1)
                                     - presets_bagNdx.get(i);
                    SF2Instrument preset = presets.get(i);
                    for (int ii = 0; ii < zone_count; ii++) {
                        if (count == 0)
                            throw new RIFFInvalidDataException();
                        int gencount = chunk.readUnsignedShort();
                        int modcount = chunk.readUnsignedShort();
                        SF2InstrumentRegion split = new SF2InstrumentRegion();
                        preset.regions.add(split);
                        while (presets_splits_gen.size() < gencount)
                            presets_splits_gen.add(split);
                        while (presets_splits_mod.size() < modcount)
                            presets_splits_mod.add(split);
                        count--;
                    }
                }
            } else if (format.equals("pmod")) {
                // Preset Modulators / Split Modulators
                for (int i = 0; i < presets_splits_mod.size(); i++) {
                    SF2Modulator modulator = new SF2Modulator();
                    modulator.sourceOperator = chunk.readUnsignedShort();
                    modulator.destinationOperator = chunk.readUnsignedShort();
                    modulator.amount = chunk.readShort();
                    modulator.amountSourceOperator = chunk.readUnsignedShort();
                    modulator.transportOperator = chunk.readUnsignedShort();
                    SF2InstrumentRegion split = presets_splits_mod.get(i);
                    if (split != null)
                        split.modulators.add(modulator);
                }
            } else if (format.equals("pgen")) {
                // Preset Generators / Split Generators
                for (int i = 0; i < presets_splits_gen.size(); i++) {
                    int operator = chunk.readUnsignedShort();
                    short amount = chunk.readShort();
                    SF2InstrumentRegion split = presets_splits_gen.get(i);
                    if (split != null)
                        split.generators.put(operator, amount);
                }
            } else if (format.equals("inst")) {
                // Instrument Header / Layers
                if (chunk.available() % 22 != 0)
                    throw new RIFFInvalidDataException();
                int count = chunk.available() / 22;
                for (int i = 0; i < count; i++) {
                    SF2Layer layer = new SF2Layer(this);
                    layer.name = chunk.readString(20);
                    instruments_bagNdx.add(chunk.readUnsignedShort());
                    instruments.add(layer);
                    if (i != count - 1)
                        this.layers.add(layer);
                }
            } else if (format.equals("ibag")) {
                // Instrument Zones / Layer splits
                if (chunk.available() % 4 != 0)
                    throw new RIFFInvalidDataException();
                int count = chunk.available() / 4;

                // Skip first record
                {
                    int gencount = chunk.readUnsignedShort();
                    int modcount = chunk.readUnsignedShort();
                    while (instruments_splits_gen.size() < gencount)
                        instruments_splits_gen.add(null);
                    while (instruments_splits_mod.size() < modcount)
                        instruments_splits_mod.add(null);
                    count--;
                }

                if (instruments_bagNdx.isEmpty()) {
                    throw new RIFFInvalidDataException();
                }
                int offset = instruments_bagNdx.get(0);
                // Offset should be 0 (but just case)
                for (int i = 0; i < offset; i++) {
                    if (count == 0)
                        throw new RIFFInvalidDataException();
                    int gencount = chunk.readUnsignedShort();
                    int modcount = chunk.readUnsignedShort();
                    while (instruments_splits_gen.size() < gencount)
                        instruments_splits_gen.add(null);
                    while (instruments_splits_mod.size() < modcount)
                        instruments_splits_mod.add(null);
                    count--;
                }

                for (int i = 0; i < instruments_bagNdx.size() - 1; i++) {
                    int zone_count = instruments_bagNdx.get(i + 1) - instruments_bagNdx.get(i);
                    SF2Layer layer = layers.get(i);
                    for (int ii = 0; ii < zone_count; ii++) {
                        if (count == 0)
                            throw new RIFFInvalidDataException();
                        int gencount = chunk.readUnsignedShort();
                        int modcount = chunk.readUnsignedShort();
                        SF2LayerRegion split = new SF2LayerRegion();
                        layer.regions.add(split);
                        while (instruments_splits_gen.size() < gencount)
                            instruments_splits_gen.add(split);
                        while (instruments_splits_mod.size() < modcount)
                            instruments_splits_mod.add(split);
                        count--;
                    }
                }

            } else if (format.equals("imod")) {
                // Instrument Modulators / Split Modulators
                for (int i = 0; i < instruments_splits_mod.size(); i++) {
                    SF2Modulator modulator = new SF2Modulator();
                    modulator.sourceOperator = chunk.readUnsignedShort();
                    modulator.destinationOperator = chunk.readUnsignedShort();
                    modulator.amount = chunk.readShort();
                    modulator.amountSourceOperator = chunk.readUnsignedShort();
                    modulator.transportOperator = chunk.readUnsignedShort();
                    if (i < 0 || i >= instruments_splits_gen.size()) {
                        throw new RIFFInvalidDataException();
                    }
                    SF2LayerRegion split = instruments_splits_gen.get(i);
                    if (split != null)
                        split.modulators.add(modulator);
                }
            } else if (format.equals("igen")) {
                // Instrument Generators / Split Generators
                for (int i = 0; i < instruments_splits_gen.size(); i++) {
                    int operator = chunk.readUnsignedShort();
                    short amount = chunk.readShort();
                    SF2LayerRegion split = instruments_splits_gen.get(i);
                    if (split != null)
                        split.generators.put(operator, amount);
                }
            } else if (format.equals("shdr")) {
                // Sample Headers
                if (chunk.available() % 46 != 0)
                    throw new RIFFInvalidDataException();
                int count = chunk.available() / 46;
                for (int i = 0; i < count; i++) {
                    SF2Sample sample = new SF2Sample(this);
                    sample.name = chunk.readString(20);
                    long start = chunk.readUnsignedInt();
                    long end = chunk.readUnsignedInt();
                    if (sampleData != null)
                        sample.data = sampleData.subbuffer(start * 2, end * 2, true);
                    if (sampleData24 != null)
                        sample.data24 = sampleData24.subbuffer(start, end, true);
                    /*
                    sample.data = new ModelByteBuffer(sampleData, (int)(start*2),
                            (int)((end - start)*2));
                    if (sampleData24 != null)
                        sample.data24 = new ModelByteBuffer(sampleData24,
                                (int)start, (int)(end - start));
                     */
                    sample.startLoop = chunk.readUnsignedInt() - start;
                    sample.endLoop = chunk.readUnsignedInt() - start;
                    if (sample.startLoop < 0)
                        sample.startLoop = -1;
                    if (sample.endLoop < 0)
                        sample.endLoop = -1;
                    sample.sampleRate = chunk.readUnsignedInt();
                    sample.originalPitch = chunk.readUnsignedByte();
                    sample.pitchCorrection = chunk.readByte();
                    sample.sampleLink = chunk.readUnsignedShort();
                    sample.sampleType = chunk.readUnsignedShort();
                    if (i != count - 1)
                        this.samples.add(sample);
                }
            }
        }

        for (SF2Layer layer : this.layers) {
            Iterator<SF2LayerRegion> siter = layer.regions.iterator();
            SF2Region globalsplit = null;
            while (siter.hasNext()) {
                SF2LayerRegion split = siter.next();
                if (split.generators.get(SF2LayerRegion.GENERATOR_SAMPLEID) != null) {
                    int sampleid = split.generators.get(
                            SF2LayerRegion.GENERATOR_SAMPLEID);
                    split.generators.remove(SF2LayerRegion.GENERATOR_SAMPLEID);
                    if (sampleid < 0 || sampleid >= samples.size()) {
                        throw new RIFFInvalidDataException();
                    }
                    split.sample = samples.get(sampleid);
                } else {
                    globalsplit = split;
                }
            }
            if (globalsplit != null) {
                layer.getRegions().remove(globalsplit);
                SF2GlobalRegion gsplit = new SF2GlobalRegion();
                gsplit.generators = globalsplit.generators;
                gsplit.modulators = globalsplit.modulators;
                layer.setGlobalZone(gsplit);
            }
        }


        for (SF2Instrument instrument : this.instruments) {
            Iterator<SF2InstrumentRegion> siter = instrument.regions.iterator();
            SF2Region globalsplit = null;
            while (siter.hasNext()) {
                SF2InstrumentRegion split = siter.next();
                if (split.generators.get(SF2LayerRegion.GENERATOR_INSTRUMENT) != null) {
                    int instrumentid = split.generators.get(
                            SF2InstrumentRegion.GENERATOR_INSTRUMENT);
                    split.generators.remove(SF2LayerRegion.GENERATOR_INSTRUMENT);
                    if (instrumentid < 0 || instrumentid >= layers.size()) {
                        throw new RIFFInvalidDataException();
                    }
                    split.layer = layers.get(instrumentid);
                } else {
                    globalsplit = split;
                }
            }

            if (globalsplit != null) {
                instrument.getRegions().remove(globalsplit);
                SF2GlobalRegion gsplit = new SF2GlobalRegion();
                gsplit.generators = globalsplit.generators;
                gsplit.modulators = globalsplit.modulators;
                instrument.setGlobalZone(gsplit);
            }
        }

    }

    public void save(String name) throws IOException {
        writeSoundbank(new RIFFWriter(name, "sfbk"));
    }

    public void save(File file) throws IOException {
        writeSoundbank(new RIFFWriter(file, "sfbk"));
    }

    public void save(OutputStream out) throws IOException {
        writeSoundbank(new RIFFWriter(out, "sfbk"));
    }

    private void writeSoundbank(RIFFWriter writer) throws IOException {
        writeInfo(writer.writeList("INFO"));
        writeSdtaChunk(writer.writeList("sdta"));
        writePdtaChunk(writer.writeList("pdta"));
        writer.close();
    }

    private void writeInfoStringChunk(RIFFWriter writer, String name,
            String value) throws IOException {
        if (value == null)
            return;
        RIFFWriter chunk = writer.writeChunk(name);
        chunk.writeString(value);
        int len = value.getBytes(US_ASCII).length;
        chunk.write(0);
        len++;
        if (len % 2 != 0)
            chunk.write(0);
    }

    private void writeInfo(RIFFWriter writer) throws IOException {
        if (this.targetEngine == null)
            this.targetEngine = "EMU8000";
        if (this.name == null)
            this.name = "";

        RIFFWriter ifil_chunk = writer.writeChunk("ifil");
        ifil_chunk.writeUnsignedShort(this.major);
        ifil_chunk.writeUnsignedShort(this.minor);
        writeInfoStringChunk(writer, "isng", this.targetEngine);
        writeInfoStringChunk(writer, "INAM", this.name);
        writeInfoStringChunk(writer, "irom", this.romName);
        if (romVersionMajor != -1) {
            RIFFWriter iver_chunk = writer.writeChunk("iver");
            iver_chunk.writeUnsignedShort(this.romVersionMajor);
            iver_chunk.writeUnsignedShort(this.romVersionMinor);
        }
        writeInfoStringChunk(writer, "ICRD", this.creationDate);
        writeInfoStringChunk(writer, "IENG", this.engineers);
        writeInfoStringChunk(writer, "IPRD", this.product);
        writeInfoStringChunk(writer, "ICOP", this.copyright);
        writeInfoStringChunk(writer, "ICMT", this.comments);
        writeInfoStringChunk(writer, "ISFT", this.tools);

        writer.close();
    }

    private void writeSdtaChunk(RIFFWriter writer) throws IOException {

        byte[] pad = new byte[32];

        RIFFWriter smpl_chunk = writer.writeChunk("smpl");
        for (SF2Sample sample : samples) {
            ModelByteBuffer data = sample.getDataBuffer();
            data.writeTo(smpl_chunk);
            /*
            smpl_chunk.write(data.array(),
            data.arrayOffset(),
            data.capacity());
             */
            smpl_chunk.write(pad);
            smpl_chunk.write(pad);
        }
        if (major < 2)
            return;
        if (major == 2 && minor < 4)
            return;


        for (SF2Sample sample : samples) {
            ModelByteBuffer data24 = sample.getData24Buffer();
            if (data24 == null)
                return;
        }

        RIFFWriter sm24_chunk = writer.writeChunk("sm24");
        for (SF2Sample sample : samples) {
            ModelByteBuffer data = sample.getData24Buffer();
            data.writeTo(sm24_chunk);
            /*
            sm24_chunk.write(data.array(),
            data.arrayOffset(),
            data.capacity());*/
            smpl_chunk.write(pad);
        }
    }

    private void writeModulators(RIFFWriter writer, List<SF2Modulator> modulators)
            throws IOException {
        for (SF2Modulator modulator : modulators) {
            writer.writeUnsignedShort(modulator.sourceOperator);
            writer.writeUnsignedShort(modulator.destinationOperator);
            writer.writeShort(modulator.amount);
            writer.writeUnsignedShort(modulator.amountSourceOperator);
            writer.writeUnsignedShort(modulator.transportOperator);
        }
    }

    private void writeGenerators(RIFFWriter writer, Map<Integer, Short> generators)
            throws IOException {
        Short keyrange = generators.get(SF2Region.GENERATOR_KEYRANGE);
        Short velrange = generators.get(SF2Region.GENERATOR_VELRANGE);
        if (keyrange != null) {
            writer.writeUnsignedShort(SF2Region.GENERATOR_KEYRANGE);
            writer.writeShort(keyrange);
        }
        if (velrange != null) {
            writer.writeUnsignedShort(SF2Region.GENERATOR_VELRANGE);
            writer.writeShort(velrange);
        }
        for (Map.Entry<Integer, Short> generator : generators.entrySet()) {
            if (generator.getKey() == SF2Region.GENERATOR_KEYRANGE)
                continue;
            if (generator.getKey() == SF2Region.GENERATOR_VELRANGE)
                continue;
            writer.writeUnsignedShort(generator.getKey());
            writer.writeShort(generator.getValue());
        }
    }

    private void writePdtaChunk(RIFFWriter writer) throws IOException {

        RIFFWriter phdr_chunk = writer.writeChunk("phdr");
        int phdr_zone_count = 0;
        for (SF2Instrument preset : this.instruments) {
            phdr_chunk.writeString(preset.name, 20);
            phdr_chunk.writeUnsignedShort(preset.preset);
            phdr_chunk.writeUnsignedShort(preset.bank);
            phdr_chunk.writeUnsignedShort(phdr_zone_count);
            if (preset.getGlobalRegion() != null)
                phdr_zone_count += 1;
            phdr_zone_count += preset.getRegions().size();
            phdr_chunk.writeUnsignedInt(preset.library);
            phdr_chunk.writeUnsignedInt(preset.genre);
            phdr_chunk.writeUnsignedInt(preset.morphology);
        }
        phdr_chunk.writeString("EOP", 20);
        phdr_chunk.writeUnsignedShort(0);
        phdr_chunk.writeUnsignedShort(0);
        phdr_chunk.writeUnsignedShort(phdr_zone_count);
        phdr_chunk.writeUnsignedInt(0);
        phdr_chunk.writeUnsignedInt(0);
        phdr_chunk.writeUnsignedInt(0);


        RIFFWriter pbag_chunk = writer.writeChunk("pbag");
        int pbag_gencount = 0;
        int pbag_modcount = 0;
        for (SF2Instrument preset : this.instruments) {
            if (preset.getGlobalRegion() != null) {
                pbag_chunk.writeUnsignedShort(pbag_gencount);
                pbag_chunk.writeUnsignedShort(pbag_modcount);
                pbag_gencount += preset.getGlobalRegion().getGenerators().size();
                pbag_modcount += preset.getGlobalRegion().getModulators().size();
            }
            for (SF2InstrumentRegion region : preset.getRegions()) {
                pbag_chunk.writeUnsignedShort(pbag_gencount);
                pbag_chunk.writeUnsignedShort(pbag_modcount);
                if (layers.indexOf(region.layer) != -1) {
                    // One generator is used to reference to instrument record
                    pbag_gencount += 1;
                }
                pbag_gencount += region.getGenerators().size();
                pbag_modcount += region.getModulators().size();

            }
        }
        pbag_chunk.writeUnsignedShort(pbag_gencount);
        pbag_chunk.writeUnsignedShort(pbag_modcount);

        RIFFWriter pmod_chunk = writer.writeChunk("pmod");
        for (SF2Instrument preset : this.instruments) {
            if (preset.getGlobalRegion() != null) {
                writeModulators(pmod_chunk,
                        preset.getGlobalRegion().getModulators());
            }
            for (SF2InstrumentRegion region : preset.getRegions())
                writeModulators(pmod_chunk, region.getModulators());
        }
        pmod_chunk.write(new byte[10]);

        RIFFWriter pgen_chunk = writer.writeChunk("pgen");
        for (SF2Instrument preset : this.instruments) {
            if (preset.getGlobalRegion() != null) {
                writeGenerators(pgen_chunk,
                        preset.getGlobalRegion().getGenerators());
            }
            for (SF2InstrumentRegion region : preset.getRegions()) {
                writeGenerators(pgen_chunk, region.getGenerators());
                int ix = layers.indexOf(region.layer);
                if (ix != -1) {
                    pgen_chunk.writeUnsignedShort(SF2Region.GENERATOR_INSTRUMENT);
                    pgen_chunk.writeShort((short) ix);
                }
            }
        }
        pgen_chunk.write(new byte[4]);

        RIFFWriter inst_chunk = writer.writeChunk("inst");
        int inst_zone_count = 0;
        for (SF2Layer instrument : this.layers) {
            inst_chunk.writeString(instrument.name, 20);
            inst_chunk.writeUnsignedShort(inst_zone_count);
            if (instrument.getGlobalRegion() != null)
                inst_zone_count += 1;
            inst_zone_count += instrument.getRegions().size();
        }
        inst_chunk.writeString("EOI", 20);
        inst_chunk.writeUnsignedShort(inst_zone_count);


        RIFFWriter ibag_chunk = writer.writeChunk("ibag");
        int ibag_gencount = 0;
        int ibag_modcount = 0;
        for (SF2Layer instrument : this.layers) {
            if (instrument.getGlobalRegion() != null) {
                ibag_chunk.writeUnsignedShort(ibag_gencount);
                ibag_chunk.writeUnsignedShort(ibag_modcount);
                ibag_gencount
                        += instrument.getGlobalRegion().getGenerators().size();
                ibag_modcount
                        += instrument.getGlobalRegion().getModulators().size();
            }
            for (SF2LayerRegion region : instrument.getRegions()) {
                ibag_chunk.writeUnsignedShort(ibag_gencount);
                ibag_chunk.writeUnsignedShort(ibag_modcount);
                if (samples.indexOf(region.sample) != -1) {
                    // One generator is used to reference to instrument record
                    ibag_gencount += 1;
                }
                ibag_gencount += region.getGenerators().size();
                ibag_modcount += region.getModulators().size();

            }
        }
        ibag_chunk.writeUnsignedShort(ibag_gencount);
        ibag_chunk.writeUnsignedShort(ibag_modcount);


        RIFFWriter imod_chunk = writer.writeChunk("imod");
        for (SF2Layer instrument : this.layers) {
            if (instrument.getGlobalRegion() != null) {
                writeModulators(imod_chunk,
                        instrument.getGlobalRegion().getModulators());
            }
            for (SF2LayerRegion region : instrument.getRegions())
                writeModulators(imod_chunk, region.getModulators());
        }
        imod_chunk.write(new byte[10]);

        RIFFWriter igen_chunk = writer.writeChunk("igen");
        for (SF2Layer instrument : this.layers) {
            if (instrument.getGlobalRegion() != null) {
                writeGenerators(igen_chunk,
                        instrument.getGlobalRegion().getGenerators());
            }
            for (SF2LayerRegion region : instrument.getRegions()) {
                writeGenerators(igen_chunk, region.getGenerators());
                int ix = samples.indexOf(region.sample);
                if (ix != -1) {
                    igen_chunk.writeUnsignedShort(SF2Region.GENERATOR_SAMPLEID);
                    igen_chunk.writeShort((short) ix);
                }
            }
        }
        igen_chunk.write(new byte[4]);


        RIFFWriter shdr_chunk = writer.writeChunk("shdr");
        long sample_pos = 0;
        for (SF2Sample sample : samples) {
            shdr_chunk.writeString(sample.name, 20);
            long start = sample_pos;
            sample_pos += sample.data.capacity() / 2;
            long end = sample_pos;
            long startLoop = sample.startLoop + start;
            long endLoop = sample.endLoop + start;
            if (startLoop < start)
                startLoop = start;
            if (endLoop > end)
                endLoop = end;
            shdr_chunk.writeUnsignedInt(start);
            shdr_chunk.writeUnsignedInt(end);
            shdr_chunk.writeUnsignedInt(startLoop);
            shdr_chunk.writeUnsignedInt(endLoop);
            shdr_chunk.writeUnsignedInt(sample.sampleRate);
            shdr_chunk.writeUnsignedByte(sample.originalPitch);
            shdr_chunk.writeByte(sample.pitchCorrection);
            shdr_chunk.writeUnsignedShort(sample.sampleLink);
            shdr_chunk.writeUnsignedShort(sample.sampleType);
            sample_pos += 32;
        }
        shdr_chunk.writeString("EOS", 20);
        shdr_chunk.write(new byte[26]);

    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return major + "." + minor;
    }

    @Override
    public String getVendor() {
        return engineers;
    }

    @Override
    public String getDescription() {
        return comments;
    }

    public void setName(String s) {
        name = s;
    }

    public void setVendor(String s) {
        engineers = s;
    }

    public void setDescription(String s) {
        comments = s;
    }

    @Override
    public SoundbankResource[] getResources() {
        SoundbankResource[] resources
                = new SoundbankResource[layers.size() + samples.size()];
        int j = 0;
        for (int i = 0; i < layers.size(); i++)
            resources[j++] = layers.get(i);
        for (int i = 0; i < samples.size(); i++)
            resources[j++] = samples.get(i);
        return resources;
    }

    @Override
    public SF2Instrument[] getInstruments() {
        SF2Instrument[] inslist_array
                = instruments.toArray(new SF2Instrument[instruments.size()]);
        Arrays.sort(inslist_array, new ModelInstrumentComparator());
        return inslist_array;
    }

    public SF2Layer[] getLayers() {
        return layers.toArray(new SF2Layer[layers.size()]);
    }

    public SF2Sample[] getSamples() {
        return samples.toArray(new SF2Sample[samples.size()]);
    }

    @Override
    public Instrument getInstrument(Patch patch) {
        int program = patch.getProgram();
        int bank = patch.getBank();
        boolean percussion = false;
        if (patch instanceof ModelPatch)
            percussion = ((ModelPatch)patch).isPercussion();
        for (Instrument instrument : instruments) {
            Patch patch2 = instrument.getPatch();
            int program2 = patch2.getProgram();
            int bank2 = patch2.getBank();
            if (program == program2 && bank == bank2) {
                boolean percussion2 = false;
                if (patch2 instanceof ModelPatch)
                    percussion2 = ((ModelPatch) patch2).isPercussion();
                if (percussion == percussion2)
                    return instrument;
            }
        }
        return null;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getRomName() {
        return romName;
    }

    public void setRomName(String romName) {
        this.romName = romName;
    }

    public int getRomVersionMajor() {
        return romVersionMajor;
    }

    public void setRomVersionMajor(int romVersionMajor) {
        this.romVersionMajor = romVersionMajor;
    }

    public int getRomVersionMinor() {
        return romVersionMinor;
    }

    public void setRomVersionMinor(int romVersionMinor) {
        this.romVersionMinor = romVersionMinor;
    }

    public String getTargetEngine() {
        return targetEngine;
    }

    public void setTargetEngine(String targetEngine) {
        this.targetEngine = targetEngine;
    }

    public String getTools() {
        return tools;
    }

    public void setTools(String tools) {
        this.tools = tools;
    }

    public void addResource(SoundbankResource resource) {
        if (resource instanceof SF2Instrument)
            instruments.add((SF2Instrument)resource);
        if (resource instanceof SF2Layer)
            layers.add((SF2Layer)resource);
        if (resource instanceof SF2Sample)
            samples.add((SF2Sample)resource);
    }

    public void removeResource(SoundbankResource resource) {
        if (resource instanceof SF2Instrument)
            instruments.remove(resource);
        if (resource instanceof SF2Layer)
            layers.remove(resource);
        if (resource instanceof SF2Sample)
            samples.remove(resource);
    }

    public void addInstrument(SF2Instrument resource) {
        instruments.add(resource);
    }

    public void removeInstrument(SF2Instrument resource) {
        instruments.remove(resource);
    }
}
