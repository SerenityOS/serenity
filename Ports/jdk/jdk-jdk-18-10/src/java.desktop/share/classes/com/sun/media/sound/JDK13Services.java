/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import javax.sound.midi.Receiver;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.midi.Transmitter;
import javax.sound.midi.spi.MidiDeviceProvider;
import javax.sound.midi.spi.MidiFileReader;
import javax.sound.midi.spi.MidiFileWriter;
import javax.sound.midi.spi.SoundbankReader;
import javax.sound.sampled.Clip;
import javax.sound.sampled.Port;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.spi.AudioFileReader;
import javax.sound.sampled.spi.AudioFileWriter;
import javax.sound.sampled.spi.FormatConversionProvider;
import javax.sound.sampled.spi.MixerProvider;


/**
 * JDK13Services uses the Service class in JDK 1.3 to discover a list of service
 * providers installed in the system.
 * <p>
 * This class is public because it is called from javax.sound.midi.MidiSystem
 * and javax.sound.sampled.AudioSystem. The alternative would be to make
 * JSSecurityManager public, which is considered worse.
 *
 * @author Matthias Pfisterer
 */
public final class JDK13Services {

    /**
     * Properties loaded from the properties file for default provider
     * properties.
     */
    private static Properties properties;

    /**
     * Private, no-args constructor to ensure against instantiation.
     */
    private JDK13Services() {
    }

    /**
     * Obtains a List containing installed instances of the providers for the
     * requested service. The returned List is immutable.
     *
     * @param serviceClass The type of providers requested. This should be one
     *                     of AudioFileReader.class, AudioFileWriter.class,
     *                     FormatConversionProvider.class, MixerProvider.class,
     *                     MidiDeviceProvider.class, MidiFileReader.class,
     *                     MidiFileWriter.class or SoundbankReader.class.
     *
     * @return A List of providers of the requested type. This List is
     *         immutable.
     */
    public static List<?> getProviders(final Class<?> serviceClass) {
        final List<?> providers;
        if (!MixerProvider.class.equals(serviceClass)
                && !FormatConversionProvider.class.equals(serviceClass)
                && !AudioFileReader.class.equals(serviceClass)
                && !AudioFileWriter.class.equals(serviceClass)
                && !MidiDeviceProvider.class.equals(serviceClass)
                && !SoundbankReader.class.equals(serviceClass)
                && !MidiFileWriter.class.equals(serviceClass)
                && !MidiFileReader.class.equals(serviceClass)) {
            providers = new ArrayList<>(0);
        } else {
            providers = JSSecurityManager.getProviders(serviceClass);
        }
        return Collections.unmodifiableList(providers);
    }

    /** Obtain the provider class name part of a default provider property.
        @param typeClass The type of the default provider property. This
        should be one of Receiver.class, Transmitter.class, Sequencer.class,
        Synthesizer.class, SourceDataLine.class, TargetDataLine.class,
        Clip.class or Port.class.
        @return The value of the provider class name part of the property
        (the part before the hash sign), if available. If the property is
        not set or the value has no provider class name part, null is returned.
     */
    public static synchronized String getDefaultProviderClassName(Class<?> typeClass) {
        String value = null;
        String defaultProviderSpec = getDefaultProvider(typeClass);
        if (defaultProviderSpec != null) {
            int hashpos = defaultProviderSpec.indexOf('#');
            if (hashpos == 0) {
                // instance name only; leave value as null
            } else if (hashpos > 0) {
                value = defaultProviderSpec.substring(0, hashpos);
            } else {
                value = defaultProviderSpec;
            }
        }
        return value;
    }

    /** Obtain the instance name part of a default provider property.
        @param typeClass The type of the default provider property. This
        should be one of Receiver.class, Transmitter.class, Sequencer.class,
        Synthesizer.class, SourceDataLine.class, TargetDataLine.class,
        Clip.class or Port.class.
        @return The value of the instance name part of the property (the
        part after the hash sign), if available. If the property is not set
        or the value has no instance name part, null is returned.
     */
    public static synchronized String getDefaultInstanceName(Class<?> typeClass) {
        String value = null;
        String defaultProviderSpec = getDefaultProvider(typeClass);
        if (defaultProviderSpec != null) {
            int hashpos = defaultProviderSpec.indexOf('#');
            if (hashpos >= 0 && hashpos < defaultProviderSpec.length() - 1) {
                value = defaultProviderSpec.substring(hashpos + 1);
            }
        }
        return value;
    }

    /** Obtain the value of a default provider property.
        @param typeClass The type of the default provider property. This
        should be one of Receiver.class, Transmitter.class, Sequencer.class,
        Synthesizer.class, SourceDataLine.class, TargetDataLine.class,
        Clip.class or Port.class.
        @return The complete value of the property, if available.
        If the property is not set, null is returned.
     */
    private static synchronized String getDefaultProvider(Class<?> typeClass) {
        if (!SourceDataLine.class.equals(typeClass)
                && !TargetDataLine.class.equals(typeClass)
                && !Clip.class.equals(typeClass)
                && !Port.class.equals(typeClass)
                && !Receiver.class.equals(typeClass)
                && !Transmitter.class.equals(typeClass)
                && !Synthesizer.class.equals(typeClass)
                && !Sequencer.class.equals(typeClass)) {
            return null;
        }
        String name = typeClass.getName();
        @SuppressWarnings("removal")
        String value = AccessController.doPrivileged(
                (PrivilegedAction<String>) () -> System.getProperty(name));
        if (value == null) {
            value = getProperties().getProperty(name);
        }
        if ("".equals(value)) {
            value = null;
        }
        return value;
    }

    /** Obtain a properties bundle containing property values from the
        properties file. If the properties file could not be loaded,
        the properties bundle is empty.
    */
    private static synchronized Properties getProperties() {
        if (properties == null) {
            properties = new Properties();
            JSSecurityManager.loadProperties(properties);
        }
        return properties;
    }
}
