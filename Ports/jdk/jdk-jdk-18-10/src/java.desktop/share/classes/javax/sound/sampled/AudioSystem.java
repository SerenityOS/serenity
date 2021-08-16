/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;

import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.spi.AudioFileWriter;
import javax.sound.sampled.spi.FormatConversionProvider;
import javax.sound.sampled.spi.MixerProvider;

import com.sun.media.sound.JDK13Services;

/* $fb TODO:
 * - consistent usage of (typed) collections
 */


/**
 * The {@code AudioSystem} class acts as the entry point to the sampled-audio
 * system resources. This class lets you query and access the mixers that are
 * installed on the system. {@code AudioSystem} includes a number of methods for
 * converting audio data between different formats, and for translating between
 * audio files and streams. It also provides a method for obtaining a
 * {@link Line} directly from the {@code AudioSystem} without dealing explicitly
 * with mixers.
 * <p>
 * Properties can be used to specify the default mixer for specific line types.
 * Both system properties and a properties file are considered. The
 * "sound.properties" properties file is read from an implementation-specific
 * location (typically it is the {@code conf} directory in the Java installation
 * directory). The optional "javax.sound.config.file" system property can be
 * used to specify the properties file that will be read as the initial
 * configuration. If a property exists both as a system property and in the
 * properties file, the system property takes precedence. If none is specified,
 * a suitable default is chosen among the available devices. The syntax of the
 * properties file is specified in
 * {@link Properties#load(InputStream) Properties.load}. The following table
 * lists the available property keys and which methods consider them:
 *
 * <table class="striped">
 * <caption>Audio System Property Keys</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Property Key
 *     <th scope="col">Interface
 *     <th scope="col">Affected Method(s)
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">{@code javax.sound.sampled.Clip}
 *     <td>{@link Clip}
 *     <td>{@link #getLine}, {@link #getClip}
 *   <tr>
 *     <th scope="row">{@code javax.sound.sampled.Port}
 *     <td>{@link Port}
 *     <td>{@link #getLine}
 *   <tr>
 *     <th scope="row">{@code javax.sound.sampled.SourceDataLine}
 *     <td>{@link SourceDataLine}
 *     <td>{@link #getLine}, {@link #getSourceDataLine}
 *   <tr>
 *     <th scope="row">{@code javax.sound.sampled.TargetDataLine}
 *     <td>{@link TargetDataLine}
 *     <td>{@link #getLine}, {@link #getTargetDataLine}
 * </tbody>
 * </table>
 *
 * The property value consists of the provider class name and the mixer name,
 * separated by the hash mark ("#"). The provider class name is the
 * fully-qualified name of a concrete {@link MixerProvider mixer provider}
 * class. The mixer name is matched against the {@code String} returned by the
 * {@code getName} method of {@code Mixer.Info}. Either the class name, or the
 * mixer name may be omitted. If only the class name is specified, the trailing
 * hash mark is optional.
 * <p>
 * If the provider class is specified, and it can be successfully retrieved from
 * the installed providers, the list of {@code Mixer.Info} objects is retrieved
 * from the provider. Otherwise, or when these mixers do not provide a
 * subsequent match, the list is retrieved from {@link #getMixerInfo} to contain
 * all available {@code Mixer.Info} objects.
 * <p>
 * If a mixer name is specified, the resulting list of {@code Mixer.Info}
 * objects is searched: the first one with a matching name, and whose
 * {@code Mixer} provides the respective line interface, will be returned. If no
 * matching {@code Mixer.Info} object is found, or the mixer name is not
 * specified, the first mixer from the resulting list, which provides the
 * respective line interface, will be returned.
 * <p>
 * For example, the property {@code javax.sound.sampled.Clip} with a value
 * {@code "com.sun.media.sound.MixerProvider#SunClip"} will have the following
 * consequences when {@code getLine} is called requesting a {@code Clip}
 * instance: if the class {@code com.sun.media.sound.MixerProvider} exists in
 * the list of installed mixer providers, the first {@code Clip} from the first
 * mixer with name {@code "SunClip"} will be returned. If it cannot be found,
 * the first {@code Clip} from the first mixer of the specified provider will be
 * returned, regardless of name. If there is none, the first {@code Clip} from
 * the first {@code Mixer} with name {@code "SunClip"} in the list of all mixers
 * (as returned by {@code getMixerInfo}) will be returned, or, if not found, the
 * first {@code Clip} of the first {@code Mixer} that can be found in the list
 * of all mixers is returned. If that fails, too, an
 * {@code IllegalArgumentException} is thrown.
 *
 * @author Kara Kytle
 * @author Florian Bomers
 * @author Matthias Pfisterer
 * @author Kevin P. Smith
 * @see AudioFormat
 * @see AudioInputStream
 * @see Mixer
 * @see Line
 * @see Line.Info
 * @since 1.3
 */
public class AudioSystem {

    /**
     * An integer that stands for an unknown numeric value. This value is
     * appropriate only for signed quantities that do not normally take negative
     * values. Examples include file sizes, frame sizes, buffer sizes, and
     * sample rates. A number of Java Sound constructors accept a value of
     * {@code NOT_SPECIFIED} for such parameters. Other methods may also accept
     * or return this value, as documented.
     */
    public static final int NOT_SPECIFIED = -1;

    /**
     * Private no-args constructor for ensuring against instantiation.
     */
    private AudioSystem() {
    }

    /**
     * Obtains an array of mixer info objects that represents the set of audio
     * mixers that are currently installed on the system.
     *
     * @return an array of info objects for the currently installed mixers. If
     *         no mixers are available on the system, an array of length 0 is
     *         returned.
     * @see #getMixer
     */
    public static Mixer.Info[] getMixerInfo() {

        List<Mixer.Info> infos = getMixerInfoList();
        Mixer.Info[] allInfos = infos.toArray(new Mixer.Info[infos.size()]);
        return allInfos;
    }

