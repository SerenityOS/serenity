/*
 * Copyright (c) 1996, 2019, Oracle and/or its affiliates. All rights reserved.
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

package sun.security.x509;

import java.io.*;
import java.util.Arrays;
import java.util.Properties;
import java.security.Key;
import java.security.PublicKey;
import java.security.KeyFactory;
import java.security.Security;
import java.security.Provider;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import sun.security.util.HexDumpEncoder;
import sun.security.util.*;

/**
 * Holds an X.509 key, for example a public key found in an X.509
 * certificate.  Includes a description of the algorithm to be used
 * with the key; these keys normally are used as
 * "SubjectPublicKeyInfo".
 *
 * <P>While this class can represent any kind of X.509 key, it may be
 * desirable to provide subclasses which understand how to parse keying
 * data.   For example, RSA public keys have two members, one for the
 * public modulus and one for the prime exponent.  If such a class is
 * provided, it is used when parsing X.509 keys.  If one is not provided,
 * the key still parses correctly.
 *
 * @author David Brownell
 */
public class X509Key implements PublicKey {

    /** use serialVersionUID from JDK 1.1. for interoperability */
    @java.io.Serial
    private static final long serialVersionUID = -5359250853002055002L;

    /* The algorithm information (name, parameters, etc). */
    protected AlgorithmId algid;

    /**
     * The key bytes, without the algorithm information.
     * @deprecated Use the BitArray form which does not require keys to
     * be byte aligned.
     * @see sun.security.x509.X509Key#setKey(BitArray)
     * @see sun.security.x509.X509Key#getKey()
     */
    @Deprecated
    protected byte[] key = null;

    /*
     * The number of bits unused in the last byte of the key.
     * Added to keep the byte[] key form consistent with the BitArray
     * form. Can de deleted when byte[] key is deleted.
     */
    @Deprecated
    private int unusedBits = 0;

    /* BitArray form of key */
    private transient BitArray bitStringKey = null;

    /* The encoding for the key. */
    protected byte[] encodedKey;

    /**
     * Default constructor.  The key constructed must have its key
     * and algorithm initialized before it may be used, for example
     * by using <code>decode</code>.
     */
    public X509Key() { }

    /*
     * Build and initialize as a "default" key.  All X.509 key
     * data is stored and transmitted losslessly, but no knowledge
     * about this particular algorithm is available.
     */
    private X509Key(AlgorithmId algid, BitArray key)
    throws InvalidKeyException {
        this.algid = algid;
        setKey(key);
        encode();
    }

    /**
     * Sets the key in the BitArray form.
     */
    protected void setKey(BitArray key) {
        this.bitStringKey = (BitArray)key.clone();

        /*
         * Do this to keep the byte array form consistent with
         * this. Can delete when byte[] key is deleted.
         */
        this.key = key.toByteArray();
        int remaining = key.length() % 8;
        this.unusedBits =
            ((remaining == 0) ? 0 : 8 - remaining);
    }

    /**
     * Gets the key. The key may or may not be byte aligned.
     * @return a BitArray containing the key.
     */
    protected BitArray getKey() {
        /*
         * Do this for consistency in case a subclass
         * modifies byte[] key directly. Remove when
         * byte[] key is deleted.
         * Note: the consistency checks fail when the subclass
         * modifies a non byte-aligned key (into a byte-aligned key)
         * using the deprecated byte[] key field.
         */
        this.bitStringKey = new BitArray(
                          this.key.length * 8 - this.unusedBits,
                          this.key);

        return (BitArray)bitStringKey.clone();
    }

    /**
     * Construct X.509 subject public key from a DER value.  If
     * the runtime environment is configured with a specific class for
     * this kind of key, a subclass is returned.  Otherwise, a generic
     * X509Key object is returned.
     *
     * <P>This mechanism gurantees that keys (and algorithms) may be
     * freely manipulated and transferred, without risk of losing
     * information.  Also, when a key (or algorithm) needs some special
     * handling, that specific need can be accomodated.
     *
     * @param in the DER-encoded SubjectPublicKeyInfo value
     * @exception IOException on data format errors
     */
    public static PublicKey parse(DerValue in) throws IOException
    {
        AlgorithmId     algorithm;
        PublicKey       subjectKey;

        if (in.tag != DerValue.tag_Sequence)
            throw new IOException("corrupt subject key");

        algorithm = AlgorithmId.parse(in.data.getDerValue());
        try {
            subjectKey = buildX509Key(algorithm,
                                      in.data.getUnalignedBitString());

        } catch (InvalidKeyException e) {
            throw new IOException("subject key, " + e.getMessage(), e);
        }

        if (in.data.available() != 0)
            throw new IOException("excess subject key");
        return subjectKey;
    }

