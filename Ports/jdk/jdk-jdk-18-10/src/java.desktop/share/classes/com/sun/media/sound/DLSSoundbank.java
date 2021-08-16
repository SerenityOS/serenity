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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.sound.midi.Instrument;
import javax.sound.midi.Patch;
import javax.sound.midi.Soundbank;
import javax.sound.midi.SoundbankResource;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * A DLS Level 1 and Level 2 soundbank reader (from files/url/streams).
 *
 * @author Karl Helgason
 */
public final class DLSSoundbank implements Soundbank {

    private static class DLSID {
        long i1;
        int s1;
        int s2;
        int x1;
        int x2;
        int x3;
        int x4;
        int x5;
        int x6;
        int x7;
        int x8;

        private DLSID() {
        }

        DLSID(long i1, int s1, int s2, int x1, int x2, int x3, int x4,
                int x5, int x6, int x7, int x8) {
            this.i1 = i1;
            this.s1 = s1;
            this.s2 = s2;
            this.x1 = x1;
            this.x2 = x2;
            this.x3 = x3;
            this.x4 = x4;
            this.x5 = x5;
            this.x6 = x6;
            this.x7 = x7;
            this.x8 = x8;
        }

        public static DLSID read(RIFFReader riff) throws IOException {
            DLSID d = new DLSID();
            d.i1 = riff.readUnsignedInt();
            d.s1 = riff.readUnsignedShort();
            d.s2 = riff.readUnsignedShort();
            d.x1 = riff.readUnsignedByte();
            d.x2 = riff.readUnsignedByte();
            d.x3 = riff.readUnsignedByte();
            d.x4 = riff.readUnsignedByte();
            d.x5 = riff.readUnsignedByte();
            d.x6 = riff.readUnsignedByte();
            d.x7 = riff.readUnsignedByte();
            d.x8 = riff.readUnsignedByte();
            return d;
        }

