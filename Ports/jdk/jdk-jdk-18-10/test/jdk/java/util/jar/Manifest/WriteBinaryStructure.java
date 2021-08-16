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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8066619
 * @run testng WriteBinaryStructure
 * @summary Tests that jar manifests are written in a particular structure
 */
public class WriteBinaryStructure {

    @Test
    public void testMainAttributes() throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(new Name("Key"), "Value");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        mf.write(buf);
        assertEquals(buf.toByteArray(), (
                "Manifest-Version: 1.0\r\n" +
                "Key: Value\r\n" +
                "\r\n").getBytes(UTF_8));
    }

    @Test
    public void testIndividualSection() throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        Attributes attributes = new Attributes();
        mf.getEntries().put("Individual-Section-Name", attributes);
        attributes.put(new Name("Key"), "Value");
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        mf.write(buf);
        assertEquals(buf.toByteArray(), (
                "Manifest-Version: 1.0\r\n" +
                "\r\n" +
                "Name: Individual-Section-Name\r\n" +
                "Key: Value\r\n" +
                "\r\n").getBytes(UTF_8));
    }

}
