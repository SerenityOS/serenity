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

package sun.security.provider;

import java.net.URL;
import java.util.*;
import java.security.CodeSource;
import java.security.Principal;
import java.security.cert.Certificate;
import java.lang.reflect.Constructor;

import javax.security.auth.Subject;
import sun.security.provider.PolicyParser.PrincipalEntry;
import sun.security.util.ResourcesMgr;

/**
 * <p> This <code>SubjectCodeSource</code> class contains
 * a <code>URL</code>, signer certificates, and either a <code>Subject</code>
 * (that represents the <code>Subject</code> in the current
 * <code>AccessControlContext</code>), or a linked list of Principals
 * (that represent a "subject" in a <code>Policy</code>).
 *
 */
class SubjectCodeSource extends CodeSource implements java.io.Serializable {

    @java.io.Serial
    private static final long serialVersionUID = 6039418085604715275L;

    private Subject subject;
    private LinkedList<PrincipalEntry> principals;
    private static final Class<?>[] PARAMS = { String.class };
    private static final sun.security.util.Debug debug =
        sun.security.util.Debug.getInstance("auth", "\t[Auth Access]");
    @SuppressWarnings("serial") // Not statically typed as Serializable
    private ClassLoader sysClassLoader;

    /**
     * Creates a new <code>SubjectCodeSource</code>
     * with the given <code>Subject</code>, principals, <code>URL</code>,
     * and signers (Certificates).  The <code>Subject</code>
     * represents the <code>Subject</code> associated with the current
     * <code>AccessControlContext</code>.
     * The Principals are given as a <code>LinkedList</code>
     * of <code>PolicyParser.PrincipalEntry</code> objects.
     * Typically either a <code>Subject</code> will be provided,
     * or a list of <code>principals</code> will be provided
     * (not both).
     *
     * <p>
     *
     * @param subject the <code>Subject</code> associated with this
     *                  <code>SubjectCodeSource</code> <p>
     *
     * @param url the <code>URL</code> associated with this
     *                  <code>SubjectCodeSource</code> <p>
     *
     * @param certs the signers associated with this
     *                  <code>SubjectCodeSource</code> <p>
     */
    @SuppressWarnings("removal")
    SubjectCodeSource(Subject subject,
        LinkedList<PrincipalEntry> principals,
        URL url, Certificate[] certs) {

        super(url, certs);
        this.subject = subject;
        this.principals = (principals == null ?
                new LinkedList<PrincipalEntry>() :
                new LinkedList<PrincipalEntry>(principals));
        sysClassLoader = java.security.AccessController.doPrivileged
        (new java.security.PrivilegedAction<ClassLoader>() {
            public ClassLoader run() {
                    return ClassLoader.getSystemClassLoader();
            }
        });
    }

    /**
     * Get the Principals associated with this <code>SubjectCodeSource</code>.
     * The Principals are retrieved as a <code>LinkedList</code>
     * of <code>PolicyParser.PrincipalEntry</code> objects.
     *
     * <p>
     *
     * @return the Principals associated with this
     *          <code>SubjectCodeSource</code> as a <code>LinkedList</code>
     *          of <code>PolicyParser.PrincipalEntry</code> objects.
     */
    LinkedList<PrincipalEntry> getPrincipals() {
        return principals;
    }

    /**
     * Get the <code>Subject</code> associated with this
     * <code>SubjectCodeSource</code>.  The <code>Subject</code>
     * represents the <code>Subject</code> associated with the
     * current <code>AccessControlContext</code>.
     *
     * <p>
     *
     * @return the <code>Subject</code> associated with this
     *          <code>SubjectCodeSource</code>.
     */
    Subject getSubject() {
        return subject;
    }

