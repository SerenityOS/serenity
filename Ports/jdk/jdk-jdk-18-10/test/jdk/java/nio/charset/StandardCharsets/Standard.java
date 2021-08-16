/*
 * Copyright (c) 2011, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4884238
 * @summary Test standard charset name constants.
 * @author Mike Duigou
 * @run main Standard
 */

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.io.*;
import java.nio.charset.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Standard {

    private final static String standardCharsets[] = {
        "US-ASCII", "ISO-8859-1", "UTF-8",
        "UTF-16BE", "UTF-16LE", "UTF-16" };

    public static void realMain(String[] args) {
        check(StandardCharsets.US_ASCII instanceof Charset);
        check(StandardCharsets.ISO_8859_1 instanceof Charset);
        check(StandardCharsets.UTF_8 instanceof Charset);
        check(StandardCharsets.UTF_16BE instanceof Charset);
        check(StandardCharsets.UTF_16LE instanceof Charset);
        check(StandardCharsets.UTF_16 instanceof Charset);

        check("US-ASCII".equals(StandardCharsets.US_ASCII.name()));
        check("ISO-8859-1".equals(StandardCharsets.ISO_8859_1.name()));
        check("UTF-8".equals(StandardCharsets.UTF_8.name()));
        check("UTF-16BE".equals(StandardCharsets.UTF_16BE.name()));
        check("UTF-16LE".equals(StandardCharsets.UTF_16LE.name()));
        check("UTF-16".equals(StandardCharsets.UTF_16.name()));

        check(Charset.forName("US-ASCII") == StandardCharsets.US_ASCII);
        check(Charset.forName("ISO-8859-1") == StandardCharsets.ISO_8859_1);
        check(Charset.forName("UTF-8") == StandardCharsets.UTF_8);
        check(Charset.forName("UTF-16BE") == StandardCharsets.UTF_16BE);
        check(Charset.forName("UTF-16LE") == StandardCharsets.UTF_16LE);
        check(Charset.forName("UTF-16") == StandardCharsets.UTF_16);

        Set<String> charsets = new HashSet<>();
        Field standardCharsetFields[] = StandardCharsets.class.getFields();

        for(Field charsetField : standardCharsetFields) {
            check(StandardCharsets.class == charsetField.getDeclaringClass());
            check(Modifier.isFinal(charsetField.getModifiers()));
            check(Modifier.isStatic(charsetField.getModifiers()));
            check(Modifier.isPublic(charsetField.getModifiers()));
            Object value;
            try {
                value = charsetField.get(null);
            } catch(IllegalAccessException failure) {
                unexpected(failure);
                continue;
            }
            check(value instanceof Charset);
            charsets.add(((Charset)value).name());
        }

        check(charsets.containsAll(Arrays.asList(standardCharsets)));
        charsets.removeAll(Arrays.asList(standardCharsets));
        check(charsets.isEmpty());
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() { passed++; }
    static void fail() { failed++; Thread.dumpStack(); }
    static void fail(String msg) { System.out.println(msg); fail(); }
    static void unexpected(Throwable t) { failed++; t.printStackTrace(); }
    static void check(boolean cond) { if (cond) pass(); else fail(); }
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else {System.out.println(x + " not equal to " + y); fail();}}
    static void equal2(Object x, Object y) {equal(x, y); equal(y, x);}
    public static void main(String[] args) throws Throwable {
        try { realMain(args); } catch (Throwable t) { unexpected(t); }

        System.out.printf("%nPassed = %d, failed = %d%n%n", passed, failed);
        if (failed > 0) throw new Exception("Some tests failed");
    }
    static byte[] serializedForm(Object obj) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            new ObjectOutputStream(baos).writeObject(obj);
            return baos.toByteArray();
        } catch (IOException e) { throw new Error(e); }}
    static Object readObject(byte[] bytes)
        throws IOException, ClassNotFoundException {
        InputStream is = new ByteArrayInputStream(bytes);
        return new ObjectInputStream(is).readObject();}
    @SuppressWarnings("unchecked")
    static <T> T serialClone(T obj) {
        try { return (T) readObject(serializedForm(obj)); }
        catch (Exception e) { throw new Error(e); }}

}
