/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.internal.doclets.toolkit;

import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.PackageElement;
import javax.tools.FileObject;

import jdk.javadoc.internal.doclets.toolkit.util.Utils;

/**
 * This is a pseudo-element wrapper for doc-files html contents, essentially to
 * associate the doc-files' html documentation's {@code DocCommentTree} to an element.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class DocFileElement implements DocletElement {

    private final PackageElement packageElement;
    private final FileObject fo;

    public DocFileElement(Utils utils, Element element, FileObject fo) {
        this.fo = fo;

        switch(element.getKind()) {
            case MODULE:
                ModuleElement moduleElement = (ModuleElement) element;
                packageElement = utils.elementUtils.getPackageElement(moduleElement, "");
                break;

            case PACKAGE:
                packageElement = (PackageElement) element;
                break;

            default:
                throw new AssertionError("unknown kind: " + element.getKind());
        }
    }

    @Override
    public PackageElement getPackageElement() {
        return packageElement;
    }

    @Override
    public FileObject getFileObject() {
        return fo;
    }

    @Override
    public Kind getSubKind() {
        return Kind.DOCFILE;
    }
}

