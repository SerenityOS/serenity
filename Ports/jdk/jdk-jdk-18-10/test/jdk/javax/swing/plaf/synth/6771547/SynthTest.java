/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 6771547
 * @author Alexander Potochkin
 * @summary SynthParser throws StringIndexOutOfBoundsException parsing custom ColorTypes
 */

import javax.swing.plaf.synth.SynthLookAndFeel;
import javax.swing.*;
import java.io.InputStream;
import java.awt.*;

public class SynthTest {

    public static void main(String[] args) throws Exception {
        SynthLookAndFeel laf = new SynthLookAndFeel();
        InputStream in = SynthTest.class.getResourceAsStream(
                "synthconfig.xml");
        laf.load(in, SynthTest.class);

        UIManager.setLookAndFeel(laf);

        if (!Color.RED.equals(new JButton().getForeground())) {
            throw new RuntimeException("The wrong foreground color!");
        }
    }
}
