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
 * A binary tree of class loaders.
 *
 * @see TreeNodesDenotation
 */
public class ClassLoadersBTree {
    /**
     * Cannot instantiate while a tree root and height
     * are not specified.
     *
     * @see #ClassLoadersBTree(ClassLoader,int)
     */
    protected ClassLoadersBTree() {
    }

    /**
     * Given the <tt>root</tt> loader, instantiate a binary tree
     * of loaders up to the given tree <tt>height</tt>.
     *
     * @see #getHeight()
     * @see #getRoot()
     * @see #getLoader(String)
     * @see #getLoader(int,int)
     */
    public ClassLoadersBTree(ClassLoader root, int height) {
        if (height < 1)
            throw new IllegalArgumentException("wrong height: " + height);
        loaders = new ClassLoader [ height ] [];
        for (int level=0; level<height; level++)
            loaders[level] = new ClassLoader [1 << level];
        loaders[0][0] = root;
        generateSubtree("");
    }
    private void generateSubtree(String name) {
        int level = name.length();
        int height = loaders.length;
        if (level+1 == height)
            return;
        String nameL = name + "L";
        String nameR = name + "R";
        int[] index  = denotation.indexFor(name);
        int[] indexL = denotation.indexFor(nameL);
        int[] indexR = denotation.indexFor(nameR);
        // assertion: index[0]  == level
        // assertion: indexL[0] == indexR[0] == level+1
        int item  = index[1];
        int itemL = indexL[1];
        int itemR = indexR[1];
        ClassLoader parent = loaders[level][item];
        ClassLoader childL = new DummyClassLoader(parent);
        ClassLoader childR = new DummyClassLoader(parent); // another item
        loaders[level+1][itemL] = childL;
        loaders[level+1][itemR] = childR;
        generateSubtree(nameL);
        generateSubtree(nameR);
    }

    private ClassLoader loaders[][]; // loaders[0][0] is root
    private Denotation denotation = new TreeNodesDenotation();

    /**
     * Return the loader found at the given <tt>level</tt> of
     * this loaders tree, and having the given <tt>item</tt>
     * number at the enumeration of that <tt>level</tt>.
     *
     * <p>The value of <tt>level</tt> must be from <tt>0</tt>
     * to <tt>getHeight()-1</tt>, and the <tt>item</tt> number
     * must be from <tt>0</tt> to <tt>2<sup>level</sup>-1</tt>.
     *
     * @see #getHeight()
     * @see #getRoot()
     * @see #getLoader(String)
     */
    public ClassLoader getLoader(int level, int item) {
        return loaders[level][item];
    }

    /**
     * Return the loader having the giving <tt>name</tt> at
     * the standard denotation of binary tree nodes.
     *
     * @see #getHeight()
     * @see #getRoot()
     * @see #getLoader(int,int)
     */
    public ClassLoader getLoader(String name) {
        int[] index = denotation.indexFor(name);
        int level = index[0];
        int item  = index[1];
        return loaders[level][item];
    }

    /**
     * Return the tree's height.
     *
     * @see #ClassLoadersBTree(ClassLoader,int)
     * @see #getLoader(int,int)
     * @see #getLoader(String)
     */
    public int getHeight() {
        return loaders.length;
    }

    /**
     * Return the tree's root loader.
     *
     * @see #ClassLoadersBTree(ClassLoader,int)
     * @see #getLoader(int,int)
     * @see #getLoader(String)
     */
    public ClassLoader getRoot() {
        return loaders[0][0];
    }
}
