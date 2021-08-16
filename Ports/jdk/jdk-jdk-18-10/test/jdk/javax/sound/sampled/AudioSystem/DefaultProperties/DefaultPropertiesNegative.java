/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.nio.file.Paths;

import com.sun.media.sound.JDK13Services;

/**
 * @test
 * @bug 8201279
 * @run main/othervm/policy=negative.policy DefaultPropertiesNegative
 * @summary this test checks that "javax.sound.config.file" will be ignored if
 *          the user has no access to the properties file
 * @modules java.desktop/com.sun.media.sound
 */
public class DefaultPropertiesNegative {

    private static final Class[] lineTypeClasses = {
        javax.sound.sampled.SourceDataLine.class,
        javax.sound.sampled.TargetDataLine.class,
        javax.sound.sampled.Clip.class,
        javax.sound.sampled.Port.class,
    };

    public static void main(String[] args) throws Exception {
        boolean allOk = true;
        String path = Paths.get(System.getProperty("test.src", "."),
                                "testdata", "conf", "sound.properties")
                           .toAbsolutePath().normalize().toString();
        System.setProperty("javax.sound.config.file", path);

        for (int i = 0; i < lineTypeClasses.length; i++) {
            Class cls = lineTypeClasses[i];
            // properties file, both provider class name and instance name
            String result = JDK13Services.getDefaultProviderClassName(cls);
            if (result != null) {
                out("type " + cls + " failed: provider class should be 'null' "
                            + "but is '" + result + "'!");
                allOk = false;
            }
            result = JDK13Services.getDefaultInstanceName(cls);
            if (result != null) {
                out("type " + cls + " failed: instance name should be 'null' "
                            + "but is '" + result + "'!");
                allOk = false;
            }
        }
        if (! allOk) {
            throw new Exception("Test failed");
        } else {
            out("Test passed");
        }
    }

    private static void out(String message) {
        System.out.println(message);
    }
}
