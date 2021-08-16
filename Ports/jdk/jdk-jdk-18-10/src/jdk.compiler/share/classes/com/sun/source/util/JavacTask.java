/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.source.util;

import java.io.IOException;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

/**
 * Provides access to functionality specific to the JDK Java Compiler, javac.
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public abstract class JavacTask implements CompilationTask {
    /**
     * Constructor for subclasses to call.
     */
    public JavacTask() {}

    /**
     * Returns the {@code JavacTask} for a {@code ProcessingEnvironment}.
     * If the compiler is being invoked using a
     * {@link javax.tools.JavaCompiler.CompilationTask CompilationTask},
     * then that task will be returned.
     * @param processingEnvironment the processing environment
     * @return the {@code JavacTask} for a {@code ProcessingEnvironment}
     * @since 1.8
     */
    public static JavacTask instance(ProcessingEnvironment processingEnvironment) {
        if (!processingEnvironment.getClass().getName().equals(
                "com.sun.tools.javac.processing.JavacProcessingEnvironment"))
            throw new IllegalArgumentException();
        Context c = ((JavacProcessingEnvironment) processingEnvironment).getContext();
        JavacTask t = c.get(JavacTask.class);
        return (t != null) ? t : new BasicJavacTask(c, true);
    }

    /**
     * Parses the specified files returning a list of abstract syntax trees.
     *
     * @return a list of abstract syntax trees
     * @throws IOException if an unhandled I/O error occurred in the compiler.
     * @throws IllegalStateException if the operation cannot be performed at this time.
     */
    public abstract Iterable<? extends CompilationUnitTree> parse()
        throws IOException;

    /**
     * Completes all analysis.
     *
     * @return a list of elements that were analyzed
     * @throws IOException if an unhandled I/O error occurred in the compiler.
     * @throws IllegalStateException if the operation cannot be performed at this time.
     */
    public abstract Iterable<? extends Element> analyze() throws IOException;

    /**
     * Generates code.
     *
     * @return a list of files that were generated
     * @throws IOException if an unhandled I/O error occurred in the compiler.
     * @throws IllegalStateException if the operation cannot be performed at this time.
     */
    public abstract Iterable<? extends JavaFileObject> generate() throws IOException;

    /**
     * Sets a specified listener to receive notification of events
     * describing the progress of this compilation task.
     *
     * If another listener is receiving notifications as a result of a prior
     * call of this method, then that listener will no longer receive notifications.
     *
     * Informally, this method is equivalent to calling {@code removeTaskListener} for
     * any listener that has been previously set, followed by {@code addTaskListener}
     * for the new listener.
     *
     * @param taskListener the task listener
     * @throws IllegalStateException if the specified listener has already been added.
     */
    public abstract void setTaskListener(TaskListener taskListener);

    /**
     * Adds a specified listener so that it receives notification of events
     * describing the progress of this compilation task.
     *
     * This method may be called at any time before or during the compilation.
     *
     * @param taskListener the task listener
     * @throws IllegalStateException if the specified listener has already been added.
     * @since 1.8
     */
    public abstract void addTaskListener(TaskListener taskListener);

    /**
     * Removes the specified listener so that it no longer receives
     * notification of events describing the progress of this
     * compilation task.
     *
     * This method may be called at any time before or during the compilation.
     *
     * @param taskListener the task listener
     * @since 1.8
     */
    public abstract void removeTaskListener(TaskListener taskListener);

    /**
     * Sets the specified {@link ParameterNameProvider}. It may be used when
     * {@link VariableElement#getSimpleName()} is called for a method parameter
     * for which an authoritative name is not found. The given
     * {@code ParameterNameProvider} may infer a user-friendly name
     * for the method parameter.
     *
     * Setting a new {@code ParameterNameProvider} will clear any previously set
     * {@code ParameterNameProvider}, which won't be queried any more.
     *
     * When no {@code ParameterNameProvider} is set, or when it returns null from
     * {@link ParameterNameProvider#getParameterName(javax.lang.model.element.VariableElement)},
     * an automatically synthesized name is returned from {@code VariableElement.getSimpleName()}.
     *
     * @implSpec The default implementation of this method does nothing.
     *
     * @param provider the provider
     * @since 13
     */
    public void setParameterNameProvider(ParameterNameProvider provider) {}

    /**
     * Returns a type mirror of the tree node determined by the specified path.
     * This method has been superseded by methods on
     * {@link com.sun.source.util.Trees Trees}.
     *
     * @param path the path
     * @return the type mirror
     * @see com.sun.source.util.Trees#getTypeMirror
     */
    public abstract TypeMirror getTypeMirror(Iterable<? extends Tree> path);

    /**
     * Returns a utility object for dealing with program elements.
     *
     * @return a utility object for dealing with program elements
     */
    public abstract Elements getElements();

    /**
     * Returns a utility object for dealing with type mirrors.
     *
     * @return the utility object for dealing with type mirrors
     */
    public abstract Types getTypes();
}
