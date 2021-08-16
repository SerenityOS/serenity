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
 * @bug 4241676
 * @summary getContinuationDirContext() should set CPE environment property.
 * @build DummyObjectFactory DummyContext
 * @run main/othervm GetContDirCtx
 */

import java.util.Hashtable;
import javax.naming.*;
import javax.naming.directory.*;
import javax.naming.spi.*;

public class GetContDirCtx {

    public static void main(String[] args) throws Exception {

        CannotProceedException cpe = new CannotProceedException();
        Hashtable<Object, Object> env = new Hashtable<>(1);
        cpe.setEnvironment(env);

        Reference ref = new Reference("java.lang.Object",
                                    "DummyObjectFactory",
                                    null);
        cpe.setResolvedObj(ref);
        Context contCtx = null;
        try {
            contCtx  = DirectoryManager.getContinuationDirContext(cpe);
        } catch (CannotProceedException e) {
        }

        Hashtable<?,?> contEnv = contCtx.getEnvironment();
        if (contEnv.get(NamingManager.CPE) != cpe) {
            throw new Exception("Test failed: CPE property not set" +
                        " in the continuation context");
        }
    }
}
