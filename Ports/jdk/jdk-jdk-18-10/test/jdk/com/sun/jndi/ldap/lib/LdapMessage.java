/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * An LDAP message.
 */
public class LdapMessage {

    private final byte[] message;
    private int messageID;
    private Operation operation;

    public enum Operation {
        BIND_REQUEST(0x60, "BindRequest"),                      // [APPLICATION 0]
        BIND_RESPONSE(0x61, "BindResponse"),                    // [APPLICATION 1]
        UNBIND_REQUEST(0x42, "UnbindRequest"),                  // [APPLICATION 2]
        SEARCH_REQUEST(0x63, "SearchRequest"),                  // [APPLICATION 3]
        SEARCH_RESULT_ENTRY(0x64, "SearchResultEntry"),         // [APPLICATION 4]
        SEARCH_RESULT_DONE(0x65, "SearchResultDone"),           // [APPLICATION 5]
        MODIFY_REQUEST(0x66, "ModifyRequest"),                  // [APPLICATION 6]
        MODIFY_RESPONSE(0x67, "ModifyResponse"),                // [APPLICATION 7]
        ADD_REQUEST(0x68, "AddRequest"),                        // [APPLICATION 8]
        ADD_RESPONSE(0x69, "AddResponse"),                      // [APPLICATION 9]
        DELETE_REQUEST(0x4A, "DeleteRequest"),                  // [APPLICATION 10]
        DELETE_RESPONSE(0x6B, "DeleteResponse"),                // [APPLICATION 11]
        MODIFY_DN_REQUEST(0x6C, "ModifyDNRequest"),             // [APPLICATION 12]
        MODIFY_DN_RESPONSE(0x6D, "ModifyDNResponse"),           // [APPLICATION 13]
        COMPARE_REQUEST(0x6E, "CompareRequest"),                // [APPLICATION 14]
        COMPARE_RESPONSE(0x6F, "CompareResponse"),              // [APPLICATION 15]
        ABANDON_REQUEST(0x50, "AbandonRequest"),                // [APPLICATION 16]
        SEARCH_RESULT_REFERENCE(0x73, "SearchResultReference"), // [APPLICATION 19]
        EXTENDED_REQUEST(0x77, "ExtendedRequest"),              // [APPLICATION 23]
        EXTENDED_RESPONSE(0x78, "ExtendedResponse"),            // [APPLICATION 24]
        INTERMEDIATE_RESPONSE(0x79, "IntermediateResponse");    // [APPLICATION 25]

        private final int id;
        private final String name;

        Operation(int id, String name) {
            this.id = id;
            this.name = name;
        }

        public int getId() {
            return id;
        }

        @Override
        public String toString() {
            return name;
        }

        private static Operation fromId(int id) {
            Optional<Operation> optional = Stream.of(Operation.values())
                    .filter(o -> o.id == id).findFirst();
            if (optional.isPresent()) {
                return optional.get();
            } else {
                throw new RuntimeException(
                        "Unknown id " + id + " for enum Operation.");
            }
        }
    }

    public LdapMessage(byte[] message) {
        this.message = message;
        parse();
    }

    public LdapMessage(String hexString) {
        this(parseHexBinary(hexString));
    }

    // Extracts the message ID and operation ID from an LDAP protocol encoding
    private void parse() {
        if (message == null || message.length < 2) {
            throw new RuntimeException(
                    "Invalid ldap message: " + Arrays.toString(message));
        }

        if (message[0] != 0x30) {
            throw new RuntimeException("Bad LDAP encoding in message, "
                    + "expected ASN.1 SEQUENCE tag (0x30), encountered "
                    + message[0]);
        }

        int index = 2;
        if ((message[1] & 0x80) == 0x80) {
            index += (message[1] & 0x0F);
        }

        if (message[index] != 0x02) {
            throw new RuntimeException("Bad LDAP encoding in message, "
                    + "expected ASN.1 INTEGER tag (0x02), encountered "
                    + message[index]);
        }
        int length = message[index + 1];
        index += 2;
        messageID = new BigInteger(1,
                                   Arrays.copyOfRange(message, index, index + length)).intValue();
        index += length;
        int operationID = message[index];
        operation = Operation.fromId(operationID);
    }

    /**
     * Return original ldap message in byte array.
     *
     * @return original ldap message
     */
    public byte[] getMessage() {
        return Arrays.copyOf(message, message.length);
    }

    /**
     * Return ldap message id.
     *
     * @return ldap message id.
     */
    public int getMessageID() {
        return messageID;
    }

    /**
     * Return ldap message's operation.
     *
     * @return ldap message's operation.
     */
    public Operation getOperation() {
        return operation;
    }

    private static byte[] parseHexBinary(String s) {

        final int len = s.length();

        // "111" is not a valid hex encoding.
        if (len % 2 != 0) {
            throw new IllegalArgumentException(
                    "hexBinary needs to be even-length: " + s);
        }

        byte[] out = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            int h = Character.digit(s.charAt(i), 16);
            int l = Character.digit(s.charAt(i + 1), 16);
            if (h == -1 || l == -1) {
                throw new IllegalArgumentException(
                        "contains illegal character for hexBinary: " + s);
            }

            out[i / 2] = (byte) (h * 16 + l);
        }

        return out;
    }

    public static int getMessageLength(byte[] encoding) {
        if (encoding.length < 2) {
            // not enough data to extract msg len, just return -1
            return -1;
        }

        if (encoding[0] != 0x30) {
            throw new RuntimeException("Error: bad LDAP encoding message: "
                                               + "expected ASN.1 SEQUENCE tag (0x30), encountered "
                                               + encoding[0]);
        }

        int len;
        int index = 1;
        int payloadLen = 0;

        if ((encoding[1] & 0x80) == 0x80) {
            len = (encoding[1] & 0x0F);
            index++;
        } else {
            len = 1;
        }

        if (len > 4) {
            throw new RuntimeException(
                    "Error: LDAP encoding message payload too large");
        }

        if (encoding.length < index + len) {
            // additional data required to extract payload len, return -1
            return -1;
        }

        for (byte b : Arrays.copyOfRange(encoding, index, index + len)) {
            payloadLen = payloadLen << 8 | (b & 0xFF);
        }

        if (payloadLen <= 0) {
            throw new RuntimeException(
                    "Error: invalid LDAP encoding message length or payload too large");
        }

        return index + len + payloadLen;
    }
}
