/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package javax.sound.midi;

import javax.sound.sampled.AudioInputStream;

/**
 * A {@code SoundbankResource} represents any audio resource stored in a
 * {@link Soundbank}. Common soundbank resources include:
 * <ul>
 *   <li>Instruments. An instrument may be specified in a variety of ways.
 *   However, all soundbanks have some mechanism for defining instruments. In
 *   doing so, they may reference other resources stored in the soundbank.
 *   Each instrument has a {@code Patch} which specifies the MIDI program and
 *   bank by which it may be referenced in MIDI messages. Instrument information
 *   may be stored in {@link Instrument} objects.
 *   <li>Audio samples. A sample typically is a sampled audio waveform which
 *   contains a short sound recording whose duration is a fraction of a
 *   second, or at most a few seconds. These audio samples may be used by a
 *   {@link Synthesizer} to synthesize sound in response to MIDI commands, or
 *   extracted for use by an application. (The terminology reflects musicians'
 *   use of the word "sample" to refer collectively to a series of contiguous
 *   audio samples or frames, rather than to a single, instantaneous sample.)
 *   The data class for an audio sample will be an object that encapsulates
 *   the audio sample data itself and information about how to interpret it
 *   (the format of the audio data), such as an {@link AudioInputStream}.
 *   <li>Embedded sequences. A sound bank may contain built-in song data stored
 *   in a data object such as a {@link Sequence}.
 * </ul>
 * Synthesizers that use wavetable synthesis or related techniques play back the
 * audio in a sample when synthesizing notes, often when emulating the
 * real-world instrument that was originally recorded. However, there is not
 * necessarily a one-to-one correspondence between the {@code Instruments} and
 * samples in a {@code Soundbank}. A single {@code Instrument} can use multiple
 * SoundbankResources (typically for notes of dissimilar pitch or brightness).
 * Also, more than one {@code Instrument} can use the same sample.
 *
 * @author Kara Kytle
 */
public abstract class SoundbankResource {

    /**
     * The sound bank that contains the {@code SoundbankResources}.
     */
    private final Soundbank soundBank;

    /**
     * The name of the {@code SoundbankResource}.
     */
    private final String name;

    /**
     * The class used to represent the sample's data.
     */
    private final Class<?> dataClass;

    /**
     * The wavetable index.
     */
    //private final int index;

    /**
     * Constructs a new {@code SoundbankResource} from the given sound bank and
     * wavetable index. (Setting the {@code SoundbankResource's} name, sampled
     * audio data, and instruments is a subclass responsibility.)
     *
     * @param  soundBank the sound bank containing this
     *         {@code SoundbankResource}
     * @param  name the name of the sample
     * @param  dataClass the class used to represent the sample's data
     * @see #getSoundbank
     * @see #getName
     * @see #getDataClass
     * @see #getData
     */
    protected SoundbankResource(Soundbank soundBank, String name, Class<?> dataClass) {

        this.soundBank = soundBank;
        this.name = name;
        this.dataClass = dataClass;
    }

    /**
     * Obtains the sound bank that contains this {@code SoundbankResource}.
     *
     * @return the sound bank in which this {@code SoundbankResource} is stored
     */
    public Soundbank getSoundbank() {
        return soundBank;
    }

    /**
     * Obtains the name of the resource. This should generally be a string
     * descriptive of the resource.
     *
     * @return the instrument's name
     */
    public String getName() {
        return name;
    }

    /**
     * Obtains the class used by this sample to represent its data. The object
     * returned by {@code getData} will be of this class. If this
     * {@code SoundbankResource} object does not support direct access to its
     * data, returns {@code null}.
     *
     * @return the class used to represent the sample's data, or null if the
     *         data is not accessible
     */
    public Class<?> getDataClass() {
        return dataClass;
    }

    /**
     * Obtains the sampled audio that is stored in this
     * {@code SoundbankResource}. The type of object returned depends on the
     * implementation of the concrete class, and may be queried using
     * {@code getDataClass}.
     *
     * @return an object containing the sampled audio data
     * @see #getDataClass
     */
    public abstract Object getData();

    /**
     * Obtains the index of this {@code SoundbankResource} into the
     * {@code Soundbank's} set of {@code SoundbankResources}.
     *
     * @return the wavetable index
     */
    //public int getIndex() {
    //  return index;
    //}

    /**
     * Obtains a list of the instruments in the sound bank that use the
     * {@code SoundbankResource} for sound synthesis.
     *
     * @return an array of {@code Instruments} that reference this
     *         {@code SoundbankResource}
     * @see Instrument#getSamples
     */
    //public abstract Instrument[] getInstruments();
}
