/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.security;

import java.io.*;

/**
 * <p> SignedObject is a class for the purpose of creating authentic
 * runtime objects whose integrity cannot be compromised without being
 * detected.
 *
 * <p> More specifically, a SignedObject contains another Serializable
 * object, the (to-be-)signed object and its signature.
 *
 * <p> The signed object is a "deep copy" (in serialized form) of an
 * original object.  Once the copy is made, further manipulation of
 * the original object has no side effect on the copy.
 *
 * <p> The underlying signing algorithm is designated by the Signature
 * object passed to the constructor and the {@code verify} method.
 * A typical usage for signing is the following:
 *
 * <pre>{@code
 * Signature signingEngine = Signature.getInstance(algorithm,
 *                                                 provider);
 * SignedObject so = new SignedObject(myobject, signingKey,
 *                                    signingEngine);
 * }</pre>
 *
 * <p> A typical usage for verification is the following (having
 * received SignedObject {@code so}):
 *
 * <pre>{@code
 * Signature verificationEngine =
 *     Signature.getInstance(algorithm, provider);
 * if (so.verify(publickey, verificationEngine))
 *     try {
 *         Object myobj = so.getObject();
 *     } catch (java.lang.ClassNotFoundException e) {};
 * }</pre>
 *
 * <p> Several points are worth noting.  First, there is no need to
 * initialize the signing or verification engine, as it will be
 * re-initialized inside the constructor and the {@code verify}
 * method. Secondly, for verification to succeed, the specified
 * public key must be the public key corresponding to the private key
 * used to generate the SignedObject.
 *
 * <p> More importantly, for flexibility reasons, the
 * constructor and {@code verify} method allow for
 * customized signature engines, which can implement signature
 * algorithms that are not installed formally as part of a crypto
 * provider.  However, it is crucial that the programmer writing the
 * verifier code be aware what {@code Signature} engine is being
 * used, as its own implementation of the {@code verify} method
 * is invoked to verify a signature.  In other words, a malicious
 * {@code Signature} may choose to always return true on
 * verification in an attempt to bypass a security check.
 *
 * <p> The signature algorithm can be, among others, the NIST standard
 * DSA, using DSA and SHA-256.  The algorithm is specified using the
 * same convention as that for signatures. The DSA algorithm using the
 * SHA-256 message digest algorithm can be specified, for example, as
 * "SHA256withDSA".  In the case of
 * RSA the signing algorithm could be specified as, for example,
 * "SHA256withRSA".  The algorithm name must be
 * specified, as there is no default.
 *
 * <p> The name of the Cryptography Package Provider is designated
 * also by the Signature parameter to the constructor and the
 * {@code verify} method.  If the provider is not
 * specified, the default provider is used.  Each installation can
 * be configured to use a particular provider as default.
 *
 * <p> Potential applications of SignedObject include:
 * <ul>
 * <li> It can be used
 * internally to any Java runtime as an unforgeable authorization
 * token -- one that can be passed around without the fear that the
 * token can be maliciously modified without being detected.
 * <li> It
 * can be used to sign and serialize data/object for storage outside
 * the Java runtime (e.g., storing critical access control data on
 * disk).
 * <li> Nested SignedObjects can be used to construct a logical
 * sequence of signatures, resembling a chain of authorization and
 * delegation.
 * </ul>
 *
 * @see Signature
 *
 * @author Li Gong
 * @since 1.2
 */

public final class SignedObject implements Serializable {

    @Serial
    private static final long serialVersionUID = 720502720485447167L;

    /**
     * The original content is "deep copied" in its serialized format
     * and stored in a byte array.
     */
    private byte[] content;

    /**
     * The signature field is stored as a byte array.
     */
    private byte[] signature;

    /**
     * The algorithm used to sign the object.
     */
    private String thealgorithm;

    /**
     * Constructs a SignedObject from any Serializable object.
     * The given object is signed with the given signing key, using the
     * designated signature engine.
     *
     * @param object the object to be signed.
     * @param signingKey the private key for signing.
     * @param signingEngine the signature signing engine.
     *
     * @throws    IOException if an error occurs during serialization
     * @throws    InvalidKeyException if the key is invalid.
     * @throws    SignatureException if signing fails.
     */
    public SignedObject(Serializable object, PrivateKey signingKey,
                        Signature signingEngine)
        throws IOException, InvalidKeyException, SignatureException {
            // creating a stream pipe-line, from a to b
            ByteArrayOutputStream b = new ByteArrayOutputStream();
            ObjectOutput a = new ObjectOutputStream(b);

            // write and flush the object content to byte array
            a.writeObject(object);
            a.flush();
            a.close();
            this.content = b.toByteArray();
            b.close();

            // now sign the encapsulated object
            this.sign(signingKey, signingEngine);
    }

    /**
     * Retrieves the encapsulated object.
     * The encapsulated object is de-serialized before it is returned.
     *
     * @return the encapsulated object.
     *
     * @throws    IOException if an error occurs during de-serialization
     * @throws    ClassNotFoundException if an error occurs during
     * de-serialization
     */
    public Object getObject()
        throws IOException, ClassNotFoundException
    {
        // creating a stream pipe-line, from b to a
        ByteArrayInputStream b = new ByteArrayInputStream(this.content);
        ObjectInput a = new ObjectInputStream(b);
        Object obj = a.readObject();
        b.close();
        a.close();
        return obj;
    }

    /**
     * Retrieves the signature on the signed object, in the form of a
     * byte array.
     *
     * @return the signature. Returns a new array each time this
     * method is called.
     */
    public byte[] getSignature() {
        return this.signature.clone();
    }

    /**
     * Retrieves the name of the signature algorithm.
     *
     * @return the signature algorithm name.
     */
    public String getAlgorithm() {
        return this.thealgorithm;
    }

    /**
     * Verifies that the signature in this SignedObject is the valid
     * signature for the object stored inside, with the given
     * verification key, using the designated verification engine.
     *
     * @param verificationKey the public key for verification.
     * @param verificationEngine the signature verification engine.
     *
     * @throws    SignatureException if signature verification failed (an
     *     exception prevented the signature verification engine from completing
     *     normally).
     * @throws    InvalidKeyException if the verification key is invalid.
     *
     * @return {@code true} if the signature
     * is valid, {@code false} otherwise
     */
    public boolean verify(PublicKey verificationKey,
                          Signature verificationEngine)
         throws InvalidKeyException, SignatureException {
             verificationEngine.initVerify(verificationKey);
             verificationEngine.update(this.content.clone());
             return verificationEngine.verify(this.signature.clone());
    }

    /*
     * Signs the encapsulated object with the given signing key, using the
     * designated signature engine.
     *
     * @param signingKey the private key for signing.
     * @param signingEngine the signature signing engine.
     *
     * @throws    InvalidKeyException if the key is invalid.
     * @throws    SignatureException if signing fails.
     */
    private void sign(PrivateKey signingKey, Signature signingEngine)
        throws InvalidKeyException, SignatureException {
            // initialize the signing engine
            signingEngine.initSign(signingKey);
            signingEngine.update(this.content.clone());
            this.signature = signingEngine.sign().clone();
            this.thealgorithm = signingEngine.getAlgorithm();
    }

    /**
     * readObject is called to restore the state of the SignedObject from
     * a stream.
     *
     * @param  s the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException {
            ObjectInputStream.GetField fields = s.readFields();
            content = ((byte[])fields.get("content", null)).clone();
            signature = ((byte[])fields.get("signature", null)).clone();
            thealgorithm = (String)fields.get("thealgorithm", null);
    }
}
