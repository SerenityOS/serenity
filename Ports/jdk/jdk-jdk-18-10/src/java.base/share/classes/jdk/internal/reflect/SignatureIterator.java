/*
 * Copyright (c) 2001, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.internal.reflect;

/** Assists in iterating down a method's signature */

class SignatureIterator {
    private final String sig;
    private int idx;

    public SignatureIterator(String sig) {
        this.sig = sig;
        reset();
    }

    public void reset() {
        idx = 1;
    }

    public boolean atEnd() {
        return sig.charAt(idx) == ')';
    }

    public String next() {
        if (atEnd()) return null;
        char c = sig.charAt(idx);
        if (c != '[' && c != 'L') {
            ++idx;
            return String.valueOf(c);
        }
        // Walk forward to end of entry
        int endIdx = idx;
        if (c == '[') {
            while ((c = sig.charAt(endIdx)) == '[') {
                endIdx++;
            }
        }

        if (c == 'L') {
            while (sig.charAt(endIdx) != ';') {
                endIdx++;
            }
        }

        int beginIdx = idx;
        idx = endIdx + 1;
        return sig.substring(beginIdx, idx);
    }

    /** Should only be called when atEnd() is true. Does not change
        state of iterator. */
    public String returnType() {
        if (!atEnd()) {
            throw new InternalError("Illegal use of SignatureIterator");
        }
        return sig.substring(idx + 1, sig.length());
    }
}
