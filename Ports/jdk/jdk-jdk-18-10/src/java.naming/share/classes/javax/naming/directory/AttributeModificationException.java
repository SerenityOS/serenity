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

import javax.naming.NamingException;

/**
  * This exception is thrown when an attempt is
  * made to add, or remove, or modify an attribute, its identifier,
  * or its values that conflicts with the attribute's (schema) definition
  * or the attribute's state.
  * It is thrown in response to DirContext.modifyAttributes().
  * It contains a list of modifications that have not been performed, in the
  * order that they were supplied to modifyAttributes().
  * If the list is null, none of the modifications were performed successfully.
  *<p>
  * An AttributeModificationException instance is not synchronized
  * against concurrent multithreaded access. Multiple threads trying
  * to access and modify a single AttributeModification instance
  * should lock the object.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see DirContext#modifyAttributes
  * @since 1.3
  */

/*
  *<p>
  * The serialized form of an AttributeModificationException object
  * consists of the serialized fields of its NamingException
  * superclass, followed by an array of ModificationItem objects.
  *
*/


public class AttributeModificationException extends NamingException {
    /**
     * Contains the possibly null list of unexecuted modifications.
     * @serial
     */
    private ModificationItem[] unexecs = null;

    /**
     * Constructs a new instance of AttributeModificationException using
     * an explanation. All other fields are set to null.
     *
     * @param   explanation     Possibly null additional detail about this exception.
     * If null, this exception has no detail message.

     * @see java.lang.Throwable#getMessage
     */
    public AttributeModificationException(String explanation) {
        super(explanation);
    }

    /**
      * Constructs a new instance of AttributeModificationException.
      * All fields are set to null.
      */
    public AttributeModificationException() {
        super();
    }

    /**
      * Sets the unexecuted modification list to be e.
      * Items in the list must appear in the same order in which they were
      * originally supplied in DirContext.modifyAttributes().
      * The first item in the list is the first one that was not executed.
      * If this list is null, none of the operations originally submitted
      * to modifyAttributes() were executed.

      * @param e        The possibly null list of unexecuted modifications.
      * @see #getUnexecutedModifications
      */
    public void setUnexecutedModifications(ModificationItem[] e) {
        unexecs = e;
    }

    /**
      * Retrieves the unexecuted modification list.
      * Items in the list appear in the same order in which they were
      * originally supplied in DirContext.modifyAttributes().
      * The first item in the list is the first one that was not executed.
      * If this list is null, none of the operations originally submitted
      * to modifyAttributes() were executed.

      * @return The possibly null unexecuted modification list.
      * @see #setUnexecutedModifications
      */
    public ModificationItem[] getUnexecutedModifications() {
        return unexecs;
    }

    /**
      * The string representation of this exception consists of
      * information about where the error occurred, and
      * the first unexecuted modification.
      * This string is meant for debugging and not mean to be interpreted
      * programmatically.
      * @return The non-null string representation of this exception.
      */
    public String toString() {
        String orig = super.toString();
        if (unexecs != null) {
            orig += ("First unexecuted modification: " +
                     unexecs[0].toString());
        }
        return orig;
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = 8060676069678710186L;
}
