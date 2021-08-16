/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import com.sun.media.sound.JDK13Services;

/**
 * @test
 * @bug 4776511
 * @summary RFE: Setting the default MixerProvider. Test the cacheing of
 *          providers.
 * @modules java.desktop/com.sun.media.sound
 */
public class ProviderCacheing {

    private static final Class[] providerClasses = {
        javax.sound.sampled.spi.AudioFileReader.class,
        javax.sound.sampled.spi.AudioFileWriter.class,
        javax.sound.sampled.spi.FormatConversionProvider.class,
        javax.sound.sampled.spi.MixerProvider.class,
    };

    public static void main(String[] args) throws Exception {
        boolean allCached = true;
        for (int i = 0; i < providerClasses.length; i++) {
            List list0 = JDK13Services.getProviders(providerClasses[i]);
            List list1 = JDK13Services.getProviders(providerClasses[i]);
            if (list0 == list1) {
                out("Providers should not be cached for " + providerClasses[i]);
                allCached = false;
            }
        }

        if (! allCached) {
            throw new Exception("Test failed");
        } else {
            out("Test passed");
        }
    }

    private static void out(String message) {
        System.out.println(message);
    }
}