    /**
     * Obtains the requested audio mixer.
     *
     * @param  info a {@code Mixer.Info} object representing the desired mixer,
     *         or {@code null} for the system default mixer
     * @return the requested mixer
     * @throws SecurityException if the requested mixer is unavailable because
     *         of security restrictions
     * @throws IllegalArgumentException if the info object does not represent a
     *         mixer installed on the system
     * @see #getMixerInfo
     */
    public static Mixer getMixer(final Mixer.Info info) {
        for (final MixerProvider provider : getMixerProviders()) {
            try {
                return provider.getMixer(info);
            } catch (IllegalArgumentException | NullPointerException ignored) {
                // The MixerProvider.getMixer(null) should return default Mixer,
                // This behaviour was assumed from the beginning, but strictly
                // specified only in the jdk9. Since the jdk1.1.5 we skipped
                // NPE for some reason and therefore skipped some
                // implementations of MixerProviders, which throw NPE. To keep
                // support of such implementations, we still ignore NPE.
            }
        }
        throw new IllegalArgumentException(
                String.format("Mixer not supported: %s", info));
    }

    //$$fb 2002-11-26: fix for 4757930: DOC: AudioSystem.getTarget/SourceLineInfo() is ambiguous

    /**
     * Obtains information about all source lines of a particular type that are
     * supported by the installed mixers.
     *
     * @param  info a {@code Line.Info} object that specifies the kind of lines
     *         about which information is requested
     * @return an array of {@code Line.Info} objects describing source lines
     *         matching the type requested. If no matching source lines are
     *         supported, an array of length 0 is returned.
     * @see Mixer#getSourceLineInfo(Line.Info)
     */
    public static Line.Info[] getSourceLineInfo(Line.Info info) {

        Vector<Line.Info> vector = new Vector<>();
        Line.Info[] currentInfoArray;

        Mixer mixer;
        Line.Info fullInfo = null;
        Mixer.Info[] infoArray = getMixerInfo();

        for (int i = 0; i < infoArray.length; i++) {

            mixer = getMixer(infoArray[i]);

            currentInfoArray = mixer.getSourceLineInfo(info);
            for (int j = 0; j < currentInfoArray.length; j++) {
                vector.addElement(currentInfoArray[j]);
            }
        }

        Line.Info[] returnedArray = new Line.Info[vector.size()];

        for (int i = 0; i < returnedArray.length; i++) {
            returnedArray[i] = vector.get(i);
        }

        return returnedArray;
    }

    /**
     * Obtains information about all target lines of a particular type that are
     * supported by the installed mixers.
     *
     * @param  info a {@code Line.Info} object that specifies the kind of lines
     *         about which information is requested
     * @return an array of {@code Line.Info} objects describing target lines
     *         matching the type requested. If no matching target lines are
     *         supported, an array of length 0 is returned.
     * @see Mixer#getTargetLineInfo(Line.Info)
     */
    public static Line.Info[] getTargetLineInfo(Line.Info info) {

        Vector<Line.Info> vector = new Vector<>();
        Line.Info[] currentInfoArray;

        Mixer mixer;
        Line.Info fullInfo = null;
        Mixer.Info[] infoArray = getMixerInfo();

        for (int i = 0; i < infoArray.length; i++) {

            mixer = getMixer(infoArray[i]);

            currentInfoArray = mixer.getTargetLineInfo(info);
            for (int j = 0; j < currentInfoArray.length; j++) {
                vector.addElement(currentInfoArray[j]);
            }
        }

        Line.Info[] returnedArray = new Line.Info[vector.size()];

        for (int i = 0; i < returnedArray.length; i++) {
            returnedArray[i] = vector.get(i);
        }

        return returnedArray;
    }

