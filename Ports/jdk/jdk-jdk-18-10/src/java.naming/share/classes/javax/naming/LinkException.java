/*
 * Copyright (c) 1999, 2019, Oracle and/or its affiliates. All rights reserved.
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

package javax.naming;

/**
 * This exception is used to describe problems encountered while resolving links.
 * Additional information is added to the base NamingException for pinpointing
 * the problem with the link.
 *<p>
 * Analogously to how NamingException captures name resolution information,
 * LinkException captures "link"-name resolution information pinpointing
 * the problem encountered while resolving a link. All these fields may
 * be null.
 * <ul>
 * <li> Link Resolved Name. Portion of link name that has been resolved.
 * <li> Link Resolved Object. Object to which resolution of link name proceeded.
 * <li> Link Remaining Name. Portion of link name that has not been resolved.
 * <li> Link Explanation. Detail explaining why link resolution failed.
 *</ul>
 *
  *<p>
  * A LinkException instance is not synchronized against concurrent
  * multithreaded access. Multiple threads trying to access and modify
  * a single LinkException instance should lock the object.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see Context#lookupLink
  * @see LinkRef
  * @since 1.3
  */


  /*<p>
  * The serialized form of a LinkException object consists of the
  * serialized fields of its NamingException superclass, the link resolved
  * name (a Name object), the link resolved object, link remaining name
  * (a Name object), and the link explanation String.
*/


public class LinkException extends NamingException {
    /**
     * Contains the part of the link that has been successfully resolved.
     * It is a composite name and can be null.
     * This field is initialized by the constructors.
     * You should access and manipulate this field
     * through its get and set methods.
     * @serial
     * @see #getLinkResolvedName
     * @see #setLinkResolvedName
     */
    protected Name linkResolvedName;

    /**
      * Contains the object to which resolution of the part of the link was successful.
      * Can be null. This field is initialized by the constructors.
      * You should access and manipulate this field
      * through its get and set methods.
      * @serial
      * @see #getLinkResolvedObj
      * @see #setLinkResolvedObj
      */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    protected Object linkResolvedObj;

    /**
     * Contains the remaining link name that has not been resolved yet.
     * It is a composite name and can be null.
     * This field is initialized by the constructors.
     * You should access and manipulate this field
     * through its get and set methods.
     * @serial
     * @see #getLinkRemainingName
     * @see #setLinkRemainingName
     */
    protected Name linkRemainingName;

    /**
     * Contains the exception of why resolution of the link failed.
     * Can be null. This field is initialized by the constructors.
     * You should access and manipulate this field
     * through its get and set methods.
     * @serial
     * @see #getLinkExplanation
     * @see #setLinkExplanation
     */
    protected String linkExplanation;

    /**
      * Constructs a new instance of LinkException with an explanation.
      * All the other fields are initialized to null.
      * @param  explanation     A possibly null string containing additional
      *                         detail about this exception.
      * @see java.lang.Throwable#getMessage
      */
    public LinkException(String explanation) {
        super(explanation);
        linkResolvedName = null;
        linkResolvedObj = null;
        linkRemainingName = null;
        linkExplanation = null;
    }

    /**
      * Constructs a new instance of LinkException.
      * All the non-link-related and link-related fields are initialized to null.
      */
    public LinkException() {
        super();
        linkResolvedName = null;
        linkResolvedObj = null;
        linkRemainingName = null;
        linkExplanation = null;
    }

    /**
     * Retrieves the leading portion of the link name that was resolved
     * successfully.
     *
     * @return The part of the link name that was resolved successfully.
     *          It is a composite name. It can be null, which means
     *          the link resolved name field has not been set.
     * @see #getLinkResolvedObj
     * @see #setLinkResolvedName
     */
    public Name getLinkResolvedName() {
        return this.linkResolvedName;
    }

    /**
     * Retrieves the remaining unresolved portion of the link name.
     * @return The part of the link name that has not been resolved.
     *          It is a composite name. It can be null, which means
     *          the link remaining name field has not been set.
     * @see #setLinkRemainingName
     */
    public Name getLinkRemainingName() {
        return this.linkRemainingName;
    }

