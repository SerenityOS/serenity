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
 * An instance of the {@code AudioFileFormat} class describes an audio file,
 * including the file type, the file's length in bytes, the length in sample
 * frames of the audio data contained in the file, and the format of the audio
 * data.
 * <p>
 * The {@link AudioSystem} class includes methods for determining the format of
 * an audio file, obtaining an audio input stream from an audio file, and
 * writing an audio file from an audio input stream.
 * <p>
 * An {@code AudioFileFormat} object can include a set of properties. A property
 * is a pair of key and value: the key is of type {@code String}, the associated
 * property value is an arbitrary object. Properties specify additional
 * informational meta data (like a author, copyright, or file duration).
 * Properties are optional information, and file reader and file writer
 * implementations are not required to provide or recognize properties.
 * <p>
 * The following table lists some common properties that should be used in
 * implementations:
 *
 * <table class="striped">
 * <caption>Audio File Format Properties</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Property key
 *     <th scope="col">Value type
 *     <th scope="col">Description
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">"duration"
 *     <td>{@link Long Long}
 *     <td>playback duration of the file in microseconds
 *   <tr>
 *     <th scope="row">"author"
 *     <td>{@link String String}
 *     <td>name of the author of this file
 *   <tr>
 *     <th scope="row">"title"
 *     <td>{@link String String}
 *     <td>title of this file
 *   <tr>
 *     <th scope="row">"copyright"
 *     <td>{@link String String}
 *     <td>copyright message
 *   <tr>
 *     <th scope="row">"date"
 *     <td>{@link java.util.Date Date}
 *     <td>date of the recording or release
 *   <tr>
 *     <th scope="row">"comment"
 *     <td>{@link String String}
 *     <td>an arbitrary text
 * </tbody>
 * </table>
 *
 * @author David Rivas
 * @author Kara Kytle
 * @author Florian Bomers
 * @see AudioInputStream
 * @since 1.3
 */
public class AudioFileFormat {

    /**
     * File type.
     */
    private final Type type;

    /**
     * File length in bytes.
     */
    private final int byteLength;

    /**
     * Format of the audio data contained in the file.
     */
    private final AudioFormat format;

    /**
     * Audio data length in sample frames.
     */
    private final int frameLength;

    /**
     * The set of properties.
     */
    private HashMap<String, Object> properties;

    /**
     * Constructs an audio file format object. This protected constructor is
     * intended for use by providers of file-reading services when returning
     * information about an audio file or about supported audio file formats.
     *
     * @param  type the type of the audio file
     * @param  byteLength the length of the file in bytes, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @param  format the format of the audio data contained in the file
     * @param  frameLength the audio data length in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @see #getType
     */
    protected AudioFileFormat(Type type, int byteLength, AudioFormat format, int frameLength) {

        this.type = type;
        this.byteLength = byteLength;
        this.format = format;
        this.frameLength = frameLength;
        this.properties = null;
    }

    /**
     * Constructs an audio file format object. This public constructor may be
     * used by applications to describe the properties of a requested audio
     * file.
     *
     * @param  type the type of the audio file
     * @param  format the format of the audio data contained in the file
     * @param  frameLength the audio data length in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     */
    public AudioFileFormat(Type type, AudioFormat format, int frameLength) {


        this(type,AudioSystem.NOT_SPECIFIED,format,frameLength);
    }

    /**
     * Construct an audio file format object with a set of defined properties.
     * This public constructor may be used by applications to describe the
     * properties of a requested audio file. The properties map will be copied
     * to prevent any changes to it.
     *
     * @param  type the type of the audio file
     * @param  format the format of the audio data contained in the file
     * @param  frameLength the audio data length in sample frames, or
     *         {@code AudioSystem.NOT_SPECIFIED}
     * @param  properties a {@code Map<String, Object>} object with properties
     * @since 1.5
     */
    public AudioFileFormat(Type type, AudioFormat format,
                           int frameLength, Map<String, Object> properties) {
        this(type,AudioSystem.NOT_SPECIFIED,format,frameLength);
        this.properties = new HashMap<>(properties);
    }

