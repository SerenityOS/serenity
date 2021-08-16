/*
 * Copyright (c) 1999, 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
  * Supports checking an attribute set satisfies a filter
  * that is specified as a set of "matching" attributes.
  * Checking is done by determining whether the given attribute set
  * is a superset of the matching ones.
  *
  * @author Rosanna Lee
  */

package com.sun.jndi.toolkit.dir;

import javax.naming.*;
import javax.naming.directory.*;

public class ContainmentFilter implements AttrFilter {
    private Attributes matchingAttrs;

    public ContainmentFilter(Attributes match) {
        matchingAttrs = match;
    }

    public boolean check(Attributes attrs) throws NamingException {
        return matchingAttrs == null ||
            matchingAttrs.size() == 0 ||
            contains(attrs, matchingAttrs);
    }

    // returns true if superset contains subset
    public static boolean contains(Attributes superset, Attributes subset)
        throws NamingException {
          if (subset == null)
            return true;  // an empty set is always a subset

            NamingEnumeration<? extends Attribute> m = subset.getAll();
            while (m.hasMore()) {
                if (superset == null) {
                    return false;  // contains nothing
                }
                Attribute target = m.next();
                Attribute fromSuper = superset.get(target.getID());
                if (fromSuper == null) {
                    return false;
                } else {
                    // check whether attribute values match
                    if (target.size() > 0) {
                        NamingEnumeration<?> vals = target.getAll();
                        while (vals.hasMore()) {
                            if (!fromSuper.contains(vals.next())) {
                                return false;
                            }
                        }
                    }
                }
            }
            return true;
        }

}
