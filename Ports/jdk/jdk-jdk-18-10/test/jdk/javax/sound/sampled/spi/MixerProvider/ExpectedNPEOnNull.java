/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.spi.MixerProvider;

import static java.util.ServiceLoader.load;

/**
 * @test
 * @bug 8135100
 * @author Sergey Bylokhov
 */
public final class ExpectedNPEOnNull {

    public static void main(final String[] args) throws Exception {
        testAS();
        for (final MixerProvider mp : load(MixerProvider.class)) {
            testMP(mp);
        }
        testMP(customMP);
    }

    /**
     * Tests the part of AudioSystem API, which implemented via MixerProvider.
     */
    private static void testAS() {
        try {
            AudioSystem.getMixer(null); // null should be accepted
        } catch (final SecurityException | IllegalArgumentException ignored) {
            // skip the specified exceptions only
        }
    }

    /**
     * Tests the MixerProvider API directly.
     */
    private static void testMP(MixerProvider mp) {
        try {
            mp.isMixerSupported(null);
            throw new RuntimeException("NPE is expected: " + mp);
        } catch (final NullPointerException ignored) {

        }
        try {
            mp.getMixer(null); // null should be accepted
        } catch (SecurityException | IllegalArgumentException e) {
            // skip the specified exceptions only
        }
    }

    /**
     * Tests some default implementation of MixerProvider API, using the
     * custom {@code MixerProvider}, which support nothing.
     */
    static final MixerProvider customMP = new MixerProvider() {
        @Override
        public Mixer.Info[] getMixerInfo() {
            return new Mixer.Info[0];
        }

        @Override
        public Mixer getMixer(Mixer.Info info) {
            return null;
        }
    };
}
