/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6255653
  @summary REGRESSION: Override isLightweight() causes access violation in awt.dll
  @author Andrei Dmitriev: area=awt-component
  @run main IsLightweightCrash
*/

/*
 * The test may not crash for several times so iteratively continue up to some limit.
 */

import java.awt.*;

public class IsLightweightCrash {
    public static int ITERATIONS = 20;

    public static void main(String []s)
    {
        for (int i = 0; i < ITERATIONS; i++){
            showFrame(i);
        }
    }

    private static void showFrame(int i){
        System.out.println("iteration = "+i);
        Frame f = new Frame();
        f.add(new AHeavyweightComponent());
        f.setVisible(true);
        f.setVisible(false);
    }
}

class AHeavyweightComponent extends Component {
    public boolean isLightweight() { return false; }
}
