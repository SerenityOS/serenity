/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @modules java.base/jdk.internal.access
 *          java.base/jdk.internal.module
 * @run testng ModuleNamesTest
 * @summary Basic test of reading a module-info.class with module names that
 *          are legal in class files but not legal in the Java Language
 */

import java.io.ByteArrayOutputStream;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Requires;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.Set;

import jdk.internal.access.SharedSecrets;
import jdk.internal.module.ModuleInfoWriter;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ModuleNamesTest {

    @DataProvider(name = "legalModuleNames")
    public Object[][] legalModuleNames() {
        return new Object[][] {

                { ".",              "." },
                { ".foo",           ".foo" },
                { "foo.",           "foo." },
                { "foo.bar",        "foo.bar" },

                { "..",             ".." },
                { "..foo",          "..foo" },
                { "foo..",          "foo.." },
                { "foo..bar",       "foo..bar" },

                { "[",              "[" },
                { "[foo",           "[foo" },
                { "foo[",           "foo[" },
                { "foo[bar",        "foo[bar" },

                { ";",              ";" },
                { ";foo",           ";foo" },
                { "foo;",           "foo;" },
                { "foo;bar",        "foo;bar" },

                { "\\\\",           "\\" },
                { "\\\\foo",        "\\foo" },
                { "foo\\\\",        "foo\\" },
                { "foo\\\\bar",     "foo\\bar" },

                { "\\\\\\\\",       "\\\\" },
                { "\\\\\\\\foo",    "\\\\foo" },
                { "foo\\\\\\\\",    "foo\\\\" },
                { "foo\\\\\\\\bar", "foo\\\\bar" },

                { "\\:",            ":" },
                { "\\:foo",         ":foo" },
                { "foo\\:",         "foo:" },
                { "foo\\:bar",      "foo:bar" },

                { "\\:\\:",         "::" },
                { "\\:\\:foo",      "::foo" },
                { "foo\\:\\:",      "foo::" },
                { "foo\\:\\:bar",   "foo::bar" },

                { "\\@",            "@" },
                { "\\@foo",         "@foo" },
                { "foo\\@",         "foo@" },
                { "foo\\@bar",      "foo@bar" },

                { "\\@\\@",         "@@" },
                { "\\@\\@foo",      "@@foo" },
                { "foo\\@\\@",      "foo@@" },
                { "foo\\@\\@bar",   "foo@@bar" },

                { makeString("", 0x20, ""),        " "  },
                { makeString("foo", 0x20, ""),     "foo " },
                { makeString("", 0x20, "foo"),     " foo" },
                { makeString("foo", 0x20, "bar"),  "foo bar" },
        };
    }

    @DataProvider(name = "illegalModuleNames")
    public Object[][] illegalModuleNames() {
        return new Object[][] {

                { "",               null },

                { ":",              null },
                { ":foo",           null },
                { "foo:",           null },
                { "foo:bar",        null },

                { "@",              null },
                { "@foo",           null },
                { "foo@",           null },
                { "foo@bar",        null },

                { "\\",            null },
                { "\\foo",         null },
                { "foo\\",         null },
                { "foo\\bar",      null },

                { makeString("", 0x00, ""),         null },
                { makeString("", 0x00, "foo"),      null },
                { makeString("foo", 0x00, ""),      null },
                { makeString("foo", 0x00, "bar"),   null },

                { makeString("", 0x1f, ""),         null },
                { makeString("", 0x1f, "foo"),      null },
                { makeString("foo", 0x1f, ""),      null },
                { makeString("foo", 0x1f, "bar"),   null },

        };
    }

    @Test(dataProvider = "legalModuleNames")
    public void testLegalModuleName(String mn, String expected) throws Exception {
        ModuleDescriptor md = newBuilder(mn).requires("java.base").build();
        ByteBuffer bb = toBuffer(md);
        String name = ModuleDescriptor.read(bb).name();
        assertEquals(name, expected);
    }

    @Test(dataProvider = "illegalModuleNames",
          expectedExceptions = InvalidModuleDescriptorException.class)
    public void testIllegalModuleName(String mn, String ignore) throws Exception {
        ModuleDescriptor md = newBuilder(mn).requires("java.base").build();
        ByteBuffer bb = toBuffer(md);
        ModuleDescriptor.read(bb);  // throws InvalidModuleDescriptorException
    }

    @Test(dataProvider = "legalModuleNames")
    public void testLegalRequires(String mn, String expected) throws Exception {
        ModuleDescriptor md = newBuilder("m").requires("java.base").requires(mn).build();
        ByteBuffer bb = toBuffer(md);
        ModuleDescriptor descriptor = ModuleDescriptor.read(bb);
        Optional<Requires> requires = descriptor.requires().stream()
                .filter(r -> !r.name().equals("java.base"))
                .findAny();
        assertTrue(requires.isPresent());
        assertEquals(requires.get().name(), expected);
    }

    @Test(dataProvider = "illegalModuleNames",
          expectedExceptions = InvalidModuleDescriptorException.class)
    public void testIllegalRequires(String mn, String ignore) throws Exception {
        ModuleDescriptor md = newBuilder("m").requires("java.base").requires(mn).build();
        ByteBuffer bb = toBuffer(md);
        ModuleDescriptor.read(bb);   // throws InvalidModuleDescriptorException
    }

    @Test(dataProvider = "legalModuleNames")
    public void testLegalExports(String mn, String expected) throws Exception {
        ModuleDescriptor md = newBuilder("m")
                .requires("java.base")
                .exports("p", Set.of(mn))
                .build();
        ByteBuffer bb = toBuffer(md);
        ModuleDescriptor descriptor = ModuleDescriptor.read(bb);
        Optional<Exports> export = descriptor.exports().stream().findAny();
        assertTrue(export.isPresent());
        assertTrue(export.get().targets().contains(expected));
    }

    @Test(dataProvider = "illegalModuleNames",
          expectedExceptions = InvalidModuleDescriptorException.class)
    public void testIllegalExports(String mn, String ignore) throws Exception {
        ModuleDescriptor md = newBuilder("m")
                .requires("java.base")
                .exports("p", Set.of(mn))
                .build();
        ByteBuffer bb = toBuffer(md);
        ModuleDescriptor.read(bb);   // throws InvalidModuleDescriptorException
    }

    @Test(dataProvider = "legalModuleNames")
    public void testLegalOpens(String mn, String expected) throws Exception {
        ModuleDescriptor md = newBuilder("m")
                .requires("java.base")
                .opens("p", Set.of(mn))
                .build();
        ByteBuffer bb = toBuffer(md);
        ModuleDescriptor descriptor = ModuleDescriptor.read(bb);
        Optional<Opens> opens = descriptor.opens().stream().findAny();
        assertTrue(opens.isPresent());
        assertTrue(opens.get().targets().contains(expected));
    }

    @Test(dataProvider = "illegalModuleNames",
          expectedExceptions = InvalidModuleDescriptorException.class)
    public void testIllegalOpens(String mn, String ignore) throws Exception {
        ModuleDescriptor md = newBuilder("m")
                .requires("java.base")
                .opens("p", Set.of(mn))
                .build();
        ByteBuffer bb = toBuffer(md);
        ModuleDescriptor.read(bb);   // throws InvalidModuleDescriptorException
    }

    /**
     * Returns a Builder that does not validate module names.
     */
    private Builder newBuilder(String mn) {
        return SharedSecrets.getJavaLangModuleAccess()
                            .newModuleBuilder(mn, false, Set.of());
    }

    /**
     * Returns a {@code ByteBuffer} containing the given module descriptor
     * in module-info.class format.
     */
    private ByteBuffer toBuffer(ModuleDescriptor descriptor) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ModuleInfoWriter.write(descriptor, baos);
        return ByteBuffer.wrap(baos.toByteArray());
    }

    /**
     * Returns a string containing a given code point.
     */
    private String makeString(String prefix, int codePoint, String suffix) {
        StringBuilder sb = new StringBuilder();
        sb.append(prefix);
        sb.appendCodePoint(codePoint);
        sb.append(suffix);
        return sb.toString();
    }
}