    /**
     * Returns true if this <code>SubjectCodeSource</code> object "implies"
     * the specified <code>CodeSource</code>.
     * More specifically, this method makes the following checks.
     * If any fail, it returns false.  If they all succeed, it returns true.
     *
     * <p>
     * <ol>
     * <li> The provided codesource must not be <code>null</code>.
     * <li> codesource must be an instance of <code>SubjectCodeSource</code>.
     * <li> super.implies(codesource) must return true.
     * <li> for each principal in this codesource's principal list:
     * <ol>
     * <li>     if the principal is an instanceof
     *          <code>Principal</code>, then the principal must
     *          imply the provided codesource's <code>Subject</code>.
     * <li>     if the principal is not an instanceof
     *          <code>Principal</code>, then the provided
     *          codesource's <code>Subject</code> must have an
     *          associated <code>Principal</code>, <i>P</i>, where
     *          P.getClass().getName equals principal.principalClass,
     *          and P.getName() equals principal.principalName.
     * </ol>
     * </ol>
     *
     * <p>
     *
     * @param codesource the <code>CodeSource</code> to compare against.
     *
     * @return true if this <code>SubjectCodeSource</code> implies
     *          the specified <code>CodeSource</code>.
     */
    public boolean implies(CodeSource codesource) {

        LinkedList<PrincipalEntry> subjectList = null;

        if (codesource == null ||
            !(codesource instanceof SubjectCodeSource) ||
            !(super.implies(codesource))) {

            if (debug != null)
                debug.println("\tSubjectCodeSource.implies: FAILURE 1");
            return false;
        }

        SubjectCodeSource that = (SubjectCodeSource)codesource;

        // if the principal list in the policy "implies"
        // the Subject associated with the current AccessControlContext,
        // then return true

        if (this.principals == null) {
            if (debug != null)
                debug.println("\tSubjectCodeSource.implies: PASS 1");
            return true;
        }

        if (that.getSubject() == null ||
            that.getSubject().getPrincipals().size() == 0) {
            if (debug != null)
                debug.println("\tSubjectCodeSource.implies: FAILURE 2");
            return false;
        }

        ListIterator<PrincipalEntry> li = this.principals.listIterator(0);
        while (li.hasNext()) {
            PrincipalEntry pppe = li.next();
            try {

                // use new Principal.implies method

                Class<?> pClass = Class.forName(pppe.principalClass,
                                                true, sysClassLoader);
                if (!Principal.class.isAssignableFrom(pClass)) {
                    // not the right subtype
                    throw new ClassCastException(pppe.principalClass +
                                                 " is not a Principal");
                }
                Constructor<?> c = pClass.getConstructor(PARAMS);
                Principal p = (Principal)c.newInstance(new Object[] {
                                                       pppe.principalName });

                if (!p.implies(that.getSubject())) {
                    if (debug != null)
                        debug.println("\tSubjectCodeSource.implies: FAILURE 3");
                    return false;
                } else {
                    if (debug != null)
                        debug.println("\tSubjectCodeSource.implies: PASS 2");
                    return true;
                }
            } catch (Exception e) {

                // simply compare Principals

                if (subjectList == null) {

                    if (that.getSubject() == null) {
                        if (debug != null)
                            debug.println("\tSubjectCodeSource.implies: " +
                                        "FAILURE 4");
                        return false;
                    }
                    Iterator<Principal> i =
                                that.getSubject().getPrincipals().iterator();

                    subjectList = new LinkedList<PrincipalEntry>();
                    while (i.hasNext()) {
                        Principal p = i.next();
                        PrincipalEntry spppe = new PrincipalEntry
                                (p.getClass().getName(), p.getName());
                        subjectList.add(spppe);
                    }
                }

                if (!subjectListImpliesPrincipalEntry(subjectList, pppe)) {
                    if (debug != null)
                        debug.println("\tSubjectCodeSource.implies: FAILURE 5");
                    return false;
                }
            }
        }

        if (debug != null)
            debug.println("\tSubjectCodeSource.implies: PASS 3");
        return true;
    }

