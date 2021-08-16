/*
 * Copyright (c) 1999, 2021, Oracle and/or its affiliates. All rights reserved.
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

package javax.security.auth;

import java.util.*;
import java.io.IOException;
import java.text.MessageFormat;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Principal;
import sun.security.util.ResourcesMgr;

/**
 * This class is used to protect access to private Credentials
 * belonging to a particular {@code Subject}.  The {@code Subject}
 * is represented by a Set of Principals.
 *
 * <p> The target name of this {@code Permission} specifies
 * a Credential class name, and a Set of Principals.
 * The only valid value for this Permission's actions is, "read".
 * The target name must abide by the following syntax:
 *
 * <pre>
 *      CredentialClass {PrincipalClass "PrincipalName"}*
 * </pre>
 *
 * For example, the following permission grants access to the
 * com.sun.PrivateCredential owned by Subjects which have
 * a com.sun.Principal with the name, "duke".  Note that although
 * this example, as well as all the examples below, do not contain
 * Codebase, SignedBy, or Principal information in the grant statement
 * (for simplicity reasons), actual policy configurations should
 * specify that information when appropriate.
 *
 * <pre>
 *
 *    grant {
 *      permission javax.security.auth.PrivateCredentialPermission
 *              "com.sun.PrivateCredential com.sun.Principal \"duke\"",
 *              "read";
 *    };
 * </pre>
 *
 * If CredentialClass is "*", then access is granted to
 * all private Credentials belonging to the specified
 * {@code Subject}.
 * If "PrincipalName" is "*", then access is granted to the
 * specified Credential owned by any {@code Subject} that has the
 * specified {@code Principal} (the actual PrincipalName doesn't matter).
 * For example, the following grants access to the
 * a.b.Credential owned by any {@code Subject} that has
 * an a.b.Principal.
 *
 * <pre>
 *    grant {
 *      permission javax.security.auth.PrivateCredentialPermission
 *              "a.b.Credential a.b.Principal "*"",
 *              "read";
 *    };
 * </pre>
 *
 * If both the PrincipalClass and "PrincipalName" are "*",
 * then access is granted to the specified Credential owned by
 * any {@code Subject}.
 *
 * <p> In addition, the PrincipalClass/PrincipalName pairing may be repeated:
 *
 * <pre>
 *    grant {
 *      permission javax.security.auth.PrivateCredentialPermission
 *              "a.b.Credential a.b.Principal "duke" c.d.Principal "dukette"",
 *              "read";
 *    };
 * </pre>
 *
 * The above grants access to the private Credential, "a.b.Credential",
 * belonging to a {@code Subject} with at least two associated Principals:
 * "a.b.Principal" with the name, "duke", and "c.d.Principal", with the name,
 * "dukette".
 *
 * @since 1.4
 */
public final class PrivateCredentialPermission extends Permission {

    @java.io.Serial
    private static final long serialVersionUID = 5284372143517237068L;

    private static final CredOwner[] EMPTY_PRINCIPALS = new CredOwner[0];

    /**
     * @serial
     */
    private String credentialClass;

    /**
     * @serial The Principals associated with this permission.
     *          The set contains elements of type,
     *          {@code PrivateCredentialPermission.CredOwner}.
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private Set<Principal> principals;  // ignored - kept around for compatibility
    private transient CredOwner[] credOwners;

    /**
     * @serial
     */
    private boolean testing = false;

    /**
     * Create a new {@code PrivateCredentialPermission}
     * with the specified {@code credentialClass} and Principals.
     */
    PrivateCredentialPermission(String credentialClass,
                        Set<Principal> principals) {

        super(credentialClass);
        this.credentialClass = credentialClass;

        synchronized(principals) {
            if (principals.size() == 0) {
                this.credOwners = EMPTY_PRINCIPALS;
            } else {
                this.credOwners = new CredOwner[principals.size()];
                int index = 0;
                Iterator<Principal> i = principals.iterator();
                while (i.hasNext()) {
                    Principal p = i.next();
                    this.credOwners[index++] = new CredOwner
                                                (p.getClass().getName(),
                                                p.getName());
                }
            }
        }
    }

    /**
     * Creates a new {@code PrivateCredentialPermission}
     * with the specified {@code name}.  The {@code name}
     * specifies both a Credential class and a {@code Principal} Set.
     *
     * @param name the name specifying the Credential class and
     *          {@code Principal} Set.
     *
     * @param actions the actions specifying that the Credential can be read.
     *
     * @throws IllegalArgumentException if {@code name} does not conform
     *          to the correct syntax or if {@code actions} is not "read".
     */
    public PrivateCredentialPermission(String name, String actions) {
        super(name);

        if (!"read".equalsIgnoreCase(actions))
            throw new IllegalArgumentException
                (ResourcesMgr.getString("actions.can.only.be.read."));
        init(name);
    }

    /**
     * Returns the Class name of the Credential associated with this
     * {@code PrivateCredentialPermission}.
     *
     * @return the Class name of the Credential associated with this
     *          {@code PrivateCredentialPermission}.
     */
    public String getCredentialClass() {
        return credentialClass;
    }

