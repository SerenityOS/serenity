/*
 * Copyright (c) 2011, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package javax.security.auth.kerberos;

import java.io.File;
import java.security.AccessControlException;
import java.util.Objects;
import sun.security.krb5.EncryptionKey;
import sun.security.krb5.KerberosSecrets;
import sun.security.krb5.PrincipalName;
import sun.security.krb5.RealmException;

/**
 * This class encapsulates a keytab file.
 * <p>
 * A Kerberos JAAS login module that obtains long term secret keys from a
 * keytab file should use this class. The login module will store
 * an instance of this class in the private credential set of a
 * {@link javax.security.auth.Subject Subject} during the commit phase of the
 * authentication process.
 * <p>
 * If a {@code KeyTab} object is obtained from {@link #getUnboundInstance()}
 * or {@link #getUnboundInstance(java.io.File)}, it is unbound and thus can be
 * used by any service principal. Otherwise, if it's obtained from
 * {@link #getInstance(KerberosPrincipal)} or
 * {@link #getInstance(KerberosPrincipal, java.io.File)}, it is bound to the
 * specific service principal and can only be used by it.
 * <p>
 * Please note the constructors {@link #getInstance()} and
 * {@link #getInstance(java.io.File)} were created when there was no support
 * for unbound keytabs. These methods should not be used anymore. An object
 * created with either of these methods are considered to be bound to an
 * unknown principal, which means, its {@link #isBound()} returns true and
 * {@link #getPrincipal()} returns null.
 * <p>
 * It might be necessary for the application to be granted a
 * {@link javax.security.auth.PrivateCredentialPermission
 * PrivateCredentialPermission} if it needs to access the {@code KeyTab}
 * instance from a {@code Subject}. This permission is not needed when the
 * application depends on the default JGSS Kerberos mechanism to access the
 * {@code KeyTab}. In that case, however, the application will need an appropriate
 * {@link javax.security.auth.kerberos.ServicePermission ServicePermission}.
 * <p>
 * The keytab file format is described at
 * <a href="http://www.ioplex.com/utilities/keytab.txt">
 * http://www.ioplex.com/utilities/keytab.txt</a>.
 *
 * @since 1.7
 */
public final class KeyTab {

    /*
     * Impl notes:
     *
     * This class is only a name, a permanent link to the keytab source
     * (can be missing). Itself has no content. In order to read content,
     * take a snapshot and read from it.
     *
     * The snapshot is of type sun.security.krb5.internal.ktab.KeyTab, which
     * contains the content of the keytab file when the snapshot is taken.
     * Itself has no refresh function and mostly an immutable class (except
     * for the create/add/save methods only used by the ktab command).
     */

    // Source, null if using the default one. Note that the default name
    // is maintained in snapshot, this field is never "resolved".
    private final File file;

    // Bound user: normally from the "principal" value in a JAAS krb5
    // login conf. Will be null if it's "*".
    private final KerberosPrincipal princ;

    private final boolean bound;

    // Set up JavaxSecurityAuthKerberosAccess in KerberosSecrets
    static {
        KerberosSecrets.setJavaxSecurityAuthKerberosAccess(
                new JavaxSecurityAuthKerberosAccessImpl());
    }

    private KeyTab(KerberosPrincipal princ, File file, boolean bound) {
        this.princ = princ;
        this.file = file;
        this.bound = bound;
    }

    /**
     * Returns a {@code KeyTab} instance from a {@code File} object
     * that is bound to an unknown service principal.
     * <p>
     * The result of this method is never null. This method only associates
     * the returned {@code KeyTab} object with the file and does not read it.
     * <p>
     * Developers should call {@link #getInstance(KerberosPrincipal,File)}
     * when the bound service principal is known.
     * @param file the keytab {@code File} object, must not be null
     * @return the keytab instance
     * @throws NullPointerException if the {@code file} argument is null
     */
    public static KeyTab getInstance(File file) {
        if (file == null) {
            throw new NullPointerException("file must be non null");
        }
        return new KeyTab(null, file, true);
    }

