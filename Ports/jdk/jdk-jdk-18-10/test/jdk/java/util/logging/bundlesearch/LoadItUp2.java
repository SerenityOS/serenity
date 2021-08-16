/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.util.MissingResourceException;
import java.util.logging.Logger;

/*
 * This class is loaded onto the call stack by LoadItUp2Invoker from a separate
 * classloader.  LoadItUp2Invoker was loaded by a class loader that does have
 * access to the bundle, but the class loader used to load this class does not.
 * Thus the logging code should not be able to see the resource bundle unless
 * it has more than a single level stack crawl, which is not allowed.
 *
 * @author Jim Gish
 */
public class LoadItUp2 {

    private static final boolean DEBUG = false;

    public Boolean test(String rbName) throws Exception {
        // we should not be able to find the resource in this directory via
        // getLogger calls.  The only way that would be possible given this setup
        // is that if Logger.getLogger searched up the call stack
        return lookupBundle(rbName);
    }

    private boolean lookupBundle(String rbName) {
        // See if Logger.getLogger can find the resource in this directory
        try {
            Logger aLogger = Logger.getLogger("NestedLogger2", rbName);
        } catch (MissingResourceException re) {
            if (DEBUG) {
                System.out.println(
                    "As expected, LoadItUp2.lookupBundle() did not find the bundle "
                    + rbName);
            }
            return false;
        }
        System.out.println("FAILED: LoadItUp2.lookupBundle() found the bundle "
                + rbName + " using a stack search.");
        return true;
    }
}
