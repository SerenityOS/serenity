/*
 * Copyright (c) 2007, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4634392
 * @summary JDK code doesn't respect contract for equals and hashCode
 * @author Andrew Fan
 */

import org.ietf.jgss.*;

public class Krb5NameEquals {

    private static String NAME_STR1 = "service@localhost";
    private static String NAME_STR2 = "service2@localhost";
    private static final Oid MECH;

    static {
        Oid temp = null;
        try {
            temp = new Oid("1.2.840.113554.1.2.2"); // KRB5
        } catch (Exception e) {
            // should never happen
        }
        MECH = temp;
    }

    public static void main(String[] argv) throws Exception {
        GSSManager mgr = GSSManager.getInstance();

        boolean result = true;
        // Create GSSName and check their equals(), hashCode() impl
        GSSName name1 = mgr.createName(NAME_STR1,
            GSSName.NT_HOSTBASED_SERVICE, MECH);
        GSSName name2 = mgr.createName(NAME_STR2,
            GSSName.NT_HOSTBASED_SERVICE, MECH);
        GSSName name3 = mgr.createName(NAME_STR1,
            GSSName.NT_HOSTBASED_SERVICE, MECH);

        if (!name1.equals(name1) || !name1.equals(name3) ||
            !name1.equals((Object) name1) ||
            !name1.equals((Object) name3)) {
            System.out.println("Error: should be the same name");
            result = false;
        } else if (name1.hashCode() != name3.hashCode()) {
            System.out.println("Error: should have same hash");
            result = false;
        }

        if (name1.equals(name2) || name1.equals((Object) name2)) {
            System.out.println("Error: should be different names");
            result = false;
        }
        if (result) {
            System.out.println("Done");
        } else System.exit(1);
    }
}
