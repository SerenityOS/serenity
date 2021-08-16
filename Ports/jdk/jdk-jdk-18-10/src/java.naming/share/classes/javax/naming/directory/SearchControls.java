/*
 * Copyright (c) 1999, 2000, Oracle and/or its affiliates. All rights reserved.
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


package javax.naming.directory;

/**
  * This class encapsulates
  * factors that determine scope of search and what gets returned
  * as a result of the search.
  *<p>
  * A SearchControls instance is not synchronized against concurrent
  * multithreaded access. Multiple threads trying to access and modify
  * a single SearchControls instance should lock the object.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */

public class SearchControls implements java.io.Serializable {
    /**
     * Search the named object.
     *<p>
     * The NamingEnumeration that results from search()
     * using OBJECT_SCOPE will contain one or zero element.
     * The enumeration contains one element if the named object satisfies
     * the search filter specified in search().
     * The element will have as its name the empty string because the names
     * of elements in the NamingEnumeration are relative to the
     * target context--in this case, the target context is the named object.
     * It contains zero element if the named object does not satisfy
     * the search filter specified in search().
     * <p>
     * The value of this constant is {@code 0}.
     */
    public static final int OBJECT_SCOPE = 0;

    /**
     * Search one level of the named context.
     *<p>
     * The NamingEnumeration that results from search()
     * using ONELEVEL_SCOPE contains elements with
     * objects in the named context that satisfy
     * the search filter specified in search().
     * The names of elements in the NamingEnumeration are atomic names
     * relative to the named context.
     * <p>
     * The value of this constant is {@code 1}.
     */
    public static final int ONELEVEL_SCOPE = 1;
    /**
     * Search the entire subtree rooted at the named object.
     *<p>
     * If the named object is not a DirContext, search only the object.
     * If the named object is a DirContext, search the subtree
     * rooted at the named object, including the named object itself.
     *<p>
     * The search will not cross naming system boundaries.
     *<p>
     * The NamingEnumeration that results from search()
     * using SUBTREE_SCOPE contains elements of objects
     * from the subtree (including the named context)
     * that satisfy the search filter specified in search().
     * The names of elements in the NamingEnumeration are either
     * relative to the named context or is a URL string.
     * If the named context satisfies the search filter, it is
     * included in the enumeration with the empty string as
     * its name.
     * <p>
     * The value of this constant is {@code 2}.
     */
    public static final int SUBTREE_SCOPE = 2;

    /**
     * Contains the scope with which to apply the search. One of
     * {@code ONELEVEL_SCOPE}, {@code OBJECT_SCOPE}, or
     * {@code SUBTREE_SCOPE}.
     * @serial
     */
    private int searchScope;

    /**
     * Contains the milliseconds to wait before returning
     * from search.
     * @serial
     */
    private int timeLimit;

    /**
     * Indicates whether JNDI links are dereferenced during
     * search.
     * @serial
     */
    private boolean derefLink;

    /**
     *  Indicates whether object is returned in {@code SearchResult}.
     * @serial
     */
    private boolean returnObj;

    /**
     * Contains the maximum number of SearchResults to return.
     * @serial
     */
    private long countLimit;

    /**
     *  Contains the list of attributes to be returned in
     * {@code SearchResult} for each matching entry of search. {@code null}
     * indicates that all attributes are to be returned.
     * @serial
     */
    private String[] attributesToReturn;

    /**
     * Constructs a search constraints using defaults.
     *<p>
     * The defaults are:
     * <ul>
     * <li>search one level
     * <li>no maximum return limit for search results
     * <li>no time limit for search
     * <li>return all attributes associated with objects that satisfy
     *   the search filter.
     * <li>do not return named object  (return only name and class)
     * <li>do not dereference links during search
     *</ul>
     */
    public SearchControls() {
        searchScope = ONELEVEL_SCOPE;
        timeLimit = 0; // no limit
        countLimit = 0; // no limit
        derefLink = false;
        returnObj = false;
        attributesToReturn = null; // return all
    }

