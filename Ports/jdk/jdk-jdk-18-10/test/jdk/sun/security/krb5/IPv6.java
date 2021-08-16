/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6877357 6885166
 * @run main/othervm IPv6
 * @modules jdk.security.auth
 * @summary IPv6 address does not work
 */

import com.sun.security.auth.module.Krb5LoginModule;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.security.auth.Subject;

public class IPv6 {

    public static void main(String[] args) throws Exception {

        String[][] kdcs = {
                {"simple.host", null},  // These are legal settings
                {"simple.host", ""},
                {"simple.host", "8080"},
                {"0.0.0.1", null},
                {"0.0.0.1", ""},
                {"0.0.0.1", "8080"},
                {"1::1", null},
                {"[1::1]", null},
                {"[1::1]", ""},
                {"[1::1]", "8080"},
                {"[1::1", null},        // Two illegal settings
                {"[1::1]abc", null},
        };
        // Prepares a krb5.conf with every kind of KDC settings
        PrintStream out = new PrintStream(new FileOutputStream("ipv6.conf"));
        out.println("[libdefaults]");
        out.println("default_realm = V6");
        out.println("kdc_timeout = 1");
        out.println("[realms]");
        out.println("V6 = {");
        for (String[] hp: kdcs) {
            if (hp[1] != null) out.println("    kdc = "+hp[0]+":"+hp[1]);
            else out.println("    kdc = " + hp[0]);
        }
        out.println("}");
        out.close();

        System.setProperty("sun.security.krb5.debug", "true");
        System.setProperty("java.security.krb5.conf", "ipv6.conf");

        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        PrintStream po = new PrintStream(bo);
        PrintStream oldout = System.out;
        System.setOut(po);

        try {
            Subject subject = new Subject();
            Krb5LoginModule krb5 = new Krb5LoginModule();
            Map<String, String> map = new HashMap<>();
            Map<String, Object> shared = new HashMap<>();

            map.put("debug", "true");
            map.put("doNotPrompt", "true");
            map.put("useTicketCache", "false");
            map.put("useFirstPass", "true");
            shared.put("javax.security.auth.login.name", "any");
            shared.put("javax.security.auth.login.password", "any".toCharArray());
            krb5.initialize(subject, null, shared, map);
            krb5.login();
        } catch (Exception e) {
            // Ignore
        }

        po.flush();

        System.setOut(oldout);
        BufferedReader br = new BufferedReader(new StringReader(
                new String(bo.toByteArray())));
        int cc = 0;
        Pattern r = Pattern.compile(".*KrbKdcReq send: kdc=(.*) UDP:(\\d+),.*");
        String line;
        while ((line = br.readLine()) != null) {
            Matcher m = r.matcher(line.subSequence(0, line.length()));
            if (m.matches()) {
                System.out.println("------------------");
                System.out.println(line);
                String h = m.group(1), p = m.group(2);
                String eh = kdcs[cc][0], ep = kdcs[cc][1];
                if (eh.charAt(0) == '[') {
                    eh = eh.substring(1, eh.length()-1);
                }
                System.out.println("Expected: " + eh + " : " + ep);
                System.out.println("Actual: " + h + " : " + p);
                if (!eh.equals(h) ||
                        (ep == null || ep.length() == 0) && !p.equals("88") ||
                        (ep != null && ep.length() > 0) && !p.equals(ep)) {
                    throw new Exception("Mismatch");
                }
                cc++;
            }
        }
        if (cc != kdcs.length - 2) {    // 2 illegal settings at the end
            throw new Exception("Not traversed");
        }
    }
}
