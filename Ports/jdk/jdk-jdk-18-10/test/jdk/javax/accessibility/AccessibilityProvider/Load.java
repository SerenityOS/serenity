/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTError;
import java.awt.Toolkit;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.accessibility.AccessibilityProvider;

public class Load {

    public static void main(String[] args) {
        // args[0]: "pass" or "fail" (the expected result)
        // args[1]: "<first provider name>"
        // args[2]: "<optional second provider name>"

        boolean passExpected = args[0].equals("pass");

        // Fill Set with provider names that were requested.
        // The providers may or may not be available:
        // - available: FooProvider, BarProvider
        // - not available: NoProvider
        List<String> requestedNames = new ArrayList<>();
        for (int i = 1; i < args.length; ++i) {
            requestedNames.add(args[i]);
        }
        // cleanup files from any prior run
        for (String name : requestedNames) {
            File f = new File(name + ".txt");
            f.delete();
        }
        // Activate getDefaultToolkit which will in turn activate the providers
        try {
            Toolkit.getDefaultToolkit();
        } catch (AWTError e) {
            if (passExpected) {
                throw new RuntimeException(e.getMessage());
            }
        }
        // Toolkit.getDefaultToolkit() already went through all the service
        // providers, loading and activating the requested ones, but now we need
        // to see if they actually got activated.
        // Go through the providers that were requested, for each one:
        //   If it was activated pass
        //   else fail (throw exception)
        boolean failure = false;
        String failingName = "";
        for (String name : requestedNames) {
            File f = new File(name + ".txt");
            if (!f.exists()) {
                failure = true;
                failingName = name;
                break;
            }
        } // if get to here, no issues, so try next provider
        if (failure && passExpected) {
            throw new RuntimeException(failingName + " was not activated");
        }
        if (!failure && !passExpected) {
            String s = "Test passed but a failure was expected.  ";
            s += "The requested providers were:\n";
            for (String name : requestedNames) {
                s += ("  " + name + "\n");
            }
            throw new RuntimeException(s);
        }
    }
}
