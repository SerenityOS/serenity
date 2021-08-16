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

/**
 * The {@code ReverbType} class provides methods for accessing various
 * reverberation settings to be applied to an audio signal.
 * <p>
 * Reverberation simulates the reflection of sound off of the walls, ceiling,
 * and floor of a room. Depending on the size of the room, and how absorbent or
 * reflective the materials in the room's surfaces are, the sound might bounce
 * around for a long time before dying away.
 * <p>
 * The reverberation parameters provided by {@code ReverbType} consist of the
 * delay time and intensity of early reflections, the delay time and intensity
 * of late reflections, and an overall decay time. Early reflections are the
 * initial individual low-order reflections of the direct signal off the
 * surfaces in the room. The late reflections are the dense, high-order
 * reflections that characterize the room's reverberation. The delay times for
 * the start of these two reflection types give the listener a sense of the
 * overall size and complexity of the room's shape and contents. The larger the
 * room, the longer the reflection delay times. The early and late reflections'
 * intensities define the gain (in decibels) of the reflected signals as
 * compared to the direct signal. These intensities give the listener an
 * impression of the absorptive nature of the surfaces and objects in the room.
 * The decay time defines how long the reverberation takes to exponentially
 * decay until it is no longer perceptible ("effective zero"). The larger and
 * less absorbent the surfaces, the longer the decay time.
 * <p>
 * The set of parameters defined here may not include all aspects of
 * reverberation as specified by some systems. For example, the Midi
 * Manufacturer's Association (MMA) has an Interactive Audio Special Interest
 * Group (IASIG), which has a 3-D Working Group that has defined a Level 2 Spec
 * (I3DL2). I3DL2 supports filtering of reverberation and control of reverb
 * density. These properties are not included in the JavaSound 1.0 definition of
 * a reverb control. In such a case, the implementing system should either
 * extend the defined reverb control to include additional parameters, or else
 * interpret the system's additional capabilities in a way that fits the model
 * described here.
 * <p>
 * If implementing JavaSound on a I3DL2-compliant device:
 * <ul>
 *   <li>Filtering is disabled (high-frequency attenuations are set to 0.0 dB)
 *   <li>Density parameters are set to midway between minimum and maximum
 * </ul>
 * <p>
 * The following table shows what parameter values an implementation might use
 * for a representative set of reverberation settings.
 *
 * <table class="striped">
 * <caption>Reverb types and params: decay time, late intensity, late delay,
 * early intensity, and early delay</caption>
 * <thead>
 *   <tr>
 *     <th scope="col">Type
 *     <th scope="col">Decay Time (ms)
 *     <th scope="col">Late Intensity (dB)
 *     <th scope="col">Late Delay (ms)
 *     <th scope="col">Early Intensity (dB)
 *     <th scope="col">Early Delay(ms)
 * </thead>
 * <tbody>
 *   <tr>
 *     <th scope="row">Cavern
 *     <td>2250
 *     <td>-2.0
 *     <td>41.3
 *     <td>-1.4
 *     <td>10.3
 *   <tr>
 *     <th scope="row">Dungeon
 *     <td>1600
 *     <td>-1.0
 *     <td>10.3
 *     <td>-0.7
 *     <td>2.6
 *   <tr>
 *     <th scope="row">Garage
 *     <td>900
 *     <td>-6.0
 *     <td>14.7
 *     <td>-4.0
 *     <td>3.9
 *   <tr>
 *     <th scope="row">Acoustic Lab
 *     <td>280
 *     <td>-3.0
 *     <td>8.0
 *     <td>-2.0
 *     <td>2.0
 *   <tr>
 *     <th scope="row">Closet
 *     <td>150
 *     <td>-10.0
 *     <td>2.5
 *     <td>-7.0
 *     <td>0.6
 * </tbody>
 * </table>
 *
 * @author Kara Kytle
 * @since 1.3
 */
public class ReverbType {

    /**
     * Descriptive name of the reverb type.
     */
    private final String name;

    /**
     * Early reflection delay in microseconds.
     */
    private final int earlyReflectionDelay;