    /**
     * Returns the {@code Principal} classes and names
     * associated with this {@code PrivateCredentialPermission}.
     * The information is returned as a two-dimensional array (array[x][y]).
     * The 'x' value corresponds to the number of {@code Principal}
     * class and name pairs.  When (y==0), it corresponds to
     * the {@code Principal} class value, and when (y==1),
     * it corresponds to the {@code Principal} name value.
     * For example, array[0][0] corresponds to the class name of
     * the first {@code Principal} in the array.  array[0][1]
     * corresponds to the {@code Principal} name of the
     * first {@code Principal} in the array.
     *
     * @return the {@code Principal} class and names associated
     *          with this {@code PrivateCredentialPermission}.
     */
    public String[][] getPrincipals() {

        if (credOwners == null || credOwners.length == 0) {
            return new String[0][0];
        }

        String[][] pArray = new String[credOwners.length][2];
        for (int i = 0; i < credOwners.length; i++) {
            pArray[i][0] = credOwners[i].principalClass;
            pArray[i][1] = credOwners[i].principalName;
        }
        return pArray;
    }

    /**
     * Checks if this {@code PrivateCredentialPermission} implies
     * the specified {@code Permission}.
     *
     * <p>
     *
     * This method returns true if:
     * <ul>
     * <li> {@code p} is an instanceof PrivateCredentialPermission and
     * <li> the target name for {@code p} is implied by this object's
     *          target name.  For example:
     * <pre>
     *  [* P1 "duke"] implies [a.b.Credential P1 "duke"].
     *  [C1 P1 "duke"] implies [C1 P1 "duke" P2 "dukette"].
     *  [C1 P2 "dukette"] implies [C1 P1 "duke" P2 "dukette"].
     * </pre>
     * </ul>
     *
     * @param p the {@code Permission} to check against.
     *
     * @return true if this {@code PrivateCredentialPermission} implies
     * the specified {@code Permission}, false if not.
     */
    public boolean implies(Permission p) {

        if (p == null || !(p instanceof PrivateCredentialPermission))
            return false;

        PrivateCredentialPermission that = (PrivateCredentialPermission)p;

        if (!impliesCredentialClass(credentialClass, that.credentialClass))
            return false;

        return impliesPrincipalSet(credOwners, that.credOwners);
    }

    /**
     * Checks two {@code PrivateCredentialPermission} objects for
     * equality.  Checks that {@code obj} is a
     * {@code PrivateCredentialPermission},
     * and has the same credential class as this object,
     * as well as the same Principals as this object.
     * The order of the Principals in the respective Permission's
     * target names is not relevant.
     *
     * @param obj the object we are testing for equality with this object.
     *
     * @return true if obj is a {@code PrivateCredentialPermission},
     *          has the same credential class as this object,
     *          and has the same Principals as this object.
     */
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (! (obj instanceof PrivateCredentialPermission))
            return false;

        PrivateCredentialPermission that = (PrivateCredentialPermission)obj;

