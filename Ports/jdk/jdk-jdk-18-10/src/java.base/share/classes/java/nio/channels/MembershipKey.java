/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package java.nio.channels;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.io.IOException;

/**
 * A token representing the membership of an Internet Protocol (IP) multicast
 * group.
 *
 * <p> A membership key may represent a membership to receive all datagrams sent
 * to the group, or it may be <em>source-specific</em>, meaning that it
 * represents a membership that receives only datagrams from a specific source
 * address. Whether or not a membership key is source-specific may be determined
 * by invoking its {@link #sourceAddress() sourceAddress} method.
 *
 * <p> A membership key is valid upon creation and remains valid until the
 * membership is dropped by invoking the {@link #drop() drop} method, or
 * the channel is closed. The validity of the membership key may be tested
 * by invoking its {@link #isValid() isValid} method.
 *
 * <p> Where a membership key is not source-specific and the underlying operation
 * system supports source filtering, then the {@link #block block} and {@link
 * #unblock unblock} methods can be used to block or unblock multicast datagrams
 * from particular source addresses.
 *
 * @see MulticastChannel
 *
 * @since 1.7
 */
public abstract class MembershipKey {

    /**
     * Initializes a new instance of this class.
     */
    protected MembershipKey() {
    }

    /**
     * Tells whether or not this membership is valid.
     *
     * <p> A multicast group membership is valid upon creation and remains
     * valid until the membership is dropped by invoking the {@link #drop() drop}
     * method, or the channel is closed.
     *
     * @return  {@code true} if this membership key is valid, {@code false}
     *          otherwise
     */
    public abstract boolean isValid();

    /**
     * Drop membership.
     *
     * <p> If the membership key represents a membership to receive all datagrams
     * then the membership is dropped and the channel will no longer receive any
     * datagrams sent to the group. If the membership key is source-specific
     * then the channel will no longer receive datagrams sent to the group from
     * that source address.
     *
     * <p> After membership is dropped it may still be possible to receive
     * datagrams sent to the group. This can arise when datagrams are waiting to
     * be received in the socket's receive buffer. After membership is dropped
     * then the channel may {@link MulticastChannel#join join} the group again
     * in which case a new membership key is returned.
     *
     * <p> Upon return, this membership object will be {@link #isValid() invalid}.
     * If the multicast group membership is already invalid then invoking this
     * method has no effect. Once a multicast group membership is invalid,
     * it remains invalid forever.
     */
    public abstract void drop();

    /**
     * Block multicast datagrams from the given source address.
     *
     * <p> If this membership key is not source-specific, and the underlying
     * operating system supports source filtering, then this method blocks
     * multicast datagrams from the given source address. If the given source
     * address is already blocked then this method has no effect.
     * After a source address is blocked it may still be possible to receive
     * datagrams from that source. This can arise when datagrams are waiting to
     * be received in the socket's receive buffer.
     *
     * @param   source
     *          The source address to block
     *
     * @return  This membership key
     *
     * @throws  IllegalArgumentException
     *          If the {@code source} parameter is not a unicast address or
     *          is not the same address type as the multicast group
     * @throws  IllegalStateException
     *          If this membership key is source-specific or is no longer valid
     * @throws  UnsupportedOperationException
     *          If the underlying operating system does not support source
     *          filtering
     * @throws  IOException
     *          If an I/O error occurs
     */
    public abstract MembershipKey block(InetAddress source) throws IOException;

    /**
     * Unblock multicast datagrams from the given source address that was
     * previously blocked using the {@link #block(InetAddress) block} method.
     *
     * @param   source
     *          The source address to unblock
     *
     * @return  This membership key
     *
     * @throws  IllegalStateException
     *          If the given source address is not currently blocked or the
     *          membership key is no longer valid
     */
    public abstract MembershipKey unblock(InetAddress source);

    /**
     * Returns the channel for which this membership key was created. This
     * method will continue to return the channel even after the membership
     * becomes {@link #isValid invalid}.
     *
     * @return  the channel
     */
    public abstract MulticastChannel channel();

    /**
     * Returns the multicast group for which this membership key was created.
     * This method will continue to return the group even after the membership
     * becomes {@link #isValid invalid}.
     *
     * @return  the multicast group
     */
    public abstract InetAddress group();

    /**
     * Returns the network interface for which this membership key was created.
     * This method will continue to return the network interface even after the
     * membership becomes {@link #isValid invalid}.
     *
     * @return  the network interface
     */
    public abstract NetworkInterface networkInterface();

    /**
     * Returns the source address if this membership key is source-specific,
     * or {@code null} if this membership is not source-specific.
     *
     * @return  The source address if this membership key is source-specific,
     *          otherwise {@code null}
     */
    public abstract InetAddress sourceAddress();
}
