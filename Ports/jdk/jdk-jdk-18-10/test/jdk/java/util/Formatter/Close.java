/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5030064
 * @summary Basic tests for close().
 */

import java.io.Closeable;
import java.util.Formatter;
import java.util.FormatterClosedException;

public class Close {

    private static class ExpectedException extends RuntimeException {}

    private static class C implements Appendable, Closeable {
        public Appendable append(CharSequence csq) { return null; }
        public Appendable append(char c) { return null; }
        public Appendable append(CharSequence csq, int s, int e) {
            return null;
        }

        public void close() {
            throw new ExpectedException();
        }
    }

    private static class NC implements Appendable {
        public Appendable append(CharSequence csq) { return null; }
        public Appendable append(char c) { return null; }
        public Appendable append(CharSequence csq, int s, int e) {
            return null;
        }

        // method coincidentally called close()
        public void close() {
            throw new RuntimeException("NC.close should not be called");
        }
    }

    private static void test(Formatter f) {
        if (f.out() instanceof C) {
            // C.close() called since C implements Closeable
            try {
                f.close();
                throw new RuntimeException("C.close not called");
            } catch (ExpectedException x) {
                System.out.println("  C.close called");
            }
        } else {
            // NC.close() not called since NC does not implement Closeable
            f.close();
        }

        // Formatter is a Closeable
        if (!(f instanceof Closeable))
            throw new RuntimeException("Formatter is not a Closeable");

        // multiple close() does not throw a FormatterClosedException
        f.close();
        try {
            f.close();
            System.out.println("  FormatterClosedException not thrown");
        } catch (FormatterClosedException x) {
            throw new RuntimeException("FormatterClosedException thrown");
        }
    }

    public static void main(String [] args) {
        System.out.println("testing Closeable");
        test(new Formatter(new C()));
        System.out.println("testing non-Closeable");
        test(new Formatter(new NC()));
    }
}
