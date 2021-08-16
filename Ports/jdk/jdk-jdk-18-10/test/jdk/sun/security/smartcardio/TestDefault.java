/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6327047 8168851
 * @summary verify that TerminalFactory.getDefault() works
 * @author Andreas Sterbenz
 * @modules java.smartcardio/javax.smartcardio
 * @run main/manual TestDefault
 * @run main/othervm/manual/java.security.policy==test.policy TestDefault
 */

// This test requires special hardware.

import java.util.List;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;

public class TestDefault {

    public static void main(String[] args) throws Exception {
        TerminalFactory factory = TerminalFactory.getDefault();
        System.out.println("Type: " + factory.getType());
        List<CardTerminal> terminals = factory.terminals().list();
        if (terminals.isEmpty()) {
            System.out.println("Skipping the test: " +
                    "no card terminals available");
            return;
        }
        System.out.println("Terminals: " + terminals);

        System.out.println("OK.");
    }

}
