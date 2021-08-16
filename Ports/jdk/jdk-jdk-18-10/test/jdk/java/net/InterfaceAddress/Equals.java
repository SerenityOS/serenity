/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 6628576
 * @modules java.base/java.net:open
 * @summary InterfaceAddress.equals() NPE when broadcast field == null
 */

import java.net.InterfaceAddress;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class Equals
{
    public static void main(String[] args) {
        InterfaceAddress ia1;
        InterfaceAddress ia2;
        InetAddress loopbackAddr = InetAddress.getLoopbackAddress();
        InetAddress broadcast1 = null;
        InetAddress broadcast2 = null;

        try {
            broadcast1 = InetAddress.getByName("255.255.255.0");
            broadcast2 = InetAddress.getByName("255.255.0.0");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        ia1 = createInterfaceAddress(loopbackAddr, (InetAddress) null, (short)45);
        ia2 = createInterfaceAddress(loopbackAddr, (InetAddress) null, (short)45);

        compare(ia1, ia2, true);

        ia2 = createInterfaceAddress(loopbackAddr, broadcast1, (short)45);
        compare(ia1, ia2, false);

        ia2 = createInterfaceAddress((InetAddress)null, broadcast1, (short)45);
        compare(ia1, ia2, false);

        ia1 = createInterfaceAddress(loopbackAddr, broadcast2, (short)45);
        ia2 = createInterfaceAddress(loopbackAddr, broadcast2, (short)45);
        compare(ia1, ia2, true);

        ia1.equals(null);
    }

    static void compare(InterfaceAddress ia1, InterfaceAddress ia2, boolean equal) {
        if (ia1.equals(ia2) != equal)
            throw new RuntimeException("Failed: " + ia1 + " not equals to " + ia2);

        if (ia2.equals(ia1) != equal)
            throw new RuntimeException("Failed: " + ia2 + " not equals to " + ia1);
    }

    /**
     * Returns an InterfaceAddress instance with its fields set the the values
     * specificed.
     */
    static InterfaceAddress createInterfaceAddress(
                InetAddress address, InetAddress broadcast, short prefixlength) {
        try {
            Class<InterfaceAddress> IAClass = InterfaceAddress.class;
            InterfaceAddress ia;
            Constructor<InterfaceAddress> ctr = IAClass.getDeclaredConstructor();
            ctr.setAccessible(true);

            Field addressField = IAClass.getDeclaredField("address");
            addressField.setAccessible(true);

            Field broadcastField = IAClass.getDeclaredField("broadcast");
            broadcastField.setAccessible(true);

            Field maskLengthField = IAClass.getDeclaredField("maskLength");
            maskLengthField.setAccessible(true);

            ia = ctr.newInstance();
            addressField.set(ia, address);
            broadcastField.set(ia, broadcast);
            maskLengthField.setShort(ia, prefixlength);

            return ia;
        } catch (NoSuchFieldException nsfe) {
            nsfe.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InstantiationException ie) {
            ie.printStackTrace();
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        } catch (InvocationTargetException ite) {
            ite.printStackTrace();
        }

        return null;
    }
}
