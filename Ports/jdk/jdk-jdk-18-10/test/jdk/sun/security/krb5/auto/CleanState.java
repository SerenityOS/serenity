/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6716534 8194486
 * @summary Krb5LoginModule has not cleaned temp info between authentication attempts
 * @library /test/lib
 * @compile -XDignore.symbol.file CleanState.java
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts CleanState
 */
import com.sun.security.auth.module.Krb5LoginModule;
import java.util.HashMap;
import java.util.Map;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;

public class CleanState {
    public static void main(String[] args) throws Exception {
        CleanState x = new CleanState();
        new OneKDC(null);
        x.go();
    }

    void go() throws Exception {
        Krb5LoginModule krb5 = new Krb5LoginModule();

        final String name = OneKDC.USER;
        final char[] password = OneKDC.PASS;
        char[] badpassword = "hellokitty".toCharArray();

        Map<String,String> map = new HashMap<>();
        map.put("useTicketCache", "false");
        map.put("doNotPrompt", "false");
        map.put("tryFirstPass", "true");
        Map<String,Object> shared = new HashMap<>();
        shared.put("javax.security.auth.login.name", name);
        shared.put("javax.security.auth.login.password", badpassword);

        krb5.initialize(new Subject(), new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) {
                for(Callback callback: callbacks) {
                    if (callback instanceof NameCallback) {
                        ((NameCallback)callback).setName(name);
                    }
                    if (callback instanceof PasswordCallback) {
                        ((PasswordCallback)callback).setPassword(password);
                    }
                }
            }
        }, shared, map);
        krb5.login();
    }
}
