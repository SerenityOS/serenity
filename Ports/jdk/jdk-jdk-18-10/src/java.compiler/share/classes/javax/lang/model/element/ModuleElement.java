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

package javax.lang.model.element;

import java.util.List;
import javax.lang.model.type.TypeMirror;

/**
 * Represents a module program element.  Provides access to
 * information about the module, its directives, and its members.
 *
 * @see javax.lang.model.util.Elements#getModuleOf
 * @since 9
 * @jls 7.7 Module Declarations
 */
public interface ModuleElement extends Element, QualifiedNameable {
    /**
     * {@return a {@linkplain javax.lang.model.type.NoType pseudo-type}
     * for this module}
     *
     * @see javax.lang.model.type.NoType
     * @see javax.lang.model.type.TypeKind#MODULE
     */
    @Override
    TypeMirror asType();

    /**
     * Returns the fully qualified name of this module.  For an
     * {@linkplain #isUnnamed() unnamed module}, an <a
     * href=Name.html#empty_name>empty name</a> is returned.
     *
     * @apiNote If the module name consists of one identifier, then
     * this method returns that identifier, which is deemed to be
     * module's fully qualified name despite not being in qualified
     * form.  If the module name consists of more than one identifier,
     * then this method returns the entire name.
     *
     * @return the fully qualified name of this module, or an
     * empty name if this is an unnamed module
     *
     * @jls 6.2 Names and Identifiers
     */
    @Override
    Name getQualifiedName();

    /**
     * Returns the simple name of this module.  For an {@linkplain
     * #isUnnamed() unnamed module}, an <a
     * href=Name.html#empty_name>empty name</a> is returned.
     *
     * @apiNote If the module name consists of one identifier, then
     * this method returns that identifier.  If the module name
     * consists of more than one identifier, then this method returns
     * the rightmost such identifier, which is deemed to be the
     * module's simple name.
     *
     * @return the simple name of this module or an empty name if
     * this is an unnamed module
     *
     * @jls 6.2 Names and Identifiers
     */
    @Override
    Name getSimpleName();

    /**
     * {@return the packages within this module}
     */
    @Override
    List<? extends Element> getEnclosedElements();

    /**
     * {@return {@code true} if this is an open module and {@code
     * false} otherwise}
     */
    boolean isOpen();

    /**
     * {@return {@code true} if this is an unnamed module and {@code
     * false} otherwise}
     *
     * @jls 7.7.5 Unnamed Modules
     */
    boolean isUnnamed();

    /**
     * Returns {@code null} since a module is not enclosed by another
     * element.
     *
     * @return {@code null}
     */
    @Override
    Element getEnclosingElement();

    /**
     * Returns the directives contained in the declaration of this module.
     * @return  the directives in the declaration of this module
     */
    List<? extends Directive> getDirectives();

    /**
     * The {@code kind} of a directive.
     *
     * <p>Note that it is possible additional directive kinds will be added
     * to accommodate new, currently unknown, language structures added to
     * future versions of the Java programming language.
     *
     * @since 9
     */
    enum DirectiveKind {
        /** A "requires (static|transitive)* module-name" directive. */
        REQUIRES,
        /** An "exports package-name [to module-name-list]" directive. */
        EXPORTS,
        /** An "opens package-name [to module-name-list]" directive. */
        OPENS,
        /** A "uses service-name" directive. */
        USES,
        /** A "provides service-name with implementation-name" directive. */
        PROVIDES
    };

    /**
     * Represents a directive within the declaration of this
     * module. The directives of a module declaration configure the
     * module in the Java Platform Module System.
     *
     * @since 9
     */
    interface Directive {
        /**
         * {@return the {@code kind} of this directive}
         * <ul>
         *
         * <li> The kind of a {@linkplain RequiresDirective requires
         * directive} is {@link DirectiveKind#REQUIRES REQUIRES}.
         *
         * <li> The kind of an {@linkplain ExportsDirective exports
         * directive} is {@link DirectiveKind#EXPORTS EXPORTS}.
         *
         * <li> The kind of an {@linkplain OpensDirective opens
         * directive} is {@link DirectiveKind#OPENS OPENS}.
         *
         * <li> The kind of a {@linkplain UsesDirective uses
         * directive} is {@link DirectiveKind#USES USES}.
         *
         * <li> The kind of a {@linkplain ProvidesDirective provides
         * directive} is {@link DirectiveKind#PROVIDES PROVIDES}.
         *
         * </ul>
         */
        DirectiveKind getKind();

        /**
         * Applies a visitor to this directive.
         *
         * @param <R> the return type of the visitor's methods
         * @param <P> the type of the additional parameter to the visitor's methods
         * @param v   the visitor operating on this directive
         * @param p   additional parameter to the visitor
         * @return a visitor-specified result
         */
        <R, P> R accept(DirectiveVisitor<R, P> v, P p);
    }