    /**
     * Obtains the audio file type, such as {@code WAVE} or {@code AU}.
     *
     * @return the audio file type
     * @see Type#WAVE
     * @see Type#AU
     * @see Type#AIFF
     * @see Type#AIFC
     * @see Type#SND
     */
    public Type getType() {
        return type;
    }

    /**
     * Obtains the size in bytes of the entire audio file (not just its audio
     * data).
     *
     * @return the audio file length in bytes
     * @see AudioSystem#NOT_SPECIFIED
     */
    public int getByteLength() {
        return byteLength;
    }

    /**
     * Obtains the format of the audio data contained in the audio file.
     *
     * @return the audio data format
     */
    public AudioFormat getFormat() {
        return format;
    }

    /**
     * Obtains the length of the audio data contained in the file, expressed in
     * sample frames.
     *
     * @return the number of sample frames of audio data in the file
     * @see AudioSystem#NOT_SPECIFIED
     */
    public int getFrameLength() {
        return frameLength;
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
    @SuppressWarnings("unchecked") // Cast of result of clone
    public Map<String, Object> properties() {
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
     * Returns a string representation of the audio file format.
     *
     * @return a string representation of the audio file format
     */
    @Override
    public String toString() {
        String str = "Unknown file format";
        //$$fb2002-11-01: fix for 4672864: AudioFileFormat.toString() throws unexpected NullPointerException
        if (getType() != null) {
            str = getType() + " (." + getType().getExtension() + ") file";
        }
        if (getByteLength() != AudioSystem.NOT_SPECIFIED) {
            str += ", byte length: " + getByteLength();
        }
        str += ", data format: " + getFormat();
        if (getFrameLength() != AudioSystem.NOT_SPECIFIED) {
            str += ", frame length: " + getFrameLength();
        }
        return str;
    }

    /**
     * An instance of the {@code Type} class represents one of the standard
     * types of audio file. Static instances are provided for the common types.
     */
    public static class Type {

        // FILE FORMAT TYPE DEFINES

        /**
         * Specifies a WAVE file.
         */
        public static final Type WAVE = new Type("WAVE", "wav");

        /**
         * Specifies an AU file.
         */
        public static final Type AU = new Type("AU", "au");

        /**
         * Specifies an AIFF file.
         */
        public static final Type AIFF = new Type("AIFF", "aif");

        /**
         * Specifies an AIFF-C file.
         */
        public static final Type AIFC = new Type("AIFF-C", "aifc");

        /**
         * Specifies a SND file.
         */
        public static final Type SND = new Type("SND", "snd");

        /**
         * File type name.
         */
        private final String name;

        /**
         * File type extension.
         */
        private final String extension;

        /**
         * Constructs a file type.
         *
         * @param  name the string that names the file type
         * @param  extension the string that commonly marks the file type
         *         without leading dot
         */
        public Type(final String name, final String extension) {
            this.name = name;
            this.extension = extension;
        }

        /**
         * Indicates whether the specified object is equal to this file type,
         * returning {@code true} if the objects are equal.
         *
         * @param  obj the reference object with which to compare
         * @return {@code true} if the specified object is equal to this file
         *         type; {@code false} otherwise
         */
        @Override
        public final boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Type)) {
                return false;
            }
            return Objects.equals(name, ((Type) obj).name);
        }

        /**
         * Returns a hash code value for this file type.
         *
         * @return a hash code value for this file type
         */
        @Override
        public final int hashCode() {
            return name != null ? name.hashCode() : 0;
        }

        /**
         * Returns type's name as the string representation of the file type.
         *
         * @return a string representation of the file type
         */
        @Override
        public final String toString() {
            return name;
        }

        /**
         * Obtains the common file name extension for this file type.
         *
         * @return file type extension
         */
        public String getExtension() {
            return extension;
        }
    }
}
