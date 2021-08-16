/*
 * Copyright (c) 1999, 2017, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.jndi.ldap;

import javax.naming.*;
import javax.naming.ldap.Control;

import java.util.Hashtable;
import java.util.Vector;

/**
  * This exception is raised when a referral to an alternative context
  * is encountered.
  * <p>
  * An {@code LdapReferralException} object contains one or more referrals.
  * Each referral is an alternative location for the same target entry.
  * For example, a referral may be an LDAP URL.
  * The referrals are attempted in sequence until one is successful or
  * all have failed. In the case of the latter then the exception generated
  * by the final referral is recorded and presented later.
  * <p>
  * A referral may be skipped or may be retried. For example, in the case
  * of an authentication error, a referral may be retried with different
  * environment properties.
  * <p>
  * An {@code LdapReferralException} object may also contain a reference
  * to a chain of unprocessed {@code LdapReferralException} objects.
  * Once the current set of referrals have been exhausted and unprocessed
  * {@code LdapReferralException} objects remain, then the
  * {@code LdapReferralException} object referenced by the current
  * object is thrown and the cycle continues.
  * <p>
  * If new {@code LdapReferralException} objects are generated while
  * following an existing referral then these new objects are appended
  * to the end of the chain of unprocessed {@code LdapReferralException}
  * objects.
  * <p>
  * If an exception was recorded while processing a chain of
  * {@code LdapReferralException} objects then it is throw once
  * processing has completed.
  *
  * @author Vincent Ryan
  */
