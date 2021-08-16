/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.color.*;

/*
 * @test
 * @bug 8058969 8178708
 * @summary test standard profiles loads with SecurityManager installed.
 * @run main/othervm -Djava.security.manager=allow LoadProfileWithSM
 */

public class LoadProfileWithSM {

    public static void main(String[] args) {
        System.setSecurityManager(new SecurityManager());
        ICC_Profile profile =
            ((ICC_ColorSpace)(ColorSpace.getInstance(
                ColorSpace.CS_GRAY))).getProfile();
        /* request profile data in order to force profile loading */
        profile.getData();
   }
}
