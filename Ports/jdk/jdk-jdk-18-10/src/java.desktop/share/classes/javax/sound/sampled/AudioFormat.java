/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.sound.sampled;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * {@code AudioFormat} is the class that specifies a particular arrangement of
 * data in a sound stream. By examining the information stored in the audio
 * format, you can discover how to interpret the bits in the binary sound data.
 * <p>
 * Every data line has an audio format associated with its data stream. The
 * audio format of a source (playback) data line indicates what kind of data the
 * data line expects to receive for output. For a target (capture) data line,
 * the audio format specifies the kind of the data that can be read from the
 * line.
 * <p>
 * Sound files also have audio formats, of course. The {@link AudioFileFormat}
 * class encapsulates an {@code AudioFormat} in addition to other, file-specific
 * information. Similarly, an {@link AudioInputStream} has an
 * {@code AudioFormat}.
 * <p>
 * The {@code AudioFormat} class accommodates a number of common sound-file
 * encoding techniques, including pulse-code modulation (PCM), mu-law encoding,
 * and a-law encoding. These encoding techniques are predefined, but service
 * providers can create new encoding types. The encoding that a specific format
 * uses is named by its {@code encoding} field.
 * <p>
 * In addition to the encoding, the audio format includes other properties that
 * further specify the exact arrangement of the data. These include the number
 * of channels, sample rate, sample size, byte order, frame rate, and frame
 * size. Sounds may have different numbers of audio channels: one for mono, two
 * for stereo. The sample rate measures how many "snapshots" (samples) of the
 * sound pressure are taken per second, per channel. (If the sound is stereo
 * rather than mono, two samples are actually measured at each instant of time:
 * one for the left channel, and another for the right channel; however, the
 * sample rate still measures the number per channel, so the rate is the same
 * regardless of the number of channels. This is the standard use of the term.)
 * The sample size indicates how many bits are used to store each snapshot; 8
 * and 16 are typical values. For 16-bit samples (or any other sample size
 * larger than a byte), byte order is important; the bytes in each sample are
 * arranged in either the "little-endian" or "big-endian" style. For encodings
 * like PCM, a frame consists of the set of samples for all channels at a given
 * point in time, and so the size of a frame (in bytes) is always equal to the
 * size of a sample (in bytes) times the number of channels. However, with some
 * other sorts of encodings a frame can contain a bundle of compressed data for
 * a whole series of samples, as well as additional, non-sample data. For such
 * encodings, the sample rate and sample size refer to the data after it is
 * decoded into PCM, and so they are completely different from the frame rate
 * and frame size.
 * <p>
 * An {@code AudioFormat} object can include a set of properties. A property is
 * a pair of key and value: the key is of type {@code String}, the associated
 * property value is an arbitrary object. Properties specify additional format
 * specifications, like the bit rate for compressed formats. Properties are
 * mainly used as a means to transport additional information of the audio
 * format to and from the service providers. Therefore, properties are ignored
 * in the {@link #matches(AudioFormat)} method. However, methods which rely on
 * the installed service providers, like
 * {@link AudioSystem#isConversionSupported (AudioFormat, AudioFormat)
 * isConversionSupported} may consider properties, depending on the respective
 * service provider implementation.
 * <p>
 * The following table lists some common properties which service providers
 * should use, if applicable:
 *
 * <table class="striped">
 * <caption>Audio Format Properties</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Property key
 *     <th scope="col">Value type
 *     <th scope="col">Description
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">"bitrate"
 *     <td>{@link java.lang.Integer Integer}
 *     <td>average bit rate in bits per second
 *   <tr>
 *     <th scope="row">"vbr"
 *     <td>{@link java.lang.Boolean Boolean}
 *     <td>{@code true}, if the file is encoded in variable bit rate (VBR)
 *   <tr>
 *     <th scope="row">"quality"
 *     <td>{@link java.lang.Integer Integer}
 *     <td>encoding/conversion quality, 1..100
 * </tbody>
 * </table>
 * <p>
 * Vendors of service providers (plugins) are encouraged to seek information
 * about other already established properties in third party plugins, and follow
 * the same conventions.
 *
 * @author Kara Kytle
 * @author Florian Bomers
 * @see DataLine#getFormat
 * @see AudioInputStream#getFormat
 * @see AudioFileFormat
 * @see javax.sound.sampled.spi.FormatConversionProvider
 * @since 1.3
 */
public class AudioFormat {

    /**
     * The audio encoding technique used by this format.
     */
    protected Encoding encoding;

    /**
     * The number of samples played or recorded per second, for sounds that have
     * this format.
     */
    protected float sampleRate;

    /**
     * The number of bits in each sample of a sound that has this format.
     */
    protected int sampleSizeInBits;

    /**
     * The number of audio channels in this format (1 for mono, 2 for stereo).
     */
    protected int channels;

    /**
     * The number of bytes in each frame of a sound that has this format.
     */
    protected int frameSize;

    /**
     * The number of frames played or recorded per second, for sounds that have
     * this format.
     */
    protected float frameRate;

    /**
     * Indicates whether the audio data is stored in big-endian or little-endian
     * order.
     */
    protected boolean bigEndian;

    /**
     * The set of properties.
     */
    private HashMap<String, Object> properties;

    /**
     * Constructs an {@code AudioFormat} with the given parameters. The encoding
     * specifies the convention used to represent the data. The other parameters
     * are further explained in the {@link AudioFormat class description}.
     *
     * @param  encoding the audio encoding technique
     * @param  sampleRate the number of samples per second
     * @param  sampleSizeInBits the number of bits in each sample
     * @param  channels the number of channels (1 for mono, 2 for stereo, and so
     *         on)
     * @param  frameSize the number of bytes in each frame
     * @param  frameRate the number of frames per second
     * @param  bigEndian indicates whether the data for a single sample is
     *         stored in big-endian byte order ({@code false} means
     *         little-endian)
     */
    public AudioFormat(Encoding encoding, float sampleRate, int sampleSizeInBits,
                       int channels, int frameSize, float frameRate, boolean bigEndian) {

        this.encoding = encoding;
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.frameSize = frameSize;
        this.frameRate = frameRate;
        this.bigEndian = bigEndian;
        this.properties = null;
    }

    /**
     * Constructs an {@code AudioFormat} with the given parameters. The encoding
     * specifies the convention used to represent the data. The other parameters
     * are further explained in the {@link AudioFormat class description}.
     *
     * @param  encoding the audio encoding technique
     * @param  sampleRate the number of samples per second
     * @param  sampleSizeInBits the number of bits in each sample
     * @param  channels the number of channels (1 for mono, 2 for stereo, and so
     *         on)
     * @param  frameSize the number of bytes in each frame
     * @param  frameRate the number of frames per second
     * @param  bigEndian indicates whether the data for a single sample is
     *         stored in big-endian byte order ({@code false} means
     *         little-endian)
     * @param  properties a {@code Map<String, Object>} object containing format
     *         properties
     * @since 1.5
     */
    public AudioFormat(Encoding encoding, float sampleRate,
                       int sampleSizeInBits, int channels,
                       int frameSize, float frameRate,
                       boolean bigEndian, Map<String, Object> properties) {
        this(encoding, sampleRate, sampleSizeInBits, channels,
             frameSize, frameRate, bigEndian);
        this.properties = new HashMap<>(properties);
    }

    /**
     * Constructs an {@code AudioFormat} with a linear PCM encoding and the
     * given parameters. The frame size is set to the number of bytes required
     * to contain one sample from each channel, and the frame rate is set to the
     * sample rate.
     *
     * @param  sampleRate the number of samples per second
     * @param  sampleSizeInBits the number of bits in each sample
     * @param  channels the number of channels (1 for mono, 2 for stereo, and so
     *         on)
     * @param  signed indicates whether the data is signed or unsigned
     * @param  bigEndian indicates whether the data for a single sample is
     *         stored in big-endian byte order ({@code false} means
     *         little-endian)
     */
    public AudioFormat(float sampleRate, int sampleSizeInBits,
                       int channels, boolean signed, boolean bigEndian) {

        this((signed == true ? Encoding.PCM_SIGNED : Encoding.PCM_UNSIGNED),
             sampleRate,
             sampleSizeInBits,
             channels,
             (channels == AudioSystem.NOT_SPECIFIED || sampleSizeInBits == AudioSystem.NOT_SPECIFIED)?
             AudioSystem.NOT_SPECIFIED:
             ((sampleSizeInBits + 7) / 8) * channels,
             sampleRate,
             bigEndian);
    }

    /**
     * Obtains the type of encoding for sounds in this format.
     *
     * @return the encoding type
     * @see Encoding#PCM_SIGNED
     * @see Encoding#PCM_UNSIGNED
     * @see Encoding#ULAW
     * @see Encoding#ALAW
     */
    public Encoding getEncoding() {
        return encoding;
    }

    /**
     * Obtains the sample rate. For compressed formats, the return value is the
     * sample rate of the uncompressed audio data. When this {@code AudioFormat}
     * is used for queries (e.g.
     * {@link AudioSystem#isConversionSupported(AudioFormat, AudioFormat)
     * AudioSystem.isConversionSupported}) or capabilities (e.g.
     * {@link DataLine.Info#getFormats DataLine.Info.getFormats}), a sample rate
     * of {@code AudioSystem.NOT_SPECIFIED} means that any sample rate is
     * acceptable. {@code AudioSystem.NOT_SPECIFIED} is also returned when the
     * sample rate is not defined for this audio format.
     *
     * @return the number of samples per second, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @see #getFrameRate()
     * @see AudioSystem#NOT_SPECIFIED
     */
    public float getSampleRate() {
        return sampleRate;
    }

    /**
     * Obtains the size of a sample. For compressed formats, the return value is
     * the sample size of the uncompressed audio data. When this
     * {@code AudioFormat} is used for queries (e.g.
     * {@link AudioSystem#isConversionSupported(AudioFormat,AudioFormat)
     * AudioSystem.isConversionSupported}) or capabilities (e.g.
     * {@link DataLine.Info#getFormats DataLine.Info.getFormats}), a sample size
     * of {@code AudioSystem.NOT_SPECIFIED} means that any sample size is
     * acceptable. {@code AudioSystem.NOT_SPECIFIED} is also returned when the
     * sample size is not defined for this audio format.
     *
     * @return the number of bits in each sample, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @see #getFrameSize()
     * @see AudioSystem#NOT_SPECIFIED
     */
    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    /**
     * Obtains the number of channels. When this {@code AudioFormat} is used for
     * queries (e.g. {@link AudioSystem#isConversionSupported(AudioFormat,
     * AudioFormat) AudioSystem.isConversionSupported}) or capabilities (e.g.
     * {@link DataLine.Info#getFormats DataLine.Info.getFormats}), a return
     * value of {@code AudioSystem.NOT_SPECIFIED} means that any (positive)
     * number of channels is acceptable.
     *
     * @return The number of channels (1 for mono, 2 for stereo, etc.), or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @see AudioSystem#NOT_SPECIFIED
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Obtains the frame size in bytes. When this {@code AudioFormat} is used
     * for queries (e.g. {@link AudioSystem#isConversionSupported(AudioFormat,
     * AudioFormat) AudioSystem.isConversionSupported}) or capabilities (e.g.
     * {@link DataLine.Info#getFormats DataLine.Info.getFormats}), a frame size
     * of {@code AudioSystem.NOT_SPECIFIED} means that any frame size is
     * acceptable. {@code AudioSystem.NOT_SPECIFIED} is also returned when the
     * frame size is not defined for this audio format.
     *
     * @return the number of bytes per frame, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @see #getSampleSizeInBits()
     * @see AudioSystem#NOT_SPECIFIED
     */
    public int getFrameSize() {
        return frameSize;
    }

    /**
     * Obtains the frame rate in frames per second. When this
     * {@code AudioFormat} is used for queries (e.g.
     * {@link AudioSystem#isConversionSupported(AudioFormat,AudioFormat)
     * AudioSystem.isConversionSupported}) or capabilities (e.g.
     * {@link DataLine.Info#getFormats DataLine.Info.getFormats}), a frame rate
     * of {@code AudioSystem.NOT_SPECIFIED} means that any frame rate is
     * acceptable. {@code AudioSystem.NOT_SPECIFIED} is also returned when the
     * frame rate is not defined for this audio format.
     *
     * @return the number of frames per second, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @see #getSampleRate()
     * @see AudioSystem#NOT_SPECIFIED
     */
    public float getFrameRate() {
        return frameRate;
    }

    /**
     * Indicates whether the audio data is stored in big-endian or little-endian
     * byte order. If the sample size is not more than one byte, the return
     * value is irrelevant.
     *
     * @return {@code true} if the data is stored in big-endian byte order,
     *         {@code false} if little-endian
     */
    public boolean isBigEndian() {
        return bigEndian;
    }

    /**
     * Obtain an unmodifiable map of properties. The concept of properties is
     * further explained in the {@link AudioFileFormat class description}.
     *
     * @return a {@code Map<String, Object>} object containing all properties.
     *         If no properties are recognized, an empty map is returned.
     * @see #getProperty(String)
     * @since 1.5
     */
    @SuppressWarnings("unchecked") // Cast of result of clone.
    public Map<String,Object> properties() {
        Map<String,Object> ret;
        if (properties == null) {
            ret = new HashMap<>(0);
        } else {
            ret = (Map<String,Object>) (properties.clone());
        }
        return Collections.unmodifiableMap(ret);
    }

    /**
     * Obtain the property value specified by the key. The concept of properties
     * is further explained in the {@link AudioFileFormat class description}.
     * <p>
     * If the specified property is not defined for a particular file format,
     * this method returns {@code null}.
     *
     * @param  key the key of the desired property
     * @return the value of the property with the specified key, or {@code null}
     *         if the property does not exist
     * @see #properties()
     * @since 1.5
     */
    public Object getProperty(String key) {
        if (properties == null) {
            return null;
        }
        return properties.get(key);
    }

    /**
     * Indicates whether this format matches the one specified. To match, two
     * formats must have the same encoding, and consistent values of the number
     * of channels, sample rate, sample size, frame rate, and frame size. The
     * values of the property are consistent if they are equal or the specified
     * format has the property value {@code AudioSystem.NOT_SPECIFIED}. The byte
     * order (big-endian or little-endian) must be the same if the sample size
     * is greater than one byte.
     *
     * @param  format format to test for match
     * @return {@code true} if this format matches the one specified,
     *         {@code false} otherwise
     */
    public boolean matches(AudioFormat format) {
        if (format.getEncoding().equals(getEncoding())
                && (format.getChannels() == AudioSystem.NOT_SPECIFIED
                    || format.getChannels() == getChannels())
                && (format.getSampleRate() == (float)AudioSystem.NOT_SPECIFIED
                    || format.getSampleRate() == getSampleRate())
                && (format.getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED
                    || format.getSampleSizeInBits() == getSampleSizeInBits())
                && (format.getFrameRate() == (float)AudioSystem.NOT_SPECIFIED
                    || format.getFrameRate() == getFrameRate())
                && (format.getFrameSize() == AudioSystem.NOT_SPECIFIED
                    || format.getFrameSize() == getFrameSize())
                && (getSampleSizeInBits() <= 8
                    || format.isBigEndian() == isBigEndian())) {
            return true;
        }
        return false;
    }

    /**
     * Returns a string that describes the audio format, such as: "PCM SIGNED
     * 22050 Hz 16 bit mono big-endian". The contents of the string may vary
     * between implementations of Java Sound.
     *
     * @return a string representation of the audio format
     */
    @Override
    public String toString() {
        String sampleRate = getSampleRate() == AudioSystem.NOT_SPECIFIED ?
                "unknown sample rate" : getSampleRate() + " Hz";

        String sampleSize = getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED ?
                "unknown bits per sample" : getSampleSizeInBits() + " bit";

        String channels = switch (getChannels()) {
            case 1 -> "mono";
            case 2 -> "stereo";
            case AudioSystem.NOT_SPECIFIED -> "unknown number of channels";
            default -> getChannels() + " channels";
        };

        String frameSize = getFrameSize() == AudioSystem.NOT_SPECIFIED ?
                "unknown frame size" : getFrameSize() + " bytes/frame";

        String frameRate = "";
        if (Math.abs(getSampleRate() - getFrameRate()) > 0.00001) {
            frameRate = getFrameRate() == AudioSystem.NOT_SPECIFIED ?
                ", unknown frame rate":", " + getFrameRate() + " frames/second";
        }

        String bigEndian = "";
        if ((getEncoding().equals(Encoding.PCM_SIGNED)
             || getEncoding().equals(Encoding.PCM_UNSIGNED))
            && ((getSampleSizeInBits() > 8)
                || (getSampleSizeInBits() == AudioSystem.NOT_SPECIFIED))) {
            bigEndian = isBigEndian() ? ", big-endian" : ", little-endian";
        }

        return String.format("%s %s, %s, %s, %s%s%s", getEncoding(),
                             sampleRate, sampleSize, channels, frameSize,
                             frameRate, bigEndian);
    }

    /**
     * The {@code Encoding} class names the specific type of data representation
     * used for an audio stream. The encoding includes aspects of the sound
     * format other than the number of channels, sample rate, sample size, frame
     * rate, frame size, and byte order.
     * <p>
     * One ubiquitous type of audio encoding is pulse-code modulation (PCM),
     * which is simply a linear (proportional) representation of the sound
     * waveform. With PCM, the number stored in each sample is proportional to
     * the instantaneous amplitude of the sound pressure at that point in time.
     * The numbers may be signed or unsigned integers or floats. Besides PCM,
     * other encodings include mu-law and a-law, which are nonlinear mappings of
     * the sound amplitude that are often used for recording speech.
     * <p>
     * You can use a predefined encoding by referring to one of the static
     * objects created by this class, such as {@code PCM_SIGNED} or
     * {@code PCM_UNSIGNED}. Service providers can create new encodings, such as
     * compressed audio formats, and make these available through the
     * {@link AudioSystem} class.
     * <p>
     * The {@code Encoding} class is static, so that all {@code AudioFormat}
     * objects that have the same encoding will refer to the same object (rather
     * than different instances of the same class). This allows matches to be
     * made by checking that two format's encodings are equal.
     *
     * @author Kara Kytle
     * @see AudioFormat
     * @see javax.sound.sampled.spi.FormatConversionProvider
     * @since 1.3
     */
    public static class Encoding {

        /**
         * Specifies signed, linear PCM data.
         */
        public static final Encoding PCM_SIGNED = new Encoding("PCM_SIGNED");

        /**
         * Specifies unsigned, linear PCM data.
         */
        public static final Encoding PCM_UNSIGNED = new Encoding("PCM_UNSIGNED");

        /**
         * Specifies floating-point PCM data.
         *
         * @since 1.7
         */
        public static final Encoding PCM_FLOAT = new Encoding("PCM_FLOAT");

        /**
         * Specifies u-law encoded data.
         */
        public static final Encoding ULAW = new Encoding("ULAW");

        /**
         * Specifies a-law encoded data.
         */
        public static final Encoding ALAW = new Encoding("ALAW");

        /**
         * Encoding name.
         */
        private final String name;

        /**
         * Constructs a new encoding.
         *
         * @param  name the name of the new type of encoding
         */
        public Encoding(final String name) {
            this.name = name;
        }

        /**
         * Indicates whether the specified object is equal to this encoding,
         * returning {@code true} if the objects are equal.
         *
         * @param  obj the reference object with which to compare
         * @return {@code true} if the specified object is equal to this
         *         encoding; {@code false} otherwise
         */
        @Override
        public final boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Encoding)) {
                return false;
            }
            return Objects.equals(name, ((Encoding) obj).name);
        }

        /**
         * Returns a hash code value for this encoding.
         *
         * @return a hash code value for this encoding
         */
        @Override
        public final int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        /**
         * Returns encoding's name as the string representation of the encoding.
         * For the predefined encodings, the name is similar to the encoding's
         * variable (field) name. For example, {@code PCM_SIGNED.toString()}
         * returns the name "PCM_SIGNED".
         *
         * @return a string representation of the encoding
         */
        @Override
        public final String toString() {
            return name;
        }
    }
}
