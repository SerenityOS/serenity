/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.UIManager;

import org.netbeans.jemmy.operators.ComponentOperator;
import org.testng.annotations.DataProvider;

public class TestHelpers {

    public static final long DELAY_BTWN_FRAME_STATE_CHANGE = 2000;

    /**
     * A DataProvider having the class name of all the available look and feels
     *
     * @return a 2d Object array containing the class name of all the available
     * look and feels
     */
    @DataProvider(name = "availableLookAndFeels")
    public static Object[][] provideAvailableLookAndFeels() {
        UIManager.LookAndFeelInfo LookAndFeelInfos[]
                = UIManager.getInstalledLookAndFeels();
        Object[][] lookAndFeels = new Object[LookAndFeelInfos.length][1];
        for (int i = 0; i < LookAndFeelInfos.length; i++) {
            lookAndFeels[i][0] = LookAndFeelInfos[i].getClassName();
        }
        return lookAndFeels;
    }

    public static void checkChangeLocation(ComponentOperator component,
            Point finalLocation) throws InterruptedException {
        Point initialLocation = component.getLocation();
        component.setLocation(finalLocation);
        component.waitComponentLocation(finalLocation);
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        delayBetweenFrameStateChange();
        component.setLocation(initialLocation);
        component.waitComponentLocation(initialLocation);
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        delayBetweenFrameStateChange();
    }

    public static void checkChangeSize(ComponentOperator component,
            Dimension dimensionFinal) throws InterruptedException {
        Dimension dimensionInitial = component.getSize();
        component.setSize(dimensionFinal);
        component.waitComponentSize(dimensionFinal);
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        delayBetweenFrameStateChange();
        component.setSize(dimensionInitial);
        component.waitComponentSize(dimensionInitial);
        // TODO This is a workaround for JDK-8210638, this delay has to remove
        // after fixing this bug, this is an unstable code.
        delayBetweenFrameStateChange();
    }

    // TODO This is a workaround for JDK-8210638, this delay has to remove
    // after fixing this bug, this is an unstable code.
    public static void delayBetweenFrameStateChange()
            throws InterruptedException {
        Thread.sleep(DELAY_BTWN_FRAME_STATE_CHANGE);
    }

}