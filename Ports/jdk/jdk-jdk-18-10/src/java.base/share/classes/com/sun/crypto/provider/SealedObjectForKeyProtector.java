/*
 * Copyright (c) 1998, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.crypto.provider;

import jdk.internal.access.SharedSecrets;

import java.io.*;
import java.security.*;
import javax.crypto.*;

final class SealedObjectForKeyProtector extends SealedObject {

    @java.io.Serial
    static final long serialVersionUID = -3650226485480866989L;

    /**
     * The InputStreamFilter for a Key object inside this SealedObject. It can
     * be either provided as a {@link Security} property or a system property
     * (when provided as latter, it shadows the former). If the result of this
     * filter is {@link java.io.ObjectInputFilter.Status.UNDECIDED}, the system
     * level filter defined by jdk.serialFilter will be consulted. The value
     * of this property uses the same format of jdk.serialFilter.
     */
    private static final String KEY_SERIAL_FILTER = "jceks.key.serialFilter";

    SealedObjectForKeyProtector(Serializable object, Cipher c)
            throws IOException, IllegalBlockSizeException {
        super(object, c);
    }

    SealedObjectForKeyProtector(SealedObject so) {
        super(so);
    }

    AlgorithmParameters getParameters() {
        AlgorithmParameters params = null;
        if (super.encodedParams != null) {
            try {
                params = AlgorithmParameters.getInstance("PBE",
                    SunJCE.getInstance());
                params.init(super.encodedParams);
            } catch (NoSuchAlgorithmException nsae) {
                throw new RuntimeException(
                    "SunJCE provider is not configured properly");
            } catch (IOException io) {
                throw new RuntimeException("Parameter failure: "+
                    io.getMessage());
            }
        }
        return params;
    }

    @SuppressWarnings("removal")
    final Key getKey(Cipher c, int maxLength)
            throws IOException, ClassNotFoundException, IllegalBlockSizeException,
            BadPaddingException {

        try (ObjectInputStream ois = SharedSecrets.getJavaxCryptoSealedObjectAccess()
                .getExtObjectInputStream(this, c)) {
            AccessController.doPrivileged(
                    (PrivilegedAction<Void>) () -> {
                        ois.setObjectInputFilter(new DeserializationChecker(maxLength));
                        return null;
                    });
            try {
                @SuppressWarnings("unchecked")
                Key t = (Key) ois.readObject();
                return t;
            } catch (InvalidClassException ice) {
                String msg = ice.getMessage();
                if (msg.contains("REJECTED")) {
                    throw new IOException("Rejected by the"
                            + " jceks.key.serialFilter or jdk.serialFilter"
                            + " property", ice);
                } else {
                    throw ice;
                }
            }
        }
    }

    /**
     * The filter for the content of a SealedObjectForKeyProtector.
     *
     * First, the jceks.key.serialFilter will be consulted. If the result
     * is UNDECIDED, the system level jdk.serialFilter will be consulted.
     */
    private static class DeserializationChecker implements ObjectInputFilter {

        private static final ObjectInputFilter OWN_FILTER;

        static {
            @SuppressWarnings("removal")
            String prop = AccessController.doPrivileged(
                    (PrivilegedAction<String>) () -> {
                        String tmp = System.getProperty(KEY_SERIAL_FILTER);
                        if (tmp != null) {
                            return tmp;
                        } else {
                            return Security.getProperty(KEY_SERIAL_FILTER);
                        }
                    });
            OWN_FILTER = prop == null
                    ? null
                    : ObjectInputFilter.Config.createFilter(prop);
        }

        // Maximum possible length of anything inside
        private final int maxLength;

        private DeserializationChecker(int maxLength) {
            this.maxLength = maxLength;
        }

        @Override
        public ObjectInputFilter.Status checkInput(
                ObjectInputFilter.FilterInfo info) {

            if (info.arrayLength() > maxLength) {
                return Status.REJECTED;
            }

            if (info.serialClass() == Object.class) {
                return Status.UNDECIDED;
            }

            if (OWN_FILTER != null) {
                Status result = OWN_FILTER.checkInput(info);
                if (result != Status.UNDECIDED) {
                    return result;
                }
            }

            ObjectInputFilter defaultFilter =
                    ObjectInputFilter.Config.getSerialFilter();
            if (defaultFilter != null) {
                return defaultFilter.checkInput(info);
            }

            return Status.UNDECIDED;
        }
    }
}
