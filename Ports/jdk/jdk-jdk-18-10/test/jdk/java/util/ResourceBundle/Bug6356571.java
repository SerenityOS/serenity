/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6356571
 * @summary Make sure that non-ResourceBundle classes are ignored and
 * properties files are loaded correctly.
 */

import java.util.ResourceBundle;

public class Bug6356571 {
    // Bug6356571.class is not a ResourceBundle class, so it will be
    // ignored and Bug6356571.properties will be loaded.
    private ResourceBundle bundle = ResourceBundle.getBundle("Bug6356571");

    void check() {
        String id = bundle.getString("id");
        if (!"6356571".equals(id)) {
            throw new RuntimeException("wrong id: " + id);
        }
    }

    public static final void main(String[] args) {
        new Bug6356571().check();
    }
}
