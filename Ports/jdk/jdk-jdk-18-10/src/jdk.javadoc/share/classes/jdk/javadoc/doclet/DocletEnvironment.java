/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.javadoc.doclet;

import java.util.Set;

import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject.Kind;

import com.sun.source.util.DocTrees;

/**
 * Represents the operating environment of a single invocation
 * of the doclet. This object can be used to access the program
 * structures, various utilities and the user specified elements
 * on the command line.
 *
 * @since 9
 */
public interface DocletEnvironment {

    /**
     * Returns the elements <a href="package-summary.html#specified">specified</a>
     * when the tool is invoked.
     *
     * @return the set of specified elements
     */
    Set<? extends Element> getSpecifiedElements();

    /**
     * Returns the module, package and type elements that should be
     * <a href="package-summary.html#included">included</a> in the
     * documentation.
     *
     * @return the set of included elements
     */
    Set<? extends Element> getIncludedElements();

    /**
     * Returns an instance of the {@code DocTrees} utility class.
     * This class provides methods to access {@code TreePath}s, {@code DocCommentTree}s
     * and so on.
     *
     * @return a utility class to operate on doc trees
     */
    DocTrees getDocTrees();

    /**
     * Returns an instance of the {@code Elements} utility class.
     * This class provides methods for operating on
     * {@link javax.lang.model.element.Element elements}.
     *
     * @return a utility class to operate on elements
     */
    Elements getElementUtils();

    /**
     * Returns an instance of the {@code Types} utility class.
     * This class provides methods for operating on
     * {@link javax.lang.model.type.TypeMirror type mirrors}.
     *
     * @return a utility class to operate on type mirrors
     */
    Types getTypeUtils();

    /**
     * Returns true if an element should be
     * <a href="package-summary.html#included">included</a> in the
     * documentation.
     *
     * @param e the element
     * @return true if included, false otherwise
     */
    boolean isIncluded(Element e);

    /**
     * Returns true if the element is <a href="package-summary.html#selected">selected</a>.
     *
     * @param e the element
     * @return true if selected, false otherwise
     */
    boolean isSelected(Element e);

    /**
     * Returns the file manager used to read and write files.
     *
     * @return the file manager used to read and write files
     */
    JavaFileManager getJavaFileManager();

    /**
     * Returns the source version of the source files that were read.
     *
     * @return the source version
     */
    SourceVersion getSourceVersion();

    /**
     * Returns the required level of module documentation.
     *
     * @return the required level of module documentation
     */
    ModuleMode getModuleMode();

    /**
     * Returns the file kind of a type element.
     *
     * @param type the type element
     * @return the file kind
     */
    Kind getFileKind(TypeElement type);

    /**
     * The mode specifying the level of detail of module documentation.
     */
    enum ModuleMode {
        /** Indicate API level documentation is required */
        API,
        /** Indicate Detailed documentation is required */
        ALL
    }
}
