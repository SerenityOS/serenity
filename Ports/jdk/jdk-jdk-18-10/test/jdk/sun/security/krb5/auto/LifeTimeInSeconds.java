/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6857802 8194486
 * @summary GSS getRemainingInitLifetime method returns milliseconds not seconds
 * @library /test/lib
 * @run main jdk.test.lib.FileInstaller TestHosts TestHosts
 * @run main/othervm -Djdk.net.hosts.file=TestHosts LifeTimeInSeconds
 */
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSManager;

public class LifeTimeInSeconds {
    public static void main(String[] args) throws Exception {
        new OneKDC(null).writeJAASConf();
        System.setProperty("javax.security.auth.useSubjectCredsOnly", "false");

        GSSManager gm = GSSManager.getInstance();
        GSSCredential cred = gm.createCredential(GSSCredential.INITIATE_AND_ACCEPT);
        int time = cred.getRemainingLifetime();
        int time2 = cred.getRemainingInitLifetime(null);
        // The test KDC issues a TGT with a default lifetime of 11 hours
        int elevenhrs = KDC.DEFAULT_LIFETIME;
        if (time > elevenhrs+60 || time < elevenhrs-60) {
            throw new Exception("getRemainingLifetime returns wrong value.");
        }
        if (time2 > elevenhrs+60 || time2 < elevenhrs-60) {
            throw new Exception("getRemainingInitLifetime returns wrong value.");
        }
    }
}
