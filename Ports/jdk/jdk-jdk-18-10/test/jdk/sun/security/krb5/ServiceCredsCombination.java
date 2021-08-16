/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8005447
 * @modules java.security.jgss/sun.security.jgss
 * @compile -XDignore.symbol.file ServiceCredsCombination.java
 * @run main ServiceCredsCombination
 * @summary default principal can act as anyone
 */

import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Objects;
import javax.security.auth.Subject;
import javax.security.auth.kerberos.KerberosKey;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.kerberos.KeyTab;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import sun.security.jgss.GSSUtil;

public class ServiceCredsCombination {

    public static void main(String[] args) throws Exception {
        // pass
        check("a", "a", princ("a"), key("a"));
        check(null, "a", princ("a"), key("a"));
        check("x", "NOCRED", princ("a"), key("a"));
        // two pass
        check("a", "a", princ("a"), key("a"), princ("b"), key("b"));
        check("b", "b", princ("a"), key("a"), princ("b"), key("b"));
        check(null, null, princ("a"), key("a"), princ("b"), key("b"));
        check("x", "NOCRED", princ("a"), key("a"), princ("b"), key("b"));
        // old ktab
        check("b", "b", princ("b"), oldktab());
        check("x", "NOCRED", princ("b"), oldktab());
        check(null, "b", princ("b"), oldktab());
        // Two old ktab
        check("a", "a", princ("a"), princ("b"), oldktab(), oldktab());
        check("b", "b", princ("a"), princ("b"), oldktab(), oldktab());
        check(null, null, princ("a"), princ("b"), oldktab(), oldktab());
        check("x", "NOCRED", princ("a"), princ("b"), oldktab(), oldktab());
        // bound ktab
        check("c", "c", princ("c"), ktab("c"));
        check(null, "c", princ("c"), ktab("c"));
        // unbound ktab
        check("x", "x", ktab());
        check(null, null, ktab());
        // Two bound ktab
        check("c1", "c1", princ("c1"), princ("c2"), ktab("c1"), ktab("c2"));
        check("c2", "c2", princ("c1"), princ("c2"), ktab("c1"), ktab("c2"));
        check("x", "NOCRED", princ("c1"), princ("c2"), ktab("c1"), ktab("c2"));
        check(null, null, princ("c1"), princ("c2"), ktab("c1"), ktab("c2"));
        // One bound, one unbound
        check("c1", "c1", princ("c1"), ktab("c1"), ktab());
        check("x", "x", princ("c1"), ktab("c1"), ktab());
        check(null, null, princ("c1"), ktab("c1"), ktab());
        // Two unbound ktab
        check("x", "x", ktab(), ktab());
        check(null, null, ktab(), ktab());
        // pass + old ktab
        check("a", "a", princ("a"), princ("b"), key("a"), oldktab());
        check("b", "b", princ("a"), princ("b"), key("a"), oldktab());
        check(null, null, princ("a"), princ("b"), key("a"), oldktab());
        check("x", "NOCRED", princ("a"), princ("b"), key("a"), oldktab());
        // pass + bound ktab
        check("a", "a", princ("a"), princ("c"), key("a"), ktab("c"));
        check("c", "c", princ("a"), princ("c"), key("a"), ktab("c"));
        check("x", "NOCRED", princ("a"), princ("c"), key("a"), ktab("c"));
        check(null, null, princ("a"), princ("c"), key("a"), ktab("c"));
        // pass + unbound ktab
        check("a", "a", princ("a"), key("a"), ktab());
        check("x", "x", princ("a"), key("a"), ktab());
        check(null, null, princ("a"), key("a"), ktab());
        // Compatibility, automatically add princ for keys
        check(null, "a", key("a"));
        check("x", "NOCRED", key("a"));
        check(null, "a", key("a"), oldktab());
        check("x", "NOCRED", key("a"), oldktab());
        // Limitation, "a" has no key, but we don't know oldktab() is for "b"
        check("a", "a", princ("a"), princ("b"), oldktab());
    }

    /**
     * Checks the correct bound
     * @param a get a creds for this principal, null for default one
     * @param b expected name, null for still unbound, "NOCRED" for no creds
     * @param objs princs, keys and keytabs in the subject
     */
    private static void check(final String a, String b, Object... objs)
            throws Exception {
        Subject subj = new Subject();
        for (Object obj: objs) {
            if (obj instanceof KerberosPrincipal) {
                subj.getPrincipals().add((KerberosPrincipal)obj);
            } else if (obj instanceof KerberosKey || obj instanceof KeyTab) {
                subj.getPrivateCredentials().add(obj);
            }
        }
        final GSSManager man = GSSManager.getInstance();
        try {
            String result = Subject.doAs(
                    subj, new PrivilegedExceptionAction<String>() {
                @Override
                public String run() throws GSSException {
                    GSSCredential cred = man.createCredential(
                            a == null ? null : man.createName(r(a), null),
                            GSSCredential.INDEFINITE_LIFETIME,
                            GSSUtil.GSS_KRB5_MECH_OID,
                            GSSCredential.ACCEPT_ONLY);
                    GSSName name = cred.getName();
                    return name == null ? null : name.toString();
                }
            });
            if (!Objects.equals(result, r(b))) {
                throw new Exception("Check failed: getInstance(" + a
                        + ") has name " + result + ", not " + b);
            }
        } catch (PrivilegedActionException e) {
            if (!"NOCRED".equals(b)) {
                throw new Exception("Check failed: getInstance(" + a
                        + ") is null " + ", but not one with name " + b);
            }
        }
    }
    private static String r(String s) {
        return s == null ? null : (s+"@REALM");
    }
    private static KerberosPrincipal princ(String s) {
        return new KerberosPrincipal(r(s));
    }
    private static KerberosKey key(String s) {
        return new KerberosKey(princ(s), new byte[0], 0, 0);
    }
    private static KeyTab oldktab() {
        return KeyTab.getInstance();
    }
    static KeyTab ktab(String s) {
        return KeyTab.getInstance(princ(s));
    }
    static KeyTab ktab() {
        return KeyTab.getUnboundInstance();
    }
}
