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

package javax.annotation.processing;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.Objects;
import javax.lang.model.element.*;
import javax.lang.model.SourceVersion;
import javax.tools.Diagnostic;

/**
 * An abstract annotation processor designed to be a convenient
 * superclass for most concrete annotation processors.  This class
 * examines annotation values to compute the {@linkplain
 * #getSupportedOptions options}, {@linkplain
 * #getSupportedAnnotationTypes annotation interfaces}, and
 * {@linkplain #getSupportedSourceVersion source version} supported by
 * its subtypes.
 *
 * <p>The getter methods may {@linkplain Messager#printMessage issue
 * warnings} about noteworthy conditions using the facilities available
 * after the processor has been {@linkplain #isInitialized
 * initialized}.
 *
 * <p>Subclasses are free to override the implementation and
 * specification of any of the methods in this class as long as the
 * general {@link javax.annotation.processing.Processor Processor}
 * contract for that method is obeyed.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public abstract class AbstractProcessor implements Processor {
    /**
     * Processing environment providing by the tool framework.
     */
    protected ProcessingEnvironment processingEnv;
    private boolean initialized = false;

    /**
     * Constructor for subclasses to call.
     */
    protected AbstractProcessor() {}

    /**
     * If the processor class is annotated with {@link
     * SupportedOptions}, return an unmodifiable set with the same set
     * of strings as the annotation.  If the class is not so
     * annotated, an empty set is returned.
     *
     * @return the options recognized by this processor, or an empty
     * set if none
     */
    public Set<String> getSupportedOptions() {
        SupportedOptions so = this.getClass().getAnnotation(SupportedOptions.class);
        return (so == null) ?
            Set.of() :
            arrayToSet(so.value(), false, "option value", "@SupportedOptions");
    }

    /**
     * If the processor class is annotated with {@link
     * SupportedAnnotationTypes}, return an unmodifiable set with the
     * same set of strings as the annotation.  If the class is not so
     * annotated, an empty set is returned.
     *
     * If the {@linkplain ProcessingEnvironment#getSourceVersion source
     * version} does not support modules, in other words if it is less
     * than or equal to {@link SourceVersion#RELEASE_8 RELEASE_8},
     * then any leading {@linkplain Processor#getSupportedAnnotationTypes
     * module prefixes} are stripped from the names.
     *
     * @return the names of the annotation interfaces supported by
     * this processor, or an empty set if none
     */
    public Set<String> getSupportedAnnotationTypes() {
            SupportedAnnotationTypes sat = this.getClass().getAnnotation(SupportedAnnotationTypes.class);
            boolean initialized = isInitialized();
            if  (sat == null) {
                if (initialized)
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                                             "No SupportedAnnotationTypes annotation " +
                                                             "found on " + this.getClass().getName() +
                                                             ", returning an empty set.");
                return Set.of();
            } else {
                boolean stripModulePrefixes =
                        initialized &&
                        processingEnv.getSourceVersion().compareTo(SourceVersion.RELEASE_8) <= 0;
                return arrayToSet(sat.value(), stripModulePrefixes,
                                  "annotation type", "@SupportedAnnotationTypes");
            }
        }

    /**
     * If the processor class is annotated with {@link
     * SupportedSourceVersion}, return the source version in the
     * annotation.  If the class is not so annotated, {@link
     * SourceVersion#RELEASE_6} is returned.
     *
     * @return the latest source version supported by this processor
     */
    public SourceVersion getSupportedSourceVersion() {
        SupportedSourceVersion ssv = this.getClass().getAnnotation(SupportedSourceVersion.class);
        SourceVersion sv = null;
        if (ssv == null) {
            sv = SourceVersion.RELEASE_6;
            if (isInitialized())
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                                         "No SupportedSourceVersion annotation " +
                                                         "found on " + this.getClass().getName() +
                                                         ", returning " + sv + ".");
        } else
            sv = ssv.value();
        return sv;
    }


    /**
     * Initializes the processor with the processing environment by
     * setting the {@code processingEnv} field to the value of the
     * {@code processingEnv} argument.  An {@code
     * IllegalStateException} will be thrown if this method is called
     * more than once on the same object.
     *
     * @param processingEnv environment to access facilities the tool framework
     * provides to the processor
     * @throws IllegalStateException if this method is called more than once.
     */
    public synchronized void init(ProcessingEnvironment processingEnv) {
        if (initialized)
            throw new IllegalStateException("Cannot call init more than once.");
        Objects.requireNonNull(processingEnv, "Tool provided null ProcessingEnvironment");

        this.processingEnv = processingEnv;
        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    public abstract boolean process(Set<? extends TypeElement> annotations,
                                    RoundEnvironment roundEnv);

    /**
     * {@return an empty iterable of completions}
     *
     * @param element {@inheritDoc}
     * @param annotation {@inheritDoc}
     * @param member {@inheritDoc}
     * @param userText {@inheritDoc}
     */
    public Iterable<? extends Completion> getCompletions(Element element,
                                                         AnnotationMirror annotation,
                                                         ExecutableElement member,
                                                         String userText) {
        return List.of();
    }

    /**
     * {@return {@code true} if this object has been {@linkplain #init
     * initialized}, {@code false} otherwise}
     */
    protected synchronized boolean isInitialized() {
        return initialized;
    }

    private Set<String> arrayToSet(String[] array,
                                          boolean stripModulePrefixes,
                                   String contentType,
                                   String annotationName) {
        assert array != null;
        Set<String> set = new HashSet<>();
        for (String s : array) {
            boolean stripped = false;
            if (stripModulePrefixes) {
                int index = s.indexOf('/');
                if (index != -1) {
                    s = s.substring(index + 1);
                    stripped = true;
                }
            }
            boolean added = set.add(s);
            // Don't issue a duplicate warning when the module name is
            // stripped off to avoid spurious warnings in a case like
            // "foo/a.B", "bar/a.B".
            if (!added && !stripped && isInitialized() ) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                                         "Duplicate " + contentType  +
                                                         " ``" + s  + "'' for processor " +
                                                         this.getClass().getName() +
                                                         " in its " + annotationName  +
                                                         "annotation.");
            }
        }
        return Collections.unmodifiableSet(set);
    }
}
