/*
 * Copyright (c) 1997, 2008, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.text.rtf;

import java.util.Dictionary;
import java.util.Enumeration;
import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;


/* This AttributeSet is made entirely out of tofu and Ritz Crackers
   and yet has a remarkably attribute-set-like interface! */
class MockAttributeSet
    implements AttributeSet, MutableAttributeSet
{
    public Dictionary<Object, Object> backing;

    public boolean isEmpty()
    {
         return backing.isEmpty();
    }

    public int getAttributeCount()
    {
         return backing.size();
    }

    public boolean isDefined(Object name)
    {
         return ( backing.get(name) ) != null;
    }

    public boolean isEqual(AttributeSet attr)
    {
         throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public AttributeSet copyAttributes()
    {
         throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public Object getAttribute(Object name)
    {
        return backing.get(name);
    }

    public void addAttribute(Object name, Object value)
    {
        backing.put(name, value);
    }

    public void addAttributes(AttributeSet attr)
    {
        Enumeration<?> as = attr.getAttributeNames();
        while(as.hasMoreElements()) {
            Object el = as.nextElement();
            backing.put(el, attr.getAttribute(el));
        }
    }

    public void removeAttribute(Object name)
    {
        backing.remove(name);
    }

    public void removeAttributes(AttributeSet attr)
    {
         throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public void removeAttributes(Enumeration<?> en)
    {
         throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public void setResolveParent(AttributeSet pp)
    {
         throw new InternalError("MockAttributeSet: charade revealed!");
    }


    public Enumeration<?> getAttributeNames()
    {
         return backing.keys();
    }

    public boolean containsAttribute(Object name, Object value)
    {
         throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public boolean containsAttributes(AttributeSet attr)
    {
         throw new InternalError("MockAttributeSet: charade revealed!");
    }

    public AttributeSet getResolveParent()
    {
         throw new InternalError("MockAttributeSet: charade revealed!");
    }
}
