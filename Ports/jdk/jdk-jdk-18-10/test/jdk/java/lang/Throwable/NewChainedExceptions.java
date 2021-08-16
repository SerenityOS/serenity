/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4836801
 * @summary Basic test for new chained exceptions added in 1.5
 * @author  Josh Bloch
 */

public class NewChainedExceptions {
    public static void main(String args[]) {
        Throwable interior = new Exception();
        String    message  = "Good heavens!";

        try {
            throw new IllegalStateException(message, interior);
        } catch(IllegalStateException e) {
            if (!(e.getCause() == interior && e.getMessage() == message))
                throw new RuntimeException("1");
        }

        try {
            throw new IllegalStateException(interior);
        } catch(IllegalStateException e) {
            if (!(e.getCause() == interior &&
                  e.getMessage().equals(interior.toString())))
                throw new RuntimeException("2");
        }

        try {
            throw new IllegalArgumentException(message, interior);
        } catch(IllegalArgumentException e) {
            if (!(e.getCause() == interior && e.getMessage() == message))
                throw new RuntimeException("3");
        }

        try {
            throw new IllegalArgumentException(interior);
        } catch(IllegalArgumentException e) {
            if (!(e.getCause() == interior &&
                  e.getMessage().equals(interior.toString())))
                throw new RuntimeException("4");
        }

        try {
            throw new UnsupportedOperationException(message, interior);
        } catch(UnsupportedOperationException e) {
            if (!(e.getCause() == interior && e.getMessage() == message))
                throw new RuntimeException("5");
        }

        try {
            throw new UnsupportedOperationException(interior);
        } catch(UnsupportedOperationException e) {
            if (!(e.getCause() == interior &&
                  e.getMessage().equals(interior.toString())))
                throw new RuntimeException("6");
        }
    }
}