        return (this.implies(that) && that.implies(this));
    }

    /**
     * Returns the hash code value for this object.
     *
     * @return a hash code value for this object.
     */
    public int hashCode() {
        return this.credentialClass.hashCode();
    }

    /**
     * Returns the "canonical string representation" of the actions.
     * This method always returns the String, "read".
     *
     * @return the actions (always returns "read").
     */
    public String getActions() {
        return "read";
    }

    /**
     * Return a homogeneous collection of PrivateCredentialPermissions
     * in a {@code PermissionCollection}.
     * No such {@code PermissionCollection} is defined,
     * so this method always returns {@code null}.
     *
     * @return null in all cases.
     */
    public PermissionCollection newPermissionCollection() {
        return null;
    }

    private void init(String name) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("invalid empty name");
        }

        ArrayList<CredOwner> pList = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(name, " ", true);
        String principalClass = null;
        String principalName = null;

        if (testing)
            System.out.println("whole name = " + name);

        // get the Credential Class
        credentialClass = tokenizer.nextToken();
        if (testing)
            System.out.println("Credential Class = " + credentialClass);

        if (tokenizer.hasMoreTokens() == false) {
            MessageFormat form = new MessageFormat(ResourcesMgr.getString
                ("permission.name.name.syntax.invalid."));
            Object[] source = {name};
            throw new IllegalArgumentException
                (form.format(source) + ResourcesMgr.getString
                        ("Credential.Class.not.followed.by.a.Principal.Class.and.Name"));
        }

        while (tokenizer.hasMoreTokens()) {

            // skip delimiter
            tokenizer.nextToken();

            // get the Principal Class
            principalClass = tokenizer.nextToken();
            if (testing)
                System.out.println("    Principal Class = " + principalClass);

            if (tokenizer.hasMoreTokens() == false) {
                MessageFormat form = new MessageFormat(ResourcesMgr.getString
                        ("permission.name.name.syntax.invalid."));
                Object[] source = {name};
                throw new IllegalArgumentException
                        (form.format(source) + ResourcesMgr.getString
                        ("Principal.Class.not.followed.by.a.Principal.Name"));
            }

            // skip delimiter
            tokenizer.nextToken();

            // get the Principal Name
            principalName = tokenizer.nextToken();

            if (!principalName.startsWith("\"")) {
                MessageFormat form = new MessageFormat(ResourcesMgr.getString
                        ("permission.name.name.syntax.invalid."));
                Object[] source = {name};
                throw new IllegalArgumentException
                        (form.format(source) + ResourcesMgr.getString
                        ("Principal.Name.must.be.surrounded.by.quotes"));
            }

            if (!principalName.endsWith("\"")) {

                // we have a name with spaces in it --
                // keep parsing until we find the end quote,
                // and keep the spaces in the name

                while (tokenizer.hasMoreTokens()) {
                    principalName = principalName + tokenizer.nextToken();
                    if (principalName.endsWith("\""))
                        break;
                }

                if (!principalName.endsWith("\"")) {
                    MessageFormat form = new MessageFormat
                        (ResourcesMgr.getString
                        ("permission.name.name.syntax.invalid."));
                    Object[] source = {name};
                    throw new IllegalArgumentException
                        (form.format(source) + ResourcesMgr.getString
                                ("Principal.Name.missing.end.quote"));
                }
            }

            if (testing)
                System.out.println("\tprincipalName = '" + principalName + "'");

            principalName = principalName.substring
                                        (1, principalName.length() - 1);

            if (principalClass.equals("*") &&
                !principalName.equals("*")) {
                    throw new IllegalArgumentException(ResourcesMgr.getString
                        ("PrivateCredentialPermission.Principal.Class.can.not.be.a.wildcard.value.if.Principal.Name.is.not.a.wildcard.value"));
            }

            if (testing)
                System.out.println("\tprincipalName = '" + principalName + "'");

            pList.add(new CredOwner(principalClass, principalName));
        }

        this.credOwners = new CredOwner[pList.size()];
        pList.toArray(this.credOwners);
    }

    private boolean impliesCredentialClass(String thisC, String thatC) {

        // this should never happen
        if (thisC == null || thatC == null)
            return false;

        if (testing)
            System.out.println("credential class comparison: " +
                                thisC + "/" + thatC);

        if (thisC.equals("*"))
            return true;

        /**
         * XXX let's not enable this for now --
         *      if people want it, we'll enable it later
         */
        /*
        if (thisC.endsWith("*")) {
            String cClass = thisC.substring(0, thisC.length() - 2);
            return thatC.startsWith(cClass);
        }
        */

        return thisC.equals(thatC);
    }

    private boolean impliesPrincipalSet(CredOwner[] thisP, CredOwner[] thatP) {

        // this should never happen
        if (thisP == null || thatP == null)
            return false;

        if (thatP.length == 0)
            return true;

        if (thisP.length == 0)
            return false;

        for (int i = 0; i < thisP.length; i++) {
            boolean foundMatch = false;
            for (int j = 0; j < thatP.length; j++) {
                if (thisP[i].implies(thatP[j])) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch) {
                return false;
            }
        }
        return true;
    }

    /**
     * Reads this object from a stream (i.e., deserializes it)
     *
     * @param  s the {@code ObjectInputStream} from which data is read
     * @throws IOException if an I/O error occurs
     * @throws ClassNotFoundException if a serialized class cannot be loaded
     */
    @java.io.Serial
    private void readObject(java.io.ObjectInputStream s) throws
                                        IOException,
                                        ClassNotFoundException {

        s.defaultReadObject();

        // perform new initialization from the permission name

        if (getName().indexOf(' ') == -1 && getName().indexOf('"') == -1) {

            // name only has a credential class specified
            credentialClass = getName();
            credOwners = EMPTY_PRINCIPALS;

        } else {

            // perform regular initialization
            init(getName());
        }
    }

    /**
     * @serial include
     */
    static class CredOwner implements java.io.Serializable {

        @java.io.Serial
        private static final long serialVersionUID = -5607449830436408266L;

        /**
         * @serial
         */
        String principalClass;
        /**
         * @serial
         */
        String principalName;

        CredOwner(String principalClass, String principalName) {
            this.principalClass = principalClass;
            this.principalName = principalName;
        }

        public boolean implies(Object obj) {
            if (obj == null || !(obj instanceof CredOwner))
                return false;

            CredOwner that = (CredOwner)obj;

            if (principalClass.equals("*") ||
                principalClass.equals(that.principalClass)) {

                if (principalName.equals("*") ||
                    principalName.equals(that.principalName)) {
                    return true;
                }
            }

            /**
             * XXX no code yet to support a.b.*
             */

            return false;
        }

        public String toString() {
            MessageFormat form = new MessageFormat(ResourcesMgr.getString
                ("CredOwner.Principal.Class.class.Principal.Name.name"));
            Object[] source = {principalClass, principalName};
            return (form.format(source));
        }
    }
}
