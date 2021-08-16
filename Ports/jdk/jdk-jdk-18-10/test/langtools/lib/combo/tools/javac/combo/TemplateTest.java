/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

package tools.javac.combo;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * TemplateTest
 */
@Test
public class TemplateTest {
    Map<String, Template> vars = new HashMap<>();

    @BeforeTest
    void before() { vars.clear(); }

    private void assertTemplate(String expected, String template) {
        String result = Template.expandTemplate(template, vars);
        assertEquals(result, expected, "for " + template);
    }

    private String dotIf(String s) {
        return s == null || s.isEmpty() ? "" : "." + s;
    }

    public void testTemplateExpansion() {
        vars.put("A", s -> "a" + dotIf(s));
        vars.put("B", s -> "b" + dotIf(s));
        vars.put("C", s -> "#{A}#{B}");
        vars.put("D", s -> "#{A" + dotIf(s) + "}#{B" + dotIf(s) + "}");
        vars.put("_D", s -> "d");

        assertTemplate("", "");
        assertTemplate("foo", "foo");
        assertTemplate("a", "#{A}");
        assertTemplate("a", "#{A.}");
        assertTemplate("a.FOO", "#{A.FOO}");
        assertTemplate("aa", "#{A}#{A}");
        assertTemplate("ab", "#{C}");
        assertTemplate("ab", "#{C.FOO}");
        assertTemplate("ab", "#{C.}");
        assertTemplate("a.FOOb.FOO", "#{D.FOO}");
        assertTemplate("ab", "#{D}");
        assertTemplate("d", "#{_D}");
        assertTemplate("#{A", "#{A");
    }

    public void testIndexedTemplate() {
        vars.put("A[0]", s -> "a" );
        vars.put("A[1]", s -> "b" );
        vars.put("A[2]", s -> "c" );
        vars.put("X", s -> "0");
        assertTemplate("a", "#{A[0]}");
        assertTemplate("b", "#{A[1]}");
        assertTemplate("c", "#{A[2]}");
    }

    public void testAngleBrackets() {
        vars.put("X", s -> "xyz");
        assertTemplate("List<String> ls = xyz;", "List<String> ls = #{X};");
    }

    @Test(expectedExceptions = IllegalStateException.class )
    public void testUnknownKey() {
        assertTemplate("#{Q}", "#{Q}");
    }
}
