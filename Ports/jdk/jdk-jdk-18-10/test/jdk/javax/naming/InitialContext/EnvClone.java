/*
 * Copyright (c) 1999, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4241351
 * @summary Ensure that initial context constructor clones its environment
 *      parameter before calling init(), and that it doesn't clone it
 *      within init().
 */

import java.util.Hashtable;
import javax.naming.*;

public class EnvClone extends InitialContext {

    EnvClone(Hashtable<Object, Object> env) throws NamingException{
        super(env);
    }

    EnvClone(boolean lazy) throws NamingException{
        super(lazy);
    }

    public static void main(String[] args) throws Exception {

        Hashtable<Object, Object> env = new Hashtable<>(5);
        EnvClone ctx = new EnvClone(env);

        if (env == ctx.myProps) {
            throw new Exception(
                    "Test failed:  constructor didn't clone environment");
        }

        ctx = new EnvClone(true);
        ctx.init(env);

        if (env != ctx.myProps) {
            throw new Exception("Test failed:  init() cloned environment");
        }
    }
}
