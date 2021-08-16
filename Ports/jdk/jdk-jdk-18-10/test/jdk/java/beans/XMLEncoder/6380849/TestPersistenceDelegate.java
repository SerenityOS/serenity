/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6380849
 * @summary Tests PersistenceDelegate finder
 * @author Sergey Malenkov
 */

import java.beans.PersistenceDelegate;
import java.beans.XMLEncoder;
import java.beans.DefaultPersistenceDelegate;

public class TestPersistenceDelegate {

    private static final XMLEncoder ENCODER = new XMLEncoder(System.out);

    public static void main(String[] args) throws InterruptedException {
        Class<?> type = TestPersistenceDelegate.class;
        test(type, DefaultPersistenceDelegate.class);
        ENCODER.setPersistenceDelegate(type, new BeanPersistenceDelegate());
        test(type, BeanPersistenceDelegate.class);
        ENCODER.setPersistenceDelegate(type, null);
        test(type, DefaultPersistenceDelegate.class);
        // the following tests fails on previous build
        test(Bean.class, BeanPersistenceDelegate.class);
        test(BeanPersistenceDelegate.class, BeanPersistenceDelegate.class);
    }

    private static void test(Class<?> type, Class<? extends PersistenceDelegate> expected) {
        PersistenceDelegate actual = ENCODER.getPersistenceDelegate(type);
        if ((actual == null) && (expected != null)) {
            throw new Error("expected delegate is not found");
        }
        if ((actual != null) && !actual.getClass().equals(expected)) {
            throw new Error("found unexpected delegate");
        }
    }
}
