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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.jar.Attributes.Name;
import java.lang.reflect.Field;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8066619
 * @modules java.base/java.util.jar:+open
 * @run testng/othervm NullAndEmptyKeysAndValues
 * @summary Tests manifests with {@code null} and empty string {@code ""}
 * values as section name, header name, or value in both main and named
 * attributes sections.
 */
/*
 * Note to future maintainer:
 * In order to actually being able to test all the cases where key and values
 * are null normal manifest and attributes manipulation through their public
 * api is not sufficient but then there were these null checks there before
 * which may or may not have had their reason and this way it's ensured that
 * the behavior does not change with that respect.
 * Once module isolation is enforced some test cases will not any longer be
 * possible and those now tested situations will be guaranteed not to occur
 * any longer at all at which point the corresponding tests can be removed
 * safely without replacement unless of course another way is found inject the
 * tested null values.
 * Another trick to access package private class members could be to use
 * deserialization or adding a new class to the same package on the classpath.
 * Here is not important how the values are set to null because it shows that
 * the behavior remains unchanged.
 */
public class NullAndEmptyKeysAndValues {

    static final String SOME_KEY = "some-key";
    static final String SOME_VALUE = "some value";
    static final String NULL_TEXT = "null";
    static final String EMPTY_STR = "";
    static final Name EMPTY_NAME = new Name("tmp") {{
        try {
            Field name = Name.class.getDeclaredField("name");
            name.setAccessible(true);
            name.set(this, EMPTY_STR);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }};

    @Test
    public void testMainAttributesHeaderNameNull() throws Exception {
        Manifest mf = new Manifest();
        Field attr = mf.getClass().getDeclaredField("attr");
        attr.setAccessible(true);
        Attributes mainAtts = new Attributes() {{
            super.put(null, SOME_VALUE);
        }};
        attr.set(mf, mainAtts);
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        assertThrows(NullPointerException.class, () -> writeAndRead(mf));
    }

    @Test
    public void testMainAttributesHeaderNameEmpty() throws Exception {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getMainAttributes().put(EMPTY_NAME, SOME_VALUE);
        assertThrows(IOException.class, () -> writeAndRead(mf));
    }

    @Test
    public void testMainAttributesHeaderValueNull() throws Exception {
        Manifest mf = new Manifest();
        Field attr = mf.getClass().getDeclaredField("attr");
        attr.setAccessible(true);
        Attributes mainAtts = new Attributes() {{
            map.put(new Name(SOME_KEY), null);
        }};
        attr.set(mf, mainAtts);
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf = writeAndRead(mf);
        assertEquals(mf.getMainAttributes().getValue(SOME_KEY), NULL_TEXT);
    }

    @Test
    public void testMainAttributesHeaderValueEmpty() throws Exception {
        Manifest mf = new Manifest();
        Field attr = mf.getClass().getDeclaredField("attr");
        attr.setAccessible(true);
        Attributes mainAtts = new Attributes() {{
            map.put(new Name(SOME_KEY), EMPTY_STR);
        }};
        attr.set(mf, mainAtts);
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf = writeAndRead(mf);
        assertEquals(mf.getMainAttributes().getValue(SOME_KEY), EMPTY_STR);
    }

    @Test
    public void testSectionNameNull() throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getEntries().put(null, new Attributes());
        mf = writeAndRead(mf);
        assertNotNull(mf.getEntries().get(NULL_TEXT));
    }

    @Test
    public void testSectionNameEmpty() throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getEntries().put(EMPTY_STR, new Attributes());
        mf = writeAndRead(mf);
        assertNotNull(mf.getEntries().get(EMPTY_STR));
    }

    @Test
    public void testNamedSectionHeaderNameNull() throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getEntries().put(SOME_KEY, new Attributes() {{
            map.put(null, SOME_VALUE);
        }});
        assertThrows(NullPointerException.class, () -> writeAndRead(mf));
    }

    @Test
    public void testNamedSectionHeaderNameEmpty() throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getEntries().put(SOME_KEY, new Attributes() {{
            map.put(EMPTY_NAME, SOME_VALUE);
        }});
        assertThrows(IOException.class, () -> writeAndRead(mf));
    }

    @Test
    public void testNamedSectionHeaderValueNull() throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getEntries().put(SOME_KEY, new Attributes() {{
            map.put(new Name(SOME_KEY), null);
        }});
        mf = writeAndRead(mf);
        assertEquals(mf.getEntries().get(SOME_KEY).getValue(SOME_KEY),
                NULL_TEXT);
    }

    @Test
    public void testNamedSectionHeaderValueEmpty() throws IOException {
        Manifest mf = new Manifest();
        mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
        mf.getEntries().put(SOME_KEY, new Attributes() {{
            map.put(new Name(SOME_KEY), EMPTY_STR);
        }});
        mf = writeAndRead(mf);
        assertEquals(mf.getEntries().get(SOME_KEY).getValue(SOME_KEY),
                EMPTY_STR);
    }

    static Manifest writeAndRead(Manifest mf) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mf.write(out);
        byte[] mfBytes = out.toByteArray();
        System.out.println("-".repeat(72));
        System.out.print(new String(mfBytes, UTF_8));
        System.out.println("-".repeat(72));
        ByteArrayInputStream in = new ByteArrayInputStream(mfBytes);
        return new Manifest(in);
    }

}
