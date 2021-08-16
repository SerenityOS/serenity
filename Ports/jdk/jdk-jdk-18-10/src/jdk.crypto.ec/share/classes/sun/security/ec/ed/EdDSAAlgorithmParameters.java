/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.ec.ed;

import java.io.IOException;
import java.security.AlgorithmParametersSpi;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.EdDSAParameterSpec;
import java.security.spec.InvalidParameterSpecException;

/**
 * This AlgorithmParametersSpi only supports NamedParameterSpec.
 * EdDSAParameterSpec is not support because there is not ASN.1 format
 */

public class EdDSAAlgorithmParameters extends AlgorithmParametersSpi {

    EdDSAParameterSpec edspec;

    // If no curve is provide, wait engineInit() to provide one.
    public EdDSAAlgorithmParameters() {
    }

    /**
     * NamedParameterSpec can only be used if curve was not specified
     * as part of getInstance(EdDSA).  If the curve was used, engineInit will
     * throws an exception for being already initialized.
     * EdDSAParameterSpec is not support because there is not ASN.1 format
     *
     * @param paramSpec NamedParameterSpec curve.
     *
     * @throws InvalidParameterSpecException
     */
    @Override
    protected void engineInit(AlgorithmParameterSpec paramSpec)
            throws InvalidParameterSpecException {
        if (paramSpec instanceof EdDSAParameterSpec) {
            edspec = (EdDSAParameterSpec)paramSpec;
            return;
        }
        throw new InvalidParameterSpecException(
                "Unknown AlgorithmParameterSpec");
    }

    @Override
    protected void engineInit(byte[] params) throws IOException {
        throw new IOException(
                "EdDSA does not support parameters as a byte array.");
    }

    @Override
    protected void engineInit(byte[] params, String format) throws IOException {
        engineInit(params);
    }

    @Override
    protected <T extends AlgorithmParameterSpec> T engineGetParameterSpec(
            Class<T> paramSpec) throws InvalidParameterSpecException {

        if (paramSpec.isAssignableFrom(ECParameterSpec.class)) {
            return paramSpec.cast(edspec);
        }
        throw new InvalidParameterSpecException(
                "Only EDDSAParameterSpec supported.");
    }

    @Override
    protected byte[] engineGetEncoded() throws IOException {
        throw new IOException(
                "EdDSA does not support parameters as a byte array.");
    }

    @Override
    protected byte[] engineGetEncoded(String format) throws IOException {
        throw new IOException(
                "EdDSA does not support parameters as a byte array.");
    }

    @Override
    protected String engineToString() {
        return edspec.toString();
    }
}
