/*
 * Copyright (c) 2010, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.util.*;
import static javax.lang.model.SourceVersion.*;

/**
 * An abstract annotation processor tailored to {@code javac} regression testing.
 */
public abstract class JavacTestingAbstractProcessor extends AbstractProcessor {
    private static final Set<String> allAnnotations = Set.of("*");

    protected Elements eltUtils;
    protected Elements elements;
    protected Types    typeUtils;
    protected Types    types;
    protected Filer    filer;
    protected Messager messager;
    protected Map<String, String> options;

    /**
     * Constructor for subclasses to call.
     */
    protected JavacTestingAbstractProcessor() {
        super();
    }

    /**
     * Return the latest source version. Unless this method is
     * overridden, an {@code IllegalStateException} will be thrown if a
     * subclass has a {@code SupportedSourceVersion} annotation.
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        SupportedSourceVersion ssv = this.getClass().getAnnotation(SupportedSourceVersion.class);
        if (ssv != null)
            throw new IllegalStateException("SupportedSourceVersion annotation not supported here.");

        return SourceVersion.latest();
    }

    /**
     * If the processor class is annotated with {@link
     * SupportedAnnotationTypes}, return an unmodifiable set with the
     * same set of strings as the annotation.  If the class is not so
     * annotated, a one-element set containing {@code "*"} is returned
     * to indicate all annotations are processed.
     *
     * @return the names of the annotation types supported by this
     * processor, or an empty set if none
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        SupportedAnnotationTypes sat = this.getClass().getAnnotation(SupportedAnnotationTypes.class);
        if (sat != null)
            return super.getSupportedAnnotationTypes();
        else
            return allAnnotations;
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        elements = eltUtils  = processingEnv.getElementUtils();
        types = typeUtils = processingEnv.getTypeUtils();
        filer     = processingEnv.getFiler();
        messager  = processingEnv.getMessager();
        options   = processingEnv.getOptions();
    }

    protected void addExports(String moduleName, String... packageNames) {
        for (String packageName : packageNames) {
            try {
                ModuleLayer layer = ModuleLayer.boot();
                Optional<Module> m = layer.findModule(moduleName);
                if (!m.isPresent())
                    throw new Error("module not found: " + moduleName);
                m.get().addExports(packageName, getClass().getModule());
            } catch (Exception e) {
                throw new Error("failed to add exports for " + moduleName + "/" + packageName);
            }
        }
    }

    /*
     * The set of visitors below will directly extend the most recent
     * corresponding platform visitor type.
     */

    @SupportedSourceVersion(RELEASE_18)
    public static abstract class AbstractAnnotationValueVisitor<R, P> extends AbstractAnnotationValueVisitor14<R, P> {

        /**
         * Constructor for concrete subclasses to call.
         */
        protected AbstractAnnotationValueVisitor() {
            super();
        }
    }

    @SupportedSourceVersion(RELEASE_18)
    public static abstract class AbstractElementVisitor<R, P> extends AbstractElementVisitor14<R, P> {
        /**
         * Constructor for concrete subclasses to call.
         */
        protected AbstractElementVisitor(){
            super();
        }
    }

    @SupportedSourceVersion(RELEASE_18)
    public static abstract class AbstractTypeVisitor<R, P> extends AbstractTypeVisitor14<R, P> {
        /**
         * Constructor for concrete subclasses to call.
         */
        protected AbstractTypeVisitor() {
            super();
        }
    }

    @SupportedSourceVersion(RELEASE_18)
    public static class ElementKindVisitor<R, P> extends ElementKindVisitor14<R, P> {
        /**
         * Constructor for concrete subclasses; uses {@code null} for the
         * default value.
         */
        protected ElementKindVisitor() {
            super(null);
        }

        /**
         * Constructor for concrete subclasses; uses the argument for the
         * default value.
         *
         * @param defaultValue the value to assign to {@link #DEFAULT_VALUE}
         */
        protected ElementKindVisitor(R defaultValue) {
            super(defaultValue);
        }
    }

    @SupportedSourceVersion(RELEASE_18)
    public static class ElementScanner<R, P> extends ElementScanner14<R, P> {
        /**
         * Constructor for concrete subclasses; uses {@code null} for the
         * default value.
         */
        protected ElementScanner(){
            super(null);
        }

        /**
         * Constructor for concrete subclasses; uses the argument for the
         * default value.
         */
        protected ElementScanner(R defaultValue){
            super(defaultValue);
        }
    }

    @SupportedSourceVersion(RELEASE_18)
    public static class SimpleAnnotationValueVisitor<R, P> extends SimpleAnnotationValueVisitor14<R, P> {
        /**
         * Constructor for concrete subclasses; uses {@code null} for the
         * default value.
         */
        protected SimpleAnnotationValueVisitor() {
            super(null);
        }

        /**
         * Constructor for concrete subclasses; uses the argument for the
         * default value.
         *
         * @param defaultValue the value to assign to {@link #DEFAULT_VALUE}
         */
        protected SimpleAnnotationValueVisitor(R defaultValue) {
            super(defaultValue);
        }
    }

    @SupportedSourceVersion(RELEASE_18)
    public static class SimpleElementVisitor<R, P> extends SimpleElementVisitor14<R, P> {
        /**
         * Constructor for concrete subclasses; uses {@code null} for the
         * default value.
         */
        protected SimpleElementVisitor(){
            super(null);
        }

        /**
         * Constructor for concrete subclasses; uses the argument for the
         * default value.
         *
         * @param defaultValue the value to assign to {@link #DEFAULT_VALUE}
         */
        protected SimpleElementVisitor(R defaultValue){
            super(defaultValue);
        }
    }

    @SupportedSourceVersion(RELEASE_18)
    public static class SimpleTypeVisitor<R, P> extends SimpleTypeVisitor14<R, P> {
        /**
         * Constructor for concrete subclasses; uses {@code null} for the
         * default value.
         */
        protected SimpleTypeVisitor(){
            super(null);
        }

        /**
         * Constructor for concrete subclasses; uses the argument for the
         * default value.
         *
         * @param defaultValue the value to assign to {@link #DEFAULT_VALUE}
         */
        protected SimpleTypeVisitor(R defaultValue){
            super(defaultValue);
        }
    }

    @SupportedSourceVersion(RELEASE_18)
    public static class TypeKindVisitor<R, P> extends TypeKindVisitor14<R, P> {
        /**
         * Constructor for concrete subclasses to call; uses {@code null}
         * for the default value.
         */
        protected TypeKindVisitor() {
            super(null);
        }

        /**
         * Constructor for concrete subclasses to call; uses the argument
         * for the default value.
         *
         * @param defaultValue the value to assign to {@link #DEFAULT_VALUE}
         */
        protected TypeKindVisitor(R defaultValue) {
            super(defaultValue);
        }
    }
}