    /**
     * Indicates whether the system supports any lines that match the specified
     * {@code Line.Info} object. A line is supported if any installed mixer
     * supports it.
     *
     * @param  info a {@code Line.Info} object describing the line for which
     *         support is queried
     * @return {@code true} if at least one matching line is supported,
     *         otherwise {@code false}
     * @see Mixer#isLineSupported(Line.Info)
     */
    public static boolean isLineSupported(Line.Info info) {

        Mixer mixer;
        Mixer.Info[] infoArray = getMixerInfo();

        for (int i = 0; i < infoArray.length; i++) {

            if( infoArray[i] != null ) {
                mixer = getMixer(infoArray[i]);
                if (mixer.isLineSupported(info)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Obtains a line that matches the description in the specified
     * {@code Line.Info} object.
     * <p>
     * If a {@code DataLine} is requested, and {@code info} is an instance of
     * {@code DataLine.Info} specifying at least one fully qualified audio
     * format, the last one will be used as the default format of the returned
     * {@code DataLine}.
     * <p>
     * If system properties
     * {@code javax.sound.sampled.Clip},
     * {@code javax.sound.sampled.Port},
     * {@code javax.sound.sampled.SourceDataLine} and
     * {@code javax.sound.sampled.TargetDataLine} are defined or they are
     * defined in the file "sound.properties", they are used to retrieve default
     * lines. For details, refer to the {@link AudioSystem class description}.
     *
     * If the respective property is not set, or the mixer requested in the
     * property is not installed or does not provide the requested line, all
     * installed mixers are queried for the requested line type. A Line will be
     * returned from the first mixer providing the requested line type.
     *
     * @param  info a {@code Line.Info} object describing the desired kind of
     *         line
     * @return a line of the requested kind
     * @throws LineUnavailableException if a matching line is not available due
     *         to resource restrictions
     * @throws SecurityException if a matching line is not available due to
     *         security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one line matching the specified {@code Line.Info} object through
     *         any installed mixer
     */
    public static Line getLine(Line.Info info) throws LineUnavailableException {
        LineUnavailableException lue = null;
        List<MixerProvider> providers = getMixerProviders();


        // 1: try from default mixer for this line class
        try {
            Mixer mixer = getDefaultMixer(providers, info);
            if (mixer != null && mixer.isLineSupported(info)) {
                return mixer.getLine(info);
            }
        } catch (LineUnavailableException e) {
            lue = e;
        } catch (IllegalArgumentException iae) {
            // must not happen... but better to catch it here,
            // if plug-ins are badly written
        }


        // 2: if that doesn't work, try to find any mixing mixer
        for(int i = 0; i < providers.size(); i++) {
            MixerProvider provider = providers.get(i);
            Mixer.Info[] infos = provider.getMixerInfo();

            for (int j = 0; j < infos.length; j++) {
                try {
                    Mixer mixer = provider.getMixer(infos[j]);
                    // see if this is an appropriate mixer which can mix
                    if (isAppropriateMixer(mixer, info, true)) {
                        return mixer.getLine(info);
                    }
                } catch (LineUnavailableException e) {
                    lue = e;
                } catch (IllegalArgumentException iae) {
                    // must not happen... but better to catch it here,
                    // if plug-ins are badly written
                }
            }
        }


        // 3: if that didn't work, try to find any non-mixing mixer
        for(int i = 0; i < providers.size(); i++) {
            MixerProvider provider = providers.get(i);
            Mixer.Info[] infos = provider.getMixerInfo();
            for (int j = 0; j < infos.length; j++) {
                try {
                    Mixer mixer = provider.getMixer(infos[j]);
                    // see if this is an appropriate mixer which can mix
                    if (isAppropriateMixer(mixer, info, false)) {
                        return mixer.getLine(info);
                    }
                } catch (LineUnavailableException e) {
                    lue = e;
                } catch (IllegalArgumentException iae) {
                    // must not happen... but better to catch it here,
                    // if plug-ins are badly written
                }
            }
        }

        // if this line was supported but was not available, throw the last
        // LineUnavailableException we got (??).
        if (lue != null) {
            throw lue;
        }

        // otherwise, the requested line was not supported, so throw
        // an Illegal argument exception
        throw new IllegalArgumentException("No line matching " +
                                           info.toString() + " is supported.");
    }

    /**
     * Obtains a clip that can be used for playing back an audio file or an
     * audio stream. The returned clip will be provided by the default system
     * mixer, or, if not possible, by any other mixer installed in the system
     * that supports a {@code Clip} object.
     * <p>
     * The returned clip must be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioInputStream)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * If the system property {@code javax.sound.sampled.Clip} is defined or it
     * is defined in the file "sound.properties", it is used to retrieve the
     * default clip. For details, refer to the
     * {@link AudioSystem class description}.
     *
     * @return the desired clip object
     * @throws LineUnavailableException if a clip object is not available due to
     *         resource restrictions
     * @throws SecurityException if a clip object is not available due to
     *         security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one clip instance through any installed mixer
     * @see #getClip(Mixer.Info)
     * @since 1.5
     */
    public static Clip getClip() throws LineUnavailableException{
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                             AudioSystem.NOT_SPECIFIED,
                                             16, 2, 4,
                                             AudioSystem.NOT_SPECIFIED, true);
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        return (Clip) AudioSystem.getLine(info);
    }

    /**
     * Obtains a clip from the specified mixer that can be used for playing back
     * an audio file or an audio stream.
     * <p>
     * The returned clip must be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioInputStream)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     *
     * @param  mixerInfo a {@code Mixer.Info} object representing the desired
     *         mixer, or {@code null} for the system default mixer
     * @return a clip object from the specified mixer
     * @throws LineUnavailableException if a clip is not available from this
     *         mixer due to resource restrictions
     * @throws SecurityException if a clip is not available from this mixer due
     *         to security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one clip through the specified mixer
     * @see #getClip()
     * @since 1.5
     */
    public static Clip getClip(Mixer.Info mixerInfo) throws LineUnavailableException{
        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                                             AudioSystem.NOT_SPECIFIED,
                                             16, 2, 4,
                                             AudioSystem.NOT_SPECIFIED, true);
        DataLine.Info info = new DataLine.Info(Clip.class, format);
        Mixer mixer = AudioSystem.getMixer(mixerInfo);
        return (Clip) mixer.getLine(info);
    }

    /**
     * Obtains a source data line that can be used for playing back audio data
     * in the format specified by the {@code AudioFormat} object. The returned
     * line will be provided by the default system mixer, or, if not possible,
     * by any other mixer installed in the system that supports a matching
     * {@code SourceDataLine} object.
     * <p>
     * The returned line should be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioFormat, int)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * The returned {@code SourceDataLine}'s default audio format will be
     * initialized with {@code format}.
     * <p>
     * If the system property {@code javax.sound.sampled.SourceDataLine} is
     * defined or it is defined in the file "sound.properties", it is used to
     * retrieve the default source data line. For details, refer to the
     * {@link AudioSystem class description}.
     *
     * @param  format an {@code AudioFormat} object specifying the supported
     *         audio format of the returned line, or {@code null} for any audio
     *         format
     * @return the desired {@code SourceDataLine} object
     * @throws LineUnavailableException if a matching source data line is not
     *         available due to resource restrictions
     * @throws SecurityException if a matching source data line is not available
     *         due to security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one source data line supporting the specified audio format
     *         through any installed mixer
     * @see #getSourceDataLine(AudioFormat, Mixer.Info)
     * @since 1.5
     */
    public static SourceDataLine getSourceDataLine(AudioFormat format)
        throws LineUnavailableException{
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        return (SourceDataLine) AudioSystem.getLine(info);
    }

    /**
     * Obtains a source data line that can be used for playing back audio data
     * in the format specified by the {@code AudioFormat} object, provided by
     * the mixer specified by the {@code Mixer.Info} object.
     * <p>
     * The returned line should be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioFormat, int)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * The returned {@code SourceDataLine}'s default audio format will be
     * initialized with {@code format}.
     *
     * @param  format an {@code AudioFormat} object specifying the supported
     *         audio format of the returned line, or {@code null} for any audio
     *         format
     * @param  mixerinfo a {@code Mixer.Info} object representing the desired
     *         mixer, or {@code null} for the system default mixer
     * @return the desired {@code SourceDataLine} object
     * @throws LineUnavailableException if a matching source data line is not
     *         available from the specified mixer due to resource restrictions
     * @throws SecurityException if a matching source data line is not available
     *         from the specified mixer due to security restrictions
     * @throws IllegalArgumentException if the specified mixer does not support
     *         at least one source data line supporting the specified audio
     *         format
     * @see #getSourceDataLine(AudioFormat)
     * @since 1.5
     */
    public static SourceDataLine getSourceDataLine(AudioFormat format,
                                                   Mixer.Info mixerinfo)
        throws LineUnavailableException{
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
        Mixer mixer = AudioSystem.getMixer(mixerinfo);
        return (SourceDataLine) mixer.getLine(info);
    }

    /**
     * Obtains a target data line that can be used for recording audio data in
     * the format specified by the {@code AudioFormat} object. The returned line
     * will be provided by the default system mixer, or, if not possible, by any
     * other mixer installed in the system that supports a matching
     * {@code TargetDataLine} object.
     * <p>
     * The returned line should be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioFormat, int)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * The returned {@code TargetDataLine}'s default audio format will be
     * initialized with {@code format}.
     * <p>
     * If the system property {@code javax.sound.sampled.TargetDataLine} is
     * defined or it is defined in the file "sound.properties", it is used to
     * retrieve the default target data line. For details, refer to the
     * {@link AudioSystem class description}.
     *
     * @param  format an {@code AudioFormat} object specifying the supported
     *         audio format of the returned line, or {@code null} for any audio
     *         format
     * @return the desired {@code TargetDataLine} object
     * @throws LineUnavailableException if a matching target data line is not
     *         available due to resource restrictions
     * @throws SecurityException if a matching target data line is not available
     *         due to security restrictions
     * @throws IllegalArgumentException if the system does not support at least
     *         one target data line supporting the specified audio format
     *         through any installed mixer
     * @see #getTargetDataLine(AudioFormat, Mixer.Info)
     * @see AudioPermission
     * @since 1.5
     */
    public static TargetDataLine getTargetDataLine(AudioFormat format)
        throws LineUnavailableException{

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        return (TargetDataLine) AudioSystem.getLine(info);
    }

    /**
     * Obtains a target data line that can be used for recording audio data in
     * the format specified by the {@code AudioFormat} object, provided by the
     * mixer specified by the {@code Mixer.Info} object.
     * <p>
     * The returned line should be opened with the {@code open(AudioFormat)} or
     * {@code open(AudioFormat, int)} method.
     * <p>
     * This is a high-level method that uses {@code getMixer} and
     * {@code getLine} internally.
     * <p>
     * The returned {@code TargetDataLine}'s default audio format will be
     * initialized with {@code format}.
     *
     * @param  format an {@code AudioFormat} object specifying the supported
     *         audio format of the returned line, or {@code null} for any audio
     *         format
     * @param  mixerinfo a {@code Mixer.Info} object representing the desired
     *         mixer, or {@code null} for the system default mixer
     * @return the desired {@code TargetDataLine} object
     * @throws LineUnavailableException if a matching target data line is not
     *         available from the specified mixer due to resource restrictions
     * @throws SecurityException if a matching target data line is not available
     *         from the specified mixer due to security restrictions
     * @throws IllegalArgumentException if the specified mixer does not support
     *         at least one target data line supporting the specified audio
     *         format
     * @see #getTargetDataLine(AudioFormat)
     * @see AudioPermission
     * @since 1.5
     */
    public static TargetDataLine getTargetDataLine(AudioFormat format,
                                                   Mixer.Info mixerinfo)
        throws LineUnavailableException {

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        Mixer mixer = AudioSystem.getMixer(mixerinfo);
        return (TargetDataLine) mixer.getLine(info);
    }

    // $$fb 2002-04-12: fix for 4662082: behavior of AudioSystem.getTargetEncodings() methods doesn't match the spec

    /**
     * Obtains the encodings that the system can obtain from an audio input
     * stream with the specified encoding using the set of installed format
     * converters.
     *
     * @param  sourceEncoding the encoding for which conversion support is
     *         queried
     * @return array of encodings. If {@code sourceEncoding} is not supported,
     *         an array of length 0 is returned. Otherwise, the array will have
     *         a length of at least 1, representing {@code sourceEncoding}
     *         (no conversion).
     * @throws NullPointerException if {@code sourceEncoding} is {@code null}
     */
    public static AudioFormat.Encoding[] getTargetEncodings(AudioFormat.Encoding sourceEncoding) {
        Objects.requireNonNull(sourceEncoding);

        List<FormatConversionProvider> codecs = getFormatConversionProviders();
        Vector<AudioFormat.Encoding> encodings = new Vector<>();

        AudioFormat.Encoding[] encs = null;

        // gather from all the codecs
        for(int i=0; i<codecs.size(); i++ ) {
            FormatConversionProvider codec = codecs.get(i);
            if( codec.isSourceEncodingSupported( sourceEncoding ) ) {
                encs = codec.getTargetEncodings();
                for (int j = 0; j < encs.length; j++) {
                    encodings.addElement( encs[j] );
                }
            }
        }
        if (!encodings.contains(sourceEncoding)) {
            encodings.addElement(sourceEncoding);
        }

        return encodings.toArray(new AudioFormat.Encoding[encodings.size()]);
    }

    // $$fb 2002-04-12: fix for 4662082: behavior of AudioSystem.getTargetEncodings() methods doesn't match the spec

    /**
     * Obtains the encodings that the system can obtain from an audio input
     * stream with the specified format using the set of installed format
     * converters.
     *
     * @param  sourceFormat the audio format for which conversion is queried
     * @return array of encodings. If {@code sourceFormat}is not supported, an
     *         array of length 0 is returned. Otherwise, the array will have a
     *         length of at least 1, representing the encoding of
     *         {@code sourceFormat} (no conversion).
     * @throws NullPointerException if {@code sourceFormat} is {@code null}
     */
    public static AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        Objects.requireNonNull(sourceFormat);

        List<FormatConversionProvider> codecs = getFormatConversionProviders();
        List<AudioFormat.Encoding> encs = new ArrayList<>();

        // gather from all the codecs
        for (final FormatConversionProvider codec : codecs) {
            Collections.addAll(encs, codec.getTargetEncodings(sourceFormat));
        }

        if (!encs.contains(sourceFormat.getEncoding())) {
            encs.add(sourceFormat.getEncoding());
        }

        return encs.toArray(new AudioFormat.Encoding[encs.size()]);
    }

    /**
     * Indicates whether an audio input stream of the specified encoding can be
     * obtained from an audio input stream that has the specified format.
     *
     * @param  targetEncoding the desired encoding after conversion
     * @param  sourceFormat the audio format before conversion
     * @return {@code true} if the conversion is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code targetEncoding} or
     *         {@code sourceFormat} are {@code null}
     */
    public static boolean isConversionSupported(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        Objects.requireNonNull(targetEncoding);
        Objects.requireNonNull(sourceFormat);
        if (sourceFormat.getEncoding().equals(targetEncoding)) {
            return true;
        }

        List<FormatConversionProvider> codecs = getFormatConversionProviders();

        for(int i=0; i<codecs.size(); i++ ) {
            FormatConversionProvider codec = codecs.get(i);
            if(codec.isConversionSupported(targetEncoding,sourceFormat) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtains an audio input stream of the indicated encoding, by converting
     * the provided audio input stream.
     *
     * @param  targetEncoding the desired encoding after conversion
     * @param  sourceStream the stream to be converted
     * @return an audio input stream of the indicated encoding
     * @throws IllegalArgumentException if the conversion is not supported
     * @throws NullPointerException if {@code targetEncoding} or
     *         {@code sourceStream} are {@code null}
     * @see #getTargetEncodings(AudioFormat.Encoding)
     * @see #getTargetEncodings(AudioFormat)
     * @see #isConversionSupported(AudioFormat.Encoding, AudioFormat)
     * @see #getAudioInputStream(AudioFormat, AudioInputStream)
     */
    public static AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding,
                                                       AudioInputStream sourceStream) {
        Objects.requireNonNull(targetEncoding);
        Objects.requireNonNull(sourceStream);
        if (sourceStream.getFormat().getEncoding().equals(targetEncoding)) {
            return sourceStream;
        }

        List<FormatConversionProvider> codecs = getFormatConversionProviders();

        for(int i = 0; i < codecs.size(); i++) {
            FormatConversionProvider codec = codecs.get(i);
            if( codec.isConversionSupported( targetEncoding, sourceStream.getFormat() ) ) {
                return codec.getAudioInputStream( targetEncoding, sourceStream );
            }
        }
        // we ran out of options, throw an exception
        throw new IllegalArgumentException("Unsupported conversion: " + targetEncoding + " from " + sourceStream.getFormat());
    }

    /**
     * Obtains the formats that have a particular encoding and that the system
     * can obtain from a stream of the specified format using the set of
     * installed format converters.
     *
     * @param  targetEncoding the desired encoding after conversion
     * @param  sourceFormat the audio format before conversion
     * @return array of formats. If no formats of the specified encoding are
     *         supported, an array of length 0 is returned.
     * @throws NullPointerException if {@code targetEncoding} or
     *         {@code sourceFormat} are {@code null}
     */
    public static AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        Objects.requireNonNull(targetEncoding);
        Objects.requireNonNull(sourceFormat);

        List<FormatConversionProvider> codecs = getFormatConversionProviders();
        List<AudioFormat> formats = new ArrayList<>();

        boolean matchFound = false;
        // gather from all the codecs
        for (final FormatConversionProvider codec : codecs) {
            AudioFormat[] elements = codec
                    .getTargetFormats(targetEncoding, sourceFormat);
            for (AudioFormat format : elements) {
                formats.add(format);
                if (sourceFormat.matches(format)) {
                    matchFound = true;
                }
            }
        }

        if (targetEncoding.equals(sourceFormat.getEncoding())) {
            if (!matchFound) {
                formats.add(sourceFormat);
            }
        }
        return formats.toArray(new AudioFormat[formats.size()]);
    }

    /**
     * Indicates whether an audio input stream of a specified format can be
     * obtained from an audio input stream of another specified format.
     *
     * @param  targetFormat the desired audio format after conversion
     * @param  sourceFormat the audio format before conversion
     * @return {@code true} if the conversion is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code targetFormat} or
     *         {@code sourceFormat} are {@code null}
     */
    public static boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        Objects.requireNonNull(targetFormat);
        Objects.requireNonNull(sourceFormat);
        if (sourceFormat.matches(targetFormat)) {
            return true;
        }

        List<FormatConversionProvider> codecs = getFormatConversionProviders();

        for(int i=0; i<codecs.size(); i++ ) {
            FormatConversionProvider codec = codecs.get(i);
            if(codec.isConversionSupported(targetFormat, sourceFormat) ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtains an audio input stream of the indicated format, by converting the
     * provided audio input stream.
     *
     * @param  targetFormat the desired audio format after conversion
     * @param  sourceStream the stream to be converted
     * @return an audio input stream of the indicated format
     * @throws IllegalArgumentException if the conversion is not supported
     * @throws NullPointerException if {@code targetFormat} or
     *         {@code sourceStream} are {@code null}
     * @see #getTargetEncodings(AudioFormat)
     * @see #getTargetFormats(AudioFormat.Encoding, AudioFormat)
     * @see #isConversionSupported(AudioFormat, AudioFormat)
     * @see #getAudioInputStream(AudioFormat.Encoding, AudioInputStream)
     */
    public static AudioInputStream getAudioInputStream(AudioFormat targetFormat,
                                                       AudioInputStream sourceStream) {
        if (sourceStream.getFormat().matches(targetFormat)) {
            return sourceStream;
        }

        List<FormatConversionProvider> codecs = getFormatConversionProviders();

        for(int i = 0; i < codecs.size(); i++) {
            FormatConversionProvider codec = codecs.get(i);
            if(codec.isConversionSupported(targetFormat,sourceStream.getFormat()) ) {
                return codec.getAudioInputStream(targetFormat,sourceStream);
            }
        }

        // we ran out of options...
        throw new IllegalArgumentException("Unsupported conversion: " + targetFormat + " from " + sourceStream.getFormat());
    }

    /**
     * Obtains the audio file format of the provided input stream. The stream
     * must point to valid audio file data. The implementation of this method
     * may require multiple parsers to examine the stream to determine whether
     * they support it. These parsers must be able to mark the stream, read
     * enough data to determine whether they support the stream, and reset the
     * stream's read pointer to its original position. If the input stream does
     * not support these operations, this method may fail with an
     * {@code IOException}.
     *
     * @param  stream the input stream from which file format information should
     *         be extracted
     * @return an {@code AudioFileFormat} object describing the stream's audio
     *         file format
     * @throws UnsupportedAudioFileException if the stream does not point to
     *         valid audio file data recognized by the system
     * @throws IOException if an input/output exception occurs
     * @throws NullPointerException if {@code stream} is {@code null}
     * @see InputStream#markSupported
     * @see InputStream#mark
     */
    public static AudioFileFormat getAudioFileFormat(final InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        Objects.requireNonNull(stream);

        for (final AudioFileReader reader : getAudioFileReaders()) {
            try {
                return reader.getAudioFileFormat(stream);
            } catch (final UnsupportedAudioFileException ignored) {
            }
        }
        throw new UnsupportedAudioFileException("Stream of unsupported format");
    }

    /**
     * Obtains the audio file format of the specified {@code URL}. The
     * {@code URL} must point to valid audio file data.
     *
     * @param  url the {@code URL} from which file format information should be
     *         extracted
     * @return an {@code AudioFileFormat} object describing the audio file
     *         format
     * @throws UnsupportedAudioFileException if the {@code URL} does not point
     *         to valid audio file data recognized by the system
     * @throws IOException if an input/output exception occurs
     * @throws NullPointerException if {@code url} is {@code null}
     */
    public static AudioFileFormat getAudioFileFormat(final URL url)
            throws UnsupportedAudioFileException, IOException {
        Objects.requireNonNull(url);

        for (final AudioFileReader reader : getAudioFileReaders()) {
            try {
                return reader.getAudioFileFormat(url);
            } catch (final UnsupportedAudioFileException ignored) {
            }
        }
        throw new UnsupportedAudioFileException("URL of unsupported format");
    }

    /**
     * Obtains the audio file format of the specified {@code File}. The
     * {@code File} must point to valid audio file data.
     *
     * @param  file the {@code File} from which file format information should
     *         be extracted
     * @return an {@code AudioFileFormat} object describing the audio file
     *         format
     * @throws UnsupportedAudioFileException if the {@code File} does not point
     *         to valid audio file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static AudioFileFormat getAudioFileFormat(final File file)
            throws UnsupportedAudioFileException, IOException {
        Objects.requireNonNull(file);

        for (final AudioFileReader reader : getAudioFileReaders()) {
            try {
                return reader.getAudioFileFormat(file);
            } catch (final UnsupportedAudioFileException ignored) {
            }
        }
        throw new UnsupportedAudioFileException("File of unsupported format");
    }

    /**
     * Obtains an audio input stream from the provided input stream. The stream
     * must point to valid audio file data. The implementation of this method
     * may require multiple parsers to examine the stream to determine whether
     * they support it. These parsers must be able to mark the stream, read
     * enough data to determine whether they support the stream, and reset the
     * stream's read pointer to its original position. If the input stream does
     * not support these operation, this method may fail with an
     * {@code IOException}.
     *
     * @param  stream the input stream from which the {@code AudioInputStream}
     *         should be constructed
     * @return an {@code AudioInputStream} object based on the audio file data
     *         contained in the input stream
     * @throws UnsupportedAudioFileException if the stream does not point to
     *         valid audio file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws NullPointerException if {@code stream} is {@code null}
     * @see InputStream#markSupported
     * @see InputStream#mark
     */
    public static AudioInputStream getAudioInputStream(final InputStream stream)
            throws UnsupportedAudioFileException, IOException {
        Objects.requireNonNull(stream);

        for (final AudioFileReader reader : getAudioFileReaders()) {
            try {
                return reader.getAudioInputStream(stream);
            } catch (final UnsupportedAudioFileException ignored) {
            }
        }
        throw new UnsupportedAudioFileException("Stream of unsupported format");
    }

    /**
     * Obtains an audio input stream from the {@code URL} provided. The
     * {@code URL} must point to valid audio file data.
     *
     * @param  url the {@code URL} for which the {@code AudioInputStream} should
     *         be constructed
     * @return an {@code AudioInputStream} object based on the audio file data
     *         pointed to by the {@code URL}
     * @throws UnsupportedAudioFileException if the {@code URL} does not point
     *         to valid audio file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws NullPointerException if {@code url} is {@code null}
     */
    public static AudioInputStream getAudioInputStream(final URL url)
            throws UnsupportedAudioFileException, IOException {
        Objects.requireNonNull(url);

        for (final AudioFileReader reader : getAudioFileReaders()) {
            try {
                return reader.getAudioInputStream(url);
            } catch (final UnsupportedAudioFileException ignored) {
            }
        }
        throw new UnsupportedAudioFileException("URL of unsupported format");
    }

    /**
     * Obtains an audio input stream from the provided {@code File}. The
     * {@code File} must point to valid audio file data.
     *
     * @param  file the {@code File} for which the {@code AudioInputStream}
     *         should be constructed
     * @return an {@code AudioInputStream} object based on the audio file data
     *         pointed to by the {@code File}
     * @throws UnsupportedAudioFileException if the {@code File} does not point
     *         to valid audio file data recognized by the system
     * @throws IOException if an I/O exception occurs
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public static AudioInputStream getAudioInputStream(final File file)
            throws UnsupportedAudioFileException, IOException {
        Objects.requireNonNull(file);

        for (final AudioFileReader reader : getAudioFileReaders()) {
            try {
                return reader.getAudioInputStream(file);
            } catch (final UnsupportedAudioFileException ignored) {
            }
        }
        throw new UnsupportedAudioFileException("File of unsupported format");
    }

    /**
     * Obtains the file types for which file writing support is provided by the
     * system.
     *
     * @return array of unique file types. If no file types are supported, an
     *         array of length 0 is returned.
     */
    public static AudioFileFormat.Type[] getAudioFileTypes() {
        List<AudioFileWriter> providers = getAudioFileWriters();
        Set<AudioFileFormat.Type> returnTypesSet = new HashSet<>();

        for(int i=0; i < providers.size(); i++) {
            AudioFileWriter writer = providers.get(i);
            AudioFileFormat.Type[] fileTypes = writer.getAudioFileTypes();
            for(int j=0; j < fileTypes.length; j++) {
                returnTypesSet.add(fileTypes[j]);
            }
        }
        AudioFileFormat.Type[] returnTypes =
            returnTypesSet.toArray(new AudioFileFormat.Type[0]);
        return returnTypes;
    }

    /**
     * Indicates whether file writing support for the specified file type is
     * provided by the system.
     *
     * @param  fileType the file type for which write capabilities are queried
     * @return {@code true} if the file type is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code fileType} is {@code null}
     */
    public static boolean isFileTypeSupported(AudioFileFormat.Type fileType) {
        Objects.requireNonNull(fileType);
        List<AudioFileWriter> providers = getAudioFileWriters();

        for(int i=0; i < providers.size(); i++) {
            AudioFileWriter writer = providers.get(i);
            if (writer.isFileTypeSupported(fileType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Obtains the file types that the system can write from the audio input
     * stream specified.
     *
     * @param  stream the audio input stream for which audio file type support
     *         is queried
     * @return array of file types. If no file types are supported, an array of
     *         length 0 is returned.
     * @throws NullPointerException if {@code stream} is {@code null}
     */
    public static AudioFileFormat.Type[] getAudioFileTypes(AudioInputStream stream) {
        Objects.requireNonNull(stream);
        List<AudioFileWriter> providers = getAudioFileWriters();
        Set<AudioFileFormat.Type> returnTypesSet = new HashSet<>();

        for(int i=0; i < providers.size(); i++) {
            AudioFileWriter writer = providers.get(i);
            AudioFileFormat.Type[] fileTypes = writer.getAudioFileTypes(stream);
            for(int j=0; j < fileTypes.length; j++) {
                returnTypesSet.add(fileTypes[j]);
            }
        }
        AudioFileFormat.Type[] returnTypes =
            returnTypesSet.toArray(new AudioFileFormat.Type[0]);
        return returnTypes;
    }

    /**
     * Indicates whether an audio file of the specified file type can be written
     * from the indicated audio input stream.
     *
     * @param  fileType the file type for which write capabilities are queried
     * @param  stream the stream for which file-writing support is queried
     * @return {@code true} if the file type is supported for this audio input
     *         stream, otherwise {@code false}
     * @throws NullPointerException if {@code fileType} or {@code stream} are
     *         {@code null}
     */
    public static boolean isFileTypeSupported(AudioFileFormat.Type fileType,
                                              AudioInputStream stream) {
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(stream);
        List<AudioFileWriter> providers = getAudioFileWriters();

        for(int i=0; i < providers.size(); i++) {
            AudioFileWriter writer = providers.get(i);
            if(writer.isFileTypeSupported(fileType, stream)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Writes a stream of bytes representing an audio file of the specified file
     * type to the output stream provided. Some file types require that the
     * length be written into the file header; such files cannot be written from
     * start to finish unless the length is known in advance. An attempt to
     * write a file of such a type will fail with an {@code IOException} if the
     * length in the audio file type is {@code AudioSystem.NOT_SPECIFIED}.
     *
     * @param  stream the audio input stream containing audio data to be written
     *         to the file
     * @param  fileType the kind of audio file to write
     * @param  out the stream to which the file data should be written
     * @return the number of bytes written to the output stream
     * @throws IOException if an input/output exception occurs
     * @throws IllegalArgumentException if the file type is not supported by the
     *         system
     * @throws NullPointerException if {@code stream} or {@code fileType} or
     *         {@code out} are {@code null}
     * @see #isFileTypeSupported
     * @see #getAudioFileTypes
     */
    public static int write(final AudioInputStream stream,
                            final AudioFileFormat.Type fileType,
                            final OutputStream out) throws IOException {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(out);

        for (final AudioFileWriter writer : getAudioFileWriters()) {
            try {
                return writer.write(stream, fileType, out);
            } catch (final IllegalArgumentException ignored) {
                // thrown if this provider cannot write the stream, try next
            }
        }
        // "File type " + type + " not supported."
        throw new IllegalArgumentException(
                "could not write audio file: file type not supported: "
                        + fileType);
    }

    /**
     * Writes a stream of bytes representing an audio file of the specified file
     * type to the external file provided.
     *
     * @param  stream the audio input stream containing audio data to be written
     *         to the file
     * @param  fileType the kind of audio file to write
     * @param  out the external file to which the file data should be written
     * @return the number of bytes written to the file
     * @throws IOException if an I/O exception occurs
     * @throws IllegalArgumentException if the file type is not supported by the
     *         system
     * @throws NullPointerException if {@code stream} or {@code fileType} or
     *         {@code out} are {@code null}
     * @see #isFileTypeSupported
     * @see #getAudioFileTypes
     */
    public static int write(final AudioInputStream stream,
                            final AudioFileFormat.Type fileType,
                            final File out) throws IOException {
        Objects.requireNonNull(stream);
        Objects.requireNonNull(fileType);
        Objects.requireNonNull(out);

        for (final AudioFileWriter writer : getAudioFileWriters()) {
            try {
                return writer.write(stream, fileType, out);
            } catch (final IllegalArgumentException ignored) {
                // thrown if this provider cannot write the stream, try next
            }
        }
        throw new IllegalArgumentException(
                "could not write audio file: file type not supported: "
                        + fileType);
    }

    // METHODS FOR INTERNAL IMPLEMENTATION USE

    /**
     * Obtains the list of MixerProviders currently installed on the system.
     *
     * @return the list of MixerProviders currently installed on the system
     */
    @SuppressWarnings("unchecked")
    private static List<MixerProvider> getMixerProviders() {
        return (List<MixerProvider>) getProviders(MixerProvider.class);
    }

    /**
     * Obtains the set of format converters (codecs, transcoders, etc.) that are
     * currently installed on the system.
     *
     * @return an array of {@link FormatConversionProvider} objects representing
     *         the available format converters. If no format converters readers
     *         are available on the system, an array of length 0 is returned.
     */
    @SuppressWarnings("unchecked")
    private static List<FormatConversionProvider> getFormatConversionProviders() {
        return (List<FormatConversionProvider>) getProviders(FormatConversionProvider.class);
    }

    /**
     * Obtains the set of audio file readers that are currently installed on the
     * system.
     *
     * @return a List of {@link AudioFileReader} objects representing the
     *         installed audio file readers. If no audio file readers are
     *         available on the system, an empty List is returned.
     */
    @SuppressWarnings("unchecked")
    private static List<AudioFileReader> getAudioFileReaders() {
        return (List<AudioFileReader>)getProviders(AudioFileReader.class);
    }

    /**
     * Obtains the set of audio file writers that are currently installed on the
     * system.
     *
     * @return a List of {@link AudioFileWriter} objects representing the
     *         available audio file writers. If no audio file writers are
     *         available on the system, an empty List is returned.
     */
    @SuppressWarnings("unchecked")
    private static List<AudioFileWriter> getAudioFileWriters() {
        return (List<AudioFileWriter>)getProviders(AudioFileWriter.class);
    }

    /**
     * Attempts to locate and return a default Mixer that provides lines of the
     * specified type.
     *
     * @param  providers the installed mixer providers
     * @param  info The requested line type TargetDataLine.class, Clip.class or
     *         Port.class
     * @return a Mixer that matches the requirements, or null if no default
     *         mixer found
     */
    private static Mixer getDefaultMixer(List<MixerProvider> providers, Line.Info info) {
        Class<?> lineClass = info.getLineClass();
        String providerClassName = JDK13Services.getDefaultProviderClassName(lineClass);
        String instanceName = JDK13Services.getDefaultInstanceName(lineClass);
        Mixer mixer;

        if (providerClassName != null) {
            MixerProvider defaultProvider = getNamedProvider(providerClassName, providers);
            if (defaultProvider != null) {
                if (instanceName != null) {
                    mixer = getNamedMixer(instanceName, defaultProvider, info);
                    if (mixer != null) {
                        return mixer;
                    }
                } else {
                    mixer = getFirstMixer(defaultProvider, info,
                                          false /* mixing not required*/);
                    if (mixer != null) {
                        return mixer;
                    }
                }

            }
        }

        /*
         *  - Provider class not specified, or
         *  - provider class cannot be found, or
         *  - provider class and instance specified and instance cannot be found
         *    or is not appropriate
         */
        if (instanceName != null) {
            mixer = getNamedMixer(instanceName, providers, info);
            if (mixer != null) {
                return mixer;
            }
        }


        /*
         * No defaults are specified, or if something is specified, everything
         * failed
         */
        return null;
    }

    /**
     * Return a MixerProvider of a given class from the list of MixerProviders.
     * This method never requires the returned Mixer to do mixing.
     *
     * @param  providerClassName The class name of the provider to be returned
     * @param  providers The list of MixerProviders that is searched
     * @return A MixerProvider of the requested class, or null if none is found
     */
    private static MixerProvider getNamedProvider(String providerClassName,
                                                  List<MixerProvider> providers) {
        for(int i = 0; i < providers.size(); i++) {
            MixerProvider provider = providers.get(i);
            if (provider.getClass().getName().equals(providerClassName)) {
                return provider;
            }
        }
        return null;
    }

    /**
     * Return a Mixer with a given name from a given MixerProvider. This method
     * never requires the returned Mixer to do mixing.
     *
     * @param  mixerName The name of the Mixer to be returned
     * @param  provider The MixerProvider to check for Mixers
     * @param  info The type of line the returned Mixer is required to support
     * @return A Mixer matching the requirements, or null if none is found
     */
    private static Mixer getNamedMixer(String mixerName,
                                       MixerProvider provider,
                                       Line.Info info) {
        Mixer.Info[] infos = provider.getMixerInfo();
        for (int i = 0; i < infos.length; i++) {
            if (infos[i].getName().equals(mixerName)) {
                Mixer mixer = provider.getMixer(infos[i]);
                if (isAppropriateMixer(mixer, info, false)) {
                    return mixer;
                }
            }
        }
        return null;
    }

    /**
     * From a List of MixerProviders, return a Mixer with a given name. This
     * method never requires the returned Mixer to do mixing.
     *
     * @param  mixerName The name of the Mixer to be returned
     * @param  providers The List of MixerProviders to check for Mixers
     * @param  info The type of line the returned Mixer is required to support
     * @return A Mixer matching the requirements, or null if none is found
     */
    private static Mixer getNamedMixer(String mixerName,
                                       List<MixerProvider> providers,
                                       Line.Info info) {
        for(int i = 0; i < providers.size(); i++) {
            MixerProvider provider = providers.get(i);
            Mixer mixer = getNamedMixer(mixerName, provider, info);
            if (mixer != null) {
                return mixer;
            }
        }
        return null;
    }

    /**
     * From a given MixerProvider, return the first appropriate Mixer.
     *
     * @param  provider The MixerProvider to check for Mixers
     * @param  info The type of line the returned Mixer is required to support
     * @param  isMixingRequired If true, only Mixers that support mixing are
     *         returned for line types of SourceDataLine and Clip
     * @return A Mixer that is considered appropriate, or null if none is found
     */
    private static Mixer getFirstMixer(MixerProvider provider,
                                       Line.Info info,
                                       boolean isMixingRequired) {
        Mixer.Info[] infos = provider.getMixerInfo();
        for (int j = 0; j < infos.length; j++) {
            Mixer mixer = provider.getMixer(infos[j]);
            if (isAppropriateMixer(mixer, info, isMixingRequired)) {
                return mixer;
            }
        }
        return null;
    }

    /**
     * Checks if a Mixer is appropriate. A Mixer is considered appropriate if it
     * support the given line type. If isMixingRequired is {@code true} and the
     * line type is an output one (SourceDataLine, Clip), the mixer is
     * appropriate if it supports at least 2 (concurrent) lines of the given
     * type.
     *
     * @param  mixer The mixer to check
     * @param  lineInfo The line to check
     * @param  isMixingRequired Is the mixing required or not
     * @return {@code true} if the mixer is considered appropriate according to
     *         the rules given above, {@code false} otherwise
     */
    private static boolean isAppropriateMixer(Mixer mixer,
                                              Line.Info lineInfo,
                                              boolean isMixingRequired) {
        if (! mixer.isLineSupported(lineInfo)) {
            return false;
        }
        Class<?> lineClass = lineInfo.getLineClass();
        if (isMixingRequired
            && (SourceDataLine.class.isAssignableFrom(lineClass) ||
                Clip.class.isAssignableFrom(lineClass))) {
            int maxLines = mixer.getMaxLines(lineInfo);
            return ((maxLines == NOT_SPECIFIED) || (maxLines > 1));
        }
        return true;
    }

    /**
     * Like getMixerInfo, but return List.
     *
     * @return a List of info objects for the currently installed mixers. If no
     *         mixers are available on the system, an empty List is returned.
     * @see #getMixerInfo()
     */
    private static List<Mixer.Info> getMixerInfoList() {
        List<MixerProvider> providers = getMixerProviders();
        return getMixerInfoList(providers);
    }

    /**
     * Like getMixerInfo, but return List.
     *
     * @param  providers The list of MixerProviders
     * @return a List of info objects for the currently installed mixers. If no
     *         mixers are available on the system, an empty List is returned.
     * @see #getMixerInfo()
     */
    private static List<Mixer.Info> getMixerInfoList(List<MixerProvider> providers) {
        List<Mixer.Info> infos = new ArrayList<>();

        Mixer.Info[] someInfos; // per-mixer
        Mixer.Info[] allInfos;  // for all mixers

        for(int i = 0; i < providers.size(); i++ ) {
            someInfos = providers.get(i).getMixerInfo();

            for (int j = 0; j < someInfos.length; j++) {
                infos.add(someInfos[j]);
            }
        }

        return infos;
    }

    /**
     * Obtains the set of services currently installed on the system using the
     * SPI mechanism in 1.3.
     *
     * @param  providerClass The type of providers requested. This should be one
     *         of AudioFileReader.class, AudioFileWriter.class,
     *         FormatConversionProvider.class, MixerProvider.class,
     *         MidiDeviceProvider.class, MidiFileReader.class,
     *         MidiFileWriter.class or SoundbankReader.class.
     * @return a List of instances of providers for the requested service. If no
     *         providers are available, a vector of length 0 will be returned.
     */
    private static List<?> getProviders(Class<?> providerClass) {
        return JDK13Services.getProviders(providerClass);
    }
}
