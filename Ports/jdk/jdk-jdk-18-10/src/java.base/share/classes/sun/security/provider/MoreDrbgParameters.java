/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.provider;

import java.io.IOException;
import java.io.Serializable;
import java.security.DrbgParameters;
import java.security.SecureRandomParameters;

/**
 * Exported and non-exported parameters that can be used by DRBGs.
 */
public class MoreDrbgParameters implements SecureRandomParameters, Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 9L;

    final transient EntropySource es;

    final String mech;
    final String algorithm;
    final boolean usedf;
    final int strength;
    final DrbgParameters.Capability capability;

    // The following 2 fields will be reassigned in readObject and
    // thus cannot be final
    byte[] nonce;
    byte[] personalizationString;

    /**
     * Creates a new {@code MoreDrbgParameters} object.
     *
     * @param es the {@link EntropySource} to use. If set to {@code null},
     *           a default entropy source will be used.
     * @param mech mech name. If set to {@code null}, the one in
     *             securerandom.drbg.config is used. This argument is ignored
     *             when passing to HashDrbg/HmacDrbg/CtrDrbg.
     * @param algorithm the requested algorithm to use. If set to {@code null},
     *                  the algorithm will be decided by strength.
     * @param nonce the nonce to use. If set to {@code null},
     *              a nonce will be assigned.
     * @param usedf whether a derivation function should be used
     * @param config a {@link DrbgParameters.Instantiation} object
     */
    public MoreDrbgParameters(EntropySource es, String mech,
                              String algorithm, byte[] nonce, boolean usedf,
                              DrbgParameters.Instantiation config) {
        this.mech = mech;
        this.algorithm = algorithm;
        this.es = es;
        this.nonce = (nonce == null) ? null : nonce.clone();
        this.usedf = usedf;

        this.strength = config.getStrength();
        this.capability = config.getCapability();
        this.personalizationString = config.getPersonalizationString();
    }

    @Override
    public String toString() {
        return mech + "," + algorithm + "," + usedf + "," + strength
                + "," + capability + "," + personalizationString;
    }

    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();
        if (nonce != null) {
            nonce = nonce.clone();
        }
        if (personalizationString != null) {
            personalizationString = personalizationString.clone();
        }
        if (capability == null) {
            throw new IllegalArgumentException("Input data is corrupted");
        }
    }
}
