/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.security.GeneralSecurityException;
import javax.crypto.Cipher;

/**
 * PBEWrapper is an abstract class for all concrete PBE Cipher wrappers.
 */
public abstract class AbstractPBEWrapper {
    /**
     * Iteration count.
     */
    public static final int DEFAULT_ITERATION = 1000;

    public static final String PBKDF2 = "PBKDF2";
    public static final String AES = "AES";
    public static final String DEFAULT = "default";

    /**
     * transformation the name of the transformation, e.g.,
     * DES/CBC/PKCS5Padding
     */
    protected final String transformation;

    /**
     * the standard name of the requested secret-key algorithm.
     */
    protected final String baseAlgo;

    /**
     * The contents of salt are copied to protect against subsequent
     * modification.
     */
    protected final byte[] salt;

    /**
     * Password.
     */
    protected final String password;

    /**
     * PBEWrapper creator.
     *
     * @param algo PBE algorithm to test
     * @param passwd a password phrase
     * @return PBEWrapper in accordance to requested algo.
     * @throws GeneralSecurityException all exceptions are thrown.
     */
    public static AbstractPBEWrapper createWrapper(PBEAlgorithm algo, String passwd)
            throws GeneralSecurityException {
        switch (algo.type) {
            case PBKDF2:
                return new PBKDF2Wrapper(algo, passwd);
            case AES:
                return new AESPBEWrapper(algo, passwd);
            default:
                return new DefaultPBEWrapper(algo, passwd);
        }
    }

    /**
     * PBEWrapper constructor.
     *
     * @param algo algorithm to wrap
     * @param password password phrase
     * @param saltSize salt size (defined in subclasses)
     */
    protected AbstractPBEWrapper(PBEAlgorithm algo, String password, int saltSize) {
        this.transformation = algo.getTransformation();
        this.baseAlgo = algo.baseAlgo;
        this.salt = TestUtilities.generateBytes(saltSize);
        this.password = password;
    }

    /**
     * Initialize Cipher object for the operation requested in the mode parameter.
     *
     * @param mode encryption or decryption
     * @return a cipher initialize by mode.
     * @throws GeneralSecurityException all security exceptions are thrown.
     */
    protected abstract Cipher initCipher(int mode) throws GeneralSecurityException;
}