public final class LdapReferralException extends
    javax.naming.ldap.LdapReferralException {
    private static final long serialVersionUID = 627059076356906399L;

        // ----------- fields initialized in constructor ---------------
    private int handleReferrals;
    private Hashtable<?,?> envprops;
    private String nextName;
    private Control[] reqCtls;

        // ----------- fields that have defaults -----------------------
    private Vector<?> referrals = null; // alternatives,set by setReferralInfo()
    private int referralIndex = 0;      // index into referrals
    private int referralCount = 0;      // count of referrals
    private boolean foundEntry = false; // will stop when entry is found
    private boolean skipThisReferral = false;
    private int hopCount = 1;
    private NamingException errorEx = null;
    private String newRdn = null;
    private boolean debug = false;
            LdapReferralException nextReferralEx = null; // referral ex. chain

    /**
     * Constructs a new instance of LdapReferralException.
     * @param   resolvedName    The part of the name that has been successfully
     *                          resolved.
     * @param   resolvedObj     The object to which resolution was successful.
     * @param   remainingName   The remaining unresolved portion of the name.
     * @param   explanation     Additional detail about this exception.
     */
    LdapReferralException(Name resolvedName,
        Object resolvedObj,
        Name remainingName,
        String explanation,
        Hashtable<?,?> envprops,
        String nextName,
        int handleReferrals,
        Control[] reqCtls) {

        super(explanation);

        if (debug)
            System.out.println("LdapReferralException constructor");

        setResolvedName(resolvedName);
        setResolvedObj(resolvedObj);
        setRemainingName(remainingName);
        this.envprops = envprops;
        this.nextName = nextName;
        this.handleReferrals = handleReferrals;

        // If following referral, request controls are passed to referral ctx
        this.reqCtls =
            (handleReferrals == LdapClient.LDAP_REF_FOLLOW ||
                    handleReferrals == LdapClient.LDAP_REF_FOLLOW_SCHEME ? reqCtls : null);
    }

    /**
     * Gets a context at which to continue processing.
     * The current environment properties are re-used.
     */
    public Context getReferralContext() throws NamingException {
        return getReferralContext(envprops, null);
    }

    /**
     * Gets a context at which to continue processing.
     * The supplied environment properties are used.
     */
    public Context getReferralContext(Hashtable<?,?> newProps) throws
        NamingException {
        return getReferralContext(newProps, null);
    }

    /**
     * Gets a context at which to continue processing.
     * The supplied environment properties and connection controls are used.
     */
    public Context getReferralContext(Hashtable<?,?> newProps, Control[] connCtls)
        throws NamingException {

        if (debug)
            System.out.println("LdapReferralException.getReferralContext");

        LdapReferralContext refCtx = new LdapReferralContext(
            this, newProps, connCtls, reqCtls,
            nextName, skipThisReferral, handleReferrals);

        refCtx.setHopCount(hopCount + 1);

        if (skipThisReferral) {
            skipThisReferral = false; // reset
        }
        return (Context)refCtx;
    }

    /**
      * Gets referral information.
      */
    public Object getReferralInfo() {
        if (debug) {
            System.out.println("LdapReferralException.getReferralInfo");
            System.out.println("  referralIndex=" + referralIndex);
        }

        if (hasMoreReferrals()) {
            return referrals.elementAt(referralIndex);
        } else {
            return null;
        }
    }

    /**
     * Marks the current referral as one to be retried.
     */
    public void retryReferral() {
        if (debug)
            System.out.println("LdapReferralException.retryReferral");

        if (referralIndex > 0)
            referralIndex--; // decrement index
    }

    /**
     * Marks the current referral as one to be ignored.
     * Returns false when there are no referrals remaining to be processed.
     */
    public boolean skipReferral() {
        if (debug)
            System.out.println("LdapReferralException.skipReferral");

        skipThisReferral = true;

        // advance to next referral
        try {
            getNextReferral();
        } catch (ReferralException e) {
            // mask the referral exception
        }

        return (hasMoreReferrals() || hasMoreReferralExceptions());
    }


    /**
     * Sets referral information.
     */
    void setReferralInfo(Vector<?> referrals, boolean continuationRef) {
        // %%% continuationRef is currently ignored

        if (debug)
            System.out.println("LdapReferralException.setReferralInfo");

        this.referrals = referrals;
        referralCount = (referrals == null) ? 0 : referrals.size();

        if (debug) {
            if (referrals != null) {
                for (int i = 0; i < referralCount; i++) {
                    System.out.println("  [" + i + "] " + referrals.elementAt(i));
                }
            } else {
                System.out.println("setReferralInfo : referrals == null");
            }
        }
    }

    /**
     * Gets the next referral. When the current set of referrals have
     * been exhausted then the next referral exception is thrown, if available.
     */
    String getNextReferral() throws ReferralException {

        if (debug)
            System.out.println("LdapReferralException.getNextReferral");

        if (hasMoreReferrals()) {
            return (String)referrals.elementAt(referralIndex++);
        } else if (hasMoreReferralExceptions()) {
            throw nextReferralEx;
        } else {
            return null;
        }
    }

    /**
     * Appends the supplied (chain of) referral exception onto the end of
     * the current (chain of) referral exception. Spent referral exceptions
     * are trimmed off.
     */
    LdapReferralException
        appendUnprocessedReferrals(LdapReferralException back) {

        if (debug) {
            System.out.println(
                "LdapReferralException.appendUnprocessedReferrals");
            dump();
            if (back != null) {
                back.dump();
            }
        }

        LdapReferralException front = this;

        if (! front.hasMoreReferrals()) {
            front = nextReferralEx; // trim

            if ((errorEx != null) && (front != null)) {
                front.setNamingException(errorEx); //advance the saved exception
            }
        }

        // don't append onto itself
        if (this == back) {
            return front;
        }

        if ((back != null) && (! back.hasMoreReferrals())) {
            back = back.nextReferralEx; // trim
        }

        if (back == null) {
            return front;
        }

        // Locate the end of the current chain
        LdapReferralException ptr = front;
        while (ptr.nextReferralEx != null) {
            ptr = ptr.nextReferralEx;
        }
        ptr.nextReferralEx = back; // append

        return front;
    }

    /**
     * Tests if there are any referrals remaining to be processed.
     * If name resolution has already completed then any remaining
     * referrals (in the current referral exception) will be ignored.
     */
    boolean hasMoreReferrals() {
        if (debug)
            System.out.println("LdapReferralException.hasMoreReferrals");

        return (! foundEntry) && (referralIndex < referralCount);
    }

    /**
     * Tests if there are any referral exceptions remaining to be processed.
     */
    boolean hasMoreReferralExceptions() {
        if (debug)
            System.out.println(
                "LdapReferralException.hasMoreReferralExceptions");

        return (nextReferralEx != null);
    }

    /**
     * Sets the counter which records the number of hops that result
     * from following a sequence of referrals.
     */
    void setHopCount(int hopCount) {
        if (debug)
            System.out.println("LdapReferralException.setHopCount");

        this.hopCount = hopCount;
    }

    /**
     * Sets the flag to indicate that the target name has been resolved.
     */
    void setNameResolved(boolean resolved) {
        if (debug)
            System.out.println("LdapReferralException.setNameResolved");

        foundEntry = resolved;
    }

    /**
     * Sets the exception generated while processing a referral.
     * Only the first exception is recorded.
     */
    void setNamingException(NamingException e) {
        if (debug)
            System.out.println("LdapReferralException.setNamingException");

        if (errorEx == null) {
            e.setRootCause(this); //record the referral exception that caused it
            errorEx = e;
        }
    }

    /**
     * Gets the new RDN name.
     */
    String getNewRdn() {
        if (debug)
            System.out.println("LdapReferralException.getNewRdn");

        return newRdn;
    }

    /**
     * Sets the new RDN name so that the rename operation can be completed
     * (when a referral is being followed).
     */
    void setNewRdn(String newRdn) {
        if (debug)
            System.out.println("LdapReferralException.setNewRdn");

        this.newRdn = newRdn;
    }

    /**
     * Gets the exception generated while processing a referral.
     */
    NamingException getNamingException() {
        if (debug)
            System.out.println("LdapReferralException.getNamingException");

        return errorEx;
    }

    /**
     * Display the state of each element in a chain of LdapReferralException
     * objects.
     */
    void dump() {

        System.out.println();
        System.out.println("LdapReferralException.dump");
        LdapReferralException ptr = this;
        while (ptr != null) {
            ptr.dumpState();
            ptr = ptr.nextReferralEx;
        }
    }

    /**
     * Display the state of this LdapReferralException object.
     */
    private void dumpState() {
        System.out.println("LdapReferralException.dumpState");
        System.out.println("  hashCode=" + hashCode());
        System.out.println("  foundEntry=" + foundEntry);
        System.out.println("  skipThisReferral=" + skipThisReferral);
        System.out.println("  referralIndex=" + referralIndex);

        if (referrals != null) {
            System.out.println("  referrals:");
            for (int i = 0; i < referralCount; i++) {
                System.out.println("    [" + i + "] " + referrals.elementAt(i));
            }
        } else {
            System.out.println("  referrals=null");
        }

        System.out.println("  errorEx=" + errorEx);

        if (nextReferralEx == null) {
            System.out.println("  nextRefEx=null");
        } else {
            System.out.println("  nextRefEx=" + nextReferralEx.hashCode());
        }
        System.out.println();
    }
}