    /**
     * This method returns, true, if the provided <i>subjectList</i>
     * "contains" the <code>Principal</code> specified
     * in the provided <i>pppe</i> argument.
     *
     * Note that the provided <i>pppe</i> argument may have
     * wildcards (*) for the <code>Principal</code> class and name,
     * which need to be considered.
     *
     * <p>
     *
     * @param subjectList a list of PolicyParser.PrincipalEntry objects
     *          that correspond to all the Principals in the Subject currently
     *          on this thread's AccessControlContext. <p>
     *
     * @param pppe the Principals specified in a grant entry.
     *
     * @return true if the provided <i>subjectList</i> "contains"
     *          the <code>Principal</code> specified in the provided
     *          <i>pppe</i> argument.
     */
    private boolean subjectListImpliesPrincipalEntry(
                LinkedList<PrincipalEntry> subjectList, PrincipalEntry pppe) {

        ListIterator<PrincipalEntry> li = subjectList.listIterator(0);
        while (li.hasNext()) {
            PrincipalEntry listPppe = li.next();

            if (pppe.getPrincipalClass().equals
                        (PrincipalEntry.WILDCARD_CLASS) ||
                pppe.getPrincipalClass().equals(listPppe.getPrincipalClass()))
            {
                if (pppe.getPrincipalName().equals
                        (PrincipalEntry.WILDCARD_NAME) ||
                    pppe.getPrincipalName().equals(listPppe.getPrincipalName()))
                    return true;
            }
        }
        return false;
    }

    /**
     * Tests for equality between the specified object and this
     * object. Two <code>SubjectCodeSource</code> objects are considered equal
     * if their locations are of identical value, if the two sets of
     * Certificates are of identical values, and if the
     * Subjects are equal, and if the PolicyParser.PrincipalEntry values
     * are of identical values.  It is not required that
     * the Certificates or PolicyParser.PrincipalEntry values
     * be in the same order.
     *
     * <p>
     *
     * @param obj the object to test for equality with this object.
     *
     * @return true if the objects are considered equal, false otherwise.
     */
    public boolean equals(Object obj) {

        if (obj == this)
            return true;

        if (super.equals(obj) == false)
            return false;

        if (!(obj instanceof SubjectCodeSource))
            return false;

        SubjectCodeSource that = (SubjectCodeSource)obj;

        // the principal lists must match
        try {
            if (this.getSubject() != that.getSubject())
                return false;
        } catch (SecurityException se) {
            return false;
        }

        if ((this.principals == null && that.principals != null) ||
            (this.principals != null && that.principals == null))
            return false;

        if (this.principals != null && that.principals != null) {
            if (!this.principals.containsAll(that.principals) ||
                !that.principals.containsAll(this.principals))

                return false;
        }

        return true;
    }

    /**
     * Return a hashcode for this <code>SubjectCodeSource</code>.
     *
     * <p>
     *
     * @return a hashcode for this <code>SubjectCodeSource</code>.
     */
    public int hashCode() {
        return super.hashCode();
    }

    /**
     * Return a String representation of this <code>SubjectCodeSource</code>.
     *
     * <p>
     *
     * @return a String representation of this <code>SubjectCodeSource</code>.
     */
    @SuppressWarnings("removal")
    public String toString() {
        String returnMe = super.toString();
        if (getSubject() != null) {
            if (debug != null) {
                final Subject finalSubject = getSubject();
                returnMe = returnMe + "\n" +
                        java.security.AccessController.doPrivileged
                                (new java.security.PrivilegedAction<String>() {
                                public String run() {
                                    return finalSubject.toString();
                                }
                        });
            } else {
                returnMe = returnMe + "\n" + getSubject().toString();
            }
        }
        if (principals != null) {
            ListIterator<PrincipalEntry> li = principals.listIterator();
            while (li.hasNext()) {
                PrincipalEntry pppe = li.next();
                returnMe = returnMe + ResourcesMgr.getAuthResourceString("NEWLINE") +
                        pppe.getPrincipalClass() + " " +
                        pppe.getPrincipalName();
            }
        }
        return returnMe;
    }
}
