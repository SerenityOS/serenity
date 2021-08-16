/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import sun.security.provider.DRBG;
import sun.security.provider.MoreDrbgParameters;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

/**
 * @test
 * @bug 8157308
 * @modules java.base/sun.security.provider:+open
 * @summary Make AbstractDrbg non-Serializable
 * @run main DRBGS11n mech
 * @run main DRBGS11n capability
 */
public class DRBGS11n {

    public static void main(String[] args) throws Exception {

        DRBG d = new DRBG(null);
        Field f = DRBG.class.getDeclaredField("mdp");
        f.setAccessible(true);
        MoreDrbgParameters mdp = (MoreDrbgParameters)f.get(d);

        // Corrupt the mech or capability fields inside DRBG#mdp.
        f = MoreDrbgParameters.class.getDeclaredField(args[0]);
        f.setAccessible(true);
        f.set(mdp, null);

        try {
            revive(d);
        } catch (IllegalArgumentException iae) {
            // Expected
            return;
        }

        throw new Exception("revive should fail");
    }

    static <T> T revive(T in) throws Exception {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        new ObjectOutputStream(bout).writeObject(in);
        return (T) new ObjectInputStream(
                new ByteArrayInputStream(bout.toByteArray())).readObject();
    }
}
