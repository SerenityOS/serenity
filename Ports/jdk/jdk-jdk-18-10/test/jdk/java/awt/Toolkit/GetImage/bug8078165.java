/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.*;
import java.net.URL;
import java.security.Permission;


/**
 * @test
 * @bug 8078165
 * @run main/othervm -Djava.security.manager=allow bug8078165
 * @summary NPE when attempting to get image from toolkit
 * @author Anton Nashatyrev
 */
public final class bug8078165 {

    public static void main(final String[] args) throws Exception {
        // Mac only
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission permission) {
                // Just allows everything
            }
        });
        // The method shouldn't throw NPE
        Toolkit.getDefaultToolkit().getImage(new URL("file://./dummyImage@2x.png"));
        Toolkit.getDefaultToolkit().getImage("./dummyImage@2x.png");
    }
}
