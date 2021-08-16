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

package javax.sound.sampled.spi;

import java.util.Arrays;

import javax.sound.sampled.Mixer;

/**
 * A provider or factory for a particular mixer type. This mechanism allows the
 * implementation to determine how resources are managed in creation /
 * management of a mixer.
 *
 * @author Kara Kytle
 * @since 1.3
 */
public abstract class MixerProvider {

    /**
     * Constructor for subclasses to call.
     */
    protected MixerProvider() {}

    /**
     * Indicates whether the mixer provider supports the mixer represented by
     * the specified mixer info object.
     * <p>
     * The full set of mixer info objects that represent the mixers supported by
     * this {@code MixerProvider} may be obtained through the
     * {@code getMixerInfo} method.
     *
     * @param  info an info object that describes the mixer for which support is
     *         queried
     * @return {@code true} if the specified mixer is supported, otherwise
     *         {@code false}
     * @throws NullPointerException if {@code info} is {@code null}
     * @see #getMixerInfo()
     */
    public boolean isMixerSupported(final Mixer.Info info) {
        return Arrays.stream(getMixerInfo()).anyMatch(info::equals);
    }

    /**
     * Obtains the set of info objects representing the mixer or mixers provided
     * by this MixerProvider.
     * <p>
     * The {@code isMixerSupported} method returns {@code true} for all the info
     * objects returned by this method. The corresponding mixer instances for
     * the info objects are returned by the {@code getMixer} method.
     *
     * @return a set of mixer info objects
     * @see #getMixer(Mixer.Info)
     * @see #isMixerSupported(Mixer.Info)
     */
    public abstract Mixer.Info[] getMixerInfo();

    /**
     * Obtains an instance of the mixer represented by the info object. If
     * {@code null} is passed, then the default mixer will be returned.
     * <p>
     * The full set of the mixer info objects that represent the mixers
     * supported by this {@code MixerProvider} may be obtained through the
     * {@code getMixerInfo} method. Use the {@code isMixerSupported} method to
     * test whether this {@code MixerProvider} supports a particular mixer.
     *
     * @param  info an info object that describes the desired mixer, or
     *         {@code null} for the default mixer
     * @return mixer instance
     * @throws IllegalArgumentException if the info object specified does not
     *         match the info object for a mixer supported by this
     *         {@code MixerProvider}, or if this {@code MixerProvider} does not
     *         have default mixer, but default mixer has been requested
     * @see #getMixerInfo()
     * @see #isMixerSupported(Mixer.Info)
     */
    public abstract Mixer getMixer(Mixer.Info info);
}
