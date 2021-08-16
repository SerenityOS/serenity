/*
 * Copyright (c) 1999, Oracle and/or its affiliates. All rights reserved.
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
  * This exception is thrown when a malformed link was encountered while
  * resolving or constructing a link.
  * <p>
  * Synchronization and serialization issues that apply to LinkException
  * apply directly here.
  *
  * @author Rosanna Lee
  * @author Scott Seligman
  *
  * @see LinkRef#getLinkName
  * @see LinkRef
  * @since 1.3
  */

public class MalformedLinkException extends LinkException {
    /**
      * Constructs a new instance of MalformedLinkException with an explanation.
      * All the other fields are initialized to null.
      * @param  explanation     A possibly null string containing additional
      *                         detail about this exception.
      */
    public MalformedLinkException(String explanation) {
        super(explanation);
    }


    /**
      * Constructs a new instance of Malformed LinkException.
      * All fields are initialized to null.
      */
    public MalformedLinkException() {
        super();
    }

    /**
     * Use serialVersionUID from JNDI 1.1.1 for interoperability
     */
    private static final long serialVersionUID = -3066740437737830242L;
}