    /**
     * Constructs a search constraints using arguments.
     * @param scope     The search scope.  One of:
     *                  OBJECT_SCOPE, ONELEVEL_SCOPE, SUBTREE_SCOPE.
     * @param timelim   The number of milliseconds to wait before returning.
     *                  If 0, wait indefinitely.
     * @param deref     If true, dereference links during search.
     * @param countlim  The maximum number of entries to return.  If 0, return
     *                  all entries that satisfy filter.
     * @param retobj    If true, return the object bound to the name of the
     *                  entry; if false, do not return object.
     * @param attrs     The identifiers of the attributes to return along with
     *                  the entry.  If null, return all attributes. If empty
     *                  return no attributes.
     */
    public SearchControls(int scope,
                             long countlim,
                             int timelim,
                             String[] attrs,
                             boolean retobj,
                             boolean deref) {
        searchScope = scope;
        timeLimit = timelim; // no limit
        derefLink = deref;
        returnObj = retobj;
        countLimit = countlim; // no limit
        attributesToReturn = attrs; // return all
    }

    /**
     * Retrieves the search scope of these SearchControls.
     *<p>
     * One of OBJECT_SCOPE, ONELEVEL_SCOPE, SUBTREE_SCOPE.
     *
     * @return The search scope of this SearchControls.
     * @see #setSearchScope
     */
    public int getSearchScope() {
        return searchScope;
    }

    /**
     * Retrieves the time limit of these SearchControls in milliseconds.
     *<p>
     * If the value is 0, this means to wait indefinitely.
     * @return The time limit of these SearchControls in milliseconds.
     * @see #setTimeLimit
     */
    public int getTimeLimit() {
        return timeLimit;
    }

    /**
     * Determines whether links will be dereferenced during the search.
     *
     * @return true if links will be dereferenced; false otherwise.
     * @see #setDerefLinkFlag
     */
    public boolean getDerefLinkFlag() {
        return derefLink;
    }

    /**
     * Determines whether objects will be returned as part of the result.
     *
     * @return true if objects will be returned; false otherwise.
     * @see #setReturningObjFlag
     */
    public boolean getReturningObjFlag() {
        return returnObj;
    }

    /**
     * Retrieves the maximum number of entries that will be returned
     * as a result of the search.
     *<p>
     * 0 indicates that all entries will be returned.
     * @return The maximum number of entries that will be returned.
     * @see #setCountLimit
     */
    public long getCountLimit() {
        return countLimit;
    }

    /**
     * Retrieves the attributes that will be returned as part of the search.
     *<p>
     * A value of null indicates that all attributes will be returned.
     * An empty array indicates that no attributes are to be returned.
     *
     * @return An array of attribute ids identifying the attributes that
     * will be returned. Can be null.
     * @see #setReturningAttributes
     */
    public String[] getReturningAttributes() {
        return attributesToReturn;
    }

    /**
     * Sets the search scope to one of:
     * OBJECT_SCOPE, ONELEVEL_SCOPE, SUBTREE_SCOPE.
     * @param scope     The search scope of this SearchControls.
     * @see #getSearchScope
     */
    public void setSearchScope(int scope) {
        searchScope = scope;
    }

    /**
     * Sets the time limit of these SearchControls in milliseconds.
     *<p>
     * If the value is 0, this means to wait indefinitely.
     * @param ms        The time limit of these SearchControls in milliseconds.
     * @see #getTimeLimit
     */
    public void setTimeLimit(int ms) {
        timeLimit = ms;
    }

    /**
     * Enables/disables link dereferencing during the search.
     *
     * @param on        if true links will be dereferenced; if false, not followed.
     * @see #getDerefLinkFlag
     */
    public void setDerefLinkFlag(boolean on) {
        derefLink = on;
    }

    /**
     * Enables/disables returning objects returned as part of the result.
     *<p>
     * If disabled, only the name and class of the object is returned.
     * If enabled, the object will be returned.
     *
     * @param on        if true, objects will be returned; if false,
     *                  objects will not be returned.
     * @see #getReturningObjFlag
     */
    public void setReturningObjFlag(boolean on) {
        returnObj = on;
    }

    /**
     * Sets the maximum number of entries to be returned
     * as a result of the search.
     *<p>
     * 0 indicates no limit:  all entries will be returned.
     *
     * @param limit The maximum number of entries that will be returned.
     * @see #getCountLimit
     */
    public void setCountLimit(long limit) {
        countLimit = limit;
    }

    /**
     * Specifies the attributes that will be returned as part of the search.
     *<p>
     * null indicates that all attributes will be returned.
     * An empty array indicates no attributes are returned.
     *
     * @param attrs An array of attribute ids identifying the attributes that
     *              will be returned. Can be null.
     * @see #getReturningAttributes
     */
    public void setReturningAttributes(String[] attrs) {
        attributesToReturn = attrs;
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability.
     */
    private static final long serialVersionUID = -2480540967773454797L;
}