    /**
     * A visitor of module directives, in the style of the visitor design
     * pattern.  Classes implementing this interface are used to operate
     * on a directive when the kind of directive is unknown at compile time.
     * When a visitor is passed to a directive's {@link Directive#accept
     * accept} method, the <code>visit<i>Xyz</i></code> method applicable
     * to that directive is invoked.
     *
     * <p> Classes implementing this interface may or may not throw a
     * {@code NullPointerException} if the additional parameter {@code p}
     * is {@code null}; see documentation of the implementing class for
     * details.
     *
     * <p> <b>WARNING:</b> It is possible that methods will be added to
     * this interface to accommodate new, currently unknown, language
     * structures added to future versions of the Java programming
     * language. Methods to accommodate new language constructs will
     * be added in a source <em>compatible</em> way using
     * <em>default methods</em>.
     *
     * @param <R> the return type of this visitor's methods.  Use {@link
     *            Void} for visitors that do not need to return results.
     * @param <P> the type of the additional parameter to this visitor's
     *            methods.  Use {@code Void} for visitors that do not need an
     *            additional parameter.
     *
     * @since 9
     */
    interface DirectiveVisitor<R, P> {
        /**
         * Visits any directive as if by passing itself to that
         * directive's {@link Directive#accept accept} method and passing
         * {@code null} for the additional parameter.
         *
         * @param d  the directive to visit
         * @return a visitor-specified result
         * @implSpec The default implementation is {@code d.accept(v, null)}.
         */
        default R visit(Directive d) {
            return d.accept(this, null);
        }

        /**
         * Visits any directive as if by passing itself to that
         * directive's {@link Directive#accept accept} method.
         *
         * @param d  the directive to visit
         * @param p  a visitor-specified parameter
         * @return a visitor-specified result
         * @implSpec The default implementation is {@code d.accept(v, p)}.
         */
        default R visit(Directive d, P p) {
            return d.accept(this, p);
        }

        /**
         * Visits a {@code requires} directive.
         * @param d  the directive to visit
         * @param p  a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitRequires(RequiresDirective d, P p);

        /**
         * Visits an {@code exports} directive.
         * @param d  the directive to visit
         * @param p  a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitExports(ExportsDirective d, P p);

        /**
         * Visits an {@code opens} directive.
         * @param d  the directive to visit
         * @param p  a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitOpens(OpensDirective d, P p);

        /**
         * Visits a {@code uses} directive.
         * @param d  the directive to visit
         * @param p  a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitUses(UsesDirective d, P p);

        /**
         * Visits a {@code provides} directive.
         * @param d  the directive to visit
         * @param p  a visitor-specified parameter
         * @return a visitor-specified result
         */
        R visitProvides(ProvidesDirective d, P p);

        /**
         * Visits an unknown directive.
         * This can occur if the language evolves and new kinds of directive are added.
         * @param d  the directive to visit
         * @param p  a visitor-specified parameter
         * @return a visitor-specified result
         * @throws UnknownDirectiveException a visitor implementation may optionally throw this exception
         * @implSpec The default implementation throws {@code new UnknownDirectiveException(d, p)}.
         */
        default R visitUnknown(Directive d, P p) {
            throw new UnknownDirectiveException(d, p);
        }
    }

    /**
     * A dependency of a module.
     * @since 9
     */
    interface RequiresDirective extends Directive {
        /**
         * {@return whether or not this is a static dependency}
         */
        boolean isStatic();

        /**
         * {@return whether or not this is a transitive dependency}
         */
        boolean isTransitive();

        /**
         * {@return the module that is required}
         */
        ModuleElement getDependency();
    }

    /**
     * An exported package of a module.
     * @since 9
     */
    interface ExportsDirective extends Directive {

        /**
         * {@return the package being exported}
         */
        PackageElement getPackage();

        /**
         * Returns the specific modules to which the package is being exported,
         * or {@code null}, if the package is exported to all modules which
         * have readability to this module.
         * @return the specific modules to which the package is being exported
         */
        List<? extends ModuleElement> getTargetModules();
    }

    /**
     * An opened package of a module.
     * @since 9
     */
    interface OpensDirective extends Directive {

        /**
         * {@return the package being opened}
         */
        PackageElement getPackage();

        /**
         * Returns the specific modules to which the package is being open
         * or {@code null}, if the package is open all modules which
         * have readability to this module.
         * @return the specific modules to which the package is being opened
         */
        List<? extends ModuleElement> getTargetModules();
    }

    /**
     * An implementation of a service provided by a module.
     * @since 9
     */
    interface ProvidesDirective extends Directive {
        /**
         * {@return the service being provided}
         */
        TypeElement getService();

        /**
         * {@return the implementations of the service being provided}
         */
        List<? extends TypeElement> getImplementations();
    }

    /**
     * A reference to a service used by a module.
     * @since 9
     */
    interface UsesDirective extends Directive {
        /**
         * {@return the service that is used}
         */
        TypeElement getService();
    }
}
