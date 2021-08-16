/*
 * Copyright (c) 2004, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4796780
 * @summary The class MLetContentTest becomes public
 * @author Shanliang JIANG
 *
 * @run clean MLetContentTest
 * @run build MLetContentTest
 * @run main MLetContentTest
 */

import java.util.*;
import java.net.*;

import javax.management.loading.*;

public class MLetContentTest {
    public static void main(String[] args) throws Exception {
        System.out.println(">>> General test for the public class MLetContent.");

        Map<String,String> attributes = new HashMap();
        attributes.put("archive", archive);
        attributes.put("Archive", "hahaha");

        attributes.put("code", code);
        attributes.put("codE", "hihi");

        attributes.put("object", object);
        attributes.put("obJect", "toto");

        attributes.put("name", name);
        attributes.put("NAME", "titi");

        attributes.put("version", version);
        attributes.put("VeRsIoN", "tttt");

        List<String> types = new ArrayList();
        types.add("my type");

        List<String> values = new ArrayList();
        values.add("my values");

        URL url = new URL(baseUrl+myfile);
        MLetContent content = new MLetContent(url, attributes, types, values);

        if (!attributes.equals(content.getAttributes())) {
            throw new RuntimeException("The user specific attributes are changed.");
        }

        if (!url.equals(content.getDocumentBase())) {
            throw new RuntimeException("The user specific document bas is changed.");
        }

        if (!archive.equals(content.getJarFiles())) {
            throw new RuntimeException("The user specific archive files are changed.");
        }

        if (!code.equals(content.getCode())) {
            throw new RuntimeException("The user specific code is changed.");
        }

        if (!object.equals(content.getSerializedObject())) {
            throw new RuntimeException("The user specific object is changed.");
        }

        if (!name.equals(content.getName())) {
            throw new RuntimeException("The user specific name is changed.");
        }

        if (!version.equals(content.getVersion())) {
            throw new RuntimeException("The user specific version is changed.");
        }

        if (!types.equals(content.getParameterTypes())) {
            throw new RuntimeException("The user specific types are changed.");
        }

        if (!values.equals(content.getParameterValues())) {
            throw new RuntimeException("The user specific values are changed.");
        }

        if (!baseUrl.equals(content.getCodeBase().toString())) {
            throw new RuntimeException("The user specific base url are changed.");
        }

        url = new URL(baseUrl);
        attributes.put("codebase", codebase);
        content = new MLetContent(url, attributes, types, values);

        if (!content.getCodeBase().toString().equals(baseUrl+codebase)) {
            throw new RuntimeException("The user specific base url are changed.");
        }

        final MyMLet myMlet = new MyMLet();

        if (myMlet.check(null, null, null, content) != content.getCodeBase()) {
            throw new RuntimeException("Failed to overrid the protected methed check");
        }

        System.out.println(">>> The test is well passed.");
    }

    private static class MyMLet extends MLet {
        public URL check(String version,
                         URL codebase,
                         String jarfile,
                         MLetContent content) {
            return content.getCodeBase();
        }
    }

    private static final String archive = "my jarfile";
    private static final String code = "my code";
    private static final String object = "my object";
    private static final String name = "my name";
    private static final String version = "my version";

    private static final String myfile = "My file";
    private static final String baseUrl = "file:/tmp/test/";

    private final static String codebase = "my code base/";
}