        @Override
        public int hashCode() {
            return (int)i1;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DLSID)) {
                return false;
            }
            DLSID t = (DLSID) obj;
            return i1 == t.i1 && s1 == t.s1 && s2 == t.s2
                && x1 == t.x1 && x2 == t.x2 && x3 == t.x3 && x4 == t.x4
                && x5 == t.x5 && x6 == t.x6 && x7 == t.x7 && x8 == t.x8;
        }
    }

    /** X = X & Y */
    private static final int DLS_CDL_AND = 0x0001;
    /** X = X | Y */
    private static final int DLS_CDL_OR = 0x0002;
    /** X = X ^ Y */
    private static final int DLS_CDL_XOR = 0x0003;
    /** X = X + Y */
    private static final int DLS_CDL_ADD = 0x0004;
    /** X = X - Y */
    private static final int DLS_CDL_SUBTRACT = 0x0005;
    /** X = X * Y */
    private static final int DLS_CDL_MULTIPLY = 0x0006;
    /** X = X / Y */
    private static final int DLS_CDL_DIVIDE = 0x0007;
    /** X = X && Y */
    private static final int DLS_CDL_LOGICAL_AND = 0x0008;
    /** X = X || Y */
    private static final int DLS_CDL_LOGICAL_OR = 0x0009;
    /** X = (X < Y) */
    private static final int DLS_CDL_LT = 0x000A;
    /** X = (X <= Y) */
    private static final int DLS_CDL_LE = 0x000B;
    /** X = (X > Y) */
    private static final int DLS_CDL_GT = 0x000C;
    /** X = (X >= Y) */
    private static final int DLS_CDL_GE = 0x000D;
    /** X = (X == Y) */
    private static final int DLS_CDL_EQ = 0x000E;
    /** X = !X */
    private static final int DLS_CDL_NOT = 0x000F;
    /** 32-bit constant */
    private static final int DLS_CDL_CONST = 0x0010;
    /** 32-bit value returned from query */
    private static final int DLS_CDL_QUERY = 0x0011;
    /** 32-bit value returned from query */
    private static final int DLS_CDL_QUERYSUPPORTED = 0x0012;

    private static final DLSID DLSID_GMInHardware = new DLSID(0x178f2f24,
            0xc364, 0x11d1, 0xa7, 0x60, 0x00, 0x00, 0xf8, 0x75, 0xac, 0x12);
    private static final DLSID DLSID_GSInHardware = new DLSID(0x178f2f25,
            0xc364, 0x11d1, 0xa7, 0x60, 0x00, 0x00, 0xf8, 0x75, 0xac, 0x12);
    private static final DLSID DLSID_XGInHardware = new DLSID(0x178f2f26,
            0xc364, 0x11d1, 0xa7, 0x60, 0x00, 0x00, 0xf8, 0x75, 0xac, 0x12);
    private static final DLSID DLSID_SupportsDLS1 = new DLSID(0x178f2f27,
            0xc364, 0x11d1, 0xa7, 0x60, 0x00, 0x00, 0xf8, 0x75, 0xac, 0x12);
    private static final DLSID DLSID_SupportsDLS2 = new DLSID(0xf14599e5,
            0x4689, 0x11d2, 0xaf, 0xa6, 0x0, 0xaa, 0x0, 0x24, 0xd8, 0xb6);
    private static final DLSID DLSID_SampleMemorySize = new DLSID(0x178f2f28,
            0xc364, 0x11d1, 0xa7, 0x60, 0x00, 0x00, 0xf8, 0x75, 0xac, 0x12);
    private static final DLSID DLSID_ManufacturersID = new DLSID(0xb03e1181,
            0x8095, 0x11d2, 0xa1, 0xef, 0x0, 0x60, 0x8, 0x33, 0xdb, 0xd8);
    private static final DLSID DLSID_ProductID = new DLSID(0xb03e1182,
            0x8095, 0x11d2, 0xa1, 0xef, 0x0, 0x60, 0x8, 0x33, 0xdb, 0xd8);
    private static final DLSID DLSID_SamplePlaybackRate = new DLSID(0x2a91f713,
            0xa4bf, 0x11d2, 0xbb, 0xdf, 0x0, 0x60, 0x8, 0x33, 0xdb, 0xd8);

    private long major = -1;
    private long minor = -1;

    private final DLSInfo info = new DLSInfo();

    private final List<DLSInstrument> instruments = new ArrayList<>();
    private final List<DLSSample> samples = new ArrayList<>();

    private boolean largeFormat = false;
    private File sampleFile;

    public DLSSoundbank() {
    }

    public DLSSoundbank(URL url) throws IOException {
        InputStream is = url.openStream();
        try {
            readSoundbank(is);
        } finally {
            is.close();
        }
    }

    public DLSSoundbank(File file) throws IOException {
        largeFormat = true;
        sampleFile = file;
        InputStream is = new FileInputStream(file);
        try {
            readSoundbank(is);
        } finally {
            is.close();
        }
    }

    public DLSSoundbank(InputStream inputstream) throws IOException {
        readSoundbank(inputstream);
    }

    private void readSoundbank(InputStream inputstream) throws IOException {
        RIFFReader riff = new RIFFReader(inputstream);
        if (!riff.getFormat().equals("RIFF")) {
            throw new RIFFInvalidFormatException(
                    "Input stream is not a valid RIFF stream!");
        }
        if (!riff.getType().equals("DLS ")) {
            throw new RIFFInvalidFormatException(
                    "Input stream is not a valid DLS soundbank!");
        }
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            if (chunk.getFormat().equals("LIST")) {
                if (chunk.getType().equals("INFO"))
                    readInfoChunk(chunk);
                if (chunk.getType().equals("lins"))
                    readLinsChunk(chunk);
                if (chunk.getType().equals("wvpl"))
                    readWvplChunk(chunk);
            } else {
                if (chunk.getFormat().equals("cdl ")) {
                    if (!readCdlChunk(chunk)) {
                        throw new RIFFInvalidFormatException(
                                "DLS file isn't supported!");
                    }
                }
                if (chunk.getFormat().equals("colh")) {
                    // skipped because we will load the entire bank into memory
                    // long instrumentcount = chunk.readUnsignedInt();
                    // System.out.println("instrumentcount = "+ instrumentcount);
                }
                if (chunk.getFormat().equals("ptbl")) {
                    // Pool Table Chunk
                    // skipped because we will load the entire bank into memory
                }
                if (chunk.getFormat().equals("vers")) {
                    major = chunk.readUnsignedInt();
                    minor = chunk.readUnsignedInt();
                }
            }
        }

        for (Map.Entry<DLSRegion, Long> entry : temp_rgnassign.entrySet()) {
            entry.getKey().sample = samples.get((int)entry.getValue().longValue());
        }

        temp_rgnassign = null;
    }

    private boolean cdlIsQuerySupported(DLSID uuid) {
        return uuid.equals(DLSID_GMInHardware)
            || uuid.equals(DLSID_GSInHardware)
            || uuid.equals(DLSID_XGInHardware)
            || uuid.equals(DLSID_SupportsDLS1)
            || uuid.equals(DLSID_SupportsDLS2)
            || uuid.equals(DLSID_SampleMemorySize)
            || uuid.equals(DLSID_ManufacturersID)
            || uuid.equals(DLSID_ProductID)
            || uuid.equals(DLSID_SamplePlaybackRate);
    }

    private long cdlQuery(DLSID uuid) {
        if (uuid.equals(DLSID_GMInHardware))
            return 1;
        if (uuid.equals(DLSID_GSInHardware))
            return 0;
        if (uuid.equals(DLSID_XGInHardware))
            return 0;
        if (uuid.equals(DLSID_SupportsDLS1))
            return 1;
        if (uuid.equals(DLSID_SupportsDLS2))
            return 1;
        if (uuid.equals(DLSID_SampleMemorySize))
            return Runtime.getRuntime().totalMemory();
        if (uuid.equals(DLSID_ManufacturersID))
            return 0;
        if (uuid.equals(DLSID_ProductID))
            return 0;
        if (uuid.equals(DLSID_SamplePlaybackRate))
            return 44100;
        return 0;
    }


    // Reading cdl-ck Chunk
    // "cdl " chunk can only appear inside : DLS,lart,lar2,rgn,rgn2
    private boolean readCdlChunk(RIFFReader riff) throws IOException {

        DLSID uuid;
        long x;
        long y;
        Stack<Long> stack = new Stack<>();

        while (riff.available() != 0) {
            int opcode = riff.readUnsignedShort();
            switch (opcode) {
            case DLS_CDL_AND:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf(((x != 0) && (y != 0)) ? 1 : 0));
                break;
            case DLS_CDL_OR:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf(((x != 0) || (y != 0)) ? 1 : 0));
                break;
            case DLS_CDL_XOR:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf(((x != 0) ^ (y != 0)) ? 1 : 0));
                break;
            case DLS_CDL_ADD:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf(x + y));
                break;
            case DLS_CDL_SUBTRACT:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf(x - y));
                break;
            case DLS_CDL_MULTIPLY:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf(x * y));
                break;
            case DLS_CDL_DIVIDE:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf(x / y));
                break;
            case DLS_CDL_LOGICAL_AND:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf(((x != 0) && (y != 0)) ? 1 : 0));
                break;
            case DLS_CDL_LOGICAL_OR:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf(((x != 0) || (y != 0)) ? 1 : 0));
                break;
            case DLS_CDL_LT:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf((x < y) ? 1 : 0));
                break;
            case DLS_CDL_LE:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf((x <= y) ? 1 : 0));
                break;
            case DLS_CDL_GT:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf((x > y) ? 1 : 0));
                break;
            case DLS_CDL_GE:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf((x >= y) ? 1 : 0));
                break;
            case DLS_CDL_EQ:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf((x == y) ? 1 : 0));
                break;
            case DLS_CDL_NOT:
                x = stack.pop();
                y = stack.pop();
                stack.push(Long.valueOf((x == 0) ? 1 : 0));
                break;
            case DLS_CDL_CONST:
                stack.push(Long.valueOf(riff.readUnsignedInt()));
                break;
            case DLS_CDL_QUERY:
                uuid = DLSID.read(riff);
                stack.push(cdlQuery(uuid));
                break;
            case DLS_CDL_QUERYSUPPORTED:
                uuid = DLSID.read(riff);
                stack.push(Long.valueOf(cdlIsQuerySupported(uuid) ? 1 : 0));
                break;
            default:
                break;
            }
        }
        if (stack.isEmpty())
            return false;

        return stack.pop() == 1;
    }

    private void readInfoChunk(RIFFReader riff) throws IOException {
        info.name = null;
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            String format = chunk.getFormat();
            if (format.equals("INAM"))
                info.name = chunk.readString(chunk.available());
            else if (format.equals("ICRD"))
                info.creationDate = chunk.readString(chunk.available());
            else if (format.equals("IENG"))
                info.engineers = chunk.readString(chunk.available());
            else if (format.equals("IPRD"))
                info.product = chunk.readString(chunk.available());
            else if (format.equals("ICOP"))
                info.copyright = chunk.readString(chunk.available());
            else if (format.equals("ICMT"))
                info.comments = chunk.readString(chunk.available());
            else if (format.equals("ISFT"))
                info.tools = chunk.readString(chunk.available());
            else if (format.equals("IARL"))
                info.archival_location = chunk.readString(chunk.available());
            else if (format.equals("IART"))
                info.artist = chunk.readString(chunk.available());
            else if (format.equals("ICMS"))
                info.commissioned = chunk.readString(chunk.available());
            else if (format.equals("IGNR"))
                info.genre = chunk.readString(chunk.available());
            else if (format.equals("IKEY"))
                info.keywords = chunk.readString(chunk.available());
            else if (format.equals("IMED"))
                info.medium = chunk.readString(chunk.available());
            else if (format.equals("ISBJ"))
                info.subject = chunk.readString(chunk.available());
            else if (format.equals("ISRC"))
                info.source = chunk.readString(chunk.available());
            else if (format.equals("ISRF"))
                info.source_form = chunk.readString(chunk.available());
            else if (format.equals("ITCH"))
                info.technician = chunk.readString(chunk.available());
        }
    }

    private void readLinsChunk(RIFFReader riff) throws IOException {
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            if (chunk.getFormat().equals("LIST")) {
                if (chunk.getType().equals("ins "))
                    readInsChunk(chunk);
            }
        }
    }

    private void readInsChunk(RIFFReader riff) throws IOException {
        DLSInstrument instrument = new DLSInstrument(this);

        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            String format = chunk.getFormat();
            if (format.equals("LIST")) {
                if (chunk.getType().equals("INFO")) {
                    readInsInfoChunk(instrument, chunk);
                }
                if (chunk.getType().equals("lrgn")) {
                    while (chunk.hasNextChunk()) {
                        RIFFReader subchunk = chunk.nextChunk();
                        if (subchunk.getFormat().equals("LIST")) {
                            if (subchunk.getType().equals("rgn ")) {
                                DLSRegion split = new DLSRegion();
                                if (readRgnChunk(split, subchunk))
                                    instrument.getRegions().add(split);
                            }
                            if (subchunk.getType().equals("rgn2")) {
                                // support for DLS level 2 regions
                                DLSRegion split = new DLSRegion();
                                if (readRgnChunk(split, subchunk))
                                    instrument.getRegions().add(split);
                            }
                        }
                    }
                }
                if (chunk.getType().equals("lart")) {
                    List<DLSModulator> modlist = new ArrayList<>();
                    while (chunk.hasNextChunk()) {
                        RIFFReader subchunk = chunk.nextChunk();
                        if (chunk.getFormat().equals("cdl ")) {
                            if (!readCdlChunk(chunk)) {
                                modlist.clear();
                                break;
                            }
                        }
                        if (subchunk.getFormat().equals("art1"))
                            readArt1Chunk(modlist, subchunk);
                    }
                    instrument.getModulators().addAll(modlist);
                }
                if (chunk.getType().equals("lar2")) {
                    // support for DLS level 2 ART
                    List<DLSModulator> modlist = new ArrayList<>();
                    while (chunk.hasNextChunk()) {
                        RIFFReader subchunk = chunk.nextChunk();
                        if (chunk.getFormat().equals("cdl ")) {
                            if (!readCdlChunk(chunk)) {
                                modlist.clear();
                                break;
                            }
                        }
                        if (subchunk.getFormat().equals("art2"))
                            readArt2Chunk(modlist, subchunk);
                    }
                    instrument.getModulators().addAll(modlist);
                }
            } else {
                if (format.equals("dlid")) {
                    instrument.guid = new byte[16];
                    chunk.readFully(instrument.guid);
                }
                if (format.equals("insh")) {
                    chunk.readUnsignedInt(); // Read Region Count - ignored

                    int bank = chunk.read();             // LSB
                    bank += (chunk.read() & 127) << 7;   // MSB
                    chunk.read(); // Read Reserved byte
                    int drumins = chunk.read();          // Drum Instrument

                    int id = chunk.read() & 127; // Read only first 7 bits
                    chunk.read(); // Read Reserved byte
                    chunk.read(); // Read Reserved byte
                    chunk.read(); // Read Reserved byte

                    instrument.bank = bank;
                    instrument.preset = id;
                    instrument.druminstrument = (drumins & 128) > 0;
                    //System.out.println("bank="+bank+" drumkit="+drumkit
                    //        +" id="+id);
                }

            }
        }
        instruments.add(instrument);
    }

    private void readArt1Chunk(List<DLSModulator> modulators, RIFFReader riff)
            throws IOException {
        long size = riff.readUnsignedInt();
        long count = riff.readUnsignedInt();

        if (size - 8 != 0)
            riff.skip(size - 8);

        for (int i = 0; i < count; i++) {
            DLSModulator modulator = new DLSModulator();
            modulator.version = 1;
            modulator.source = riff.readUnsignedShort();
            modulator.control = riff.readUnsignedShort();
            modulator.destination = riff.readUnsignedShort();
            modulator.transform = riff.readUnsignedShort();
            modulator.scale = riff.readInt();
            modulators.add(modulator);
        }
    }

    private void readArt2Chunk(List<DLSModulator> modulators, RIFFReader riff)
            throws IOException {
        long size = riff.readUnsignedInt();
        long count = riff.readUnsignedInt();

        if (size - 8 != 0)
            riff.skip(size - 8);

        for (int i = 0; i < count; i++) {
            DLSModulator modulator = new DLSModulator();
            modulator.version = 2;
            modulator.source = riff.readUnsignedShort();
            modulator.control = riff.readUnsignedShort();
            modulator.destination = riff.readUnsignedShort();
            modulator.transform = riff.readUnsignedShort();
            modulator.scale = riff.readInt();
            modulators.add(modulator);
        }
    }

    private Map<DLSRegion, Long> temp_rgnassign = new HashMap<>();

    private boolean readRgnChunk(DLSRegion split, RIFFReader riff)
            throws IOException {
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            String format = chunk.getFormat();
            if (format.equals("LIST")) {
                if (chunk.getType().equals("lart")) {
                    List<DLSModulator> modlist = new ArrayList<>();
                    while (chunk.hasNextChunk()) {
                        RIFFReader subchunk = chunk.nextChunk();
                        if (chunk.getFormat().equals("cdl ")) {
                            if (!readCdlChunk(chunk)) {
                                modlist.clear();
                                break;
                            }
                        }
                        if (subchunk.getFormat().equals("art1"))
                            readArt1Chunk(modlist, subchunk);
                    }
                    split.getModulators().addAll(modlist);
                }
                if (chunk.getType().equals("lar2")) {
                    // support for DLS level 2 ART
                    List<DLSModulator> modlist = new ArrayList<>();
                    while (chunk.hasNextChunk()) {
                        RIFFReader subchunk = chunk.nextChunk();
                        if (chunk.getFormat().equals("cdl ")) {
                            if (!readCdlChunk(chunk)) {
                                modlist.clear();
                                break;
                            }
                        }
                        if (subchunk.getFormat().equals("art2"))
                            readArt2Chunk(modlist, subchunk);
                    }
                    split.getModulators().addAll(modlist);
                }
            } else {

                if (format.equals("cdl ")) {
                    if (!readCdlChunk(chunk))
                        return false;
                }
                if (format.equals("rgnh")) {
                    split.keyfrom = chunk.readUnsignedShort();
                    split.keyto = chunk.readUnsignedShort();
                    split.velfrom = chunk.readUnsignedShort();
                    split.velto = chunk.readUnsignedShort();
                    split.options = chunk.readUnsignedShort();
                    split.exclusiveClass = chunk.readUnsignedShort();
                }
                if (format.equals("wlnk")) {
                    split.fusoptions = chunk.readUnsignedShort();
                    split.phasegroup = chunk.readUnsignedShort();
                    split.channel = chunk.readUnsignedInt();
                    long sampleid = chunk.readUnsignedInt();
                    temp_rgnassign.put(split, sampleid);
                }
                if (format.equals("wsmp")) {
                    split.sampleoptions = new DLSSampleOptions();
                    readWsmpChunk(split.sampleoptions, chunk);
                }
            }
        }
        return true;
    }

    private void readWsmpChunk(DLSSampleOptions sampleOptions, RIFFReader riff)
            throws IOException {
        long size = riff.readUnsignedInt();
        sampleOptions.unitynote = riff.readUnsignedShort();
        sampleOptions.finetune = riff.readShort();
        sampleOptions.attenuation = riff.readInt();
        sampleOptions.options = riff.readUnsignedInt();
        long loops = riff.readInt();

        if (size > 20)
            riff.skip(size - 20);

        for (int i = 0; i < loops; i++) {
            DLSSampleLoop loop = new DLSSampleLoop();
            long size2 = riff.readUnsignedInt();
            loop.type = riff.readUnsignedInt();
            loop.start = riff.readUnsignedInt();
            loop.length = riff.readUnsignedInt();
            sampleOptions.loops.add(loop);
            if (size2 > 16)
                riff.skip(size2 - 16);
        }
    }

    private void readInsInfoChunk(DLSInstrument dlsinstrument, RIFFReader riff)
            throws IOException {
        dlsinstrument.info.name = null;
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            String format = chunk.getFormat();
            if (format.equals("INAM")) {
                dlsinstrument.info.name = chunk.readString(chunk.available());
            } else if (format.equals("ICRD")) {
                dlsinstrument.info.creationDate =
                        chunk.readString(chunk.available());
            } else if (format.equals("IENG")) {
                dlsinstrument.info.engineers =
                        chunk.readString(chunk.available());
            } else if (format.equals("IPRD")) {
                dlsinstrument.info.product = chunk.readString(chunk.available());
            } else if (format.equals("ICOP")) {
                dlsinstrument.info.copyright =
                        chunk.readString(chunk.available());
            } else if (format.equals("ICMT")) {
                dlsinstrument.info.comments =
                        chunk.readString(chunk.available());
            } else if (format.equals("ISFT")) {
                dlsinstrument.info.tools = chunk.readString(chunk.available());
            } else if (format.equals("IARL")) {
                dlsinstrument.info.archival_location =
                        chunk.readString(chunk.available());
            } else if (format.equals("IART")) {
                dlsinstrument.info.artist = chunk.readString(chunk.available());
            } else if (format.equals("ICMS")) {
                dlsinstrument.info.commissioned =
                        chunk.readString(chunk.available());
            } else if (format.equals("IGNR")) {
                dlsinstrument.info.genre = chunk.readString(chunk.available());
            } else if (format.equals("IKEY")) {
                dlsinstrument.info.keywords =
                        chunk.readString(chunk.available());
            } else if (format.equals("IMED")) {
                dlsinstrument.info.medium = chunk.readString(chunk.available());
            } else if (format.equals("ISBJ")) {
                dlsinstrument.info.subject = chunk.readString(chunk.available());
            } else if (format.equals("ISRC")) {
                dlsinstrument.info.source = chunk.readString(chunk.available());
            } else if (format.equals("ISRF")) {
                dlsinstrument.info.source_form =
                        chunk.readString(chunk.available());
            } else if (format.equals("ITCH")) {
                dlsinstrument.info.technician =
                        chunk.readString(chunk.available());
            }
        }
    }

    private void readWvplChunk(RIFFReader riff) throws IOException {
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            if (chunk.getFormat().equals("LIST")) {
                if (chunk.getType().equals("wave"))
                    readWaveChunk(chunk);
            }
        }
    }

    private void readWaveChunk(RIFFReader riff) throws IOException {
        DLSSample sample = new DLSSample(this);

        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            String format = chunk.getFormat();
            if (format.equals("LIST")) {
                if (chunk.getType().equals("INFO")) {
                    readWaveInfoChunk(sample, chunk);
                }
            } else {
                if (format.equals("dlid")) {
                    sample.guid = new byte[16];
                    chunk.readFully(sample.guid);
                }

                if (format.equals("fmt ")) {
                    int sampleformat = chunk.readUnsignedShort();
                    if (sampleformat != 1 && sampleformat != 3) {
                        throw new RIFFInvalidDataException(
                                "Only PCM samples are supported!");
                    }
                    int channels = chunk.readUnsignedShort();
                    long samplerate = chunk.readUnsignedInt();
                    // bytes per sec
                    /* long framerate = */ chunk.readUnsignedInt();
                    // block align, framesize
                    int framesize = chunk.readUnsignedShort();
                    int bits = chunk.readUnsignedShort();
                    AudioFormat audioformat = null;
                    if (sampleformat == 1) {
                        if (bits == 8) {
                            audioformat = new AudioFormat(
                                    Encoding.PCM_UNSIGNED, samplerate, bits,
                                    channels, framesize, samplerate, false);
                        } else {
                            audioformat = new AudioFormat(
                                    Encoding.PCM_SIGNED, samplerate, bits,
                                    channels, framesize, samplerate, false);
                        }
                    }
                    if (sampleformat == 3) {
                        audioformat = new AudioFormat(
                                Encoding.PCM_FLOAT, samplerate, bits,
                                channels, framesize, samplerate, false);
                    }

                    sample.format = audioformat;
                }

                if (format.equals("data")) {
                    if (largeFormat) {
                        sample.setData(new ModelByteBuffer(sampleFile,
                                chunk.getFilePointer(), chunk.available()));
                    } else {
                        byte[] buffer = new byte[chunk.available()];
                        //  chunk.read(buffer);
                        sample.setData(buffer);

                        int read = 0;
                        int avail = chunk.available();
                        while (read != avail) {
                            if (avail - read > 65536) {
                                chunk.readFully(buffer, read, 65536);
                                read += 65536;
                            } else {
                                chunk.readFully(buffer, read, avail - read);
                                read = avail;
                            }
                        }
                    }
                }

                if (format.equals("wsmp")) {
                    sample.sampleoptions = new DLSSampleOptions();
                    readWsmpChunk(sample.sampleoptions, chunk);
                }
            }
        }

        samples.add(sample);

    }

    private void readWaveInfoChunk(DLSSample dlssample, RIFFReader riff)
            throws IOException {
        dlssample.info.name = null;
        while (riff.hasNextChunk()) {
            RIFFReader chunk = riff.nextChunk();
            String format = chunk.getFormat();
            if (format.equals("INAM")) {
                dlssample.info.name = chunk.readString(chunk.available());
            } else if (format.equals("ICRD")) {
                dlssample.info.creationDate =
                        chunk.readString(chunk.available());
            } else if (format.equals("IENG")) {
                dlssample.info.engineers = chunk.readString(chunk.available());
            } else if (format.equals("IPRD")) {
                dlssample.info.product = chunk.readString(chunk.available());
            } else if (format.equals("ICOP")) {
                dlssample.info.copyright = chunk.readString(chunk.available());
            } else if (format.equals("ICMT")) {
                dlssample.info.comments = chunk.readString(chunk.available());
            } else if (format.equals("ISFT")) {
                dlssample.info.tools = chunk.readString(chunk.available());
            } else if (format.equals("IARL")) {
                dlssample.info.archival_location =
                        chunk.readString(chunk.available());
            } else if (format.equals("IART")) {
                dlssample.info.artist = chunk.readString(chunk.available());
            } else if (format.equals("ICMS")) {
                dlssample.info.commissioned =
                        chunk.readString(chunk.available());
            } else if (format.equals("IGNR")) {
                dlssample.info.genre = chunk.readString(chunk.available());
            } else if (format.equals("IKEY")) {
                dlssample.info.keywords = chunk.readString(chunk.available());
            } else if (format.equals("IMED")) {
                dlssample.info.medium = chunk.readString(chunk.available());
            } else if (format.equals("ISBJ")) {
                dlssample.info.subject = chunk.readString(chunk.available());
            } else if (format.equals("ISRC")) {
                dlssample.info.source = chunk.readString(chunk.available());
            } else if (format.equals("ISRF")) {
                dlssample.info.source_form = chunk.readString(chunk.available());
            } else if (format.equals("ITCH")) {
                dlssample.info.technician = chunk.readString(chunk.available());
            }
        }
    }

    public void save(String name) throws IOException {
        writeSoundbank(new RIFFWriter(name, "DLS "));
    }

    public void save(File file) throws IOException {
        writeSoundbank(new RIFFWriter(file, "DLS "));
    }

    public void save(OutputStream out) throws IOException {
        writeSoundbank(new RIFFWriter(out, "DLS "));
    }

    private void writeSoundbank(RIFFWriter writer) throws IOException {
        RIFFWriter colh_chunk = writer.writeChunk("colh");
        colh_chunk.writeUnsignedInt(instruments.size());

        if (major != -1 && minor != -1) {
            RIFFWriter vers_chunk = writer.writeChunk("vers");
            vers_chunk.writeUnsignedInt(major);
            vers_chunk.writeUnsignedInt(minor);
        }

        writeInstruments(writer.writeList("lins"));

        RIFFWriter ptbl = writer.writeChunk("ptbl");
        ptbl.writeUnsignedInt(8);
        ptbl.writeUnsignedInt(samples.size());
        long ptbl_offset = writer.getFilePointer();
        for (int i = 0; i < samples.size(); i++)
            ptbl.writeUnsignedInt(0);

        RIFFWriter wvpl = writer.writeList("wvpl");
        long off = wvpl.getFilePointer();
        List<Long> offsettable = new ArrayList<>();
        for (DLSSample sample : samples) {
            offsettable.add(Long.valueOf(wvpl.getFilePointer() - off));
            writeSample(wvpl.writeList("wave"), sample);
        }

        // small cheat, we are going to rewrite data back in wvpl
        long bak = writer.getFilePointer();
        writer.seek(ptbl_offset);
        writer.setWriteOverride(true);
        for (Long offset : offsettable)
            writer.writeUnsignedInt(offset.longValue());
        writer.setWriteOverride(false);
        writer.seek(bak);

        writeInfo(writer.writeList("INFO"), info);

        writer.close();
    }

    private void writeSample(RIFFWriter writer, DLSSample sample)
            throws IOException {

        AudioFormat audioformat = sample.getFormat();

        Encoding encoding = audioformat.getEncoding();
        float sampleRate = audioformat.getSampleRate();
        int sampleSizeInBits = audioformat.getSampleSizeInBits();
        int channels = audioformat.getChannels();
        int frameSize = audioformat.getFrameSize();
        float frameRate = audioformat.getFrameRate();
        boolean bigEndian = audioformat.isBigEndian();

        boolean convert_needed = false;

        if (audioformat.getSampleSizeInBits() == 8) {
            if (!encoding.equals(Encoding.PCM_UNSIGNED)) {
                encoding = Encoding.PCM_UNSIGNED;
                convert_needed = true;
            }
        } else {
            if (!encoding.equals(Encoding.PCM_SIGNED)) {
                encoding = Encoding.PCM_SIGNED;
                convert_needed = true;
            }
            if (bigEndian) {
                bigEndian = false;
                convert_needed = true;
            }
        }

        if (convert_needed) {
            audioformat = new AudioFormat(encoding, sampleRate,
                    sampleSizeInBits, channels, frameSize, frameRate, bigEndian);
        }

        // fmt
        RIFFWriter fmt_chunk = writer.writeChunk("fmt ");
        int sampleformat = 0;
        if (audioformat.getEncoding().equals(Encoding.PCM_UNSIGNED))
            sampleformat = 1;
        else if (audioformat.getEncoding().equals(Encoding.PCM_SIGNED))
            sampleformat = 1;
        else if (audioformat.getEncoding().equals(Encoding.PCM_FLOAT))
            sampleformat = 3;

        fmt_chunk.writeUnsignedShort(sampleformat);
        fmt_chunk.writeUnsignedShort(audioformat.getChannels());
        fmt_chunk.writeUnsignedInt((long) audioformat.getSampleRate());
        long srate = ((long)audioformat.getFrameRate())*audioformat.getFrameSize();
        fmt_chunk.writeUnsignedInt(srate);
        fmt_chunk.writeUnsignedShort(audioformat.getFrameSize());
        fmt_chunk.writeUnsignedShort(audioformat.getSampleSizeInBits());
        fmt_chunk.write(0);
        fmt_chunk.write(0);

        writeSampleOptions(writer.writeChunk("wsmp"), sample.sampleoptions);

        if (convert_needed) {
            RIFFWriter data_chunk = writer.writeChunk("data");
            AudioInputStream stream = AudioSystem.getAudioInputStream(
                    audioformat, (AudioInputStream)sample.getData());
            stream.transferTo(data_chunk);
        } else {
            RIFFWriter data_chunk = writer.writeChunk("data");
            ModelByteBuffer databuff = sample.getDataBuffer();
            databuff.writeTo(data_chunk);
            /*
            data_chunk.write(databuff.array(),
            databuff.arrayOffset(),
            databuff.capacity());
             */
        }

        writeInfo(writer.writeList("INFO"), sample.info);
    }

    private void writeInstruments(RIFFWriter writer) throws IOException {
        for (DLSInstrument instrument : instruments) {
            writeInstrument(writer.writeList("ins "), instrument);
        }
    }

    private void writeInstrument(RIFFWriter writer, DLSInstrument instrument)
            throws IOException {

        int art1_count = 0;
        int art2_count = 0;
        for (DLSModulator modulator : instrument.getModulators()) {
            if (modulator.version == 1)
                art1_count++;
            if (modulator.version == 2)
                art2_count++;
        }
        for (DLSRegion region : instrument.regions) {
            for (DLSModulator modulator : region.getModulators()) {
                if (modulator.version == 1)
                    art1_count++;
                if (modulator.version == 2)
                    art2_count++;
            }
        }

        int version = 1;
        if (art2_count > 0)
            version = 2;

        RIFFWriter insh_chunk = writer.writeChunk("insh");
        insh_chunk.writeUnsignedInt(instrument.getRegions().size());
        insh_chunk.writeUnsignedInt(instrument.bank +
                (instrument.druminstrument ? 2147483648L : 0));
        insh_chunk.writeUnsignedInt(instrument.preset);

        RIFFWriter lrgn = writer.writeList("lrgn");
        for (DLSRegion region: instrument.regions)
            writeRegion(lrgn, region, version);

        writeArticulators(writer, instrument.getModulators());

        writeInfo(writer.writeList("INFO"), instrument.info);

    }

    private void writeArticulators(RIFFWriter writer,
            List<DLSModulator> modulators) throws IOException {
        int art1_count = 0;
        int art2_count = 0;
        for (DLSModulator modulator : modulators) {
            if (modulator.version == 1)
                art1_count++;
            if (modulator.version == 2)
                art2_count++;
        }
        if (art1_count > 0) {
            RIFFWriter lar1 = writer.writeList("lart");
            RIFFWriter art1 = lar1.writeChunk("art1");
            art1.writeUnsignedInt(8);
            art1.writeUnsignedInt(art1_count);
            for (DLSModulator modulator : modulators) {
                if (modulator.version == 1) {
                    art1.writeUnsignedShort(modulator.source);
                    art1.writeUnsignedShort(modulator.control);
                    art1.writeUnsignedShort(modulator.destination);
                    art1.writeUnsignedShort(modulator.transform);
                    art1.writeInt(modulator.scale);
                }
            }
        }
        if (art2_count > 0) {
            RIFFWriter lar2 = writer.writeList("lar2");
            RIFFWriter art2 = lar2.writeChunk("art2");
            art2.writeUnsignedInt(8);
            art2.writeUnsignedInt(art2_count);
            for (DLSModulator modulator : modulators) {
                if (modulator.version == 2) {
                    art2.writeUnsignedShort(modulator.source);
                    art2.writeUnsignedShort(modulator.control);
                    art2.writeUnsignedShort(modulator.destination);
                    art2.writeUnsignedShort(modulator.transform);
                    art2.writeInt(modulator.scale);
                }
            }
        }
    }

    private void writeRegion(RIFFWriter writer, DLSRegion region, int version)
            throws IOException {
        RIFFWriter rgns = null;
        if (version == 1)
            rgns = writer.writeList("rgn ");
        if (version == 2)
            rgns = writer.writeList("rgn2");
        if (rgns == null)
            return;

        RIFFWriter rgnh = rgns.writeChunk("rgnh");
        rgnh.writeUnsignedShort(region.keyfrom);
        rgnh.writeUnsignedShort(region.keyto);
        rgnh.writeUnsignedShort(region.velfrom);
        rgnh.writeUnsignedShort(region.velto);
        rgnh.writeUnsignedShort(region.options);
        rgnh.writeUnsignedShort(region.exclusiveClass);

        if (region.sampleoptions != null)
            writeSampleOptions(rgns.writeChunk("wsmp"), region.sampleoptions);

        if (region.sample != null) {
            if (samples.indexOf(region.sample) != -1) {
                RIFFWriter wlnk = rgns.writeChunk("wlnk");
                wlnk.writeUnsignedShort(region.fusoptions);
                wlnk.writeUnsignedShort(region.phasegroup);
                wlnk.writeUnsignedInt(region.channel);
                wlnk.writeUnsignedInt(samples.indexOf(region.sample));
            }
        }
        writeArticulators(rgns, region.getModulators());
        rgns.close();
    }

    private void writeSampleOptions(RIFFWriter wsmp,
            DLSSampleOptions sampleoptions) throws IOException {
        wsmp.writeUnsignedInt(20);
        wsmp.writeUnsignedShort(sampleoptions.unitynote);
        wsmp.writeShort(sampleoptions.finetune);
        wsmp.writeInt(sampleoptions.attenuation);
        wsmp.writeUnsignedInt(sampleoptions.options);
        wsmp.writeInt(sampleoptions.loops.size());

        for (DLSSampleLoop loop : sampleoptions.loops) {
            wsmp.writeUnsignedInt(16);
            wsmp.writeUnsignedInt(loop.type);
            wsmp.writeUnsignedInt(loop.start);
            wsmp.writeUnsignedInt(loop.length);
        }
    }

    private void writeInfoStringChunk(RIFFWriter writer,
            String name, String value) throws IOException {
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

    private void writeInfo(RIFFWriter writer, DLSInfo info) throws IOException {
        writeInfoStringChunk(writer, "INAM", info.name);
        writeInfoStringChunk(writer, "ICRD", info.creationDate);
        writeInfoStringChunk(writer, "IENG", info.engineers);
        writeInfoStringChunk(writer, "IPRD", info.product);
        writeInfoStringChunk(writer, "ICOP", info.copyright);
        writeInfoStringChunk(writer, "ICMT", info.comments);
        writeInfoStringChunk(writer, "ISFT", info.tools);
        writeInfoStringChunk(writer, "IARL", info.archival_location);
        writeInfoStringChunk(writer, "IART", info.artist);
        writeInfoStringChunk(writer, "ICMS", info.commissioned);
        writeInfoStringChunk(writer, "IGNR", info.genre);
        writeInfoStringChunk(writer, "IKEY", info.keywords);
        writeInfoStringChunk(writer, "IMED", info.medium);
        writeInfoStringChunk(writer, "ISBJ", info.subject);
        writeInfoStringChunk(writer, "ISRC", info.source);
        writeInfoStringChunk(writer, "ISRF", info.source_form);
        writeInfoStringChunk(writer, "ITCH", info.technician);
    }

    public DLSInfo getInfo() {
        return info;
    }

    @Override
    public String getName() {
        return info.name;
    }

    @Override
    public String getVersion() {
        return major + "." + minor;
    }

    @Override
    public String getVendor() {
        return info.engineers;
    }

    @Override
    public String getDescription() {
        return info.comments;
    }

    public void setName(String s) {
        info.name = s;
    }

    public void setVendor(String s) {
        info.engineers = s;
    }

    public void setDescription(String s) {
        info.comments = s;
    }

    @Override
    public SoundbankResource[] getResources() {
        SoundbankResource[] resources = new SoundbankResource[samples.size()];
        int j = 0;
        for (int i = 0; i < samples.size(); i++)
            resources[j++] = samples.get(i);
        return resources;
    }

    @Override
    public DLSInstrument[] getInstruments() {
        DLSInstrument[] inslist_array =
                instruments.toArray(new DLSInstrument[instruments.size()]);
        Arrays.sort(inslist_array, new ModelInstrumentComparator());
        return inslist_array;
    }

    public DLSSample[] getSamples() {
        return samples.toArray(new DLSSample[samples.size()]);
    }

    @Override
    public Instrument getInstrument(Patch patch) {
        int program = patch.getProgram();
        int bank = patch.getBank();
        boolean percussion = false;
        if (patch instanceof ModelPatch)
            percussion = ((ModelPatch) patch).isPercussion();
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

    public void addResource(SoundbankResource resource) {
        if (resource instanceof DLSInstrument)
            instruments.add((DLSInstrument) resource);
        if (resource instanceof DLSSample)
            samples.add((DLSSample) resource);
    }

    public void removeResource(SoundbankResource resource) {
        if (resource instanceof DLSInstrument)
            instruments.remove(resource);
        if (resource instanceof DLSSample)
            samples.remove(resource);
    }

    public void addInstrument(DLSInstrument resource) {
        instruments.add(resource);
    }

    public void removeInstrument(DLSInstrument resource) {
        instruments.remove(resource);
    }

    public long getMajor() {
        return major;
    }

    public void setMajor(long major) {
        this.major = major;
    }

    public long getMinor() {
        return minor;
    }

    public void setMinor(long minor) {
        this.minor = minor;
    }
}
