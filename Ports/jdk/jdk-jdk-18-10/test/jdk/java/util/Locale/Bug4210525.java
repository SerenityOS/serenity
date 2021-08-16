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
/**
    @test
    @summary Locale variant should not be uppercased
    @run main Bug4210525
    @bug 4210525
*/

import java.util.Locale;

public class Bug4210525 {

    public static void main(String[] args) throws Exception {
        String language = "en";
        String country = "US";
        String variant = "socal";

        Locale aLocale = new Locale(language, country, variant);

        String localeVariant = aLocale.getVariant();
        if (localeVariant.equals(variant)) {
            System.out.println("passed");
        } else {
            System.out.println("failed");
            throw new Exception("Bug4210525 test failed.");
        }
    }
}
