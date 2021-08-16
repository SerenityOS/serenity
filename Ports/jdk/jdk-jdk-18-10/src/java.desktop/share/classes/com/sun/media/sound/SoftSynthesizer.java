/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiChannel;
import javax.sound.midi.MidiDevice;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Patch;
import javax.sound.midi.Receiver;
import javax.sound.midi.Soundbank;
import javax.sound.midi.Transmitter;
import javax.sound.midi.VoiceStatus;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

/**
 * The software synthesizer class.
 *
 * @author Karl Helgason
 */
public final class SoftSynthesizer implements AudioSynthesizer,
        ReferenceCountingDevice {

    protected static final class WeakAudioStream extends InputStream
    {
        private volatile AudioInputStream stream;
        public SoftAudioPusher pusher = null;
        public AudioInputStream jitter_stream = null;
        public SourceDataLine sourceDataLine = null;
        public volatile long silent_samples = 0;
        private int framesize = 0;
        private final WeakReference<AudioInputStream> weak_stream_link;
        private final AudioFloatConverter converter;
        private float[] silentbuffer = null;
        private final int samplesize;

        public void setInputStream(AudioInputStream stream)
        {
            this.stream = stream;
        }

        @Override
        public int available() throws IOException {
            AudioInputStream local_stream = stream;
            if(local_stream != null)
                return local_stream.available();
            return 0;
        }

        @Override
        public int read() throws IOException {
             byte[] b = new byte[1];
             if (read(b) == -1)
                  return -1;
             return b[0] & 0xFF;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
             AudioInputStream local_stream = stream;
             if(local_stream != null)
                 return local_stream.read(b, off, len);
             else
             {
                 int flen = len / samplesize;
                 if(silentbuffer == null || silentbuffer.length < flen)
                     silentbuffer = new float[flen];
                 converter.toByteArray(silentbuffer, flen, b, off);

                 silent_samples += (long)((len / framesize));

                 if(pusher != null)
                 if(weak_stream_link.get() == null)
                 {
                     Runnable runnable = new Runnable()
                     {
                         SoftAudioPusher _pusher = pusher;
                         AudioInputStream _jitter_stream = jitter_stream;
                         SourceDataLine _sourceDataLine = sourceDataLine;
                         @Override
                         public void run()
                         {
                             _pusher.stop();
                             if(_jitter_stream != null)
                                try {
                                    _jitter_stream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                             if(_sourceDataLine != null)
                                 _sourceDataLine.close();
                         }
                     };
                     pusher = null;
                     jitter_stream = null;
                     sourceDataLine = null;
                     new Thread(null, runnable, "Synthesizer",0,false).start();
                 }
                 return len;
             }
        }

        public WeakAudioStream(AudioInputStream stream) {
            this.stream = stream;
            weak_stream_link = new WeakReference<>(stream);
            converter = AudioFloatConverter.getConverter(stream.getFormat());
            samplesize = stream.getFormat().getFrameSize() / stream.getFormat().getChannels();
            framesize = stream.getFormat().getFrameSize();
        }

        public AudioInputStream getAudioInputStream()
        {
            return new AudioInputStream(this, stream.getFormat(), AudioSystem.NOT_SPECIFIED);
        }

        @Override
        public void close() throws IOException
        {
            AudioInputStream astream  = weak_stream_link.get();
            if(astream != null)
                astream.close();
        }
    }

    private static class Info extends MidiDevice.Info {
        Info() {
            super(INFO_NAME, INFO_VENDOR, INFO_DESCRIPTION, INFO_VERSION);
        }
    }

    static final String INFO_NAME = "Gervill";
    static final String INFO_VENDOR = "OpenJDK";
    static final String INFO_DESCRIPTION = "Software MIDI Synthesizer";
    static final String INFO_VERSION = "1.0";
    static final MidiDevice.Info info = new Info();

    private static SourceDataLine testline = null;

    private static Soundbank defaultSoundBank = null;

    WeakAudioStream weakstream = null;

    final Object control_mutex = this;

    int voiceIDCounter = 0;

    // 0: default
    // 1: DLS Voice Allocation
    int voice_allocation_mode = 0;

    boolean load_default_soundbank = false;
    boolean reverb_light = true;
    boolean reverb_on = true;
    boolean chorus_on = true;
    boolean agc_on = true;

    SoftChannel[] channels;
    SoftChannelProxy[] external_channels = null;

    private boolean largemode = false;

    // 0: GM Mode off (default)
    // 1: GM Level 1
    // 2: GM Level 2
    private int gmmode = 0;

    private int deviceid = 0;

    private AudioFormat format = new AudioFormat(44100, 16, 2, true, false);

    private SourceDataLine sourceDataLine = null;

    private SoftAudioPusher pusher = null;
    private AudioInputStream pusher_stream = null;

    private float controlrate = 147f;

    private boolean open = false;
    private boolean implicitOpen = false;

    private String resamplerType = "linear";
    private SoftResampler resampler = new SoftLinearResampler();

    private int number_of_midi_channels = 16;
    private int maxpoly = 64;
    private long latency = 200000; // 200 msec
    private boolean jitter_correction = false;

    private SoftMainMixer mainmixer;
    private SoftVoice[] voices;

    private final Map<String, SoftTuning> tunings = new HashMap<>();
    private final Map<String, SoftInstrument> inslist = new HashMap<>();
    private final Map<String, ModelInstrument> loadedlist = new HashMap<>();
    private final ArrayList<Receiver> recvslist = new ArrayList<>();

    private void getBuffers(ModelInstrument instrument,
            List<ModelByteBuffer> buffers) {
        for (ModelPerformer performer : instrument.getPerformers()) {
            if (performer.getOscillators() != null) {
                for (ModelOscillator osc : performer.getOscillators()) {
                    if (osc instanceof ModelByteBufferWavetable) {
                        ModelByteBufferWavetable w = (ModelByteBufferWavetable)osc;
                        ModelByteBuffer buff = w.getBuffer();
                        if (buff != null)
                            buffers.add(buff);
                        buff = w.get8BitExtensionBuffer();
                        if (buff != null)
                            buffers.add(buff);
                    }
                }
            }
        }
    }

    private boolean loadSamples(List<ModelInstrument> instruments) {
        if (largemode)
            return true;
        List<ModelByteBuffer> buffers = new ArrayList<>();
        for (ModelInstrument instrument : instruments)
            getBuffers(instrument, buffers);
        try {
            ModelByteBuffer.loadAll(buffers);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private boolean loadInstruments(List<ModelInstrument> instruments) {
        if (!isOpen())
            return false;
        if (!loadSamples(instruments))
            return false;

        synchronized (control_mutex) {
            if (channels != null)
                for (SoftChannel c : channels)
                {
                    c.current_instrument = null;
                    c.current_director = null;
                }
            for (Instrument instrument : instruments) {
                String pat = patchToString(instrument.getPatch());
                SoftInstrument softins
                        = new SoftInstrument((ModelInstrument) instrument);
                inslist.put(pat, softins);
                loadedlist.put(pat, (ModelInstrument) instrument);
            }
        }

        return true;
    }

    private void processPropertyInfo(Map<String, Object> info) {
        AudioSynthesizerPropertyInfo[] items = getPropertyInfo(info);

        String resamplerType = (String)items[0].value;
        if (resamplerType.equalsIgnoreCase("point"))
        {
            this.resampler = new SoftPointResampler();
            this.resamplerType = "point";
        }
        else if (resamplerType.equalsIgnoreCase("linear"))
        {
            this.resampler = new SoftLinearResampler2();
            this.resamplerType = "linear";
        }
        else if (resamplerType.equalsIgnoreCase("linear1"))
        {
            this.resampler = new SoftLinearResampler();
            this.resamplerType = "linear1";
        }
        else if (resamplerType.equalsIgnoreCase("linear2"))
        {
            this.resampler = new SoftLinearResampler2();
            this.resamplerType = "linear2";
        }
        else if (resamplerType.equalsIgnoreCase("cubic"))
        {
            this.resampler = new SoftCubicResampler();
            this.resamplerType = "cubic";
        }
        else if (resamplerType.equalsIgnoreCase("lanczos"))
        {
            this.resampler = new SoftLanczosResampler();
            this.resamplerType = "lanczos";
        }
        else if (resamplerType.equalsIgnoreCase("sinc"))
        {
            this.resampler = new SoftSincResampler();
            this.resamplerType = "sinc";
        }

        setFormat((AudioFormat)items[2].value);
        controlrate = (Float)items[1].value;
        latency = (Long)items[3].value;
        deviceid = (Integer)items[4].value;
        maxpoly = (Integer)items[5].value;
        reverb_on = (Boolean)items[6].value;
        chorus_on = (Boolean)items[7].value;
        agc_on = (Boolean)items[8].value;
        largemode = (Boolean)items[9].value;
        number_of_midi_channels = (Integer)items[10].value;
        jitter_correction = (Boolean)items[11].value;
        reverb_light = (Boolean)items[12].value;
        load_default_soundbank = (Boolean)items[13].value;
    }

    private String patchToString(Patch patch) {
        if (patch instanceof ModelPatch && ((ModelPatch) patch).isPercussion())
            return "p." + patch.getProgram() + "." + patch.getBank();
        else
            return patch.getProgram() + "." + patch.getBank();
    }

    private void setFormat(AudioFormat format) {
        if (format.getChannels() > 2) {
            throw new IllegalArgumentException(
                    "Only mono and stereo audio supported.");
        }
        if (AudioFloatConverter.getConverter(format) == null)
            throw new IllegalArgumentException("Audio format not supported.");
        this.format = format;
    }

    void removeReceiver(Receiver recv) {
        boolean perform_close = false;
        synchronized (control_mutex) {
            if (recvslist.remove(recv)) {
                if (implicitOpen && recvslist.isEmpty())
                    perform_close = true;
            }
        }
        if (perform_close)
            close();
    }

    SoftMainMixer getMainMixer() {
        if (!isOpen())
            return null;
        return mainmixer;
    }

    SoftInstrument findInstrument(int program, int bank, int channel) {

        // Add support for GM2 banks 0x78 and 0x79
        // as specified in DLS 2.2 in Section 1.4.6
        // which allows using percussion and melodic instruments
        // on all channels
        if (bank >> 7 == 0x78 || bank >> 7 == 0x79) {
            SoftInstrument current_instrument
                    = inslist.get(program + "." + bank);
            if (current_instrument != null)
                return current_instrument;

            String p_plaf;
            if (bank >> 7 == 0x78)
                p_plaf = "p.";
            else
                p_plaf = "";

            // Instrument not found fallback to MSB:bank, LSB:0
            current_instrument = inslist.get(p_plaf + program + "."
                    + ((bank & 128) << 7));
            if (current_instrument != null)
                return current_instrument;
            // Instrument not found fallback to MSB:0, LSB:bank
            current_instrument = inslist.get(p_plaf + program + "."
                    + (bank & 128));
            if (current_instrument != null)
                return current_instrument;
            // Instrument not found fallback to MSB:0, LSB:0
            current_instrument = inslist.get(p_plaf + program + ".0");
            if (current_instrument != null)
                return current_instrument;
            // Instrument not found fallback to MSB:0, LSB:0, program=0
            current_instrument = inslist.get(p_plaf + program + "0.0");
            if (current_instrument != null)
                return current_instrument;
            return null;
        }

        // Channel 10 uses percussion instruments
        String p_plaf;
        if (channel == 9)
            p_plaf = "p.";
        else
            p_plaf = "";

        SoftInstrument current_instrument
                = inslist.get(p_plaf + program + "." + bank);
        if (current_instrument != null)
            return current_instrument;
        // Instrument not found fallback to MSB:0, LSB:0
        current_instrument = inslist.get(p_plaf + program + ".0");
        if (current_instrument != null)
            return current_instrument;
        // Instrument not found fallback to MSB:0, LSB:0, program=0
        current_instrument = inslist.get(p_plaf + "0.0");
        if (current_instrument != null)
            return current_instrument;
        return null;
    }

    int getVoiceAllocationMode() {
        return voice_allocation_mode;
    }

    int getGeneralMidiMode() {
        return gmmode;
    }

    void setGeneralMidiMode(int gmmode) {
        this.gmmode = gmmode;
    }

    int getDeviceID() {
        return deviceid;
    }

    float getControlRate() {
        return controlrate;
    }

    SoftVoice[] getVoices() {
        return voices;
    }

    SoftTuning getTuning(Patch patch) {
        String t_id = patchToString(patch);
        SoftTuning tuning = tunings.get(t_id);
        if (tuning == null) {
            tuning = new SoftTuning(patch);
            tunings.put(t_id, tuning);
        }
        return tuning;
    }

    @Override
    public long getLatency() {
        synchronized (control_mutex) {
            return latency;
        }
    }

    @Override
    public AudioFormat getFormat() {
        synchronized (control_mutex) {
            return format;
        }
    }

    @Override
    public int getMaxPolyphony() {
        synchronized (control_mutex) {
            return maxpoly;
        }
    }

    @Override
    public MidiChannel[] getChannels() {

        synchronized (control_mutex) {
            // if (external_channels == null) => the synthesizer is not open,
            // create 16 proxy channels
            // otherwise external_channels has the same length as channels array
            if (external_channels == null) {
                external_channels = new SoftChannelProxy[16];
                for (int i = 0; i < external_channels.length; i++)
                    external_channels[i] = new SoftChannelProxy();
            }
            MidiChannel[] ret;
            if (isOpen())
                ret = new MidiChannel[channels.length];
            else
                ret = new MidiChannel[16];
            for (int i = 0; i < ret.length; i++)
                ret[i] = external_channels[i];
            return ret;
        }
    }

    @Override
    public VoiceStatus[] getVoiceStatus() {
        if (!isOpen()) {
            VoiceStatus[] tempVoiceStatusArray
                    = new VoiceStatus[getMaxPolyphony()];
            for (int i = 0; i < tempVoiceStatusArray.length; i++) {
                VoiceStatus b = new VoiceStatus();
                b.active = false;
                b.bank = 0;
                b.channel = 0;
                b.note = 0;
                b.program = 0;
                b.volume = 0;
                tempVoiceStatusArray[i] = b;
            }
            return tempVoiceStatusArray;
        }

        synchronized (control_mutex) {
            VoiceStatus[] tempVoiceStatusArray = new VoiceStatus[voices.length];
            for (int i = 0; i < voices.length; i++) {
                VoiceStatus a = voices[i];
                VoiceStatus b = new VoiceStatus();
                b.active = a.active;
                b.bank = a.bank;
                b.channel = a.channel;
                b.note = a.note;
                b.program = a.program;
                b.volume = a.volume;
                tempVoiceStatusArray[i] = b;
            }
            return tempVoiceStatusArray;
        }
    }

    @Override
    public boolean isSoundbankSupported(Soundbank soundbank) {
        for (Instrument ins: soundbank.getInstruments())
            if (!(ins instanceof ModelInstrument))
                return false;
        return true;
    }

    @Override
    public boolean loadInstrument(Instrument instrument) {
        if (instrument == null || (!(instrument instanceof ModelInstrument))) {
            throw new IllegalArgumentException("Unsupported instrument: " +
                    instrument);
        }
        List<ModelInstrument> instruments = new ArrayList<>();
        instruments.add((ModelInstrument)instrument);
        return loadInstruments(instruments);
    }

    @Override
    public void unloadInstrument(Instrument instrument) {
        if (instrument == null || (!(instrument instanceof ModelInstrument))) {
            throw new IllegalArgumentException("Unsupported instrument: " +
                    instrument);
        }
        if (!isOpen())
            return;

        String pat = patchToString(instrument.getPatch());
        synchronized (control_mutex) {
            for (SoftChannel c: channels)
                c.current_instrument = null;
            inslist.remove(pat);
            loadedlist.remove(pat);
            for (int i = 0; i < channels.length; i++) {
                channels[i].allSoundOff();
            }
        }
    }

    @Override
    public boolean remapInstrument(Instrument from, Instrument to) {

        if (from == null)
            throw new NullPointerException();
        if (to == null)
            throw new NullPointerException();
        if (!(from instanceof ModelInstrument)) {
            throw new IllegalArgumentException("Unsupported instrument: " +
                    from.toString());
        }
        if (!(to instanceof ModelInstrument)) {
            throw new IllegalArgumentException("Unsupported instrument: " +
                    to.toString());
        }
        if (!isOpen())
            return false;

        synchronized (control_mutex) {
            if (!loadedlist.containsValue(to))
                throw new IllegalArgumentException("Instrument to is not loaded.");
            unloadInstrument(from);
            ModelMappedInstrument mfrom = new ModelMappedInstrument(
                    (ModelInstrument)to, from.getPatch());
            return loadInstrument(mfrom);
        }
    }

    @Override
    public Soundbank getDefaultSoundbank() {
        synchronized (SoftSynthesizer.class) {
            if (defaultSoundBank != null)
                return defaultSoundBank;

            List<PrivilegedAction<InputStream>> actions = new ArrayList<>();

            actions.add(new PrivilegedAction<InputStream>() {
                @Override
                public InputStream run() {
                    File javahome = new File(System.getProperties()
                            .getProperty("java.home"));
                    File libaudio = new File(new File(javahome, "lib"), "audio");
                    if (libaudio.isDirectory()) {
                        File foundfile = null;
                        File[] files = libaudio.listFiles();
                        if (files != null) {
                            for (int i = 0; i < files.length; i++) {
                                File file = files[i];
                                if (file.isFile()) {
                                    String lname = file.getName().toLowerCase();
                                    if (lname.endsWith(".sf2")
                                            || lname.endsWith(".dls")) {
                                        if (foundfile == null
                                                || (file.length() > foundfile
                                                        .length())) {
                                            foundfile = file;
                                        }
                                    }
                                }
                            }
                        }
                        if (foundfile != null) {
                            try {
                                return new FileInputStream(foundfile);
                            } catch (IOException e) {
                            }
                        }
                    }
                    return null;
                }
            });

            actions.add(new PrivilegedAction<InputStream>() {
                @Override
                public InputStream run() {
                    if (System.getProperties().getProperty("os.name")
                            .startsWith("Linux")) {

                        File[] systemSoundFontsDir = new File[] {
                            /* Arch, Fedora, Mageia */
                            new File("/usr/share/soundfonts/"),
                            new File("/usr/local/share/soundfonts/"),
                            /* Debian, Gentoo, OpenSUSE, Ubuntu */
                            new File("/usr/share/sounds/sf2/"),
                            new File("/usr/local/share/sounds/sf2/"),
                        };

                        /*
                         * Look for a default.sf2
                         */
                        for (File systemSoundFontDir : systemSoundFontsDir) {
                            if (systemSoundFontDir.isDirectory()) {
                                File defaultSoundFont = new File(systemSoundFontDir, "default.sf2");
                                if (defaultSoundFont.isFile()) {
                                    try {
                                        return new FileInputStream(defaultSoundFont);
                                    } catch (IOException e) {
                                        // continue with lookup
                                    }
                                }
                            }
                        }
                    }
                    return null;
                }
            });

            actions.add(new PrivilegedAction<InputStream>() {
                @Override
                public InputStream run() {
                    if (System.getProperties().getProperty("os.name")
                            .startsWith("Windows")) {
                        File gm_dls = new File(System.getenv("SystemRoot")
                                + "\\system32\\drivers\\gm.dls");
                        if (gm_dls.isFile()) {
                            try {
                                return new FileInputStream(gm_dls);
                            } catch (IOException e) {
                            }
                        }
                    }
                    return null;
                }
            });

            actions.add(new PrivilegedAction<InputStream>() {
                @Override
                public InputStream run() {
                    /*
                     * Try to load saved generated soundbank
                     */
                    File userhome = new File(System.getProperty("user.home"),
                            ".gervill");
                    File emg_soundbank_file = new File(userhome,
                            "soundbank-emg.sf2");
                    if (emg_soundbank_file.isFile()) {
                        try {
                            return new FileInputStream(emg_soundbank_file);
                        } catch (IOException e) {
                        }
                    }
                    return null;
                }
            });

            for (PrivilegedAction<InputStream> action : actions) {
                try {
                    @SuppressWarnings("removal")
                    InputStream is = AccessController.doPrivileged(action);
                    if(is == null) continue;
                    Soundbank sbk;
                    try {
                        sbk = MidiSystem.getSoundbank(new BufferedInputStream(is));
                    } finally {
                        is.close();
                    }
                    if (sbk != null) {
                        defaultSoundBank = sbk;
                        return defaultSoundBank;
                    }
                } catch (Exception e) {
                }
            }

            try {
                /*
                 * Generate emergency soundbank
                 */
                defaultSoundBank = EmergencySoundbank.createSoundbank();
            } catch (Exception e) {
            }

            if (defaultSoundBank != null) {
                /*
                 * Save generated soundbank to disk for faster future use.
                 */
                @SuppressWarnings("removal")
                OutputStream out = AccessController
                        .doPrivileged((PrivilegedAction<OutputStream>) () -> {
                            try {
                                File userhome = new File(System
                                        .getProperty("user.home"), ".gervill");
                                if (!userhome.isDirectory()) {
                                    if (!userhome.mkdirs()) {
                                        return null;
                                    }
                                }
                                File emg_soundbank_file = new File(
                                        userhome, "soundbank-emg.sf2");
                                if (emg_soundbank_file.isFile()) {
                                    return null;
                                }
                                return new FileOutputStream(emg_soundbank_file);
                            } catch (final FileNotFoundException ignored) {
                            }
                            return null;
                        });
                if (out != null) {
                    try {
                        ((SF2Soundbank) defaultSoundBank).save(out);
                        out.close();
                    } catch (final IOException ignored) {
                    }
                }
            }
        }
        return defaultSoundBank;
    }

    @Override
    public Instrument[] getAvailableInstruments() {
        Soundbank defsbk = getDefaultSoundbank();
        if (defsbk == null)
            return new Instrument[0];
        Instrument[] inslist_array = defsbk.getInstruments();
        Arrays.sort(inslist_array, new ModelInstrumentComparator());
        return inslist_array;
    }

    @Override
    public Instrument[] getLoadedInstruments() {
        if (!isOpen())
            return new Instrument[0];

        synchronized (control_mutex) {
            ModelInstrument[] inslist_array =
                    new ModelInstrument[loadedlist.values().size()];
            loadedlist.values().toArray(inslist_array);
            Arrays.sort(inslist_array, new ModelInstrumentComparator());
            return inslist_array;
        }
    }

    @Override
    public boolean loadAllInstruments(Soundbank soundbank) {
        List<ModelInstrument> instruments = new ArrayList<>();
        for (Instrument ins: soundbank.getInstruments()) {
            if (ins == null || !(ins instanceof ModelInstrument)) {
                throw new IllegalArgumentException(
                        "Unsupported instrument: " + ins);
            }
            instruments.add((ModelInstrument)ins);
        }
        return loadInstruments(instruments);
    }

    @Override
    public void unloadAllInstruments(Soundbank soundbank) {
        if (soundbank == null || !isSoundbankSupported(soundbank))
            throw new IllegalArgumentException("Unsupported soundbank: " + soundbank);

        if (!isOpen())
            return;

        for (Instrument ins: soundbank.getInstruments()) {
            if (ins instanceof ModelInstrument) {
                unloadInstrument(ins);
            }
        }
    }

    @Override
    public boolean loadInstruments(Soundbank soundbank, Patch[] patchList) {
        List<ModelInstrument> instruments = new ArrayList<>();
        for (Patch patch: patchList) {
            Instrument ins = soundbank.getInstrument(patch);
            if (ins == null || !(ins instanceof ModelInstrument)) {
                throw new IllegalArgumentException(
                        "Unsupported instrument: " + ins);
            }
            instruments.add((ModelInstrument)ins);
        }
        return loadInstruments(instruments);
    }

    @Override
    public void unloadInstruments(Soundbank soundbank, Patch[] patchList) {
        if (soundbank == null || !isSoundbankSupported(soundbank))
            throw new IllegalArgumentException("Unsupported soundbank: " + soundbank);

        if (!isOpen())
            return;

        for (Patch pat: patchList) {
            Instrument ins = soundbank.getInstrument(pat);
            if (ins instanceof ModelInstrument) {
                unloadInstrument(ins);
            }
        }
    }

    @Override
    public MidiDevice.Info getDeviceInfo() {
        return info;
    }

    @SuppressWarnings("removal")
    private Properties getStoredProperties() {
        return AccessController
                .doPrivileged((PrivilegedAction<Properties>) () -> {
                    Properties p = new Properties();
                    String notePath = "/com/sun/media/sound/softsynthesizer";
                    try {
                        Preferences prefroot = Preferences.userRoot();
                        if (prefroot.nodeExists(notePath)) {
                            Preferences prefs = prefroot.node(notePath);
                            String[] prefs_keys = prefs.keys();
                            for (String prefs_key : prefs_keys) {
                                String val = prefs.get(prefs_key, null);
                                if (val != null) {
                                    p.setProperty(prefs_key, val);
                                }
                            }
                        }
                    } catch (final BackingStoreException ignored) {
                    }
                    return p;
                });
    }

    @Override
    public AudioSynthesizerPropertyInfo[] getPropertyInfo(Map<String, Object> info) {
        List<AudioSynthesizerPropertyInfo> list = new ArrayList<>();

        AudioSynthesizerPropertyInfo item;

        // If info != null or synthesizer is closed
        //   we return how the synthesizer will be set on next open
        // If info == null and synthesizer is open
        //   we return current synthesizer properties.
        boolean o = info == null && open;

        item = new AudioSynthesizerPropertyInfo("interpolation", o?resamplerType:"linear");
        item.choices = new String[]{"linear", "linear1", "linear2", "cubic",
                                    "lanczos", "sinc", "point"};
        item.description = "Interpolation method";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("control rate", o?controlrate:147f);
        item.description = "Control rate";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("format",
                o?format:new AudioFormat(44100, 16, 2, true, false));
        item.description = "Default audio format";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("latency", o?latency:120000L);
        item.description = "Default latency";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("device id", o?deviceid:0);
        item.description = "Device ID for SysEx Messages";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("max polyphony", o?maxpoly:64);
        item.description = "Maximum polyphony";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("reverb", o?reverb_on:true);
        item.description = "Turn reverb effect on or off";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("chorus", o?chorus_on:true);
        item.description = "Turn chorus effect on or off";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("auto gain control", o?agc_on:true);
        item.description = "Turn auto gain control on or off";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("large mode", o?largemode:false);
        item.description = "Turn large mode on or off.";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("midi channels", o?channels.length:16);
        item.description = "Number of midi channels.";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("jitter correction", o?jitter_correction:true);
        item.description = "Turn jitter correction on or off.";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("light reverb", o?reverb_light:true);
        item.description = "Turn light reverb mode on or off";
        list.add(item);

        item = new AudioSynthesizerPropertyInfo("load default soundbank", o?load_default_soundbank:true);
        item.description = "Enabled/disable loading default soundbank";
        list.add(item);

        AudioSynthesizerPropertyInfo[] items;
        items = list.toArray(new AudioSynthesizerPropertyInfo[list.size()]);

        Properties storedProperties = getStoredProperties();

        for (AudioSynthesizerPropertyInfo item2 : items) {
            Object v = (info == null) ? null : info.get(item2.name);
            v = (v != null) ? v : storedProperties.getProperty(item2.name);
            if (v != null) {
                Class<?> c = (item2.valueClass);
                if (c.isInstance(v))
                    item2.value = v;
                else if (v instanceof String) {
                    String s = (String) v;
                    if (c == Boolean.class) {
                        if (s.equalsIgnoreCase("true"))
                            item2.value = Boolean.TRUE;
                        if (s.equalsIgnoreCase("false"))
                            item2.value = Boolean.FALSE;
                    } else if (c == AudioFormat.class) {
                        int channels = 2;
                        boolean signed = true;
                        boolean bigendian = false;
                        int bits = 16;
                        float sampleRate = 44100f;
                        try {
                            StringTokenizer st = new StringTokenizer(s, ", ");
                            String prevToken = "";
                            while (st.hasMoreTokens()) {
                                String token = st.nextToken().toLowerCase();
                                if (token.equals("mono"))
                                    channels = 1;
                                if (token.startsWith("channel"))
                                    channels = Integer.parseInt(prevToken);
                                if (token.contains("unsigned"))
                                    signed = false;
                                if (token.equals("big-endian"))
                                    bigendian = true;
                                if (token.equals("bit"))
                                    bits = Integer.parseInt(prevToken);
                                if (token.equals("hz"))
                                    sampleRate = Float.parseFloat(prevToken);
                                prevToken = token;
                            }
                            item2.value = new AudioFormat(sampleRate, bits,
                                    channels, signed, bigendian);
                        } catch (NumberFormatException e) {
                        }

                    } else
                        try {
                            if (c == Byte.class)
                                item2.value = Byte.valueOf(s);
                            else if (c == Short.class)
                                item2.value = Short.valueOf(s);
                            else if (c == Integer.class)
                                item2.value = Integer.valueOf(s);
                            else if (c == Long.class)
                                item2.value = Long.valueOf(s);
                            else if (c == Float.class)
                                item2.value = Float.valueOf(s);
                            else if (c == Double.class)
                                item2.value = Double.valueOf(s);
                        } catch (NumberFormatException e) {
                        }
                } else if (v instanceof Number) {
                    Number n = (Number) v;
                    if (c == Byte.class)
                        item2.value = Byte.valueOf(n.byteValue());
                    if (c == Short.class)
                        item2.value = Short.valueOf(n.shortValue());
                    if (c == Integer.class)
                        item2.value = Integer.valueOf(n.intValue());
                    if (c == Long.class)
                        item2.value = Long.valueOf(n.longValue());
                    if (c == Float.class)
                        item2.value = Float.valueOf(n.floatValue());
                    if (c == Double.class)
                        item2.value = Double.valueOf(n.doubleValue());
                }
            }
        }

        return items;
    }

    @Override
    public void open() throws MidiUnavailableException {
        if (isOpen()) {
            synchronized (control_mutex) {
                implicitOpen = false;
            }
            return;
        }
        open(null, null);
    }

    @Override
    public void open(SourceDataLine line, Map<String, Object> info) throws MidiUnavailableException {
        if (isOpen()) {
            synchronized (control_mutex) {
                implicitOpen = false;
            }
            return;
        }
        synchronized (control_mutex) {
            try {
                if (line != null) {
                    // can throw IllegalArgumentException
                    setFormat(line.getFormat());
                }

                AudioInputStream ais = openStream(getFormat(), info);

                weakstream = new WeakAudioStream(ais);
                ais = weakstream.getAudioInputStream();

                if (line == null)
                {
                    if (testline != null) {
                        line = testline;
                    } else {
                        // can throw LineUnavailableException,
                        // IllegalArgumentException, SecurityException
                        line = AudioSystem.getSourceDataLine(getFormat());
                    }
                }

                double latency = this.latency;

                if (!line.isOpen()) {
                    int bufferSize = getFormat().getFrameSize()
                        * (int)(getFormat().getFrameRate() * (latency/1000000f));
                    // can throw LineUnavailableException,
                    // IllegalArgumentException, SecurityException
                    line.open(getFormat(), bufferSize);

                    // Remember that we opened that line
                    // so we can close again in SoftSynthesizer.close()
                    sourceDataLine = line;
                }
                if (!line.isActive())
                    line.start();

                int controlbuffersize = 512;
                try {
                    controlbuffersize = ais.available();
                } catch (IOException e) {
                }

                // Tell mixer not fill read buffers fully.
                // This lowers latency, and tells DataPusher
                // to read in smaller amounts.
                //mainmixer.readfully = false;
                //pusher = new DataPusher(line, ais);

                int buffersize = line.getBufferSize();
                buffersize -= buffersize % controlbuffersize;

                if (buffersize < 3 * controlbuffersize)
                    buffersize = 3 * controlbuffersize;

                if (jitter_correction) {
                    ais = new SoftJitterCorrector(ais, buffersize,
                            controlbuffersize);
                    if(weakstream != null)
                        weakstream.jitter_stream = ais;
                }
                pusher = new SoftAudioPusher(line, ais, controlbuffersize);
                pusher_stream = ais;
                pusher.start();

                if(weakstream != null)
                {
                    weakstream.pusher = pusher;
                    weakstream.sourceDataLine = sourceDataLine;
                }

            } catch (final LineUnavailableException | SecurityException
                    | IllegalArgumentException e) {
                if (isOpen()) {
                    close();
                }
                // am: need MidiUnavailableException(Throwable) ctor!
                MidiUnavailableException ex = new MidiUnavailableException(
                        "Can not open line");
                ex.initCause(e);
                throw ex;
            }
        }
    }

    @Override
    public AudioInputStream openStream(AudioFormat targetFormat,
                                       Map<String, Object> info) throws MidiUnavailableException {

        if (isOpen())
            throw new MidiUnavailableException("Synthesizer is already open");

        synchronized (control_mutex) {

            gmmode = 0;
            voice_allocation_mode = 0;

            processPropertyInfo(info);

            open = true;
            implicitOpen = false;

            if (targetFormat != null)
                setFormat(targetFormat);

            if (load_default_soundbank)
            {
                Soundbank defbank = getDefaultSoundbank();
                if (defbank != null) {
                    loadAllInstruments(defbank);
                }
            }

            voices = new SoftVoice[maxpoly];
            for (int i = 0; i < maxpoly; i++)
                voices[i] = new SoftVoice(this);

            mainmixer = new SoftMainMixer(this);

            channels = new SoftChannel[number_of_midi_channels];
            for (int i = 0; i < channels.length; i++)
                channels[i] = new SoftChannel(this, i);

            if (external_channels == null) {
                // Always create external_channels array
                // with 16 or more channels
                // so getChannels works correctly
                // when the synhtesizer is closed.
                if (channels.length < 16)
                    external_channels = new SoftChannelProxy[16];
                else
                    external_channels = new SoftChannelProxy[channels.length];
                for (int i = 0; i < external_channels.length; i++)
                    external_channels[i] = new SoftChannelProxy();
            } else {
                // We must resize external_channels array
                // but we must also copy the old SoftChannelProxy
                // into the new one
                if (channels.length > external_channels.length) {
                    SoftChannelProxy[] new_external_channels
                            = new SoftChannelProxy[channels.length];
                    for (int i = 0; i < external_channels.length; i++)
                        new_external_channels[i] = external_channels[i];
                    for (int i = external_channels.length;
                            i < new_external_channels.length; i++) {
                        new_external_channels[i] = new SoftChannelProxy();
                    }
                }
            }

            for (int i = 0; i < channels.length; i++)
                external_channels[i].setChannel(channels[i]);

            for (SoftVoice voice: getVoices())
                voice.resampler = resampler.openStreamer();

            for (Receiver recv: getReceivers()) {
                SoftReceiver srecv = ((SoftReceiver)recv);
                srecv.open = open;
                srecv.mainmixer = mainmixer;
                srecv.midimessages = mainmixer.midimessages;
            }

            return mainmixer.getInputStream();
        }
    }

    @Override
    public void close() {

        if (!isOpen())
            return;

        SoftAudioPusher pusher_to_be_closed = null;
        AudioInputStream pusher_stream_to_be_closed = null;
        synchronized (control_mutex) {
            if (pusher != null) {
                pusher_to_be_closed = pusher;
                pusher_stream_to_be_closed = pusher_stream;
                pusher = null;
                pusher_stream = null;
            }
        }

        if (pusher_to_be_closed != null) {
            // Pusher must not be closed synchronized against control_mutex,
            // this may result in synchronized conflict between pusher
            // and current thread.
            pusher_to_be_closed.stop();

            try {
                pusher_stream_to_be_closed.close();
            } catch (IOException e) {
                //e.printStackTrace();
            }
        }

        synchronized (control_mutex) {

            if (mainmixer != null)
                mainmixer.close();
            open = false;
            implicitOpen = false;
            mainmixer = null;
            voices = null;
            channels = null;

            if (external_channels != null)
                for (int i = 0; i < external_channels.length; i++)
                    external_channels[i].setChannel(null);

            if (sourceDataLine != null) {
                sourceDataLine.close();
                sourceDataLine = null;
            }

            inslist.clear();
            loadedlist.clear();
            tunings.clear();

            while (recvslist.size() != 0)
                recvslist.get(recvslist.size() - 1).close();

        }
    }

    @Override
    public boolean isOpen() {
        synchronized (control_mutex) {
            return open;
        }
    }

    @Override
    public long getMicrosecondPosition() {

        if (!isOpen())
            return 0;

        synchronized (control_mutex) {
            return mainmixer.getMicrosecondPosition();
        }
    }

    @Override
    public int getMaxReceivers() {
        return -1;
    }

    @Override
    public int getMaxTransmitters() {
        return 0;
    }

    @Override
    public Receiver getReceiver() throws MidiUnavailableException {

        synchronized (control_mutex) {
            SoftReceiver receiver = new SoftReceiver(this);
            receiver.open = open;
            recvslist.add(receiver);
            return receiver;
        }
    }

    @Override
    public List<Receiver> getReceivers() {

        synchronized (control_mutex) {
            ArrayList<Receiver> recvs = new ArrayList<>();
            recvs.addAll(recvslist);
            return recvs;
        }
    }

    @Override
    public Transmitter getTransmitter() throws MidiUnavailableException {

        throw new MidiUnavailableException("No transmitter available");
    }

    @Override
    public List<Transmitter> getTransmitters() {

        return new ArrayList<>();
    }

    @Override
    public Receiver getReceiverReferenceCounting()
            throws MidiUnavailableException {

        if (!isOpen()) {
            open();
            synchronized (control_mutex) {
                implicitOpen = true;
            }
        }

        return getReceiver();
    }

    @Override
    public Transmitter getTransmitterReferenceCounting()
            throws MidiUnavailableException {

        throw new MidiUnavailableException("No transmitter available");
    }
}
