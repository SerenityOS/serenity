/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A {@code VoiceStatus} object contains information about the current status of
 * one of the voices produced by a {@link Synthesizer}.
 * <p>
 * MIDI synthesizers are generally capable of producing some maximum number of
 * simultaneous notes, also referred to as voices. A voice is a stream of
 * successive single notes, and the process of assigning incoming MIDI notes to
 * specific voices is known as voice allocation. However, the voice-allocation
 * algorithm and the contents of each voice are normally internal to a MIDI
 * synthesizer and hidden from outside view. One can, of course, learn from MIDI
 * messages which notes the synthesizer is playing, and one might be able deduce
 * something about the assignment of notes to voices. But MIDI itself does not
 * provide a means to report which notes a synthesizer has assigned to which
 * voice, nor even to report how many voices the synthesizer is capable of
 * synthesizing.
 * <p>
 * In Java Sound, however, a {@code Synthesizer} class can expose the contents
 * of its voices through its
 * {@link Synthesizer#getVoiceStatus() getVoiceStatus()} method. This behavior
 * is recommended but optional; synthesizers that don't expose their voice
 * allocation simply return a zero-length array. A {@code Synthesizer} that does
 * report its voice status should maintain this information at all times for all
 * of its voices, whether they are currently sounding or not. In other words, a
 * given type of {@code Synthesizer} always has a fixed number of voices, equal
 * to the maximum number of simultaneous notes it is capable of sounding.
 * <p>
 * <a id="description_of_active"></a>If the voice is not currently processing a
 * MIDI note, it is considered inactive. A voice is inactive when it has been
 * given no note-on commands, or when every note-on command received has been
 * terminated by a corresponding note-off (or by an "all notes off" message).
 * For example, this happens when a synthesizer capable of playing 16
 * simultaneous notes is told to play a four-note chord; only four voices are
 * active in this case (assuming no earlier notes are still playing). Usually, a
 * voice whose status is reported as active is producing audible sound, but this
 * is not always true; it depends on the details of the instrument (that is, the
 * synthesis algorithm) and how long the note has been going on. For example, a
 * voice may be synthesizing the sound of a single hand-clap. Because this sound
 * dies away so quickly, it may become inaudible before a note-off message is
 * received. In such a situation, the voice is still considered active even
 * though no sound is currently being produced.
 * <p>
 * Besides its active or inactive status, the {@code VoiceStatus} class provides
 * fields that reveal the voice's current MIDI channel, bank and program number,
 * MIDI note number, and MIDI volume. All of these can change during the course
 * of a voice. While the voice is inactive, each of these fields has an
 * unspecified value, so you should check the active field first.
 *
 * @author David Rivas
 * @author Kara Kytle
 * @see Synthesizer#getMaxPolyphony
 * @see Synthesizer#getVoiceStatus
 */
public class VoiceStatus {

    /**
     * Indicates whether the voice is currently processing a MIDI note. See the
     * explanation of
     * <a HREF="#description_of_active">active and inactive voices</a>.
     */
    public boolean active = false;

    /**
     * The MIDI channel on which this voice is playing. The value is a
     * zero-based channel number if the voice is active, or unspecified if the
     * voice is inactive.
     *
     * @see MidiChannel
     * @see #active
     */
    public int channel = 0;

    /**
     * The bank number of the instrument that this voice is currently using.
     * This is a number dictated by the MIDI bank-select message; it does not
     * refer to a {@code SoundBank} object. The value ranges from 0 to 16383 if
     * the voice is active, and is unspecified if the voice is inactive.
     *
     * @see Patch
     * @see Soundbank
     * @see #active
     * @see MidiChannel#programChange(int, int)
     */
    public int bank = 0;

    /**
     * The program number of the instrument that this voice is currently using.
     * The value ranges from 0 to 127 if the voice is active, and is unspecified
     * if the voice is inactive.
     *
     * @see MidiChannel#getProgram
     * @see Patch
     * @see #active
     */
    public int program = 0;

    /**
     * The MIDI note that this voice is playing. The range for an active voice
     * is from 0 to 127 in semitones, with 60 referring to Middle C. The value
     * is unspecified if the voice is inactive.
     *
     * @see MidiChannel#noteOn
     * @see #active
     */
    public int note = 0;

    /**
     * The current MIDI volume level for the voice. The value ranges from 0 to
     * 127 if the voice is active, and is unspecified if the voice is inactive.
     * <p>
     * Note that this value does not necessarily reflect the instantaneous level
     * of the sound produced by this voice; that level is the result of many
     * contributing factors, including the current instrument and the shape of
     * the amplitude envelope it produces.
     *
     * @see #active
     */
    public int volume = 0;

    /**
     * Constructs a {@code VoiceStatus}.
     */
    public VoiceStatus() {}
}
