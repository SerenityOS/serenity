/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package p;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import javax.script.ScriptEngineFactory;

public class Main {

    public static void main(String[] args) {
        Module thisModule = Main.class.getModule();
        assertTrue(thisModule.isNamed());

        // this module does not declare that it uses ScriptEngineFactory
        assertFalse(thisModule.canUse(ScriptEngineFactory.class));
        try {
            ServiceLoader.load(ScriptEngineFactory.class);
            assertTrue(false);
        } catch (ServiceConfigurationError expected) { }

        // invoke addUses and retry
        thisModule.addUses(ScriptEngineFactory.class);
        ServiceLoader.load(ScriptEngineFactory.class).findFirst();
    }

    static void assertFalse(boolean value) {
        if (value) throw new RuntimeException();
    }

    static void assertTrue(boolean value) {
        if (!value) throw new RuntimeException();
    }
}
