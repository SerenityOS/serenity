/*
 * Copyright (c) 2004, 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5063390
 * @summary Tests the order of Properties
 * @author Brent Christian
 */

import java.beans.PropertyDescriptor;

public class Test5063390 {
    private int alpha;
    private int foxtrot;
    private int zulu;

    public int getZulu() {
        return zulu;
    }

    public void setZulu(int zulu) {
        this.zulu = zulu;
    }

    public int getAlpha() {
        return alpha;
    }

    public void setAlpha(int alpha) {
        this.alpha = alpha;
    }

    public int getFoxtrot() {
        return foxtrot;
    }

    public void setFoxtrot(int foxtrot) {
        this.foxtrot = foxtrot;
    }

    public static void main(String[] args) {
        String[] names = {"alpha", "class", "foxtrot", "zulu"};

        PropertyDescriptor[] pd = BeanUtils.getPropertyDescriptors(Test5063390.class);
        if (pd.length != names.length)
            throw new Error("unexpected count of properties: " + pd.length);

        for (int i = 0; i < pd.length; i++) {
            String name = pd[i].getName();
            System.out.println("property: " + name);
            if (!name.equals(names[i])) {
                System.out.println("expected: " + names[i]);
                throw new Error("unexpected order of properties");
            }
        }
    }
}
