/*
 * Copyright (c) 2007, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049021
 * @summary Test if we can write new provider for smart card
 * @run main/othervm/java.security.policy=policy TerminalFactorySpiTest
 */
import java.security.Provider;
import java.security.Security;
import java.util.Arrays;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.TerminalFactory;
import javax.smartcardio.TerminalFactorySpi;

public class TerminalFactorySpiTest {

    static boolean callMethod = false;

    public static void main(String[] args) throws Exception {
        Provider myProvider = new MyProvider();
        Security.addProvider(myProvider);
        System.out.println(Arrays.asList(Security.getProviders()));

        TerminalFactory.getInstance("MyType", new Object()).terminals();
        if (!callMethod) {
            throw new RuntimeException("Expected engineTerminals() not called");
        }
    }

    public static class MyProvider extends Provider {

        MyProvider() {
            super("MyProvider", 1.0d, "smart Card Example");
            put("TerminalFactory.MyType", "TerminalFactorySpiTest$MyTerminalFactorySpi");
        }
    }

    public static class MyTerminalFactorySpi extends TerminalFactorySpi {

        public MyTerminalFactorySpi(Object ob) {
        }

        protected CardTerminals engineTerminals() {
            System.out.println("MyTerminalFactory.engineTerminals()");
            callMethod = true;
            return null;
        }

    }
}
