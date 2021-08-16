/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @run testng ModuleDescriptorTest
 * @summary Basic test for java.lang.module.ModuleDescriptor and its builder
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.module.InvalidModuleDescriptorException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Builder;
import java.lang.module.ModuleDescriptor.Exports;
import java.lang.module.ModuleDescriptor.Opens;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleDescriptor.Provides;
import java.lang.module.ModuleDescriptor.Requires.Modifier;
import java.lang.module.ModuleDescriptor.Version;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.module.ModuleDescriptor.Requires.Modifier.*;

import jdk.internal.access.JavaLangModuleAccess;
import jdk.internal.access.SharedSecrets;
import jdk.internal.module.ModuleInfoWriter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ModuleDescriptorTest {

    @DataProvider(name = "invalidNames")
    public Object[][] invalidNames() {
        return new Object[][]{

            { null,             null },
            { "1",              null },
            { "1foo",           null },
            { ".foo",           null },
            { "foo.",           null },
            { "[foo]",          null },
            { "foo.1",          null },
            { "1foo.bar",       null },
            { "foo.1bar",       null },
            { "foo.[bar]",      null },
            { "foo..bar",       null },
            { "foo.bar.1",      null },
            { "foo.bar.1gus",   null },
            { "foo.bar.[gus]",  null },

            { "class",          null },
            { "interface",      null },
            { "true",           null },
            { "false",          null },
            { "null",           null },

            { "x.class",        null },
            { "x.interface",    null },
            { "x.true",         null },
            { "x.false",        null },
            { "x.null",         null },

            { "class.x",        null },
            { "interface.x",    null },
            { "true.x",         null },
            { "false.x",        null },
            { "null.x",         null },

            { "x.class.x",      null },
            { "x.interface.x",  null },
            { "x.true.x",       null },
            { "x.false.x",      null },
            { "x.null.x",       null },

            { "_",              null },

        };
    }


    // requires

    private Requires requires(Set<Modifier> mods, String mn) {
        return requires(mods, mn, null);
    }

    private Requires requires(Set<Modifier> mods, String mn, Version v) {
        Builder builder = ModuleDescriptor.newModule("m");
        if (v == null) {
            builder.requires(mods, mn);
        } else {
            builder.requires(mods, mn, v);
        }
        Set<Requires> requires = builder.build().requires();
        assertTrue(requires.size() == 2);
        Iterator<Requires> iterator = requires.iterator();
        Requires r = iterator.next();
        if (r.name().equals("java.base")) {
            r = iterator.next();
        } else {
            Requires other = iterator.next();
            assertEquals(other.name(), "java.base");
        }
        return r;
    }

    private Requires requires(String mn) {
        return requires(Collections.emptySet(), mn);
    }

    public void testRequiresWithRequires() {
        Requires r1 = requires("foo");
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("m").requires(r1).build();
        assertEquals(descriptor.requires().size(), 2);
        var iterator = descriptor.requires().iterator();
        Requires r2 = iterator.next();
        if (r2.name().equals("java.base")) {
            r2 = iterator.next();
        }
        assertEquals(r1, r2);
    }

    public void testRequiresWithNoModifiers() {
        Requires r = requires(EnumSet.noneOf(Requires.Modifier.class), "foo");
        assertEquals(r, r);
        assertTrue(r.compareTo(r) == 0);
        assertTrue(r.modifiers().isEmpty());
        assertEquals(r.name(), "foo");
        assertFalse(r.compiledVersion().isPresent());
    }

    public void testRequiresWithOneModifier() {
        Requires r = requires(EnumSet.of(TRANSITIVE), "foo");
        assertEquals(r, r);
        assertTrue(r.compareTo(r) == 0);
        assertEquals(r.modifiers(), EnumSet.of(TRANSITIVE));
        assertEquals(r.name(), "foo");
        assertFalse(r.compiledVersion().isPresent());
    }

    public void testRequiresWithTwoModifiers() {
        Requires r = requires(EnumSet.of(TRANSITIVE, SYNTHETIC), "foo");
        assertEquals(r, r);
        assertTrue(r.compareTo(r) == 0);
        assertEquals(r.modifiers(), EnumSet.of(TRANSITIVE, SYNTHETIC));
        assertEquals(r.name(), "foo");
        assertFalse(r.compiledVersion().isPresent());
    }

    public void testRequiresWithAllModifiers() {
        Requires r = requires(EnumSet.allOf(Modifier.class), "foo");
        assertEquals(r, r);
        assertTrue(r.compareTo(r) == 0);
        assertEquals(r.modifiers(), EnumSet.of(TRANSITIVE, STATIC, SYNTHETIC, MANDATED));
        assertEquals(r.name(), "foo");
        assertFalse(r.compiledVersion().isPresent());
    }

    public void testRequiresWithCompiledVersion() {
        Version v = Version.parse("1.0");
        Requires r = requires(Set.of(), "foo", v);
        assertEquals(r, r);
        assertTrue(r.compareTo(r) == 0);
        assertEquals(r.modifiers(), Set.of());
        assertEquals(r.name(), "foo");
        assertTrue(r.compiledVersion().isPresent());
        assertEquals(r.compiledVersion().get().toString(), "1.0");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testRequiresWithDuplicatesRequires() {
        Requires r = requires("foo");
        ModuleDescriptor.newModule("m").requires(r).requires(r);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRequiresSelfWithRequires() {
        Requires r = requires("foo");
        ModuleDescriptor.newModule("foo").requires(r);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRequiresSelfWithNoModifier() {
        ModuleDescriptor.newModule("m").requires("m");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRequiresSelfWithOneModifier() {
        ModuleDescriptor.newModule("m").requires(Set.of(TRANSITIVE), "m");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testRequiresSelfWithAllModifiers() {
        ModuleDescriptor.newModule("m").requires(EnumSet.allOf(Modifier.class), "m");
    }

    @Test(dataProvider = "invalidNames",
          expectedExceptions = IllegalArgumentException.class )
    public void testRequiresWithBadModuleName(String mn, String ignore) {
        requires(EnumSet.noneOf(Modifier.class), mn);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRequiresWithNullRequires() {
        ModuleDescriptor.newModule("m").requires((Requires) null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRequiresWithNullModifiers() {
        ModuleDescriptor.newModule("m").requires(null, "foo");
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testRequiresWithNullVersion() {
        ModuleDescriptor.newModule("m").requires(Set.of(), "foo", null);
    }

    public void testRequiresCompare() {
        Requires r1 = requires(EnumSet.noneOf(Modifier.class), "foo");
        Requires r2 = requires(EnumSet.noneOf(Modifier.class), "bar");
        int n = "foo".compareTo("bar");
        assertTrue(r1.compareTo(r2) == n);
        assertTrue(r2.compareTo(r1) == -n);
    }

    public void testRequiresCompareWithDifferentModifiers() {
        Requires r1 = requires(EnumSet.of(TRANSITIVE), "foo");
        Requires r2 = requires(EnumSet.of(SYNTHETIC), "foo");
        int n = Integer.compare(1 << TRANSITIVE.ordinal(), 1 << SYNTHETIC.ordinal());
        assertTrue(r1.compareTo(r2) == n);
        assertTrue(r2.compareTo(r1) == -n);
    }

    public void testRequiresCompareWithSameModifiers() {
        Requires r1 = requires(EnumSet.of(SYNTHETIC), "foo");
        Requires r2 = requires(EnumSet.of(SYNTHETIC), "foo");
        assertTrue(r1.compareTo(r2) == 0);
        assertTrue(r2.compareTo(r1) == 0);
    }

    public void testRequiresCompareWithSameCompiledVersion() {
        Requires r1 = requires(Set.of(), "foo", Version.parse("2.0"));
        Requires r2 = requires(Set.of(), "foo", Version.parse("2.0"));
        assertTrue(r1.compareTo(r2) == 0);
        assertTrue(r2.compareTo(r1) == 0);
    }

    public void testRequiresCompareWithDifferentCompiledVersion() {
        Requires r1 = requires(Set.of(), "foo", Version.parse("1.0"));
        Requires r2 = requires(Set.of(), "foo", Version.parse("2.0"));
        assertTrue(r1.compareTo(r2) < 0);
        assertTrue(r2.compareTo(r1) > 0);
    }

    public void testRequiresEqualsAndHashCode() {
        Requires r1 = requires("foo");
        Requires r2 = requires("foo");
        assertEquals(r1, r2);
        assertTrue(r1.hashCode() == r2.hashCode());

        r1 = requires(EnumSet.allOf(Requires.Modifier.class), "foo");
        r2 = requires(EnumSet.allOf(Requires.Modifier.class), "foo");
        assertEquals(r1, r2);
        assertTrue(r1.hashCode() == r2.hashCode());

        r1 = requires("foo");
        r2 = requires("bar");
        assertNotEquals(r1, r2);

        r1 = requires(EnumSet.allOf(Requires.Modifier.class), "foo");
        r2 = requires(Set.of(), "foo");
        assertNotEquals(r1, r2);

        Version v1 = Version.parse("1.0");
        r1 = requires(EnumSet.allOf(Requires.Modifier.class), "foo", v1);
        r2 = requires(EnumSet.allOf(Requires.Modifier.class), "foo", v1);
        assertEquals(r1, r2);
        assertTrue(r1.hashCode() == r2.hashCode());

        Version v2 = Version.parse("2.0");
        r1 = requires(EnumSet.allOf(Requires.Modifier.class), "foo", v1);
        r2 = requires(EnumSet.allOf(Requires.Modifier.class), "foo", v2);
        assertNotEquals(r1, r2);
    }

    public void testRequiresToString() {
        Requires r = requires(EnumSet.noneOf(Modifier.class), "foo");
        assertTrue(r.toString().contains("foo"));
    }


    // exports

    private Exports exports(Set<Exports.Modifier> mods, String pn) {
        return ModuleDescriptor.newModule("foo")
            .exports(mods, pn)
            .build()
            .exports()
            .iterator()
            .next();
    }

    private Exports exports(String pn) {
        return exports(Set.of(), pn);
    }

    private Exports exports(Set<Exports.Modifier> mods, String pn, String target) {
        return ModuleDescriptor.newModule("foo")
            .exports(mods, pn, Set.of(target))
            .build()
            .exports()
            .iterator()
            .next();
    }

    private Exports exports(String pn, String target) {
        return exports(Set.of(), pn, target);
    }


    public void testExportsExports() {
        Exports e1 = exports("p");
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("m").exports(e1).build();
        Exports e2 = descriptor.exports().iterator().next();
        assertEquals(e1, e2);
    }

    public void testExportsToAll() {
        Exports e = exports("p");
        assertEquals(e, e);
        assertTrue(e.modifiers().isEmpty());
        assertEquals(e.source(), "p");
        assertFalse(e.isQualified());
        assertTrue(e.targets().isEmpty());
    }

    public void testExportsToTarget() {
        Exports e = exports("p", "bar");
        assertEquals(e, e);
        assertTrue(e.modifiers().isEmpty());
        assertEquals(e.source(), "p");
        assertTrue(e.isQualified());
        assertTrue(e.targets().size() == 1);
        assertTrue(e.targets().contains("bar"));
    }

    public void testExportsToTargets() {
        Set<String> targets = new HashSet<>();
        targets.add("bar");
        targets.add("gus");
        Exports e
            = ModuleDescriptor.newModule("foo")
                .exports("p", targets)
                .build()
                .exports()
                .iterator()
                .next();
        assertEquals(e, e);
        assertTrue(e.modifiers().isEmpty());
        assertEquals(e.source(), "p");
        assertTrue(e.isQualified());
        assertTrue(e.targets().size() == 2);
        assertTrue(e.targets().contains("bar"));
        assertTrue(e.targets().contains("gus"));
    }

    public void testExportsToAllWithModifier() {
        Exports e = exports(Set.of(Exports.Modifier.SYNTHETIC), "p");
        assertEquals(e, e);
        assertTrue(e.modifiers().size() == 1);
        assertTrue(e.modifiers().contains(Exports.Modifier.SYNTHETIC));
        assertEquals(e.source(), "p");
        assertFalse(e.isQualified());
        assertTrue(e.targets().isEmpty());
    }

    public void testExportsToTargetWithModifier() {
        Exports e = exports(Set.of(Exports.Modifier.SYNTHETIC), "p", "bar");
        assertEquals(e, e);
        assertTrue(e.modifiers().size() == 1);
        assertTrue(e.modifiers().contains(Exports.Modifier.SYNTHETIC));
        assertEquals(e.source(), "p");
        assertTrue(e.isQualified());
        assertTrue(e.targets().size() == 1);
        assertTrue(e.targets().contains("bar"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testExportsWithDuplicate1() {
        Exports e = exports("p");
        ModuleDescriptor.newModule("foo").exports(e).exports(e);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testExportsWithDuplicate2() {
        ModuleDescriptor.newModule("foo").exports("p").exports("p");
    }

    @Test(expectedExceptions = IllegalArgumentException.class )
    public void testExportsWithEmptySet() {
        ModuleDescriptor.newModule("foo").exports("p", Collections.emptySet());
    }

    @Test(dataProvider = "invalidNames",
          expectedExceptions = IllegalArgumentException.class )
    public void testExportsWithBadName(String pn, String ignore) {
        ModuleDescriptor.newModule("foo").exports(pn);
    }

    @Test(expectedExceptions = NullPointerException.class )
    public void testExportsWithNullExports() {
        ModuleDescriptor.newModule("foo").exports((Exports) null);
    }

    @Test(expectedExceptions = NullPointerException.class )
    public void testExportsWithNullTargets() {
        ModuleDescriptor.newModule("foo").exports("p", (Set<String>) null);
    }

    public void testExportsCompare() {
        Exports e1 = exports("p");
        Exports e2 = exports("p");
        assertEquals(e1, e2);
        assertTrue(e1.hashCode() == e2.hashCode());
        assertTrue(e1.compareTo(e2) == 0);
        assertTrue(e2.compareTo(e1) == 0);
    }

    public void testExportsCompareWithSameModifiers() {
        Exports e1 = exports(Set.of(Exports.Modifier.SYNTHETIC), "p");
        Exports e2 = exports(Set.of(Exports.Modifier.SYNTHETIC), "p");
        assertEquals(e1, e2);
        assertTrue(e1.hashCode() == e2.hashCode());
        assertTrue(e1.compareTo(e2) == 0);
        assertTrue(e2.compareTo(e1) == 0);
    }

    public void testExportsCompareWithDifferentModifiers() {
        Exports e1 = exports(Set.of(Exports.Modifier.SYNTHETIC), "p");
        Exports e2 = exports("p");
        assertNotEquals(e1, e2);
        assertTrue(e1.compareTo(e2) == 1);
        assertTrue(e2.compareTo(e1) == -1);
    }

    public void testExportsCompareWithSameTargets() {
        Exports e1 = exports("p", "x");
        Exports e2 = exports("p", "x");
        assertEquals(e1, e2);
        assertTrue(e1.hashCode() == e2.hashCode());
        assertTrue(e1.compareTo(e2) == 0);
        assertTrue(e2.compareTo(e1) == 0);
    }

    public void testExportsCompareWithDifferentTargets() {
        Exports e1 = exports("p", "y");
        Exports e2 = exports("p", "x");
        assertNotEquals(e1, e2);
        assertTrue(e1.compareTo(e2) == 1);
        assertTrue(e2.compareTo(e1) == -1);
    }

    public void testExportsToString() {
        String s = ModuleDescriptor.newModule("foo")
            .exports("p1", Set.of("bar"))
            .build()
            .exports()
            .iterator()
            .next()
            .toString();
        assertTrue(s.contains("p1"));
        assertTrue(s.contains("bar"));
    }


    // opens

    private Opens opens(Set<Opens.Modifier> mods, String pn) {
        return ModuleDescriptor.newModule("foo")
                .opens(mods, pn)
                .build()
                .opens()
                .iterator()
                .next();
    }

    private Opens opens(String pn) {
        return opens(Set.of(), pn);
    }

    private Opens opens(Set<Opens.Modifier> mods, String pn, String target) {
        return ModuleDescriptor.newModule("foo")
                .opens(mods, pn, Set.of(target))
                .build()
                .opens()
                .iterator()
                .next();
    }

    private Opens opens(String pn, String target) {
        return opens(Set.of(), pn, target);
    }

    public void testOpensOpens() {
        Opens o1 = opens("p");
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("m").opens(o1).build();
        Opens o2 = descriptor.opens().iterator().next();
        assertEquals(o1, o2);
    }

    public void testOpensToAll() {
        Opens o = opens("p");
        assertEquals(o, o);
        assertTrue(o.modifiers().isEmpty());
        assertEquals(o.source(), "p");
        assertFalse(o.isQualified());
        assertTrue(o.targets().isEmpty());
    }


    public void testOpensToTarget() {
        Opens o = opens("p", "bar");
        assertEquals(o, o);
        assertTrue(o.modifiers().isEmpty());
        assertEquals(o.source(), "p");
        assertTrue(o.isQualified());
        assertTrue(o.targets().size() == 1);
        assertTrue(o.targets().contains("bar"));
    }

    public void testOpensToTargets() {
        Set<String> targets = new HashSet<>();
        targets.add("bar");
        targets.add("gus");
        Opens o = ModuleDescriptor.newModule("foo")
                .opens("p", targets)
                .build()
                .opens()
                .iterator()
                .next();
        assertEquals(o, o);
        assertTrue(o.modifiers().isEmpty());
        assertEquals(o.source(), "p");
        assertTrue(o.isQualified());
        assertTrue(o.targets().size() == 2);
        assertTrue(o.targets().contains("bar"));
        assertTrue(o.targets().contains("gus"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testOpensWithDuplicate1() {
        Opens o = opens("p");
        ModuleDescriptor.newModule("foo").opens(o).opens(o);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testOpensWithDuplicate2() {
        ModuleDescriptor.newModule("foo").opens("p").opens("p");
    }

    @Test(expectedExceptions = IllegalArgumentException.class )
    public void testOpensWithEmptySet() {
        ModuleDescriptor.newModule("foo").opens("p", Collections.emptySet());
    }

    @Test(dataProvider = "invalidNames",
            expectedExceptions = IllegalArgumentException.class )
    public void testOpensWithBadName(String pn, String ignore) {
        ModuleDescriptor.newModule("foo").opens(pn);
    }

    @Test(expectedExceptions = NullPointerException.class )
    public void testOpensWithNullExports() {
        ModuleDescriptor.newModule("foo").opens((Opens) null);
    }

    @Test(expectedExceptions = NullPointerException.class )
    public void testOpensWithNullTargets() {
        ModuleDescriptor.newModule("foo").opens("p", (Set<String>) null);
    }

    public void testOpensCompare() {
        Opens o1 = opens("p");
        Opens o2 = opens("p");
        assertEquals(o1, o2);
        assertTrue(o1.hashCode() == o2.hashCode());
        assertTrue(o1.compareTo(o2) == 0);
        assertTrue(o2.compareTo(o1) == 0);
    }

    public void testOpensCompareWithSameModifiers() {
        Opens o1 = opens(Set.of(Opens.Modifier.SYNTHETIC), "p");
        Opens o2 = opens(Set.of(Opens.Modifier.SYNTHETIC), "p");
        assertEquals(o1, o2);
        assertTrue(o1.hashCode() == o2.hashCode());
        assertTrue(o1.compareTo(o2) == 0);
        assertTrue(o2.compareTo(o1) == 0);
    }

    public void testOpensCompareWithDifferentModifiers() {
        Opens o1 = opens(Set.of(Opens.Modifier.SYNTHETIC), "p");
        Opens o2 = opens("p");
        assertNotEquals(o1, o2);
        assertTrue(o1.compareTo(o2) == 1);
        assertTrue(o2.compareTo(o1) == -1);
    }

    public void testOpensCompareWithSameTargets() {
        Opens o1 = opens("p", "x");
        Opens o2 = opens("p", "x");
        assertEquals(o1, o2);
        assertTrue(o1.hashCode() == o2.hashCode());
        assertTrue(o1.compareTo(o2) == 0);
        assertTrue(o2.compareTo(o1) == 0);
    }

    public void testOpensCompareWithDifferentTargets() {
        Opens o1 = opens("p", "y");
        Opens o2 = opens("p", "x");
        assertNotEquals(o1, o2);
        assertTrue(o1.compareTo(o2) == 1);
        assertTrue(o2.compareTo(o1) == -1);
    }

    public void testOpensToString() {
        String s = ModuleDescriptor.newModule("foo")
                .opens("p1", Set.of("bar"))
                .build()
                .opens()
                .iterator()
                .next()
                .toString();
        assertTrue(s.contains("p1"));
        assertTrue(s.contains("bar"));
    }


    // uses

    public void testUses() {
        Set<String> uses
            = ModuleDescriptor.newModule("foo")
                .uses("p.S")
                .uses("q.S")
                .build()
                .uses();
        assertTrue(uses.size() == 2);
        assertTrue(uses.contains("p.S"));
        assertTrue(uses.contains("q.S"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testUsesWithDuplicate() {
        ModuleDescriptor.newModule("foo").uses("p.S").uses("p.S");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testUsesWithSimpleIdentifier() {
        ModuleDescriptor.newModule("foo").uses("S");
    }

    @Test(dataProvider = "invalidNames",
          expectedExceptions = IllegalArgumentException.class )
    public void testUsesWithBadName(String service, String ignore) {
        ModuleDescriptor.newModule("foo").uses(service);
    }


    // provides

    private Provides provides(String st, String pc) {
        return ModuleDescriptor.newModule("foo")
            .provides(st, List.of(pc))
            .build()
            .provides()
            .iterator()
            .next();
    }

    private Provides provides(String st, List<String> pns) {
        return ModuleDescriptor.newModule("foo")
                .provides(st, pns)
                .build()
                .provides()
                .iterator()
                .next();
    }

    public void testProvidesWithProvides() {
        Provides p1 = provides("p.S", "q.S1");
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("m")
                .provides(p1)
                .build();
        Provides p2 = descriptor.provides().iterator().next();
        assertEquals(p1, p2);
    }


    public void testProvides() {
        Set<Provides> set = ModuleDescriptor.newModule("foo")
                .provides("p.S", List.of("q.P1", "q.P2"))
                .build()
                .provides();
        assertTrue(set.size() == 1);

        Provides p = set.iterator().next();
        assertEquals(p, p);
        assertEquals(p.service(), "p.S");
        assertTrue(p.providers().size() == 2);
        assertEquals(p.providers().get(0), "q.P1");
        assertEquals(p.providers().get(1), "q.P2");
    }

    @Test(expectedExceptions = IllegalStateException.class )
    public void testProvidesWithDuplicateProvides() {
        Provides p = provides("p.S", "q.S2");
        ModuleDescriptor.newModule("m").provides("p.S", List.of("q.S1")).provides(p);
    }

    @Test(expectedExceptions = IllegalArgumentException.class )
    public void testProvidesWithEmptySet() {
        ModuleDescriptor.newModule("foo").provides("p.Service", Collections.emptyList());
    }

    @Test(expectedExceptions = IllegalArgumentException.class )
    public void testProvidesWithSimpleIdentifier1() {
        ModuleDescriptor.newModule("foo").provides("S", List.of("q.P"));
    }

    @Test(expectedExceptions = IllegalArgumentException.class )
    public void testProvidesWithSimpleIdentifier2() {
        ModuleDescriptor.newModule("foo").provides("p.S", List.of("P"));
    }

    @Test(dataProvider = "invalidNames",
          expectedExceptions = IllegalArgumentException.class )
    public void testProvidesWithBadService(String service, String ignore) {
        ModuleDescriptor.newModule("foo").provides(service, List.of("p.Provider"));
    }

    @Test(dataProvider = "invalidNames",
          expectedExceptions = IllegalArgumentException.class )
    public void testProvidesWithBadProvider(String provider, String ignore) {
        List<String> names = new ArrayList<>(); // allows nulls
        names.add(provider);
        ModuleDescriptor.newModule("foo").provides("p.Service", names);
    }

    @Test(expectedExceptions = NullPointerException.class )
    public void testProvidesWithNullProvides() {
        ModuleDescriptor.newModule("foo").provides((Provides) null);
    }

    @Test(expectedExceptions = NullPointerException.class )
    public void testProvidesWithNullProviders() {
        ModuleDescriptor.newModule("foo").provides("p.S", (List<String>) null);
    }

    public void testProvidesCompare() {
        Provides p1 = provides("p.S", "q.S1");
        Provides p2 = provides("p.S", "q.S1");
        assertEquals(p1, p2);
        assertTrue(p1.hashCode() == p2.hashCode());
        assertTrue(p1.compareTo(p2) == 0);
        assertTrue(p2.compareTo(p1) == 0);
    }

    public void testProvidesCompareWithDifferentService() {
        Provides p1 = provides("p.S2", "q.S1");
        Provides p2 = provides("p.S1", "q.S1");
        assertNotEquals(p1, p2);
        assertTrue(p1.compareTo(p2) == 1);
        assertTrue(p2.compareTo(p1) == -1);
    }

    public void testProvidesCompareWithDifferentProviders1() {
        Provides p1 = provides("p.S", "q.S2");
        Provides p2 = provides("p.S", "q.S1");
        assertNotEquals(p1, p2);
        assertTrue(p1.compareTo(p2) == 1);
        assertTrue(p2.compareTo(p1) == -1);
    }

    public void testProvidesCompareWithDifferentProviders2() {
        Provides p1 = provides("p.S", List.of("q.S1", "q.S2"));
        Provides p2 = provides("p.S", "q.S1");
        assertNotEquals(p1, p2);
        assertTrue(p1.compareTo(p2) == 1);
        assertTrue(p2.compareTo(p1) == -1);
    }

    // packages

    public void testPackages1() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .packages(Set.of("p", "q"))
                .build()
                .packages();
        assertTrue(packages.size() == 2);
        assertTrue(packages.contains("p"));
        assertTrue(packages.contains("q"));
    }

    public void testPackages2() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .packages(Set.of("p"))
                .packages(Set.of("q"))
                .build()
                .packages();
        assertTrue(packages.size() == 2);
        assertTrue(packages.contains("p"));
        assertTrue(packages.contains("q"));
    }


    public void testPackagesWithEmptySet() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .packages(Collections.emptySet())
                .build()
                .packages();
        assertTrue(packages.size() == 0);
    }

    public void testPackagesDuplicate() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .packages(Set.of("p"))
                .packages(Set.of("p"))
                .build()
                .packages();
        assertTrue(packages.size() == 1);
        assertTrue(packages.contains("p"));
    }

    public void testPackagesAndExportsPackage1() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .packages(Set.of("p"))
                .exports("p")
                .build()
                .packages();
        assertTrue(packages.size() == 1);
        assertTrue(packages.contains("p"));
    }

    public void testPackagesAndExportsPackage2() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .exports("p")
                .packages(Set.of("p"))
                .build()
                .packages();
        assertTrue(packages.size() == 1);
        assertTrue(packages.contains("p"));
    }

    public void testPackagesAndOpensPackage1() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .packages(Set.of("p"))
                .opens("p")
                .build()
                .packages();
        assertTrue(packages.size() == 1);
        assertTrue(packages.contains("p"));
    }

    public void testPackagesAndOpensPackage2() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .opens("p")
                .packages(Set.of("p"))
                .build()
                .packages();
        assertTrue(packages.size() == 1);
        assertTrue(packages.contains("p"));
    }

    public void testPackagesAndProvides1() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .packages(Set.of("p"))
                .provides("q.S", List.of("p.T"))
                .build()
                .packages();
        assertTrue(packages.size() == 1);
        assertTrue(packages.contains("p"));
    }

    public void testPackagesAndProvides2() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .provides("q.S", List.of("p.T"))
                .packages(Set.of("p"))
                .build()
                .packages();
        assertTrue(packages.size() == 1);
        assertTrue(packages.contains("p"));
    }

    public void testPackagesAndMainClass1() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .packages(Set.of("p"))
                .mainClass("p.Main")
                .build()
                .packages();
        assertTrue(packages.size() == 1);
        assertTrue(packages.contains("p"));
    }

    public void testPackagesAndMainClass2() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .mainClass("p.Main")
                .packages(Set.of("p"))
                .build()
                .packages();
        assertTrue(packages.size() == 1);
        assertTrue(packages.contains("p"));
    }

    public void testPackagesAndAll() {
        Set<String> packages = ModuleDescriptor.newModule("foo")
                .exports("p1")
                .opens("p2")
                .packages(Set.of("p3"))
                .provides("q.S", List.of("p4.T"))
                .mainClass("p5.Main")
                .build()
                .packages();
        assertTrue(Objects.equals(packages, Set.of("p1", "p2", "p3", "p4", "p5")));
    }

    @Test(dataProvider = "invalidNames",
          expectedExceptions = IllegalArgumentException.class )
    public void testPackagesWithBadName(String pn, String ignore) {
        Set<String> pkgs = new HashSet<>();  // allows nulls
        pkgs.add(pn);
        ModuleDescriptor.newModule("foo").packages(pkgs);
    }

    // name

    public void testModuleName() {
        String mn = ModuleDescriptor.newModule("foo").build().name();
        assertEquals(mn, "foo");
    }

    @Test(dataProvider = "invalidNames",
          expectedExceptions = IllegalArgumentException.class )
    public void testBadModuleName(String mn, String ignore) {
        ModuleDescriptor.newModule(mn);
    }


    // version

    public void testVersion1() {
        Version v1 = Version.parse("1.0");
        Version v2 = ModuleDescriptor.newModule("foo")
                .version(v1)
                .build()
                .version()
                .get();
        assertEquals(v1, v2);
    }

    public void testVersion2() {
        String vs = "1.0";
        Version v1 = ModuleDescriptor.newModule("foo")
                .version(vs)
                .build()
                .version()
                .get();
        Version v2 = Version.parse(vs);
        assertEquals(v1, v2);
    }

    @Test(expectedExceptions = NullPointerException.class )
    public void testNullVersion1() {
        ModuleDescriptor.newModule("foo").version((Version) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class )
    public void testNullVersion2() {
        ModuleDescriptor.newModule("foo").version((String) null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class )
    public void testEmptyVersion() {
        ModuleDescriptor.newModule("foo").version("");
    }


    @DataProvider(name = "unparseableVersions")
    public Object[][] unparseableVersions() {
        return new Object[][]{

                { null,  "A1" },    // no version < unparseable
                { "A1",  "A2" },    // unparseable < unparseable
                { "A1",  "1.0" },   // unparseable < parseable

        };
    }

    /**
     * Basic test for unparseable module versions
     */
    @Test(dataProvider = "unparseableVersions")
    public void testUnparseableModuleVersion(String vs1, String vs2) {
        ModuleDescriptor descriptor1 = newModule("m", vs1);
        ModuleDescriptor descriptor2 = newModule("m", vs2);

        if (vs1 != null && !isParsableVersion(vs1)) {
            assertFalse(descriptor1.version().isPresent());
            assertTrue(descriptor1.rawVersion().isPresent());
            assertEquals(descriptor1.rawVersion().get(), vs1);
        }

        if (vs2 != null && !isParsableVersion(vs2)) {
            assertFalse(descriptor2.version().isPresent());
            assertTrue(descriptor2.rawVersion().isPresent());
            assertEquals(descriptor2.rawVersion().get(), vs2);
        }

        assertFalse(descriptor1.equals(descriptor2));
        assertFalse(descriptor2.equals(descriptor1));
        assertTrue(descriptor1.compareTo(descriptor2) == -1);
        assertTrue(descriptor2.compareTo(descriptor1) == 1);
    }

    /**
     * Basic test for requiring a module with an unparseable version recorded
     * at compile version.
     */
    @Test(dataProvider = "unparseableVersions")
    public void testUnparseableCompiledVersion(String vs1, String vs2) {
        Requires r1 = newRequires("m", vs1);
        Requires r2 = newRequires("m", vs2);

        if (vs1 != null && !isParsableVersion(vs1)) {
            assertFalse(r1.compiledVersion().isPresent());
            assertTrue(r1.rawCompiledVersion().isPresent());
            assertEquals(r1.rawCompiledVersion().get(), vs1);
        }

        if (vs2 != null && !isParsableVersion(vs2)) {
            assertFalse(r2.compiledVersion().isPresent());
            assertTrue(r2.rawCompiledVersion().isPresent());
            assertEquals(r2.rawCompiledVersion().get(), vs2);
        }

        assertFalse(r1.equals(r2));
        assertFalse(r2.equals(r1));
        assertTrue(r1.compareTo(r2) == -1);
        assertTrue(r2.compareTo(r1) == 1);
    }

    private ModuleDescriptor newModule(String name, String vs) {
        JavaLangModuleAccess JLMA = SharedSecrets.getJavaLangModuleAccess();
        Builder builder = JLMA.newModuleBuilder(name, false, Set.of());
        if (vs != null)
            builder.version(vs);
        builder.requires("java.base");
        ByteBuffer bb = ModuleInfoWriter.toByteBuffer(builder.build());
        return ModuleDescriptor.read(bb);
    }

    private Requires newRequires(String name, String vs) {
        JavaLangModuleAccess JLMA = SharedSecrets.getJavaLangModuleAccess();
        Builder builder = JLMA.newModuleBuilder("foo", false, Set.of());
        if (vs == null) {
            builder.requires(name);
        } else {
            JLMA.requires(builder, Set.of(), name, vs);
        }
        Set<ModuleDescriptor.Requires> requires = builder.build().requires();
        Iterator<ModuleDescriptor.Requires> iterator = requires.iterator();
        ModuleDescriptor.Requires r = iterator.next();
        if (r.name().equals("java.base")) {
            r = iterator.next();
        }
        return r;
    }

    private boolean isParsableVersion(String vs) {
        try {
            Version.parse(vs);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }


    // toNameAndVersion

    public void testToNameAndVersion() {
        ModuleDescriptor md1 = ModuleDescriptor.newModule("foo").build();
        assertEquals(md1.toNameAndVersion(), "foo");

        ModuleDescriptor md2 = ModuleDescriptor.newModule("foo").version("1.0").build();
        assertEquals(md2.toNameAndVersion(), "foo@1.0");
    }


    // open modules

    public void testOpenModule() {
        ModuleDescriptor descriptor = ModuleDescriptor.newOpenModule("foo")
                .requires("bar")
                .exports("p")
                .provides("p.Service", List.of("q.ServiceImpl"))
                .build();

        // modifiers
        assertTrue(descriptor.modifiers().contains(ModuleDescriptor.Modifier.OPEN));
        assertTrue(descriptor.isOpen());

        // requires
        assertTrue(descriptor.requires().size() == 2);
        Set<String> names = descriptor.requires()
                .stream()
                .map(Requires::name)
                .collect(Collectors.toSet());
        assertEquals(names, Set.of("bar", "java.base"));

        // packages
        assertEquals(descriptor.packages(), Set.of("p", "q"));

        // exports
        assertTrue(descriptor.exports().size() == 1);
        names = descriptor.exports()
                .stream()
                .map(Exports::source)
                .collect(Collectors.toSet());
        assertEquals(names, Set.of("p"));

        // opens
        assertTrue(descriptor.opens().isEmpty());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testOpensOnOpenModule1() {
        ModuleDescriptor.newOpenModule("foo").opens("p");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testOpensOnOpenModule2() {
        ModuleDescriptor.newOpenModule("foo").opens("p", Set.of("bar"));
    }

    public void testIsOpen() {
        assertFalse(ModuleDescriptor.newModule("m").build().isOpen());
        assertFalse(ModuleDescriptor.newAutomaticModule("m").build().isOpen());
        assertTrue(ModuleDescriptor.newOpenModule("m").build().isOpen());
    }


    // automatic modules

    public void testAutomaticModule() {
        ModuleDescriptor descriptor = ModuleDescriptor.newAutomaticModule("foo")
                .packages(Set.of("p"))
                .provides("p.Service", List.of("q.ServiceImpl"))
                .build();

        // modifiers
        assertTrue(descriptor.modifiers().contains(ModuleDescriptor.Modifier.AUTOMATIC));
        assertTrue(descriptor.isAutomatic());

        // requires
        assertTrue(descriptor.requires().size() == 1);
        Set<String> names = descriptor.requires()
                .stream()
                .map(Requires::name)
                .collect(Collectors.toSet());
        assertEquals(names, Set.of("java.base"));

        // packages
        assertEquals(descriptor.packages(), Set.of("p", "q"));
        assertTrue(descriptor.exports().isEmpty());
        assertTrue(descriptor.opens().isEmpty());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testRequiresOnAutomaticModule() {
        ModuleDescriptor.newAutomaticModule("foo").requires("java.base");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testExportsOnAutomaticModule1() {
        ModuleDescriptor.newAutomaticModule("foo").exports("p");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testExportsOnAutomaticModule2() {
        ModuleDescriptor.newAutomaticModule("foo").exports("p", Set.of("bar"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testOpensOnAutomaticModule1() {
        ModuleDescriptor.newAutomaticModule("foo").opens("p");
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testOpensOnAutomaticModule2() {
        ModuleDescriptor.newAutomaticModule("foo").opens("p", Set.of("bar"));
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testUsesOnAutomaticModule() {
        ModuleDescriptor.newAutomaticModule("foo").uses("p.Service");
    }

    public void testIsAutomatic() {
        ModuleDescriptor descriptor1 = ModuleDescriptor.newModule("foo").build();
        assertFalse(descriptor1.isAutomatic());

        ModuleDescriptor descriptor2 = ModuleDescriptor.newOpenModule("foo").build();
        assertFalse(descriptor2.isAutomatic());

        ModuleDescriptor descriptor3 = ModuleDescriptor.newAutomaticModule("foo").build();
        assertTrue(descriptor3.isAutomatic());
    }


    // newModule with modifiers

    public void testNewModuleToBuildAutomaticModule() {
        Set<ModuleDescriptor.Modifier> ms = Set.of(ModuleDescriptor.Modifier.AUTOMATIC);
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("foo", ms).build();
        assertTrue(descriptor.modifiers().equals(ms));
        assertTrue(descriptor.isAutomatic());
    }

    public void testNewModuleToBuildOpenModule() {
        Set<ModuleDescriptor.Modifier> ms = Set.of(ModuleDescriptor.Modifier.OPEN);
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("foo", ms).build();
        assertTrue(descriptor.modifiers().equals(ms));
        assertTrue(descriptor.isOpen());

        ms = Set.of(ModuleDescriptor.Modifier.OPEN, ModuleDescriptor.Modifier.SYNTHETIC);
        descriptor = ModuleDescriptor.newModule("foo", ms).build();
        assertTrue(descriptor.modifiers().equals(ms));
        assertTrue(descriptor.isOpen());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testNewModuleToBuildAutomaticAndOpenModule() {
        Set<ModuleDescriptor.Modifier> ms = Set.of(ModuleDescriptor.Modifier.AUTOMATIC,
                                                   ModuleDescriptor.Modifier.OPEN);
        ModuleDescriptor.newModule("foo", ms);
    }


    // mainClass

    public void testMainClass() {
        String mainClass
            = ModuleDescriptor.newModule("foo").mainClass("p.Main").build().mainClass().get();
        assertEquals(mainClass, "p.Main");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMainClassWithSimpleIdentifier() {
        ModuleDescriptor.newModule("foo").mainClass("Main");
    }

    @Test(dataProvider = "invalidNames",
          expectedExceptions = IllegalArgumentException.class )
    public void testMainClassWithBadName(String mainClass, String ignore) {
        Builder builder = ModuleDescriptor.newModule("foo");
        builder.mainClass(mainClass);
    }


    // reads

    private static InputStream EMPTY_INPUT_STREAM = new InputStream() {
        @Override
        public int read() {
            return -1;
        }
    };

    private static InputStream FAILING_INPUT_STREAM = new InputStream() {
        @Override
        public int read() throws IOException {
            throw new IOException();
        }
    };

    /**
     * Basic test reading module-info.class
     */
    public void testRead() throws Exception {
        Module base = Object.class.getModule();

        try (InputStream in = base.getResourceAsStream("module-info.class")) {
            ModuleDescriptor descriptor = ModuleDescriptor.read(in);
            assertTrue(in.read() == -1); // all bytes read
            assertEquals(descriptor.name(), "java.base");
        }

        try (InputStream in = base.getResourceAsStream("module-info.class")) {
            ByteBuffer bb = ByteBuffer.wrap(in.readAllBytes());
            ModuleDescriptor descriptor = ModuleDescriptor.read(bb);
            assertFalse(bb.hasRemaining()); // no more remaining bytes
            assertEquals(descriptor.name(), "java.base");
        }
    }

    /**
     * Test ModuleDescriptor with a packager finder
     */
    public void testReadsWithPackageFinder() throws Exception {
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("foo")
                .requires("java.base")
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ModuleInfoWriter.write(descriptor, baos);
        ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());

        descriptor = ModuleDescriptor.read(bb, () -> Set.of("p", "q"));

        assertTrue(descriptor.packages().size() == 2);
        assertTrue(descriptor.packages().contains("p"));
        assertTrue(descriptor.packages().contains("q"));
    }

    /**
     * Test ModuleDescriptor with a packager finder that doesn't return the
     * complete set of packages.
     */
    @Test(expectedExceptions = InvalidModuleDescriptorException.class)
    public void testReadsWithBadPackageFinder() throws Exception {
        ModuleDescriptor descriptor = ModuleDescriptor.newModule("foo")
                .requires("java.base")
                .exports("p")
                .build();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ModuleInfoWriter.write(descriptor, baos);
        ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());

        // package finder returns a set that doesn't include p
        ModuleDescriptor.read(bb, () -> Set.of("q"));
    }

    @Test(expectedExceptions = InvalidModuleDescriptorException.class)
    public void testReadFromEmptyInputStream() throws Exception {
        ModuleDescriptor.read(EMPTY_INPUT_STREAM);
    }

    @Test(expectedExceptions = IOException.class)
    public void testReadFromFailingInputStream() throws Exception {
        ModuleDescriptor.read(FAILING_INPUT_STREAM);
    }

    @Test(expectedExceptions = InvalidModuleDescriptorException.class)
    public void testReadFromEmptyBuffer() {
        ByteBuffer bb = ByteBuffer.allocate(0);
        ModuleDescriptor.read(bb);
    }

    // The requires table for java.base must be 0 length
    @Test(expectedExceptions = InvalidModuleDescriptorException.class)
    public void testReadOfJavaBaseWithRequires() {
        ModuleDescriptor descriptor
            = ModuleDescriptor.newModule("java.base")
                .requires("other")
                .build();
        ByteBuffer bb = ModuleInfoWriter.toByteBuffer(descriptor);
        ModuleDescriptor.read(bb);
    }

    // The requires table must have an entry for java.base
    @Test(expectedExceptions = InvalidModuleDescriptorException.class)
    public void testReadWithEmptyRequires() {
        ModuleDescriptor descriptor = SharedSecrets.getJavaLangModuleAccess()
                .newModuleBuilder("m1", false, Set.of()).build();
        ByteBuffer bb = ModuleInfoWriter.toByteBuffer(descriptor);
        ModuleDescriptor.read(bb);
    }

    // The requires table must have an entry for java.base
    @Test(expectedExceptions = InvalidModuleDescriptorException.class)
    public void testReadWithNoRequiresBase() {
        ModuleDescriptor descriptor = SharedSecrets.getJavaLangModuleAccess()
                .newModuleBuilder("m1", false, Set.of()).requires("m2").build();
        ByteBuffer bb = ModuleInfoWriter.toByteBuffer(descriptor);
        ModuleDescriptor.read(bb);
    }

    public void testReadWithNull() throws Exception {
        Module base = Object.class.getModule();

        try {
            ModuleDescriptor.read((InputStream)null);
            assertTrue(false);
        } catch (NullPointerException expected) { }


        try (InputStream in = base.getResourceAsStream("module-info.class")) {
            try {
                ModuleDescriptor.read(in, null);
                assertTrue(false);
            } catch (NullPointerException expected) { }
        }

        try {
            ModuleDescriptor.read((ByteBuffer)null);
            assertTrue(false);
        } catch (NullPointerException expected) { }


        try (InputStream in = base.getResourceAsStream("module-info.class")) {
            ByteBuffer bb = ByteBuffer.wrap(in.readAllBytes());
            try {
                ModuleDescriptor.read(bb, null);
                assertTrue(false);
            } catch (NullPointerException expected) { }
        }
    }


    // equals/hashCode/compareTo/toString

    public void testEqualsAndHashCode() {
        ModuleDescriptor md1 = ModuleDescriptor.newModule("m").build();
        ModuleDescriptor md2 = ModuleDescriptor.newModule("m").build();
        assertEquals(md1, md1);
        assertEquals(md1.hashCode(), md2.hashCode());
        assertTrue(md1.compareTo(md2) == 0);
        assertTrue(md2.compareTo(md1) == 0);
    }

    @DataProvider(name = "sortedModuleDescriptors")
    public Object[][] sortedModuleDescriptors() {
        return new Object[][]{

            { ModuleDescriptor.newModule("m2").build(),
              ModuleDescriptor.newModule("m1").build()
            },

            { ModuleDescriptor.newModule("m").version("2").build(),
              ModuleDescriptor.newModule("m").version("1").build()
            },

            { ModuleDescriptor.newModule("m").version("1").build(),
              ModuleDescriptor.newModule("m").build()
            },

            { ModuleDescriptor.newOpenModule("m").build(),
              ModuleDescriptor.newModule("m").build()
            },

        };
    }

    @Test(dataProvider = "sortedModuleDescriptors")
    public void testCompare(ModuleDescriptor md1, ModuleDescriptor md2) {
        assertNotEquals(md1, md2);
        assertTrue(md1.compareTo(md2) == 1);
        assertTrue(md2.compareTo(md1) == -1);
    }

    public void testToString() {
        String s = ModuleDescriptor.newModule("m1")
                .requires("m2")
                .exports("p1")
                .build()
                .toString();
        assertTrue(s.contains("m1"));
        assertTrue(s.contains("m2"));
        assertTrue(s.contains("p1"));
    }

}
