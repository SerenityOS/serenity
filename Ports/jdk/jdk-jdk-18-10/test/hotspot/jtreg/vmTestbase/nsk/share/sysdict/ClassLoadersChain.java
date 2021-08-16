/*
 * Copyright (c) 2002, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package nsk.share.sysdict;

import nsk.share.*;

/**
 * A chain of class loaders. Imagine a chain as a tower
 * growing downstairs: the 0<sup>th</sup> level is the
 * root, and a level n+1 loader has the n<sup>th</sup>
 * loader as the parent.
 */
public class ClassLoadersChain {
    /**
     * Cannot instantiate while a chain root and height
     * are not specified.
     *
     * @see #ClassLoadersChain(ClassLoader,int)
     */
    protected ClassLoadersChain() {
    }

    /**
     * Given the <tt>root</tt> loader, instantiate a chain
     * of loaders up to the given <tt>height</tt>.
     *
     * @see #getHeight()
     * @see #getRoot()
     * @see #getLoader(int)
     */
    public ClassLoadersChain(ClassLoader root, int height) {
        if (height < 1)
            throw new IllegalArgumentException("wrong height: " + height);
        loaders = new ClassLoader [ height ];
        loaders[0] = root;
        for (int i=1; i<height; i++)
            loaders[i] = new DummyClassLoader(loaders[i-1]);
    }

    private ClassLoader loaders[]; // loaders[0] is the root

    /**
     * Return the loader found at the given <tt>level</tt>.
     * The value of <tt>level</tt> must be from <tt>0</tt>
     * to <tt>getHeight()-1</tt>.
     *
     * @see #getHeight()
     * @see #getRoot()
     */
    public ClassLoader getLoader(int level) {
        return loaders[level];
    }

    /**
     * Return the chain's height.
     *
     * @see #ClassLoadersChain(ClassLoader,int)
     * @see #getLoader(int)
     */
    public int getHeight() {
        return loaders.length;
    }

    /**
     * Return the chain's root loader.
     *
     * @see #ClassLoadersChain(ClassLoader,int)
     * @see #getLoader(int)
     */
    public ClassLoader getRoot() {
        return loaders[0];
    }
}
