/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4776511 8201279
 * @summary RFE: Setting the default MixerProvider. Test the retrieving and
 *          parsing of properties. This is a part of the test for 4776511.
 * @run main/othervm DefaultProperties
 * @run main/othervm/policy=java.policy DefaultProperties
 * @modules java.desktop/com.sun.media.sound
 */
public class DefaultProperties {

    private static final Class[] lineTypeClasses = {
        javax.sound.midi.Receiver.class,
        javax.sound.midi.Transmitter.class,
        javax.sound.midi.Sequencer.class,
        javax.sound.midi.Synthesizer.class,
    };

    public static void main(String[] args) throws Exception {
        boolean allOk = true;
        String path = Paths.get(System.getProperty("test.src", "."),
                                "testdata", "conf", "sound.properties")
                           .toAbsolutePath().normalize().toString();
        System.setProperty("javax.sound.config.file", path);

        for (int i = 0; i < lineTypeClasses.length; i++) {
            Class cls = lineTypeClasses[i];
            String propertyName = cls.getName();
            String result;
            String provClassName;
            String instanceName;

            // properties file, both provider class name and instance name
            provClassName = "xyz";
            instanceName = "123";
            result = JDK13Services.getDefaultProviderClassName(cls);
            if (! provClassName.equals(result)) {
                out("type " + cls + " failed: provider class should be '" +
                    provClassName + "' but is '" + result + "'!");
                allOk = false;
            }
            result = JDK13Services.getDefaultInstanceName(cls);
            if (! instanceName.equals(result)) {
                out("type " + cls + " failed: instance name should be '" +
                    instanceName + "' but is '" + result + "'!");
                allOk = false;
            }

            // system property, provider class name only, no trailing hash
            provClassName = "abc";
            System.setProperty(propertyName, provClassName);
            result = JDK13Services.getDefaultProviderClassName(cls);
            if (! provClassName.equals(result)) {
                out("type " + cls + " failed: provider class should be '" +
                    provClassName + "' but is '" + result + "'!");
                allOk = false;
            }
            result = JDK13Services.getDefaultInstanceName(cls);
            if (result != null) {
                out("type " + cls + " failed: instance name should be " +
                    "null but is '" + result + "'!");
                allOk = false;
            }

            // system property, provider class name only, trailing hash
            provClassName = "def";
            System.setProperty(propertyName, provClassName + "#");
            result = JDK13Services.getDefaultProviderClassName(cls);
            if (! provClassName.equals(result)) {
                out("type " + cls + " failed: provider class should be '" +
                    provClassName + "' but is '" + result + "'!");
                allOk = false;
            }
            result = JDK13Services.getDefaultInstanceName(cls);
            if (result != null) {
                out("type " + cls + " failed: instance name should be " +
                    "null but is '" + result + "'!");
                allOk = false;
            }

            // system property, instance name only
            instanceName = "ghi";
            System.setProperty(propertyName, "#" + instanceName);
            result = JDK13Services.getDefaultProviderClassName(cls);
            if (result != null) {
                out("type " + cls + " failed: provider class should be " +
                    "null but is '" + result + "'!");
                allOk = false;
            }
            result = JDK13Services.getDefaultInstanceName(cls);
            if (! instanceName.equals(result)) {
                out("type " + cls + " failed: instance name should be '" +
                    instanceName + "' but is '" + result + "'!");
                allOk = false;
            }

            // system property, both provider class and instance name
            provClassName = "jkl";
            instanceName = "mno";
            System.setProperty(propertyName, provClassName + "#" + instanceName);
            result = JDK13Services.getDefaultProviderClassName(cls);
            if (! provClassName.equals(result)) {
                out("type " + cls + "failed: provider class should be '" +
                    provClassName + "' but is '" + result + "'!");
                allOk = false;
            }
            result = JDK13Services.getDefaultInstanceName(cls);
            if (! instanceName.equals(result)) {
                out("type " + cls + "failed: instance name should be '" +
                    instanceName + "' but is '" + result + "'!");
                allOk = false;
            }

            // system property, empty
            System.setProperty(propertyName, "");
            result = JDK13Services.getDefaultProviderClassName(cls);
            if (result != null) {
                out("type " + cls + " failed: provider class should be " +
                    "null but is '" + result + "'!");
                allOk = false;
            }
            result = JDK13Services.getDefaultInstanceName(cls);
            if (result != null) {
                out("type " + cls + "failed: instance name should be " +
                    "null but is '" + result + "'!");
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