    /**
     * Returns an unbound {@code KeyTab} instance from a {@code File}
     * object.
     * <p>
     * The result of this method is never null. This method only associates
     * the returned {@code KeyTab} object with the file and does not read it.
     * @param file the keytab {@code File} object, must not be null
     * @return the keytab instance
     * @throws NullPointerException if the file argument is null
     * @since 1.8
     */
    public static KeyTab getUnboundInstance(File file) {
        if (file == null) {
            throw new NullPointerException("file must be non null");
        }
        return new KeyTab(null, file, false);
    }

    /**
     * Returns a {@code KeyTab} instance from a {@code File} object
     * that is bound to the specified service principal.
     * <p>
     * The result of this method is never null. This method only associates
     * the returned {@code KeyTab} object with the file and does not read it.
     * @param princ the bound service principal, must not be null
     * @param file the keytab {@code File} object, must not be null
     * @return the keytab instance
     * @throws NullPointerException if either of the arguments is null
     * @since 1.8
     */
    public static KeyTab getInstance(KerberosPrincipal princ, File file) {
        if (princ == null) {
            throw new NullPointerException("princ must be non null");
        }
        if (file == null) {
            throw new NullPointerException("file must be non null");
        }
        return new KeyTab(princ, file, true);
    }

    /**
     * Returns the default {@code KeyTab} instance that is bound
     * to an unknown service principal.
     * <p>
     * The result of this method is never null. This method only associates
     * the returned {@code KeyTab} object with the default keytab file and
     * does not read it.
     * <p>
     * Developers should call {@link #getInstance(KerberosPrincipal)}
     * when the bound service principal is known.
     * @return the default keytab instance.
     */
    public static KeyTab getInstance() {
        return new KeyTab(null, null, true);
    }

    /**
     * Returns the default unbound {@code KeyTab} instance.
     * <p>
     * The result of this method is never null. This method only associates
     * the returned {@code KeyTab} object with the default keytab file and
     * does not read it.
     * @return the default keytab instance
     * @since 1.8
     */
    public static KeyTab getUnboundInstance() {
        return new KeyTab(null, null, false);
    }

    /**
     * Returns the default {@code KeyTab} instance that is bound
     * to the specified service principal.
     * <p>
     * The result of this method is never null. This method only associates
     * the returned {@code KeyTab} object with the default keytab file and
     * does not read it.
     * @param princ the bound service principal, must not be null
     * @return the default keytab instance
     * @throws NullPointerException if {@code princ} is null
     * @since 1.8
     */
    public static KeyTab getInstance(KerberosPrincipal princ) {
        if (princ == null) {
            throw new NullPointerException("princ must be non null");
        }
        return new KeyTab(princ, null, true);
    }

    // Takes a snapshot of the keytab content. This method is called by
    // JavaxSecurityAuthKerberosAccessImpl so no more private
    sun.security.krb5.internal.ktab.KeyTab takeSnapshot() {
        try {
            return sun.security.krb5.internal.ktab.KeyTab.getInstance(file);
        } catch (@SuppressWarnings("removal") AccessControlException ace) {
            if (file != null) {
                // It's OK to show the name if caller specified it
                throw ace;
            } else {
                @SuppressWarnings("removal")
                AccessControlException ace2 = new AccessControlException(
                        "Access to default keytab denied (modified exception)");
                ace2.setStackTrace(ace.getStackTrace());
                throw ace2;
            }
        }
    }

