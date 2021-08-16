/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
package sun.security.util;

import java.security.spec.AlgorithmParameterSpec;

import sun.security.util.ObjectIdentifier;

/**
 * This immutable class is used when randomly generating a key pair and the
 * consumer only specifies the length of the key and therefore a curve for that
 * key size must be picked from a the list of supported curves using this spec.
 *
 * @see AlgorithmParameterSpec
 * @see ECGenParameterSpec
 */
public class ECKeySizeParameterSpec implements AlgorithmParameterSpec {

    private int keySize;

    /**
     * Creates a parameter specification for EC curve
     * generation using a standard (or predefined) key size
     * <code>keySize</code> in order to generate the corresponding
     * (precomputed) elliptic curve.
     * <p>
     * Note, if the curve of the specified length is not supported,
     * <code>AlgorithmParameters.init</code> will throw an exception.
     *
     * @param keySize the key size of the curve to lookup
     */
    public ECKeySizeParameterSpec(int keySize) {
        this.keySize = keySize;
    }

    /**
     * Returns the key size of this spec.
     *
     * @return the standard or predefined key size.
     */
    public int getKeySize() {
        return keySize;
    }
}
