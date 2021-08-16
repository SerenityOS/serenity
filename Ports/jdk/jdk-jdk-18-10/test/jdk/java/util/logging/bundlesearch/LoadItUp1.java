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
import java.util.logging.Logger;

/*
 * This class is loaded onto the call stack when the getLogger methods are
 * called and then the classes classloader can be used to find a bundle in
 * the same directory as the class.  However, Logger is not allowed
 * to find the bundle by looking up the stack for this classloader.
 * We verify that this cannot happen.
 *
 * @author Jim Gish
 */
public class LoadItUp1 {
    public Logger getAnonymousLogger(String rbName) throws Exception {
        // we should not be able to find the resource in this directory via
        // getLogger calls.  The only way that would be possible given this setup
        // is that if Logger.getLogger searched up the call stack
        return Logger.getAnonymousLogger(rbName);
    }

    public Logger getLogger(String loggerName) {
        return Logger.getLogger(loggerName);
    }

    public Logger getLogger(String loggerName,String bundleName) {
        return Logger.getLogger(loggerName, bundleName);
    }
}