    /**
     * Parse the key bits.  This may be redefined by subclasses to take
     * advantage of structure within the key.  For example, RSA public
     * keys encapsulate two unsigned integers (modulus and exponent) as
     * DER values within the <code>key</code> bits; Diffie-Hellman and
     * DSS/DSA keys encapsulate a single unsigned integer.
     *
     * <P>This function is called when creating X.509 SubjectPublicKeyInfo
     * values using the X509Key member functions, such as <code>parse</code>
     * and <code>decode</code>.
     *
     * @exception IOException on parsing errors.
     * @exception InvalidKeyException on invalid key encodings.
     */
    protected void parseKeyBits() throws IOException, InvalidKeyException {
        encode();
    }

    /*
     * Factory interface, building the kind of key associated with this
     * specific algorithm ID or else returning this generic base class.
     * See the description above.
     */
    static PublicKey buildX509Key(AlgorithmId algid, BitArray key)
      throws IOException, InvalidKeyException
    {
        /*
         * Use the algid and key parameters to produce the ASN.1 encoding
         * of the key, which will then be used as the input to the
         * key factory.
         */
        DerOutputStream x509EncodedKeyStream = new DerOutputStream();
        encode(x509EncodedKeyStream, algid, key);
        X509EncodedKeySpec x509KeySpec
            = new X509EncodedKeySpec(x509EncodedKeyStream.toByteArray());

        try {
            // Instantiate the key factory of the appropriate algorithm
            KeyFactory keyFac = KeyFactory.getInstance(algid.getName());

            // Generate the public key
            return keyFac.generatePublic(x509KeySpec);
        } catch (NoSuchAlgorithmException e) {
            // Return generic X509Key with opaque key data (see below)
        } catch (InvalidKeySpecException e) {
            throw new InvalidKeyException(e.getMessage(), e);
        }

        /*
         * Try again using JDK1.1-style for backwards compatibility.
         */
        String classname = "";
        try {
            Properties props;
            String keytype;
            Provider sunProvider;

            sunProvider = Security.getProvider("SUN");
            if (sunProvider == null)
                throw new InstantiationException();
            classname = sunProvider.getProperty("PublicKey.X.509." +
              algid.getName());
            if (classname == null) {
                throw new InstantiationException();
            }

            Class<?> keyClass = null;
            try {
                keyClass = Class.forName(classname);
            } catch (ClassNotFoundException e) {
                ClassLoader cl = ClassLoader.getSystemClassLoader();
                if (cl != null) {
                    keyClass = cl.loadClass(classname);
                }
            }

            @SuppressWarnings("deprecation")
            Object      inst = (keyClass != null) ? keyClass.newInstance() : null;
            X509Key     result;

            if (inst instanceof X509Key) {
                result = (X509Key) inst;
                result.algid = algid;
                result.setKey(key);
                result.parseKeyBits();
                return result;
            }
        } catch (ClassNotFoundException e) {
        } catch (InstantiationException e) {
        } catch (IllegalAccessException e) {
            // this should not happen.
            throw new IOException (classname + " [internal error]");
        }

        X509Key result = new X509Key(algid, key);
        return result;
    }

    /**
     * Returns the algorithm to be used with this key.
     */
    public String getAlgorithm() {
        return algid.getName();
    }

    /**
     * Returns the algorithm ID to be used with this key.
     */
    public AlgorithmId  getAlgorithmId() { return algid; }

    /**
     * Encode SubjectPublicKeyInfo sequence on the DER output stream.
     *
     * @exception IOException on encoding errors.
     */
    public final void encode(DerOutputStream out) throws IOException
    {
        encode(out, this.algid, getKey());
    }

    /**
     * Returns the DER-encoded form of the key as a byte array.
     */
    public byte[] getEncoded() {
        try {
            return getEncodedInternal().clone();
        } catch (InvalidKeyException e) {
            // XXX
        }
        return null;
    }

    public byte[] getEncodedInternal() throws InvalidKeyException {
        byte[] encoded = encodedKey;
        if (encoded == null) {
            try {
                DerOutputStream out = new DerOutputStream();
                encode(out);
                encoded = out.toByteArray();
            } catch (IOException e) {
                throw new InvalidKeyException("IOException : " +
                                               e.getMessage());
            }
            encodedKey = encoded;
        }
        return encoded;
    }

