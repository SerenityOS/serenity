/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

import sun.security.ec.ParametersMap;
import sun.security.provider.SHAKE256;
import sun.security.util.ObjectIdentifier;
import sun.security.util.KnownOIDs;
import sun.security.util.math.*;
import sun.security.util.math.intpoly.*;
import sun.security.x509.AlgorithmId;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.function.Function;

/*
 * The set of parameters that defines an instance of the EdDSA signature
 * scheme.
 */
public class EdDSAParameters {

    public interface DigesterFactory {
        // Default digest creator
        Digester createDigester();

        // Override this method if multiple key lengths are needed
        default Digester createDigester(int len) {
            return createDigester();
        }

        // Return a digest over all the provided byte arrays
        default byte[] digest(byte[]... data) {
            Digester d = createDigester();
            for (byte[] curData : data) {
                d.update(curData, 0, curData.length);
            }
            return d.digest();
        }
    }

    // Hash for Ed25519
    private static class SHA512DigesterFactory implements DigesterFactory {
        @Override
        public Digester createDigester() {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-512");
                return new MessageDigester(md);
            } catch (NoSuchAlgorithmException ex) {
                throw new ProviderException(ex);
            }
        }
    }

    // Hash for Ed448
    private static class SHAKE256DigesterFactory implements DigesterFactory {
        @Override
        // Most usage for Ed448 is 114bytes long
        public Digester createDigester() {
            return new SHAKE256Digester(114);
        }

        // Ed448 uses 64bytes long hasg for the signature message
        @Override
        public Digester createDigester(int len) {
            return new SHAKE256Digester(len);
        }
    }

    public interface Digester {
        void update(byte data);
        void update(byte[] data, int off, int len);
        byte[] digest();
    }

    private static class MessageDigester implements Digester {
        private final MessageDigest md;

        private MessageDigester(MessageDigest md) {
            this.md = md;
        }

        @Override
        public void update(byte data) {
            md.update(data);
        }
        @Override
        public void update(byte[] data, int off, int len) {
            md.update(data, off, len);
        }
        @Override
        public byte[] digest() {
            try {
                return md.digest();
            } finally {
                md.reset();
            }
        }
    }

    private static class SHAKE256Digester implements Digester {
        SHAKE256 md;

        SHAKE256Digester(int len) {
            md = new SHAKE256(len);
        }
        @Override
        public void update(byte data) {
            md.update(data);
        }
        @Override
        public void update(byte[] data, int off, int len) {
            md.update(data, off, len);
        }
        @Override
        public byte[] digest() {
            try {
                return md.digest();
            } finally {
                md.reset();
            }
        }
    }

    static ParametersMap<EdDSAParameters> namedParams = new ParametersMap<>();

    private final String name;
    private final ObjectIdentifier oid;
    private final IntegerFieldModuloP field;
    private final IntegerFieldModuloP orderField;
    private final ImmutableIntegerModuloP d;
    private final EdECOperations edOperations;
    private final DigesterFactory digester;
    private final int keyLength;
    private final int bits;
    private final int logCofactor;
    private final Function<EdDSAParameterSpec, byte[]> dom;

    public EdDSAParameters(String name, ObjectIdentifier oid,
                           IntegerFieldModuloP field,
                           IntegerFieldModuloP orderField,
                           ImmutableIntegerModuloP d,
                           EdECOperations edOps,
                           DigesterFactory digester,
                           Function<EdDSAParameterSpec, byte[]> dom,
                           int keyLength, int bits, int logCofactor) {
        this.oid = oid;
        this.name = name;
        this.field = field;
        this.orderField = orderField;
        this.d = d;
        this.edOperations = edOps;
        this.digester = digester;
        this.keyLength = keyLength;
        this.bits = bits;
        this.logCofactor = logCofactor;
        this.dom = dom;
    }

    public String getName() {
        return name;
    }
    public ObjectIdentifier getOid() {
        return oid;
    }
    public IntegerFieldModuloP getField() {
        return field;
    }
    public IntegerFieldModuloP getOrderField() {
        return orderField;
    }
    public ImmutableIntegerModuloP getD() {
        return d;
    }
    public EdECOperations getEdOperations() {
        return edOperations;
    }
    public int getKeyLength() {
        return keyLength;
    }
    public int getBits() {
        return bits;
    }
    public int getLogCofactor() {
        return logCofactor;
    }

    public Digester createDigester() {
        return digester.createDigester();
    }

    public Digester createDigester(int len) {
        return digester.createDigester(len);
    }

    public byte[] digest(byte[]... data) {
        return digester.digest(data);
    }

    public byte[] dom(EdDSAParameterSpec sigParams) {
        return dom.apply(sigParams);
    }

    private static final String prefixStr25519 =
        "SigEd25519 no Ed25519 collisions";
    private static final String prefixStr448 = "SigEd448";

    // Used for Ed25519
    static byte[] dom2(EdDSAParameterSpec sigParams) {
        if (!sigParams.isPrehash() && !sigParams.getContext().isPresent()) {
            return new byte[0];
        }
        return domImpl(prefixStr25519, sigParams);
    }

    // Used for Ed488
    static byte[] dom4(EdDSAParameterSpec sigParams) {
        return domImpl(prefixStr448, sigParams);
    }

    static byte[] domImpl(String prefixStr, EdDSAParameterSpec sigParams) {
        byte[] prefix = prefixStr.getBytes(StandardCharsets.US_ASCII);
        byte[] context = sigParams.getContext().orElse(new byte[0]);
        int length = prefix.length + 2 + context.length;
        byte[] result = new byte[length];
        System.arraycopy(prefix, 0, result, 0, prefix.length);
        byte x = (byte) (sigParams.isPrehash() ? 1 : 0);
        result[prefix.length] = x;
        result[prefix.length + 1] = (byte) context.length;
        System.arraycopy(context, 0, result, prefix.length + 2,
            context.length);
        return result;
    }

    static {
        // set up Ed25519
        IntegerFieldModuloP ed25519Field = new IntegerPolynomial25519();
        IntegerFieldModuloP ed25519OrderField = new Curve25519OrderField();
        BigInteger biD = new BigInteger("3709570593466943934313808350875" +
                "4565189542113879843219016388785533085940283555");
        ImmutableIntegerModuloP d = ed25519Field.getElement(biD);
        BigInteger baseX = new BigInteger("15112221349535400772501151409" +
                "588531511454012693041857206046113283949847762202");
        BigInteger baseY = new BigInteger("46316835694926478169428394003" +
                "475163141307993866256225615783033603165251855960");
        EdECOperations edOps = new Ed25519Operations(d, baseX, baseY);
        String name = NamedParameterSpec.ED25519.getName();
        ObjectIdentifier oid = ObjectIdentifier.of(KnownOIDs.Ed25519);
        int bits = 255;
        DigesterFactory digester = new SHA512DigesterFactory();
        EdDSAParameters params = new EdDSAParameters(name, oid,
                ed25519Field, ed25519OrderField, d, edOps,
                digester, EdDSAParameters::dom2, 32, bits, 3);

        namedParams.put(name, oid, bits, params);

        // set up Ed448
        IntegerFieldModuloP ed448Field = new IntegerPolynomial448();
        IntegerFieldModuloP ed448OrderField = new Curve448OrderField();
        biD = ed448Field.getSize().subtract(new BigInteger("39081"));
        d = ed448Field.getElement(biD);
        baseX = new BigInteger("224580040295924300187604334" +
                "099896036246789641632564134246125461686950415467406032909" +
                "029192869357953282578032075146446173674602635247710");
        baseY = new BigInteger("298819210078481492676017930" +
                "443930673437544040154080242095928241372331506189835876003" +
                "536878655418784733982303233503462500531545062832660");
        edOps = new Ed448Operations(d, baseX, baseY);
        name = NamedParameterSpec.ED448.getName();
        oid = ObjectIdentifier.of(KnownOIDs.Ed448);
        bits = 448;
        digester = new SHAKE256DigesterFactory();
        params = new EdDSAParameters(name, oid,
                ed448Field, ed448OrderField, d, edOps,
                digester, EdDSAParameters::dom4, 57, bits, 2);

        namedParams.put(name, oid, bits, params);

        namedParams.fix();
    }

    public static
    <T extends Throwable>
    EdDSAParameters getBySize(Function<String, T> exception,
                            int size) throws T {

        return namedParams.getBySize(exception, size);
    }

    public static
    <T extends Throwable>
    EdDSAParameters get(Function<String, T> exception,
                      AlgorithmId algId) throws T {

        return namedParams.get(exception, algId);
    }

    public static
    <T extends Throwable>
    EdDSAParameters get(Function<String, T> exception,
                      AlgorithmParameterSpec params) throws T {

        return namedParams.get(exception, params);
    }
}
