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

import java.util.Hashtable;
import javax.naming.*;
import javax.naming.spi.ObjectFactory;

/**
 * This is an object factory that when given a reference for a Fruit
 * object, will create an instance of the corresponding Fruit.
 */
public class FruitFactory implements ObjectFactory {
    public FruitFactory() {
    }

    public Object getObjectInstance(Object obj, Name name, Context ctx,
        Hashtable env) throws Exception {
        if (obj instanceof Reference) {
            Reference ref = (Reference)obj;
            if (ref.getClassName().equals(Fruit.class.getName())) {
                RefAddr addr = ref.get("fruit");
                if (addr != null) {
                    return new Fruit((String)addr.getContent());
                }
            }
        }
        return null;
    }
}
