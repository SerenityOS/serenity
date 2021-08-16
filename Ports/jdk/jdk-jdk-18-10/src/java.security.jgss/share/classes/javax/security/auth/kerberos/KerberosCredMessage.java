/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth.kerberos;

import javax.security.auth.Destroyable;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

/**
 * This class encapsulates a Kerberos 5 KRB_CRED message which can be used to
 * send Kerberos credentials from one principal to another.<p>
 *
 * A KRB_CRED message is defined in Section 5.8.1 of the Kerberos Protocol
 * Specification (<a href=http://www.ietf.org/rfc/rfc4120.txt>RFC 4120</a>) as:
 * <pre>
 *    KRB-CRED        ::= [APPLICATION 22] SEQUENCE {
 *            pvno            [0] INTEGER (5),
 *            msg-type        [1] INTEGER (22),
 *            tickets         [2] SEQUENCE OF Ticket,
 *            enc-part        [3] EncryptedData -- EncKrbCredPart
 *    }
 * </pre>
 *
 * @since 9
 */
public final class KerberosCredMessage implements Destroyable {

    private final KerberosPrincipal sender;
    private final KerberosPrincipal recipient;
    private final byte[] message;

    private boolean destroyed = false;

    /**
     * Constructs a {@code KerberosCredMessage} object.
     * <p>
     * The contents of the {@code message} argument are copied; subsequent
     * modification of the byte array does not affect the newly created object.
     *
     * @param sender the sender of the message
     * @param recipient the recipient of the message
     * @param message the DER encoded KRB_CRED message
     * @throws NullPointerException if any of sender, recipient
     *                              or message is null
     */
    public KerberosCredMessage(KerberosPrincipal sender,
                               KerberosPrincipal recipient,
                               byte[] message) {
        this.sender = Objects.requireNonNull(sender);
        this.recipient = Objects.requireNonNull(recipient);
        this.message = Objects.requireNonNull(message).clone();
    }

    /**
     * Returns the DER encoded form of the KRB_CRED message.
     *
     * @return a newly allocated byte array that contains the encoded form
     * @throws IllegalStateException if the object is destroyed
     */
    public byte[] getEncoded() {
        if (destroyed) {
            throw new IllegalStateException("This object is no longer valid");
        }
        return message.clone();
    }

    /**
     * Returns the sender of this message.
     *
     * @return the sender
     * @throws IllegalStateException if the object is destroyed
     */
    public KerberosPrincipal getSender() {
        if (destroyed) {
            throw new IllegalStateException("This object is no longer valid");
        }
        return sender;
    }

    /**
     * Returns the recipient of this message.
     *
     * @return the recipient
     * @throws IllegalStateException if the object is destroyed
     */
    public KerberosPrincipal getRecipient() {
        if (destroyed) {
            throw new IllegalStateException("This object is no longer valid");
        }
        return recipient;
    }

    /**
     * Destroys this object by clearing out the message.
     */
    @Override
    public void destroy() {
        if (!destroyed) {
            Arrays.fill(message, (byte)0);
            destroyed = true;
        }
    }

    @Override
    public boolean isDestroyed() {
        return destroyed;
    }

    /**
     * Returns an informative textual representation of this {@code KerberosCredMessage}.
     *
     * @return an informative textual representation of this {@code KerberosCredMessage}.
     */
    @Override
    public String toString() {
        if (destroyed) {
            return "Destroyed KerberosCredMessage";
        } else {
            return "KRB_CRED from " + sender + " to " + recipient + ":\n"
                    + Base64.getUrlEncoder().encodeToString(message);
        }
    }

    /**
     * Returns a hash code for this {@code KerberosCredMessage}.
     *
     * @return a hash code for this {@code KerberosCredMessage}.
     */
    @Override
    public int hashCode() {
        if (isDestroyed()) {
            return -1;
        } else {
            return Objects.hash(sender, recipient, Arrays.hashCode(message));
        }
    }

    /**
     * Compares the specified object with this {@code KerberosCredMessage}
     * for equality. Returns true if the given object is also a
     * {@code KerberosCredMessage} and the two {@code KerberosCredMessage}
     * instances are equivalent. More formally two {@code KerberosCredMessage}
     * instances are equal if they have equal sender, recipient, and encoded
     * KRB_CRED messages.
     * A destroyed {@code KerberosCredMessage} object is only equal to itself.
     *
     * @param other the object to compare to
     * @return true if the specified object is equal to this
     * {@code KerberosCredMessage}, false otherwise.
     */
    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        if (! (other instanceof KerberosCredMessage)) {
            return false;
        }

        KerberosCredMessage otherMessage = ((KerberosCredMessage) other);
        if (isDestroyed() || otherMessage.isDestroyed()) {
            return false;
        }

        return Objects.equals(sender, otherMessage.sender)
                && Objects.equals(recipient, otherMessage.recipient)
                && Arrays.equals(message, otherMessage.message);
    }
}
