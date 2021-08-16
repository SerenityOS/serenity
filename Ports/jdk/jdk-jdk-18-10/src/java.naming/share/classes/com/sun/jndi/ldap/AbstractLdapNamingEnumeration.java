/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.jndi.toolkit.ctx.Continuation;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.naming.*;
import javax.naming.directory.Attributes;
import javax.naming.ldap.Control;

/**
 * Basic enumeration for NameClassPair, Binding, and SearchResults.
 */

abstract class AbstractLdapNamingEnumeration<T extends NameClassPair>
        implements NamingEnumeration<T>, ReferralEnumeration<T> {

    protected Name listArg;

    private boolean cleaned = false;
    private LdapResult res;
    private LdapClient enumClnt;
    private Continuation cont;  // used to fill in exceptions
    private Vector<LdapEntry> entries = null;
    private int limit = 0;
    private int posn = 0;
    protected LdapCtx homeCtx;
    private LdapReferralException refEx = null;
    private NamingException errEx = null;

    /*
     * Record the next set of entries and/or referrals.
     */
    AbstractLdapNamingEnumeration(LdapCtx homeCtx, LdapResult answer, Name listArg,
        Continuation cont) throws NamingException {

            // These checks are to accommodate referrals and limit exceptions
            // which will generate an enumeration and defer the exception
            // to be thrown at the end of the enumeration.
            // All other exceptions are thrown immediately.
            // Exceptions shouldn't be thrown here anyhow because
            // process_return_code() is called before the constructor
            // is called, so these are just safety checks.

            if ((answer.status != LdapClient.LDAP_SUCCESS) &&
                (answer.status != LdapClient.LDAP_SIZE_LIMIT_EXCEEDED) &&
                (answer.status != LdapClient.LDAP_TIME_LIMIT_EXCEEDED) &&
                (answer.status != LdapClient.LDAP_ADMIN_LIMIT_EXCEEDED) &&
                (answer.status != LdapClient.LDAP_REFERRAL) &&
                (answer.status != LdapClient.LDAP_PARTIAL_RESULTS)) {

                // %%% need to deal with referral
                NamingException e = new NamingException(
                                    LdapClient.getErrorMessage(
                                    answer.status, answer.errorMessage));

                throw cont.fillInException(e);
            }

            // otherwise continue

            res = answer;
            entries = answer.entries;
            limit = (entries == null) ? 0 : entries.size(); // handle empty set
            this.listArg = listArg;
            this.cont = cont;

            if (answer.refEx != null) {
                refEx = answer.refEx;
            }

            // Ensures that context won't get closed from underneath us
            this.homeCtx = homeCtx;
            homeCtx.incEnumCount();
            enumClnt = homeCtx.clnt; // remember
    }

    @Override
    public final T nextElement() {
        try {
            return next();
        } catch (NamingException e) {
            // can't throw exception
            cleanup();
            return null;
        }
    }

    @Override
    public final boolean hasMoreElements() {
        try {
            return hasMore();
        } catch (NamingException e) {
            // can't throw exception
            cleanup();
            return false;
        }
    }

    /*
     * Retrieve the next set of entries and/or referrals.
     */
    private void getNextBatch() throws NamingException {

        res = homeCtx.getSearchReply(enumClnt, res);
        if (res == null) {
            limit = posn = 0;
            return;
        }

        entries = res.entries;
        limit = (entries == null) ? 0 : entries.size(); // handle empty set
        posn = 0; // reset

        // minimize the number of calls to processReturnCode()
        // (expensive when batchSize is small and there are many results)
        if ((res.status != LdapClient.LDAP_SUCCESS) ||
            ((res.status == LdapClient.LDAP_SUCCESS) &&
                (res.referrals != null))) {

            try {
                // convert referrals into a chain of LdapReferralException
                homeCtx.processReturnCode(res, listArg);

            } catch (LimitExceededException | PartialResultException e) {
                setNamingException(e);

            }
        }

        // merge any newly received referrals with any current referrals
        if (res.refEx != null) {
            if (refEx == null) {
                refEx = res.refEx;
            } else {
                refEx = refEx.appendUnprocessedReferrals(res.refEx);
            }
            res.refEx = null; // reset
        }

        if (res.resControls != null) {
            homeCtx.respCtls = res.resControls;
        }
    }

    private boolean more = true;  // assume we have something to start with
    private boolean hasMoreCalled = false;

    /*
     * Test if unprocessed entries or referrals exist.
     */
    @Override
    public final boolean hasMore() throws NamingException {

        if (hasMoreCalled) {
            return more;
        }

        hasMoreCalled = true;

        if (!more) {
            return false;
        } else {
            return (more = hasMoreImpl());
        }
    }

    /*
     * Retrieve the next entry.
     */
    @Override
    public final T next() throws NamingException {

        if (!hasMoreCalled) {
            hasMore();
        }
        hasMoreCalled = false;
        return nextImpl();
    }

    /*
     * Test if unprocessed entries or referrals exist.
     */
    private boolean hasMoreImpl() throws NamingException {
        // when page size is supported, this
        // might generate an exception while attempting
        // to fetch the next batch to determine
        // whether there are any more elements

        // test if the current set of entries has been processed
        if (posn == limit) {
            getNextBatch();
        }

        // test if any unprocessed entries exist
        if (posn < limit) {
            return true;
        } else {

            try {
                // try to process another referral
                return hasMoreReferrals();

            } catch (LdapReferralException |
                     LimitExceededException |
                     PartialResultException e) {
                cleanup();
                throw e;

            } catch (NamingException e) {
                cleanup();
                PartialResultException pre = new PartialResultException();
                pre.setRootCause(e);
                throw pre;
            }
        }
    }

    /*
     * Retrieve the next entry.
     */
    private T nextImpl() throws NamingException {
        try {
            return nextAux();
        } catch (NamingException e) {
            cleanup();
            throw cont.fillInException(e);
        }
    }

    private T nextAux() throws NamingException {
        if (posn == limit) {
            getNextBatch();  // updates posn and limit
        }

        if (posn >= limit) {
            cleanup();
            throw new NoSuchElementException("invalid enumeration handle");
        }

        LdapEntry result = entries.elementAt(posn++);

        // gets and outputs DN from the entry
        return createItem(result.DN, result.attributes, result.respCtls);
    }

    protected final String getAtom(String dn) {
        // need to strip off all but lowest component of dn
        // so that is relative to current context (currentDN)
        try {
            Name parsed = new LdapName(dn);
            return parsed.get(parsed.size() - 1);
        } catch (NamingException e) {
            return dn;
        }
    }

    protected abstract T createItem(String dn, Attributes attrs,
        Vector<Control> respCtls) throws NamingException;

    /*
     * Append the supplied (chain of) referrals onto the
     * end of the current (chain of) referrals.
     */
    @Override
    public void appendUnprocessedReferrals(LdapReferralException ex) {
        if (refEx != null) {
            refEx = refEx.appendUnprocessedReferrals(ex);
        } else {
            refEx = ex.appendUnprocessedReferrals(refEx);
        }
    }

    final void setNamingException(NamingException e) {
        errEx = e;
    }

    protected abstract AbstractLdapNamingEnumeration<? extends NameClassPair> getReferredResults(
            LdapReferralContext refCtx) throws NamingException;

    /*
     * Iterate through the URLs of a referral. If successful then perform
     * a search operation and merge the received results with the current
     * results.
     */
    protected final boolean hasMoreReferrals() throws NamingException {

        if ((refEx != null) &&
            (refEx.hasMoreReferrals() ||
             refEx.hasMoreReferralExceptions()
                && !(errEx instanceof LimitExceededException))) {

            if (homeCtx.handleReferrals == LdapClient.LDAP_REF_THROW) {
                throw (NamingException)(refEx.fillInStackTrace());
            }

            // process the referrals sequentially
            while (true) {

                LdapReferralContext refCtx =
                    (LdapReferralContext)refEx.getReferralContext(
                    homeCtx.envprops, homeCtx.reqCtls);

                try {

                    update(getReferredResults(refCtx));
                    break;

                } catch (LdapReferralException re) {

                    // record a previous exception
                    if (errEx == null) {
                        errEx = re.getNamingException();
                    }
                    refEx = re;
                    continue;

                } finally {
                    // Make sure we close referral context
                    refCtx.close();
                }
            }
            return hasMoreImpl();

        } else {
            cleanup();

            if (errEx != null) {
                throw errEx;
            }
            return (false);
        }
    }

    /*
     * Merge the entries and/or referrals from the supplied enumeration
     * with those of the current enumeration.
     */
    protected void update(AbstractLdapNamingEnumeration<? extends NameClassPair> ne) {
        // Cleanup previous context first
        homeCtx.decEnumCount();

        // New enum will have already incremented enum count and recorded clnt
        homeCtx = ne.homeCtx;
        enumClnt = ne.enumClnt;

        // Do this to prevent referral enumeration (ne) from decrementing
        // enum count because we'll be doing that here from this
        // enumeration.
        ne.homeCtx = null;

        // Record rest of information from new enum
        posn = ne.posn;
        limit = ne.limit;
        res = ne.res;
        entries = ne.entries;
        refEx = ne.refEx;
        listArg = ne.listArg;
    }

    @SuppressWarnings("deprecation")
    protected final void finalize() {
        cleanup();
    }

    protected final void cleanup() {
        if (cleaned) return; // been there; done that

        if(enumClnt != null) {
            enumClnt.clearSearchReply(res, homeCtx.reqCtls);
        }

        enumClnt = null;
        cleaned = true;
        if (homeCtx != null) {
            homeCtx.decEnumCount();
            homeCtx = null;
        }
    }

    @Override
    public final void close() {
        cleanup();
    }
}