    /**
     * Returns the format for this key: "X.509"
     */
    public String getFormat() {
        return "X.509";
    }

    /**
     * Returns the DER-encoded form of the key as a byte array.
     *
     * @exception InvalidKeyException on encoding errors.
     */
    public byte[] encode() throws InvalidKeyException {
        return getEncodedInternal().clone();
    }

    /*
     * Returns a printable representation of the key
     */
    public String toString()
    {
        HexDumpEncoder  encoder = new HexDumpEncoder();

        return "algorithm = " + algid.toString()
            + ", unparsed keybits = \n" + encoder.encodeBuffer(key);
    }

    /**
     * Initialize an X509Key object from an input stream.  The data on that
     * input stream must be encoded using DER, obeying the X.509
     * <code>SubjectPublicKeyInfo</code> format.  That is, the data is a
     * sequence consisting of an algorithm ID and a bit string which holds
     * the key.  (That bit string is often used to encapsulate another DER
     * encoded sequence.)
     *
     * <P>Subclasses should not normally redefine this method; they should
     * instead provide a <code>parseKeyBits</code> method to parse any
     * fields inside the <code>key</code> member.
     *
     * <P>The exception to this rule is that since private keys need not
     * be encoded using the X.509 <code>SubjectPublicKeyInfo</code> format,
     * private keys may override this method, <code>encode</code>, and
     * of course <code>getFormat</code>.
     *
     * @param in an input stream with a DER-encoded X.509
     *          SubjectPublicKeyInfo value
     * @exception InvalidKeyException on parsing errors.
     */
    public void decode(InputStream in)
    throws InvalidKeyException
    {
        DerValue        val;

        try {
            val = new DerValue(in);
            if (val.tag != DerValue.tag_Sequence)
                throw new InvalidKeyException("invalid key format");

            algid = AlgorithmId.parse(val.data.getDerValue());
            setKey(val.data.getUnalignedBitString());
            parseKeyBits();
            if (val.data.available() != 0)
                throw new InvalidKeyException ("excess key data");

        } catch (IOException e) {
            throw new InvalidKeyException("IOException: " +
                                          e.getMessage());
        }
    }

    public void decode(byte[] encodedKey) throws InvalidKeyException {
        decode(new ByteArrayInputStream(encodedKey));
    }

    /**
     * Serialization write ... X.509 keys serialize as
     * themselves, and they're parsed when they get read back.
     */
    @java.io.Serial
    private void writeObject(ObjectOutputStream stream) throws IOException {
        stream.write(getEncoded());
    }

    /**
     * Serialization read ... X.509 keys serialize as
     * themselves, and they're parsed when they get read back.
     */
    @java.io.Serial
    private void readObject(ObjectInputStream stream) throws IOException {
        try {
            decode(stream);
        } catch (InvalidKeyException e) {
            e.printStackTrace();
            throw new IOException("deserialized key is invalid: " +
                                  e.getMessage());
        }
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof Key == false) {
            return false;
        }
        try {
            byte[] thisEncoded = this.getEncodedInternal();
            byte[] otherEncoded;
            if (obj instanceof X509Key) {
                otherEncoded = ((X509Key)obj).getEncodedInternal();
            } else {
                otherEncoded = ((Key)obj).getEncoded();
            }
            return Arrays.equals(thisEncoded, otherEncoded);
        } catch (InvalidKeyException e) {
            return false;
        }
    }

    /**
     * Calculates a hash code value for the object. Objects
     * which are equal will also have the same hashcode.
     */
    public int hashCode() {
        try {
            byte[] b1 = getEncodedInternal();
            int r = b1.length;
            for (int i = 0; i < b1.length; i++) {
                r += (b1[i] & 0xff) * 37;
            }
            return r;
        } catch (InvalidKeyException e) {
            // should not happen
            return 0;
        }
    }

    /*
     * Produce SubjectPublicKey encoding from algorithm id and key material.
     */
    static void encode(DerOutputStream out, AlgorithmId algid, BitArray key)
        throws IOException {
            DerOutputStream tmp = new DerOutputStream();
            algid.encode(tmp);
            tmp.putUnalignedBitString(key);
            out.write(DerValue.tag_Sequence, tmp);
    }
}
