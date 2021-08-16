/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

package org.example.fruit;

import javax.naming.*;

/**
  * This class is used by the StoreFruit test.
  * It is a referenceable class that can be stored by service
  * providers like the LDAP and file system providers.
  */
public class Fruit implements Referenceable {
    static String location = null;
    String fruit;

    public Fruit(String f) {
        fruit = f;
    }

    public Reference getReference() throws NamingException {
        return new Reference(
            Fruit.class.getName(),
            new StringRefAddr("fruit", fruit),
            FruitFactory.class.getName(),
            location);          // factory location
    }

    public String toString() {
        return fruit;
    }

    public static void setLocation(String loc) {
        location = loc;
System.out.println("setting location to : " + location);
    }
}