    /**
     * Early reflection intensity.
     */
    private final float earlyReflectionIntensity;

    /**
     * Late reflection delay in microseconds.
     */
    private final int lateReflectionDelay;

    /**
     * Late reflection intensity.
     */
    private final float lateReflectionIntensity;

    /**
     * Total decay time.
     */
    private final int decayTime;

    /**
     * Constructs a new reverb type that has the specified reverberation
     * parameter values.
     *
     * @param  name the name of the new reverb type, or a zero-length
     *         {@code String}
     * @param  earlyReflectionDelay the new type's early reflection delay time
     *         in microseconds
     * @param  earlyReflectionIntensity the new type's early reflection
     *         intensity in dB
     * @param  lateReflectionDelay the new type's late reflection delay time in
     *         microseconds
     * @param  lateReflectionIntensity the new type's late reflection intensity
     *         in dB
     * @param  decayTime the new type's decay time in microseconds
     */
    protected ReverbType(String name, int earlyReflectionDelay, float earlyReflectionIntensity, int lateReflectionDelay, float lateReflectionIntensity, int decayTime) {

        this.name = name;
        this.earlyReflectionDelay = earlyReflectionDelay;
        this.earlyReflectionIntensity = earlyReflectionIntensity;
        this.lateReflectionDelay = lateReflectionDelay;
        this.lateReflectionIntensity = lateReflectionIntensity;
        this.decayTime = decayTime;
    }

    /**
     * Obtains the name of this reverb type.
     *
     * @return the name of this reverb type
     * @since 1.5
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the early reflection delay time in microseconds. This is the
     * amount of time between when the direct signal is heard and when the first
     * early reflections are heard.
     *
     * @return early reflection delay time for this reverb type, in microseconds
     */
    public final int getEarlyReflectionDelay() {
        return earlyReflectionDelay;
    }

    /**
     * Returns the early reflection intensity in decibels. This is the amplitude
     * attenuation of the first early reflections relative to the direct signal.
     *
     * @return early reflection intensity for this reverb type, in dB
     */
    public final float getEarlyReflectionIntensity() {
        return earlyReflectionIntensity;
    }

    /**
     * Returns the late reflection delay time in microseconds. This is the
     * amount of time between when the first early reflections are heard and
     * when the first late reflections are heard.
     *
     * @return late reflection delay time for this reverb type, in microseconds
     */
    public final int getLateReflectionDelay() {
        return lateReflectionDelay;
    }

    /**
     * Returns the late reflection intensity in decibels. This is the amplitude
     * attenuation of the first late reflections relative to the direct signal.
     *
     * @return late reflection intensity for this reverb type, in dB
     */
    public final float getLateReflectionIntensity() {
        return lateReflectionIntensity;
    }

    /**
     * Obtains the decay time, which is the amount of time over which the late
     * reflections attenuate to effective zero. The effective zero value is
     * implementation-dependent.
     *
     * @return the decay time of the late reflections, in microseconds
     */
    public final int getDecayTime() {
        return decayTime;
    }

    /**
     * Indicates whether the specified object is equal to this reverb type,
     * returning {@code true} if the objects are the same.
     *
     * @param  obj the reference object with which to compare
     * @return {@code true} if the specified object is equal to this reverb
     *         type; {@code false} otherwise
     */
    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    /**
     * Returns a hash code value for this reverb type.
     *
     * @return a hash code value for this reverb type
     */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * Returns a string representation of the reverb type, including its name
     * and its parameter settings. The exact contents of the string may vary
     * between implementations of Java Sound.
     *
     * @return a string representation of the reverb type
     */
    @Override
    public final String toString() {
        return String.format("%s, early reflection delay %d ns, " +
                             "early reflection intensity %s dB, " +
                             "late deflection delay %d ns, " +
                             "late reflection intensity %s dB, decay time %d",
                             getName(), earlyReflectionDelay,
                             earlyReflectionIntensity,
                             lateReflectionDelay, lateReflectionIntensity,
                             decayTime);
    }
}
