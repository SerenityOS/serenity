/*
 * Copyright (c) 1999, 2001, Oracle and/or its affiliates. All rights reserved.
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
  * This class represents a modification item.
  * It consists of a modification code and an attribute on which to operate.
  *<p>
  * A ModificationItem instance is not synchronized against concurrent
  * multithreaded access. Multiple threads trying to access and modify
  * a single ModificationItem instance should lock the object.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  * @since 1.3
  */

/*
  *<p>
  * The serialized form of a ModificationItem object consists of the
  * modification op (and int) and the corresponding Attribute.
*/

public class ModificationItem implements java.io.Serializable {
    /**
     * Contains an integer identify the modification
     * to be performed.
     * @serial
     */
    private int mod_op;
    /**
     * Contains the attribute identifying
     * the attribute and/or its value to be applied for the modification.
     * @serial
     */
    private Attribute attr;

    /**
      * Creates a new instance of ModificationItem.
      * @param mod_op Modification to apply.  It must be one of:
      *         DirContext.ADD_ATTRIBUTE
      *         DirContext.REPLACE_ATTRIBUTE
      *         DirContext.REMOVE_ATTRIBUTE
      * @param attr     The non-null attribute to use for modification.
      * @exception IllegalArgumentException If attr is null, or if mod_op is
      *         not one of the ones specified above.
      */
    public ModificationItem(int mod_op, Attribute attr) {
        switch (mod_op) {
        case DirContext.ADD_ATTRIBUTE:
        case DirContext.REPLACE_ATTRIBUTE:
        case DirContext.REMOVE_ATTRIBUTE:
            if (attr == null)
                throw new IllegalArgumentException("Must specify non-null attribute for modification");

            this.mod_op = mod_op;
            this.attr = attr;
            break;

        default:
            throw new IllegalArgumentException("Invalid modification code " + mod_op);
        }
    }

    /**
      * Retrieves the modification code of this modification item.
      * @return The modification code.  It is one of:
      *         DirContext.ADD_ATTRIBUTE
      *         DirContext.REPLACE_ATTRIBUTE
      *         DirContext.REMOVE_ATTRIBUTE
      */
    public int getModificationOp() {
        return mod_op;
    }

    /**
      * Retrieves the attribute associated with this modification item.
      * @return The non-null attribute to use for the modification.
      */
    public Attribute getAttribute() {
        return attr;
    }

    /**
      * Generates the string representation of this modification item,
      * which consists of the modification operation and its related attribute.
      * The string representation is meant for debugging and not to be
      * interpreted programmatically.
      *
      * @return The non-null string representation of this modification item.
      */
    public String toString() {
        switch (mod_op) {
        case DirContext.ADD_ATTRIBUTE:
            return ("Add attribute: " + attr.toString());

        case DirContext.REPLACE_ATTRIBUTE:
            return ("Replace attribute: " + attr.toString());

        case DirContext.REMOVE_ATTRIBUTE:
            return ("Remove attribute: " + attr.toString());
        }
        return "";      // should never happen
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = 7573258562534746850L;
}
