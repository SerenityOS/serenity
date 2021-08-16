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
 * @bug 5030063
 * @summary Basic tests for flush().
 */

import java.io.Closeable;
import java.io.Flushable;
import java.util.Formatter;
import java.util.FormatterClosedException;

public class Flush {

    private static class ExpectedException extends RuntimeException {}

    private static class F implements Appendable, Closeable, Flushable {
        public Appendable append(CharSequence csq) { return null; }
        public Appendable append(char c) { return null; }
        public Appendable append(CharSequence csq, int s, int e) {
            return null;
        }
        public void close() {}

        public void flush() {
            throw new ExpectedException();
        }
    }

    private static class NF implements Appendable, Closeable {
        public Appendable append(CharSequence csq) { return null; }
        public Appendable append(char c) { return null; }
        public Appendable append(CharSequence csq, int s, int e) {
            return null;
        }
        public void close() {}

        // method coincidentally called flush()
        public void flush() {
            throw new RuntimeException("NF.flush should not be called");
        }
    }

    private static void test(Formatter f) {
        if (f.out() instanceof F) {
            // F.flush() called since F implements Flushable
            try {
                f.flush();
                throw new RuntimeException("F.flush not called");
            } catch (ExpectedException x) {
                System.out.println("  F.flush called");
            }
        } else {
            // NF.flush() not called since NF does not implement Flushable
            f.flush();
        }

        // Formatter is a Flushable
        if (!(f instanceof Flushable))
            throw new RuntimeException("Formatter is not a Flushable");

        // flush() after close() throws a FormatterClosedException
        f.close();
        try {
            f.flush();
            throw new RuntimeException("FormatterClosedException not thrown");
        } catch (FormatterClosedException x) {
            System.out.println("  FormatterClosedException thrown");
        }
    }

    public static void main(String [] args) {
        System.out.println("testing Flushable");
        test(new Formatter(new F()));
        System.out.println("testing non-Flushable");
        test(new Formatter(new NF()));
    }
}
