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
 * @bug 6239117 6445367
 * @summary test that CardTerminals.waitForCard() works
 * @author Andreas Sterbenz
 * @modules java.smartcardio/javax.smartcardio
 * @run main/manual TestMultiplePresent
 */

// This test requires special hardware.

import java.util.List;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import static javax.smartcardio.CardTerminals.State.*;

public class TestMultiplePresent {

    public static void main(String[] args) throws Exception {
        Utils.setLibrary(args);
        TerminalFactory factory = Utils.getTerminalFactory(null);
        if (factory == null) {
            System.out.println("Skipping the test: " +
                    "no card terminals available");
            return;
        }
        System.out.println(factory);

        CardTerminals terminals = factory.terminals();
        List<CardTerminal> list = terminals.list();
        System.out.println("Terminals: " + list);
        boolean multipleReaders = true;
        if (list.size() < 2) {
            if (list.isEmpty()) {
                System.out.println("Skipping the test: " +
                        "no card terminals available");
                return;
            }
            System.out.println("Only one reader present, using simplified test");
            multipleReaders = false;
        }

        while (true) {
            boolean present = false;
            for (CardTerminal terminal : list) {
                present |= terminal.isCardPresent();
            }
            if (present == false) {
                break;
            }
            System.out.println("*** Remove all cards!");
            Thread.sleep(1000);
        }
        System.out.println("OK");

        List<CardTerminal> result;

        result = terminals.list(CARD_PRESENT);
        if (result.size() != 0) {
            throw new Exception("List not empty: " + result);
        }

        if (terminals.waitForChange(1000)) {
            throw new Exception("no timeout");
        }
        if (terminals.waitForChange(1000)) {
            throw new Exception("no timeout");
        }
        result = terminals.list(CARD_PRESENT);
        if (result.size() != 0) {
            throw new Exception("List not empty: " + result);
        }

        System.out.println("*** Insert card!");
        terminals.waitForChange();

        result = terminals.list(CARD_INSERTION);
        System.out.println(result);
        if (result.size() != 1) {
            throw new Exception("no card present");
        }
        CardTerminal t1 = result.get(0);

        result = terminals.list(CARD_INSERTION);
        System.out.println(result);
        if ((result.size() != 1) || (result.get(0) != t1)) {
            throw new Exception("no card present");
        }

        if (terminals.list(CARD_REMOVAL).size() != 0) {
            throw new Exception("List not empty: " + result);
        }
        if (terminals.list(CARD_REMOVAL).size() != 0) {
            throw new Exception("List not empty: " + result);
        }


        if (terminals.waitForChange(1000)) {
            throw new Exception("no timeout");
        }

        if (terminals.list(CARD_REMOVAL).size() != 0) {
            throw new Exception("List not empty: " + result);
        }

        if (multipleReaders) {
            System.out.println("*** Insert card into other reader!");
            terminals.waitForChange();

            result = terminals.list(CARD_INSERTION);
            System.out.println(result);
            if (result.size() != 1) {
                throw new Exception("no card present");
            }
            CardTerminal t2 = result.get(0);
            if (t1.getName().equals(t2.getName())) {
                throw new Exception("same terminal");
            }

            System.out.println("*** Remove card from 2nd reader!");
            terminals.waitForChange();

            if (terminals.list(CARD_INSERTION).size() != 0) {
                throw new Exception("List not empty: " + result);
            }
            result = terminals.list(CARD_REMOVAL);
            if ((result.size() != 1) || (result.get(0) != t2)) {
                throw new Exception("no or wrong terminal");
            }
        }

        System.out.println("*** Remove card from 1st reader!");
        terminals.waitForChange();

        if (terminals.list(CARD_INSERTION).size() != 0) {
            throw new Exception("List not empty: " + result);
        }
        result = terminals.list(CARD_REMOVAL);
        if ((result.size() != 1) || (result.get(0) != t1)) {
            throw new Exception("no or wrong terminal");
        }

        System.out.println("OK.");
    }

}
