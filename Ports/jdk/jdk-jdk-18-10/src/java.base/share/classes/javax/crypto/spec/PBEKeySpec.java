/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

package javax.crypto.spec;

import java.security.spec.KeySpec;
import java.util.Arrays;

/**
 * A user-chosen password that can be used with password-based encryption
 * (<i>PBE</i>).
 *
 * <p>The password can be viewed as some kind of raw key material, from which
 * the encryption mechanism that uses it derives a cryptographic key.
 *
 * <p>Different PBE mechanisms may consume different bits of each password
 * character. For example, the PBE mechanism defined in
 * <a href="http://www.ietf.org/rfc/rfc2898.txt">
 * PKCS #5</a> looks at only the low order 8 bits of each character, whereas
 * PKCS #12 looks at all 16 bits of each character.
 *
 * <p>You convert the password characters to a PBE key by creating an
 * instance of the appropriate secret-key factory. For example, a secret-key
 * factory for PKCS #5 will construct a PBE key from only the low order 8 bits
 * of each password character, whereas a secret-key factory for PKCS #12 will
 * take all 16 bits of each character.
 *
 * <p>Also note that this class stores passwords as char arrays instead of
 * <code>String</code> objects (which would seem more logical), because the
 * String class is immutable and there is no way to overwrite its
 * internal value when the password stored in it is no longer needed. Hence,
 * this class requests the password as a char array, so it can be overwritten
 * when done.
 *
 * @author Jan Luehe
 * @author Valerie Peng
 *
 * @see javax.crypto.SecretKeyFactory
 * @see PBEParameterSpec
 * @since 1.4
 */
public class PBEKeySpec implements KeySpec {

    private char[] password;
    private byte[] salt = null;
    private int iterationCount = 0;
    private int keyLength = 0;

    /**
     * Constructor that takes a password. An empty char[] is used if
     * null is specified.
     *
     * <p> Note: <code>password</code> is cloned before it is stored in
     * the new <code>PBEKeySpec</code> object.
     *
     * @param password the password.
     */
    public PBEKeySpec(char[] password) {
        if ((password == null) || (password.length == 0)) {
            this.password = new char[0];
        } else {
            this.password = password.clone();
        }
    }


    /**
     * Constructor that takes a password, salt, iteration count, and
     * to-be-derived key length for generating PBEKey of variable-key-size
     * PBE ciphers.  An empty char[] is used if null is specified for
     * <code>password</code>.
     *
     * <p> Note: the <code>password</code> and <code>salt</code>
     * are cloned before they are stored in
     * the new <code>PBEKeySpec</code> object.
     *
     * @param password the password.
     * @param salt the salt.
     * @param iterationCount the iteration count.
     * @param keyLength the to-be-derived key length.
     * @exception NullPointerException if <code>salt</code> is null.
     * @exception IllegalArgumentException if <code>salt</code> is empty,
     * i.e. 0-length, <code>iterationCount</code> or
     * <code>keyLength</code> is not positive.
     */
    public PBEKeySpec(char[] password, byte[] salt, int iterationCount,
        int keyLength) {
        if ((password == null) || (password.length == 0)) {
            this.password = new char[0];
        } else {
            this.password = password.clone();
        }
        if (salt == null) {
            throw new NullPointerException("the salt parameter " +
                                            "must be non-null");
        } else if (salt.length == 0) {
            throw new IllegalArgumentException("the salt parameter " +
                                                "must not be empty");
        } else {
            this.salt = salt.clone();
        }
        if (iterationCount<=0) {
            throw new IllegalArgumentException("invalid iterationCount value");
        }
        if (keyLength<=0) {
            throw new IllegalArgumentException("invalid keyLength value");
        }
        this.iterationCount = iterationCount;
        this.keyLength = keyLength;
    }


    /**
     * Constructor that takes a password, salt, iteration count for
     * generating PBEKey of fixed-key-size PBE ciphers. An empty
     * char[] is used if null is specified for <code>password</code>.
     *
     * <p> Note: the <code>password</code> and <code>salt</code>
     * are cloned before they are stored in the new
     * <code>PBEKeySpec</code> object.
     *
     * @param password the password.
     * @param salt the salt.
     * @param iterationCount the iteration count.
     * @exception NullPointerException if <code>salt</code> is null.
     * @exception IllegalArgumentException if <code>salt</code> is empty,
     * i.e. 0-length, or <code>iterationCount</code> is not positive.
     */
    public PBEKeySpec(char[] password, byte[] salt, int iterationCount) {
        if ((password == null) || (password.length == 0)) {
            this.password = new char[0];
        } else {
            this.password = password.clone();
        }
        if (salt == null) {
            throw new NullPointerException("the salt parameter " +
                                            "must be non-null");
        } else if (salt.length == 0) {
            throw new IllegalArgumentException("the salt parameter " +
                                                "must not be empty");
        } else {
            this.salt = salt.clone();
        }
        if (iterationCount<=0) {
            throw new IllegalArgumentException("invalid iterationCount value");
        }
        this.iterationCount = iterationCount;
    }

    /**
     * Clears the internal copy of the password.
     *
     */
    public final void clearPassword() {
        if (password != null) {
            Arrays.fill(password, ' ');
            password = null;
        }
    }

    /**
     * Returns a copy of the password.
     *
     * <p> Note: this method returns a copy of the password. It is
     * the caller's responsibility to zero out the password information after
     * it is no longer needed.
     *
     * @exception IllegalStateException if password has been cleared by
     * calling <code>clearPassword</code> method.
     * @return the password.
     */
    public final char[] getPassword() {
        if (password == null) {
            throw new IllegalStateException("password has been cleared");
        }
        return password.clone();
    }

    /**
     * Returns a copy of the salt or null if not specified.
     *
     * <p> Note: this method should return a copy of the salt. It is
     * the caller's responsibility to zero out the salt information after
     * it is no longer needed.
     *
     * @return the salt.
     */
    public final byte[] getSalt() {
        if (salt != null) {
            return salt.clone();
        } else {
            return null;
        }
    }

    /**
     * Returns the iteration count or 0 if not specified.
     *
     * @return the iteration count.
     */
    public final int getIterationCount() {
        return iterationCount;
    }

    /**
     * Returns the to-be-derived key length or 0 if not specified.
     *
     * <p> Note: this is used to indicate the preference on key length
     * for variable-key-size ciphers. The actual key size depends on
     * each provider's implementation.
     *
     * @return the to-be-derived key length.
     */
    public final int getKeyLength() {
        return keyLength;
    }
}