    /**
     * Retrieves the object to which resolution was successful.
     * This is the object to which the resolved link name is bound.
     *
     * @return The possibly null object that was resolved so far.
     * If null, it means the link resolved object field has not been set.
     * @see #getLinkResolvedName
     * @see #setLinkResolvedObj
     */
    public Object getLinkResolvedObj() {
        return this.linkResolvedObj;
    }

    /**
      * Retrieves the explanation associated with the problem encountered
      * when resolving a link.
      *
      * @return The possibly null detail string explaining more about the problem
      * with resolving a link.
      *         If null, it means there is no
      *         link detail message for this exception.
      * @see #setLinkExplanation
      */
    public String getLinkExplanation() {
        return this.linkExplanation;
    }

    /**
      * Sets the explanation associated with the problem encountered
      * when resolving a link.
      *
      * @param msg The possibly null detail string explaining more about the problem
      * with resolving a link. If null, it means no detail will be recorded.
      * @see #getLinkExplanation
      */
    public void setLinkExplanation(String msg) {
        this.linkExplanation = msg;
    }

    /**
     * Sets the resolved link name field of this exception.
     *<p>
     * {@code name} is a composite name. If the intent is to set
     * this field using a compound name or string, you must
     * "stringify" the compound name, and create a composite
     * name with a single component using the string. You can then
     * invoke this method using the resulting composite name.
     *<p>
     * A copy of <code>name</code> is made and stored.
     * Subsequent changes to <code>name</code> do not
     * affect the copy in this NamingException and vice versa.
     *
     *
     * @param name The name to set resolved link name to. This can be null.
     *          If null, it sets the link resolved name field to null.
     * @see #getLinkResolvedName
     */
    public void setLinkResolvedName(Name name) {
        if (name != null) {
            this.linkResolvedName = (Name)(name.clone());
        } else {
            this.linkResolvedName = null;
        }
    }

    /**
     * Sets the remaining link name field of this exception.
     *<p>
     * {@code name} is a composite name. If the intent is to set
     * this field using a compound name or string, you must
     * "stringify" the compound name, and create a composite
     * name with a single component using the string. You can then
     * invoke this method using the resulting composite name.
     *<p>
     * A copy of <code>name</code> is made and stored.
     * Subsequent changes to <code>name</code> do not
     * affect the copy in this NamingException and vice versa.
     *
     * @param name The name to set remaining link name to. This can be null.
     *  If null, it sets the remaining name field to null.
     * @see #getLinkRemainingName
     */
    public void setLinkRemainingName(Name name) {
        if (name != null)
            this.linkRemainingName = (Name)(name.clone());
        else
            this.linkRemainingName = null;
    }

    /**
     * Sets the link resolved object field of this exception.
     * This indicates the last successfully resolved object of link name.
     * @param obj The object to set link resolved object to. This can be null.
     *            If null, the link resolved object field is set to null.
     * @see #getLinkResolvedObj
     */
    public void setLinkResolvedObj(Object obj) {
        this.linkResolvedObj = obj;
    }

    /**
     * Generates the string representation of this exception.
     * This string consists of the NamingException information plus
     * the link's remaining name.
     * This string is used for debugging and not meant to be interpreted
     * programmatically.
     * @return The non-null string representation of this link exception.
     */
    public String toString() {
        return super.toString() + "; Link Remaining Name: '" +
            this.linkRemainingName + "'";
    }

    /**
     * Generates the string representation of this exception.
     * This string consists of the NamingException information plus
     * the additional information of resolving the link.
     * If 'detail' is true, the string also contains information on
     * the link resolved object. If false, this method is the same
     * as the form of toString() that accepts no parameters.
     * This string is used for debugging and not meant to be interpreted
     * programmatically.
     *
     * @param   detail  If true, add information about the link resolved
     *                  object.
     * @return The non-null string representation of this link exception.
     */
    public String toString(boolean detail) {
        if (!detail || this.linkResolvedObj == null)
            return this.toString();

        return this.toString() + "; Link Resolved Object: " +
            this.linkResolvedObj;
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = -7967662604076777712L;
};
