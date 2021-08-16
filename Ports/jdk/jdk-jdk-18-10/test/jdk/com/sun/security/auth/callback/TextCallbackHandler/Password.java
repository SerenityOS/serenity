/*
 * Copyright (c) 2009, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6825240
 * @summary Password.readPassword() echos the input when System.Console is null
 * @run main/manual Password
 */

import com.sun.security.auth.callback.TextCallbackHandler;
import javax.security.auth.callback.*;

public class Password {
   public static void main(String args[]) throws Exception {
        TextCallbackHandler h = new TextCallbackHandler();
        PasswordCallback nc = new PasswordCallback("Invisible: ", false);
        PasswordCallback nc2 = new PasswordCallback("Visible: ", true);

        System.out.println("Two passwords will be prompted for. The first one " +
                "should have echo off, the second one on. Otherwise, this test fails");
        Callback[] callbacks = { nc, nc2 };
        h.handle(callbacks);
        System.out.println("You input " + new String(nc.getPassword()) +
                " and " + new String(nc2.getPassword()));
   }
}
