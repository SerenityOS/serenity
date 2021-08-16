/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8179531 8010319
 * @summary Check that ClassLoader.getResource works as expected in the JShell agent.
 * @modules jdk.jshell
 * @build KullaTesting TestingInputStream
 * @run testng GetResourceTest
 */

import jdk.jshell.Snippet;
import static jdk.jshell.Snippet.Status.OVERWRITTEN;
import static jdk.jshell.Snippet.Status.VALID;
import org.testng.annotations.Test;


@Test
public class GetResourceTest extends KullaTesting {

    public void checkGetResource() {
        assertEval("import java.util.Arrays;");
        assertEval("boolean match(byte[] data, byte[] snippet) {\n" +
                   "    for (int i = 0; i < data.length - snippet.length; i++) {\n" +
                   "        if (Arrays.equals(Arrays.copyOfRange(data, i, i + snippet.length), snippet)) {\n" +
                   "            return true;\n" +
                   "        }\n" +
                   "    }\n" +
                   "    return false;\n" +
                   "}");
        assertEval("boolean test() throws Exception {\n" +
                   "    Class c = new Object() {}.getClass().getEnclosingClass();\n" +
                   "    byte[] data = c.getClassLoader().getResource(c.getName().replace('.', '/') + \".class\").openStream().readAllBytes();\n" +
                   "    return match(data, \"check text\".getBytes(\"UTF-8\"));\n" +
                   "}");
        assertEval("test()", "true");
    }

    public void checkRedefine() {
        assertEval("import java.util.Arrays;");
        assertEval("boolean match(byte[] data, byte[] snippet) {\n" +
                   "    for (int i = 0; i < data.length - snippet.length; i++) {\n" +
                   "        if (Arrays.equals(Arrays.copyOfRange(data, i, i + snippet.length), snippet)) {\n" +
                   "            return true;\n" +
                   "        }\n" +
                   "    }\n" +
                   "    return false;\n" +
                   "}");
        Snippet testMethod =
                methodKey(assertEval("boolean test() throws Exception {\n" +
                                     "    return false;\n" +
                                     "}"));
        assertEval("boolean test() throws Exception {\n" +
                   "    Class c = new Object() {}.getClass().getEnclosingClass();\n" +
                   "    byte[] data = c.getClassLoader().getResource(c.getName().replace('.', '/') + \".class\").openStream().readAllBytes();\n" +
                   "    return match(data, \"updated variant\".getBytes(\"UTF-8\"));\n" +
                   "}",
                   IGNORE_VALUE,
                   null,
                   DiagCheck.DIAG_OK,
                   DiagCheck.DIAG_OK,
                   ste(MAIN_SNIPPET, VALID, VALID, true, null),
                   ste(testMethod, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("test()", "true");
    }

    public void checkResourceSize() {
        assertEval("import java.net.*;");
        assertEval("boolean test() throws Exception {\n" +
                   "    Class c = new Object() {}.getClass().getEnclosingClass();" +
                   "    URL url = c.getClassLoader().getResource(c.getName().replace('.', '/') + \".class\");\n" +
                   "    URLConnection connection = url.openConnection();\n" +
                   "    connection.connect();\n" +
                   "    return connection.getContentLength() == connection.getInputStream().readAllBytes().length;\n" +
                   "}");
        assertEval("test()", "true");
    }

    public void checkTimestampCheck() {
        assertEval("import java.net.*;");
        assertEval("import java.time.*;");
        assertEval("import java.time.format.*;");
        assertEval("long[] times(Class c) throws Exception {\n" +
                   "    URL url = c.getClassLoader().getResource(c.getName().replace('.', '/') + \".class\");\n" +
                   "    URLConnection connection = url.openConnection();\n" +
                   "    connection.connect();\n" +
                   "    return new long[] {connection.getDate(),\n" +
                   "                       connection.getLastModified()," +
                   "                       Instant.from(DateTimeFormatter.RFC_1123_DATE_TIME.parse(connection.getHeaderField(\"last-modified\"))).toEpochMilli()};\n" +
                   "}");
        Snippet testMethod =
                methodKey(assertEval("long[] test() throws Exception {\n" +
                                     "    int i = 0;\n" +
                                     "    return times(new Object() {}.getClass().getEnclosingClass());\n" +
                                     "}"));
        assertEval("long[] orig = test();");
        long s = System.currentTimeMillis();
        while ((System.currentTimeMillis() - s) < 1000) { //ensure time change:
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {}
        }
        assertEval("long[] test() throws Exception {\n" +
                   "    int i = 1;\n" +
                   "    return times(new Object() {}.getClass().getEnclosingClass());\n" +
                   "}",
                   IGNORE_VALUE,
                   null,
                   DiagCheck.DIAG_OK,
                   DiagCheck.DIAG_OK,
                   ste(MAIN_SNIPPET, VALID, VALID, false, null),
                   ste(testMethod, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("long[] nue = test();");
        assertEval("orig[0] < nue[0]", "true");
        assertEval("orig[1] < nue[1]", "true");
        assertEval("orig[0] == orig[2]", "true");
        assertEval("nue[0] == nue[2]", "true");
    }

    public void checkFieldAccess() {
        assertEval("import java.net.*;");
        assertEval("Class c = new Object() {}.getClass().getEnclosingClass();");
        assertEval("URL url = c.getClassLoader().getResource(c.getName().replace('.', '/') + \".class\");");
        assertEval("URLConnection connection = url.openConnection();");
        assertEval("connection.connect();");
        assertEval("connection.getHeaderFieldKey(0)", "\"content-length\"");
        assertEval("connection.getHeaderFieldKey(1)", "\"date\"");
        assertEval("connection.getHeaderFieldKey(2)", "\"last-modified\"");
        assertEval("connection.getHeaderFieldKey(3)", "null");
        assertEval("connection.getHeaderField(0) != null", "true");
        assertEval("connection.getHeaderField(1) != null", "true");
        assertEval("connection.getHeaderField(2) != null", "true");
        assertEval("connection.getHeaderField(3) == null", "true");
    }

    public void checkGetResources() {
        assertEval("import java.net.*;");
        assertEval("Class c = new Object() {}.getClass().getEnclosingClass();");
        assertEval("c.getClassLoader().getResources(c.getName().replace('.', '/') + \".class\").hasMoreElements()", "true");
    }

}

