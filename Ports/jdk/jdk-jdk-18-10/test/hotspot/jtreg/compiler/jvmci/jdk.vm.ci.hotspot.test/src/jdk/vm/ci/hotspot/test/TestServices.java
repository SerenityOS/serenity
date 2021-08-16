/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @requires vm.jvmci
 * @modules jdk.internal.vm.ci/jdk.vm.ci.services:+open
 * @library /compiler/jvmci/jdk.vm.ci.hotspot.test/src
 * @run testng/othervm
 *      -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler
 *      jdk.vm.ci.hotspot.test.TestServices
 */

package jdk.vm.ci.hotspot.test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import jdk.vm.ci.services.Services;

public class TestServices {

    @SuppressWarnings("unchecked")
    @Test
    public void serializeSavedPropertiesTest() throws Exception {

        Field f = Services.class.getDeclaredField("MAX_UTF8_PROPERTY_STRING_LENGTH");
        f.setAccessible(true);
        int maxUtf8PropertyStringLength = (int) f.get(null);

        Method serializeProperties = Services.class.getDeclaredMethod("serializeProperties", Map.class);
        Method deserializeProperties = Services.class.getDeclaredMethod("deserializeProperties", byte[].class);
        serializeProperties.setAccessible(true);
        deserializeProperties.setAccessible(true);

        Map<String, String> props = new HashMap<>(Services.getSavedProperties());
        String[] names = {
                        new String(new char[maxUtf8PropertyStringLength - 100]).replace('\0', 'x'),
                        new String(new char[maxUtf8PropertyStringLength - 1]).replace('\0', 'x'),
                        new String(new char[maxUtf8PropertyStringLength]).replace('\0', 'y'),
                        new String(new char[maxUtf8PropertyStringLength + 1]).replace('\0', 'z'),
                        new String(new char[maxUtf8PropertyStringLength + 100]).replace('\0', 'z')
        };
        String[] values = {
                        new String(new char[maxUtf8PropertyStringLength - 100]).replace('\0', '1'),
                        new String(new char[maxUtf8PropertyStringLength - 1]).replace('\0', '1'),
                        new String(new char[maxUtf8PropertyStringLength]).replace('\0', '2'),
                        new String(new char[maxUtf8PropertyStringLength + 1]).replace('\0', '1'),
                        new String(new char[maxUtf8PropertyStringLength + 100]).replace('\0', '3')
        };
        for (String name : names) {
            for (String value : values) {
                props.put(name, value);
            }
        }

        byte[] data = (byte[]) serializeProperties.invoke(null, props);

        Map<String, String> newProps = (Map<String, String>) deserializeProperties.invoke(null, data);

        Assert.assertEquals(props.size(), newProps.size());
        for (String name : props.keySet()) {
            String expect = props.get(name);
            String actual = newProps.get(name);
            Assert.assertEquals(expect, actual);
        }
    }
}
