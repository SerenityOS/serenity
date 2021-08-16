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

package javax.naming.spi;

import javax.naming.Name;
import javax.naming.Context;
import javax.naming.CompositeName;
import javax.naming.InvalidNameException;

/**
  * This class represents the result of resolution of a name.
  * It contains the object to which name was resolved, and the portion
  * of the name that has not been resolved.
  *<p>
  * A ResolveResult instance is not synchronized against concurrent
  * multithreaded access. Multiple threads trying to access and modify
  * a single ResolveResult instance should lock the object.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */
public class ResolveResult implements java.io.Serializable {
    /**
     * Field containing the Object that was resolved to successfully.
     * It can be null only when constructed using a subclass.
     * Constructors should always initialize this.
     * @serial
     */
    @SuppressWarnings("serial") // Not statically typed as Serializable
    protected Object resolvedObj;
    /**
     * Field containing the remaining name yet to be resolved.
     * It can be null only when constructed using a subclass.
     * Constructors should always initialize this.
     * @serial
     */
    protected Name remainingName;

    /**
      * Constructs an instance of ResolveResult with the
      * resolved object and remaining name both initialized to null.
      */
    protected ResolveResult() {
        resolvedObj = null;
        remainingName = null;
    }

    /**
      * Constructs a new instance of ResolveResult consisting of
      * the resolved object and the remaining unresolved component.
      *
      * @param robj The non-null object resolved to.
      * @param rcomp The single remaining name component that has yet to be
      *                 resolved. Cannot be null (but can be empty).
      */
    public ResolveResult(Object robj, String rcomp) {
        resolvedObj = robj;
        try {
        remainingName = new CompositeName(rcomp);
//          remainingName.appendComponent(rcomp);
        } catch (InvalidNameException e) {
            // ignore; shouldn't happen
        }
    }

    /**
      * Constructs a new instance of ResolveResult consisting of
      * the resolved Object and the remaining name.
      *
      * @param robj The non-null Object resolved to.
      * @param rname The non-null remaining name that has yet to be resolved.
      */
    public ResolveResult(Object robj, Name rname) {
        resolvedObj = robj;
        setRemainingName(rname);
    }

    /**
     * Retrieves the remaining unresolved portion of the name.
     *
     * @return The remaining unresolved portion of the name.
     *          Cannot be null but empty OK.
     * @see #appendRemainingName
     * @see #appendRemainingComponent
     * @see #setRemainingName
     */
    public Name getRemainingName() {
        return this.remainingName;
    }

    /**
     * Retrieves the Object to which resolution was successful.
     *
     * @return The Object to which resolution was successful. Cannot be null.
      * @see #setResolvedObj
     */
    public Object getResolvedObj() {
        return this.resolvedObj;
    }

    /**
      * Sets the remaining name field of this result to name.
      * A copy of name is made so that modifying the copy within
      * this ResolveResult does not affect <code>name</code> and
      * vice versa.
      *
      * @param name The name to set remaining name to. Cannot be null.
      * @see #getRemainingName
      * @see #appendRemainingName
      * @see #appendRemainingComponent
      */
    public void setRemainingName(Name name) {
        if (name != null)
            this.remainingName = (Name)(name.clone());
        else {
            // ??? should throw illegal argument exception
            this.remainingName = null;
        }
    }

    /**
      * Adds components to the end of remaining name.
      *
      * @param name The components to add. Can be null.
      * @see #getRemainingName
      * @see #setRemainingName
      * @see #appendRemainingComponent
      */
    public void appendRemainingName(Name name) {
//      System.out.println("appendingRemainingName: " + name.toString());
//      Exception e = new Exception();
//      e.printStackTrace();
        if (name != null) {
            if (this.remainingName != null) {
                try {
                    this.remainingName.addAll(name);
                } catch (InvalidNameException e) {
                    // ignore; shouldn't happen for composite name
                }
            } else {
                this.remainingName = (Name)(name.clone());
            }
        }
    }

    /**
      * Adds a single component to the end of remaining name.
      *
      * @param name The component to add. Can be null.
      * @see #getRemainingName
      * @see #appendRemainingName
      */
    public void appendRemainingComponent(String name) {
        if (name != null) {
            CompositeName rname = new CompositeName();
            try {
                rname.add(name);
            } catch (InvalidNameException e) {
                // ignore; shouldn't happen for empty composite name
            }
            appendRemainingName(rname);
        }
    }

    /**
      * Sets the resolved Object field of this result to obj.
      *
      * @param obj The object to use for setting the resolved obj field.
      *            Cannot be null.
      * @see #getResolvedObj
      */
    public void setResolvedObj(Object obj) {
        this.resolvedObj = obj;
        // ??? should check for null?
    }

    private static final long serialVersionUID = -4552108072002407559L;
}
