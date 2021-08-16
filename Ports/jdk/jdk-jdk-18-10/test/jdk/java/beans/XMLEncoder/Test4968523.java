/*
 * Copyright (c) 2006, 2007, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4968523
 * @summary Tests persistence delegate in different encoders
 * @author Sergey Malenkov
 */

import java.beans.DefaultPersistenceDelegate;
import java.beans.Encoder;
import java.beans.XMLEncoder;
import java.beans.PersistenceDelegate;
import java.util.Date;

public class Test4968523 {
    public static void main(String[] args) {
        String[] names = {"time"};
        test(Date.class, new DefaultPersistenceDelegate(names));
        test(null, new DefaultPersistenceDelegate());
    }

    private static void test(Class<?> type, PersistenceDelegate pd) {
        Encoder encoder1 = new Encoder();
        Encoder encoder2 = new XMLEncoder(System.out);

        PersistenceDelegate pd1 = encoder1.getPersistenceDelegate(type);
        PersistenceDelegate pd2 = encoder2.getPersistenceDelegate(type);

        encoder1.setPersistenceDelegate(type, pd);

        if (pd1 == encoder1.getPersistenceDelegate(type))
            throw new Error("first persistence delegate is not changed");

        if (pd2 != encoder2.getPersistenceDelegate(type))
            throw new Error("second persistence delegate is changed");
    }
}