    /**
     * Returns fresh keys for the given Kerberos principal.
     * <p>
     * Implementation of this method should make sure the returned keys match
     * the latest content of the keytab file. The result is a newly created
     * copy that can be modified by the caller without modifying the keytab
     * object. The caller should {@link KerberosKey#destroy() destroy} the
     * result keys after they are used.
     * <p>
     * Please note that the keytab file can be created after the
     * {@code KeyTab} object is instantiated and its content may change over
     * time. Therefore, an application should call this method only when it
     * needs to use the keys. Any previous result from an earlier invocation
     * could potentially be expired.
     * <p>
     * If there is any error (say, I/O error or format error)
     * during the reading process of the keytab file, a saved result should be
     * returned. If there is no saved result (say, this is the first time this
     * method is called, or, all previous read attempts failed), an empty array
     * should be returned. This can make sure the result is not drastically
     * changed during the (probably slow) update of the keytab file.
     * <p>
     * Each time this method is called and the reading of the file succeeds
     * with no exception (say, I/O error or file format error),
     * the result should be saved for {@code principal}. The implementation can
     * also save keys for other principals having keys in the same keytab object
     * if convenient.
     * <p>
     * Any unsupported key read from the keytab is ignored and not included
     * in the result.
     * <p>
     * If this keytab is bound to a specific principal, calling this method on
     * another principal will return an empty array.
     *
     * @param principal the Kerberos principal, must not be null.
     * @return the keys (never null, may be empty)
     * @throws NullPointerException if the {@code principal}
     * argument is null
     * @throws SecurityException if a security manager exists and the read
     * access to the keytab file is not permitted
     */
    public KerberosKey[] getKeys(KerberosPrincipal principal) {
        try {
            if (princ != null && !principal.equals(princ)) {
                return new KerberosKey[0];
            }
            PrincipalName pn = new PrincipalName(principal.getName());
            EncryptionKey[] keys = takeSnapshot().readServiceKeys(pn);
            KerberosKey[] kks = new KerberosKey[keys.length];
            for (int i=0; i<kks.length; i++) {
                Integer tmp = keys[i].getKeyVersionNumber();
                kks[i] = new KerberosKey(
                        principal,
                        keys[i].getBytes(),
                        keys[i].getEType(),
                        tmp == null ? 0 : tmp.intValue());
                keys[i].destroy();
            }
            return kks;
        } catch (RealmException re) {
            return new KerberosKey[0];
        }
    }

    EncryptionKey[] getEncryptionKeys(PrincipalName principal) {
        return takeSnapshot().readServiceKeys(principal);
    }

    /**
     * Checks if the keytab file exists. Implementation of this method
     * should make sure that the result matches the latest status of the
     * keytab file.
     *
     * @return true if the keytab file exists; false otherwise.
     * @throws SecurityException if a security manager exists and the read
     * access to the keytab file is not permitted
     */
    public boolean exists() {
        return !takeSnapshot().isMissing();
    }

    /**
     * Returns an informative textual representation of this {@code KeyTab}.
     *
     * @return an informative textual representation of this {@code KeyTab}.
     */
    public String toString() {
        String s = (file == null) ? "Default keytab" : file.toString();
        if (!bound) return s;
        else if (princ == null) return s + " for someone";
        else return s + " for " + princ;
    }

    /**
     * Returns a hash code for this {@code KeyTab}.
     *
     * @return a hash code for this {@code KeyTab}.
     */
    public int hashCode() {
        return Objects.hash(file, princ, bound);
    }

    /**
     * Compares the specified object with this {@code KeyTab} for equality.
     * Returns true if the given object is also a
     * {@code KeyTab} and the two
     * {@code KeyTab} instances are equivalent.
     *
     * @param other the object to compare to
     * @return true if the specified object is equal to this {@code KeyTab}
     */
    public boolean equals(Object other) {
        if (other == this)
            return true;

        if (! (other instanceof KeyTab)) {
            return false;
        }

        KeyTab otherKtab = (KeyTab) other;
        return Objects.equals(otherKtab.princ, princ) &&
                Objects.equals(otherKtab.file, file) &&
                bound == otherKtab.bound;
    }

    /**
     * Returns the service principal this {@code KeyTab} object
     * is bound to. Returns {@code null} if it's not bound.
     * <p>
     * Please note the deprecated constructors create a {@code KeyTab} object
     * bound for some unknown principal. In this case, this method also returns
     * null. User can call {@link #isBound()} to verify this case.
     * @return the service principal
     * @since 1.8
     */
    public KerberosPrincipal getPrincipal() {
        return princ;
    }

    /**
     * Returns if the keytab is bound to a principal
     * @return if the keytab is bound to a principal
     * @since 1.8
     */
    public boolean isBound() {
        return bound;
    }
}
