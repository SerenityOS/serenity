/*
 * Copyright (c) 2009, 2021, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.module;

import java.io.InputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static jdk.internal.module.Checks.*;
import static java.util.Objects.*;

import jdk.internal.module.Checks;
import jdk.internal.module.ModuleInfo;


/**
 * A module descriptor.
 *
 * <p> A module descriptor describes a named module and defines methods to
 * obtain each of its components. The module descriptor for a named module
 * in the Java virtual machine is obtained by invoking the {@link
 * java.lang.Module Module}'s {@link java.lang.Module#getDescriptor
 * getDescriptor} method. Module descriptors can also be created using the
 * {@link ModuleDescriptor.Builder} class or by reading the binary form of a
 * module declaration ({@code module-info.class}) using the {@link
 * #read(InputStream,Supplier) read} methods defined here. </p>
 *
 * <p> A module descriptor describes a <em>normal</em>, open, or automatic
 * module. <em>Normal</em> modules and open modules describe their {@linkplain
 * #requires() dependences}, {@link #exports() exported-packages}, the services
 * that they {@linkplain #uses() use} or {@linkplain #provides() provide}, and other
 * components. <em>Normal</em> modules may {@linkplain #opens() open} specific
 * packages. The module descriptor for an open module does not declare any
 * open packages (its {@code opens} method returns an empty set) but when
 * instantiated in the Java virtual machine then it is treated as if all
 * packages are open. The module descriptor for an automatic module does not
 * declare any dependences (except for the mandatory dependency on {@code
 * java.base}), and does not declare any exported or open packages. Automatic
 * modules receive special treatment during resolution so that they read all
 * other modules in the configuration. When an automatic module is instantiated
 * in the Java virtual machine then it reads every unnamed module and is
 * treated as if all packages are exported and open. </p>
 *
 * <p> {@code ModuleDescriptor} objects are immutable and safe for use by
 * multiple concurrent threads.</p>
 *
 * @see java.lang.Module
 * @since 9
 */

public class ModuleDescriptor
    implements Comparable<ModuleDescriptor>
{

    /**
     * A modifier on a module.
     *
     * @see ModuleDescriptor#modifiers()
     * @since 9
     */
    public enum Modifier {
        /**
         * An open module. An open module does not declare any open packages
         * but the resulting module is treated as if all packages are open.
         */
        OPEN,

        /**
         * An automatic module. An automatic module is treated as if it exports
         * and opens all packages.
         *
         * @apiNote This modifier does not correspond to a module flag in the
         * binary form of a module declaration ({@code module-info.class}).
         */
        AUTOMATIC,

        /**
         * The module was not explicitly or implicitly declared.
         */
        SYNTHETIC,

        /**
         * The module was implicitly declared.
         */
        MANDATED;
    }


    /**
     * <p> A dependence upon a module. </p>
     *
     * @see ModuleDescriptor#requires()
     * @since 9
     */

    public static final class Requires
        implements Comparable<Requires>
    {

        /**
         * A modifier on a module dependence.
         *
         * @see Requires#modifiers()
         * @since 9
         */
        public enum Modifier {

            /**
             * The dependence causes any module which depends on the <i>current
             * module</i> to have an implicitly declared dependence on the module
             * named by the {@code Requires}.
             */
            TRANSITIVE,

            /**
             * The dependence is mandatory in the static phase, during compilation,
             * but is optional in the dynamic phase, during execution.
             */
            STATIC,

            /**
             * The dependence was not explicitly or implicitly declared in the
             * source of the module declaration.
             */
            SYNTHETIC,

            /**
             * The dependence was implicitly declared in the source of the module
             * declaration.
             */
            MANDATED;

        }

        private final Set<Modifier> mods;
        private final String name;
        private final Version compiledVersion;
        private final String rawCompiledVersion;

        private Requires(Set<Modifier> ms, String mn, Version v, String vs) {
            assert v == null || vs == null;
            this.mods = Set.copyOf(ms);
            this.name = mn;
            this.compiledVersion = v;
            this.rawCompiledVersion = vs;
        }

        private Requires(Set<Modifier> ms, String mn, Version v, boolean unused) {
            this.mods = ms;
            this.name = mn;
            this.compiledVersion = v;
            this.rawCompiledVersion = null;
        }

        /**
         * Returns the set of modifiers.
         *
         * @return A possibly-empty unmodifiable set of modifiers
         */
        public Set<Modifier> modifiers() {
            return mods;
        }

        /**
         * Return the module name.
         *
         * @return The module name
         */
        public String name() {
            return name;
        }

        /**
         * Returns the version of the module if recorded at compile-time.
         *
         * @return The version of the module if recorded at compile-time,
         *         or an empty {@code Optional} if no version was recorded or
         *         the version string recorded is {@linkplain Version#parse(String)
         *         unparseable}
         */
        public Optional<Version> compiledVersion() {
            return Optional.ofNullable(compiledVersion);
        }

        /**
         * Returns the string with the possibly-unparseable version of the module
         * if recorded at compile-time.
         *
         * @return The string containing the version of the module if recorded
         *         at compile-time, or an empty {@code Optional} if no version
         *         was recorded
         *
         * @see #compiledVersion()
         */
        public Optional<String> rawCompiledVersion() {
            if (compiledVersion != null) {
                return Optional.of(compiledVersion.toString());
            } else {
                return Optional.ofNullable(rawCompiledVersion);
            }
        }

        /**
         * Compares this module dependence to another.
         *
         * <p> Two {@code Requires} objects are compared by comparing their
         * module names lexicographically. Where the module names are equal
         * then the sets of modifiers are compared in the same way that
         * module modifiers are compared (see {@link ModuleDescriptor#compareTo
         * ModuleDescriptor.compareTo}). Where the module names are equal and
         * the set of modifiers are equal then the version of the modules
         * recorded at compile-time are compared. When comparing the versions
         * recorded at compile-time then a dependence that has a recorded
         * version is considered to succeed a dependence that does not have a
         * recorded version. If both recorded versions are {@linkplain
         * Version#parse(String) unparseable} then the {@linkplain
         * #rawCompiledVersion() raw version strings} are compared
         * lexicographically. </p>
         *
         * @param  that
         *         The module dependence to compare
         *
         * @return A negative integer, zero, or a positive integer if this module
         *         dependence is less than, equal to, or greater than the given
         *         module dependence
         */
        @Override
        public int compareTo(Requires that) {
            if (this == that) return 0;

            int c = this.name().compareTo(that.name());
            if (c != 0) return c;

            // modifiers
            long v1 = modsValue(this.modifiers());
            long v2 = modsValue(that.modifiers());
            c = Long.compare(v1, v2);
            if (c != 0) return c;

            // compiledVersion
            c = compare(this.compiledVersion, that.compiledVersion);
            if (c != 0) return c;

            // rawCompiledVersion
            c = compare(this.rawCompiledVersion, that.rawCompiledVersion);
            if (c != 0) return c;

            return 0;
        }

        /**
         * Tests this module dependence for equality with the given object.
         *
         * <p> If the given object is not a {@code Requires} then this method
         * returns {@code false}. Two module dependence objects are equal if
         * the module names are equal, set of modifiers are equal, and the
         * compiled version of both modules is equal or not recorded for
         * both modules. </p>
         *
         * <p> This method satisfies the general contract of the {@link
         * java.lang.Object#equals(Object) Object.equals} method. </p>
         *
         * @param   ob
         *          the object to which this object is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a module
         *          dependence that is equal to this module dependence
         */
        @Override
        public boolean equals(Object ob) {
            return (ob instanceof Requires that)
                    && name.equals(that.name) && mods.equals(that.mods)
                    && Objects.equals(compiledVersion, that.compiledVersion)
                    && Objects.equals(rawCompiledVersion, that.rawCompiledVersion);
        }

        /**
         * Computes a hash code for this module dependence.
         *
         * <p> The hash code is based upon the module name, modifiers, and the
         * module version if recorded at compile time. It satisfies the general
         * contract of the {@link Object#hashCode Object.hashCode} method. </p>
         *
         * @return The hash-code value for this module dependence
         */
        @Override
        public int hashCode() {
            int hash = name.hashCode() * 43 + mods.hashCode();
            if (compiledVersion != null)
                hash = hash * 43 + compiledVersion.hashCode();
            if (rawCompiledVersion != null)
                hash = hash * 43 + rawCompiledVersion.hashCode();
            return hash;
        }

        /**
         * Returns a string describing this module dependence.
         *
         * @return A string describing this module dependence
         */
        @Override
        public String toString() {
            String what;
            if (compiledVersion != null) {
                what = name() + " (@" + compiledVersion + ")";
            } else {
                what = name();
            }
            return ModuleDescriptor.toString(mods, what);
        }
    }


    /**
     * <p> A package exported by a module, may be qualified or unqualified. </p>
     *
     * @see ModuleDescriptor#exports()
     * @since 9
     */

    public static final class Exports
        implements Comparable<Exports>
    {

        /**
         * A modifier on an exported package.
         *
         * @see Exports#modifiers()
         * @since 9
         */
        public enum Modifier {

            /**
             * The export was not explicitly or implicitly declared in the
             * source of the module declaration.
             */
            SYNTHETIC,

            /**
             * The export was implicitly declared in the source of the module
             * declaration.
             */
            MANDATED;

        }

        private final Set<Modifier> mods;
        private final String source;
        private final Set<String> targets;  // empty if unqualified export

        /**
         * Constructs an export
         */
        private Exports(Set<Modifier> ms, String source, Set<String> targets) {
            this.mods = Set.copyOf(ms);
            this.source = source;
            this.targets = Set.copyOf(targets);
        }

        private Exports(Set<Modifier> ms,
                        String source,
                        Set<String> targets,
                        boolean unused) {
            this.mods = ms;
            this.source = source;
            this.targets = targets;
        }

        /**
         * Returns the set of modifiers.
         *
         * @return A possibly-empty unmodifiable set of modifiers
         */
        public Set<Modifier> modifiers() {
            return mods;
        }

        /**
         * Returns {@code true} if this is a qualified export.
         *
         * @return {@code true} if this is a qualified export
         */
        public boolean isQualified() {
            return !targets.isEmpty();
        }

        /**
         * Returns the package name.
         *
         * @return The package name
         */
        public String source() {
            return source;
        }

        /**
         * For a qualified export, returns the non-empty and immutable set
         * of the module names to which the package is exported. For an
         * unqualified export, returns an empty set.
         *
         * @return The set of target module names or for an unqualified
         *         export, an empty set
         */
        public Set<String> targets() {
            return targets;
        }

        /**
         * Compares this module export to another.
         *
         * <p> Two {@code Exports} objects are compared by comparing the package
         * names lexicographically. Where the packages names are equal then the
         * sets of modifiers are compared in the same way that module modifiers
         * are compared (see {@link ModuleDescriptor#compareTo
         * ModuleDescriptor.compareTo}). Where the package names are equal and
         * the set of modifiers are equal then the set of target modules are
         * compared. This is done by sorting the names of the target modules
         * in ascending order, and according to their natural ordering, and then
         * comparing the corresponding elements lexicographically. Where the
         * sets differ in size, and the larger set contains all elements of the
         * smaller set, then the larger set is considered to succeed the smaller
         * set. </p>
         *
         * @param  that
         *         The module export to compare
         *
         * @return A negative integer, zero, or a positive integer if this module
         *         export is less than, equal to, or greater than the given
         *         export dependence
         */
        @Override
        public int compareTo(Exports that) {
            if (this == that) return 0;

            int c = source.compareTo(that.source);
            if (c != 0)
                return c;

            // modifiers
            long v1 = modsValue(this.modifiers());
            long v2 = modsValue(that.modifiers());
            c = Long.compare(v1, v2);
            if (c != 0)
                return c;

            // targets
            c = compare(targets, that.targets);
            if (c != 0)
                return c;

            return 0;
        }

        /**
         * Computes a hash code for this module export.
         *
         * <p> The hash code is based upon the modifiers, the package name,
         * and for a qualified export, the set of modules names to which the
         * package is exported. It satisfies the general contract of the
         * {@link Object#hashCode Object.hashCode} method.
         *
         * @return The hash-code value for this module export
         */
        @Override
        public int hashCode() {
            int hash = mods.hashCode();
            hash = hash * 43 + source.hashCode();
            return hash * 43 + targets.hashCode();
        }

        /**
         * Tests this module export for equality with the given object.
         *
         * <p> If the given object is not an {@code Exports} then this method
         * returns {@code false}. Two module exports objects are equal if their
         * set of modifiers is equal, the package names are equal and the set
         * of target module names is equal. </p>
         *
         * <p> This method satisfies the general contract of the {@link
         * java.lang.Object#equals(Object) Object.equals} method. </p>
         *
         * @param   ob
         *          the object to which this object is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a module
         *          dependence that is equal to this module dependence
         */
        @Override
        public boolean equals(Object ob) {
            return (ob instanceof Exports other)
                    && Objects.equals(this.mods, other.mods)
                    && Objects.equals(this.source, other.source)
                    && Objects.equals(this.targets, other.targets);
        }

        /**
         * Returns a string describing the exported package.
         *
         * @return A string describing the exported package
         */
        @Override
        public String toString() {
            String s = ModuleDescriptor.toString(mods, source);
            if (targets.isEmpty())
                return s;
            else
                return s + " to " + targets;
        }
    }


    /**
     * <p> A package opened by a module, may be qualified or unqualified. </p>
     *
     * <p> The <em>opens</em> directive in a module declaration declares a
     * package to be open to allow all types in the package, and all their
     * members, not just public types and their public members to be reflected
     * on by APIs that support private access or a way to bypass or suppress
     * default Java language access control checks. </p>
     *
     * @see ModuleDescriptor#opens()
     * @since 9
     */

    public static final class Opens
        implements Comparable<Opens>
    {
        /**
         * A modifier on an open package.
         *
         * @see Opens#modifiers()
         * @since 9
         */
        public enum Modifier {

            /**
             * The open package was not explicitly or implicitly declared in
             * the source of the module declaration.
             */
            SYNTHETIC,

            /**
             * The open package was implicitly declared in the source of the
             * module declaration.
             */
            MANDATED;

        }

        private final Set<Modifier> mods;
        private final String source;
        private final Set<String> targets;  // empty if unqualified export

        /**
         * Constructs an {@code Opens}.
         */
        private Opens(Set<Modifier> ms, String source, Set<String> targets) {
            this.mods = Set.copyOf(ms);
            this.source = source;
            this.targets = Set.copyOf(targets);
        }

        private Opens(Set<Modifier> ms,
                      String source,
                      Set<String> targets,
                      boolean unused) {
            this.mods = ms;
            this.source = source;
            this.targets = targets;
        }

        /**
         * Returns the set of modifiers.
         *
         * @return A possibly-empty unmodifiable set of modifiers
         */
        public Set<Modifier> modifiers() {
            return mods;
        }

        /**
         * Returns {@code true} if this is a qualified {@code Opens}.
         *
         * @return {@code true} if this is a qualified {@code Opens}
         */
        public boolean isQualified() {
            return !targets.isEmpty();
        }

        /**
         * Returns the package name.
         *
         * @return The package name
         */
        public String source() {
            return source;
        }

        /**
         * For a qualified {@code Opens}, returns the non-empty and immutable set
         * of the module names to which the package is open. For an
         * unqualified {@code Opens}, returns an empty set.
         *
         * @return The set of target module names or for an unqualified
         *         {@code Opens}, an empty set
         */
        public Set<String> targets() {
            return targets;
        }

        /**
         * Compares this module {@code Opens} to another.
         *
         * <p> Two {@code Opens} objects are compared by comparing the package
         * names lexicographically. Where the packages names are equal then the
         * sets of modifiers are compared in the same way that module modifiers
         * are compared (see {@link ModuleDescriptor#compareTo
         * ModuleDescriptor.compareTo}). Where the package names are equal and
         * the set of modifiers are equal then the set of target modules are
         * compared. This is done by sorting the names of the target modules
         * in ascending order, and according to their natural ordering, and then
         * comparing the corresponding elements lexicographically. Where the
         * sets differ in size, and the larger set contains all elements of the
         * smaller set, then the larger set is considered to succeed the smaller
         * set. </p>
         *
         * @param  that
         *         The module {@code Opens} to compare
         *
         * @return A negative integer, zero, or a positive integer if this module
         *         {@code Opens} is less than, equal to, or greater than the given
         *         module {@code Opens}
         */
        @Override
        public int compareTo(Opens that) {
            if (this == that) return 0;

            int c = source.compareTo(that.source);
            if (c != 0)
                return c;

            // modifiers
            long v1 = modsValue(this.modifiers());
            long v2 = modsValue(that.modifiers());
            c = Long.compare(v1, v2);
            if (c != 0)
                return c;

            // targets
            c = compare(targets, that.targets);
            if (c != 0)
                return c;

            return 0;
        }

        /**
         * Computes a hash code for this module {@code Opens}.
         *
         * <p> The hash code is based upon the modifiers, the package name,
         * and for a qualified {@code Opens}, the set of modules names to which the
         * package is opened. It satisfies the general contract of the
         * {@link Object#hashCode Object.hashCode} method.
         *
         * @return The hash-code value for this module {@code Opens}
         */
        @Override
        public int hashCode() {
            int hash = mods.hashCode();
            hash = hash * 43 + source.hashCode();
            return hash * 43 + targets.hashCode();
        }

        /**
         * Tests this module {@code Opens} for equality with the given object.
         *
         * <p> If the given object is not an {@code Opens} then this method
         * returns {@code false}. Two {@code Opens} objects are equal if their
         * set of modifiers is equal, the package names are equal and the set
         * of target module names is equal. </p>
         *
         * <p> This method satisfies the general contract of the {@link
         * java.lang.Object#equals(Object) Object.equals} method. </p>
         *
         * @param   ob
         *          the object to which this object is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a module
         *          dependence that is equal to this module dependence
         */
        @Override
        public boolean equals(Object ob) {
           return (ob instanceof Opens other)
                   && Objects.equals(this.mods, other.mods)
                   && Objects.equals(this.source, other.source)
                   && Objects.equals(this.targets, other.targets);
        }

        /**
         * Returns a string describing the open package.
         *
         * @return A string describing the open package
         */
        @Override
        public String toString() {
            String s = ModuleDescriptor.toString(mods, source);
            if (targets.isEmpty())
                return s;
            else
                return s + " to " + targets;
        }
    }


    /**
     * <p> A service that a module provides one or more implementations of. </p>
     *
     * @see ModuleDescriptor#provides()
     * @since 9
     */

    public static final class Provides
        implements Comparable<Provides>
    {
        private final String service;
        private final List<String> providers;

        private Provides(String service, List<String> providers) {
            this.service = service;
            this.providers = List.copyOf(providers);
        }

        private Provides(String service, List<String> providers, boolean unused) {
            this.service = service;
            this.providers = providers;
        }

        /**
         * Returns the fully qualified class name of the service type.
         *
         * @return The fully qualified class name of the service type
         */
        public String service() { return service; }

        /**
         * Returns the list of the fully qualified class names of the providers
         * or provider factories.
         *
         * @return A non-empty and unmodifiable list of the fully qualified class
         *         names of the providers or provider factories
         */
        public List<String> providers() { return providers; }

        /**
         * Compares this {@code Provides} to another.
         *
         * <p> Two {@code Provides} objects are compared by comparing the fully
         * qualified class name of the service type lexicographically. Where the
         * class names are equal then the list of the provider class names are
         * compared by comparing the corresponding elements of both lists
         * lexicographically and in sequence. Where the lists differ in size,
         * {@code N} is the size of the shorter list, and the first {@code N}
         * corresponding elements are equal, then the longer list is considered
         * to succeed the shorter list. </p>
         *
         * @param  that
         *         The {@code Provides} to compare
         *
         * @return A negative integer, zero, or a positive integer if this
         *         {@code Provides} is less than, equal to, or greater than
         *         the given {@code Provides}
         */
        public int compareTo(Provides that) {
            if (this == that) return 0;

            int c = service.compareTo(that.service);
            if (c != 0) return c;

            // compare provider class names in sequence
            int size1 = this.providers.size();
            int size2 = that.providers.size();
            for (int index=0; index<Math.min(size1, size2); index++) {
                String e1 = this.providers.get(index);
                String e2 = that.providers.get(index);
                c = e1.compareTo(e2);
                if (c != 0) return c;
            }
            if (size1 == size2) {
                return 0;
            } else {
                return (size1 > size2) ? 1 : -1;
            }
        }

        /**
         * Computes a hash code for this {@code Provides}.
         *
         * <p> The hash code is based upon the service type and the set of
         * providers. It satisfies the general contract of the {@link
         * Object#hashCode Object.hashCode} method. </p>
         *
         * @return The hash-code value for this module provides
         */
        @Override
        public int hashCode() {
            return service.hashCode() * 43 + providers.hashCode();
        }

        /**
         * Tests this {@code Provides} for equality with the given object.
         *
         * <p> If the given object is not a {@code Provides} then this method
         * returns {@code false}. Two {@code Provides} objects are equal if the
         * service type is equal and the list of providers is equal. </p>
         *
         * <p> This method satisfies the general contract of the {@link
         * java.lang.Object#equals(Object) Object.equals} method. </p>
         *
         * @param   ob
         *          the object to which this object is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a
         *          {@code Provides} that is equal to this {@code Provides}
         */
        @Override
        public boolean equals(Object ob) {
            return (ob instanceof Provides other)
                    && Objects.equals(this.service, other.service)
                    && Objects.equals(this.providers, other.providers);
        }

        /**
         * Returns a string describing this {@code Provides}.
         *
         * @return A string describing this {@code Provides}
         */
        @Override
        public String toString() {
            return service + " with " + providers;
        }

    }


    /**
     * A module's version string.
     *
     * <p> A version string has three components: The version number itself, an
     * optional pre-release version, and an optional build version.  Each
     * component is a sequence of tokens; each token is either a non-negative
     * integer or a string.  Tokens are separated by the punctuation characters
     * {@code '.'}, {@code '-'}, or {@code '+'}, or by transitions from a
     * sequence of digits to a sequence of characters that are neither digits
     * nor punctuation characters, or vice versa.
     *
     * <ul>
     *
     *   <li> The <i>version number</i> is a sequence of tokens separated by
     *   {@code '.'} characters, terminated by the first {@code '-'} or {@code
     *   '+'} character. </li>
     *
     *   <li> The <i>pre-release version</i> is a sequence of tokens separated
     *   by {@code '.'} or {@code '-'} characters, terminated by the first
     *   {@code '+'} character. </li>
     *
     *   <li> The <i>build version</i> is a sequence of tokens separated by
     *   {@code '.'}, {@code '-'}, or {@code '+'} characters.
     *
     * </ul>
     *
     * <p> When comparing two version strings, the elements of their
     * corresponding components are compared in pointwise fashion.  If one
     * component is longer than the other, but otherwise equal to it, then the
     * first component is considered the greater of the two; otherwise, if two
     * corresponding elements are integers then they are compared as such;
     * otherwise, at least one of the elements is a string, so the other is
     * converted into a string if it is an integer and the two are compared
     * lexicographically.  Trailing integer elements with the value zero are
     * ignored.
     *
     * <p> Given two version strings, if their version numbers differ then the
     * result of comparing them is the result of comparing their version
     * numbers; otherwise, if one of them has a pre-release version but the
     * other does not then the first is considered to precede the second,
     * otherwise the result of comparing them is the result of comparing their
     * pre-release versions; otherwise, the result of comparing them is the
     * result of comparing their build versions.
     *
     * @see ModuleDescriptor#version()
     * @since 9
     */

    public static final class Version
        implements Comparable<Version>
    {

        private final String version;

        // If Java had disjunctive types then we'd write List<Integer|String> here
        //
        private final List<Object> sequence;
        private final List<Object> pre;
        private final List<Object> build;

        // Take a numeric token starting at position i
        // Append it to the given list
        // Return the index of the first character not taken
        // Requires: s.charAt(i) is (decimal) numeric
        //
        private static int takeNumber(String s, int i, List<Object> acc) {
            char c = s.charAt(i);
            int d = (c - '0');
            int n = s.length();
            while (++i < n) {
                c = s.charAt(i);
                if (c >= '0' && c <= '9') {
                    d = d * 10 + (c - '0');
                    continue;
                }
                break;
            }
            acc.add(d);
            return i;
        }

        // Take a string token starting at position i
        // Append it to the given list
        // Return the index of the first character not taken
        // Requires: s.charAt(i) is not '.'
        //
        private static int takeString(String s, int i, List<Object> acc) {
            int b = i;
            int n = s.length();
            while (++i < n) {
                char c = s.charAt(i);
                if (c != '.' && c != '-' && c != '+' && !(c >= '0' && c <= '9'))
                    continue;
                break;
            }
            acc.add(s.substring(b, i));
            return i;
        }

        // Syntax: tok+ ( '-' tok+)? ( '+' tok+)?
        // First token string is sequence, second is pre, third is build
        // Tokens are separated by '.' or '-', or by changes between alpha & numeric
        // Numeric tokens are compared as decimal integers
        // Non-numeric tokens are compared lexicographically
        // A version with a non-empty pre is less than a version with same seq but no pre
        // Tokens in build may contain '-' and '+'
        //
        private Version(String v) {

            if (v == null)
                throw new IllegalArgumentException("Null version string");
            int n = v.length();
            if (n == 0)
                throw new IllegalArgumentException("Empty version string");

            int i = 0;
            char c = v.charAt(i);
            if (!(c >= '0' && c <= '9'))
                throw new IllegalArgumentException(v
                                                   + ": Version string does not start"
                                                   + " with a number");

            List<Object> sequence = new ArrayList<>(4);
            List<Object> pre = new ArrayList<>(2);
            List<Object> build = new ArrayList<>(2);

            i = takeNumber(v, i, sequence);

            while (i < n) {
                c = v.charAt(i);
                if (c == '.') {
                    i++;
                    continue;
                }
                if (c == '-' || c == '+') {
                    i++;
                    break;
                }
                if (c >= '0' && c <= '9')
                    i = takeNumber(v, i, sequence);
                else
                    i = takeString(v, i, sequence);
            }

            if (c == '-' && i >= n)
                throw new IllegalArgumentException(v + ": Empty pre-release");

            while (i < n) {
                c = v.charAt(i);
                if (c >= '0' && c <= '9')
                    i = takeNumber(v, i, pre);
                else
                    i = takeString(v, i, pre);
                if (i >= n)
                    break;
                c = v.charAt(i);
                if (c == '.' || c == '-') {
                    i++;
                    continue;
                }
                if (c == '+') {
                    i++;
                    break;
                }
            }

            if (c == '+' && i >= n)
                throw new IllegalArgumentException(v + ": Empty pre-release");

            while (i < n) {
                c = v.charAt(i);
                if (c >= '0' && c <= '9')
                    i = takeNumber(v, i, build);
                else
                    i = takeString(v, i, build);
                if (i >= n)
                    break;
                c = v.charAt(i);
                if (c == '.' || c == '-' || c == '+') {
                    i++;
                    continue;
                }
            }

            this.version = v;
            this.sequence = sequence;
            this.pre = pre;
            this.build = build;
        }

        /**
         * Parses the given string as a version string.
         *
         * @param  v
         *         The string to parse
         *
         * @return The resulting {@code Version}
         *
         * @throws IllegalArgumentException
         *         If {@code v} is {@code null}, an empty string, or cannot be
         *         parsed as a version string
         */
        public static Version parse(String v) {
            return new Version(v);
        }

        @SuppressWarnings("unchecked")
        private int cmp(Object o1, Object o2) {
            return ((Comparable)o1).compareTo(o2);
        }

        private int compareTokens(List<Object> ts1, List<Object> ts2) {
            int n = Math.min(ts1.size(), ts2.size());
            for (int i = 0; i < n; i++) {
                Object o1 = ts1.get(i);
                Object o2 = ts2.get(i);
                if ((o1 instanceof Integer && o2 instanceof Integer)
                    || (o1 instanceof String && o2 instanceof String))
                {
                    int c = cmp(o1, o2);
                    if (c == 0)
                        continue;
                    return c;
                }
                // Types differ, so convert number to string form
                int c = o1.toString().compareTo(o2.toString());
                if (c == 0)
                    continue;
                return c;
            }
            List<Object> rest = ts1.size() > ts2.size() ? ts1 : ts2;
            int e = rest.size();
            for (int i = n; i < e; i++) {
                Object o = rest.get(i);
                if (o instanceof Integer && ((Integer)o) == 0)
                    continue;
                return ts1.size() - ts2.size();
            }
            return 0;
        }

        /**
         * Compares this module version to another module version. Module
         * versions are compared as described in the class description.
         *
         * @param that
         *        The module version to compare
         *
         * @return A negative integer, zero, or a positive integer as this
         *         module version is less than, equal to, or greater than the
         *         given module version
         */
        @Override
        public int compareTo(Version that) {
            int c = compareTokens(this.sequence, that.sequence);
            if (c != 0) return c;
            if (this.pre.isEmpty()) {
                if (!that.pre.isEmpty()) return +1;
            } else {
                if (that.pre.isEmpty()) return -1;
            }
            c = compareTokens(this.pre, that.pre);
            if (c != 0) return c;
            return compareTokens(this.build, that.build);
        }

        /**
         * Tests this module version for equality with the given object.
         *
         * <p> If the given object is not a {@code Version} then this method
         * returns {@code false}. Two module version are equal if their
         * corresponding components are equal. </p>
         *
         * <p> This method satisfies the general contract of the {@link
         * java.lang.Object#equals(Object) Object.equals} method. </p>
         *
         * @param   ob
         *          the object to which this object is to be compared
         *
         * @return  {@code true} if, and only if, the given object is a module
         *          reference that is equal to this module reference
         */
        @Override
        public boolean equals(Object ob) {
            if (!(ob instanceof Version))
                return false;
            return compareTo((Version)ob) == 0;
        }

        /**
         * Computes a hash code for this module version.
         *
         * <p> The hash code is based upon the components of the version and
         * satisfies the general contract of the {@link Object#hashCode
         * Object.hashCode} method. </p>
         *
         * @return The hash-code value for this module version
         */
        @Override
        public int hashCode() {
            return version.hashCode();
        }

        /**
         * Returns the string from which this version was parsed.
         *
         * @return The string from which this version was parsed.
         */
        @Override
        public String toString() {
            return version;
        }

    }


    private final String name;
    private final Version version;
    private final String rawVersionString;
    private final Set<Modifier> modifiers;
    private final boolean open;  // true if modifiers contains OPEN
    private final boolean automatic;  // true if modifiers contains AUTOMATIC
    private final Set<Requires> requires;
    private final Set<Exports> exports;
    private final Set<Opens> opens;
    private final Set<String> uses;
    private final Set<Provides> provides;
    private final Set<String> packages;
    private final String mainClass;

    private ModuleDescriptor(String name,
                             Version version,
                             String rawVersionString,
                             Set<Modifier> modifiers,
                             Set<Requires> requires,
                             Set<Exports> exports,
                             Set<Opens> opens,
                             Set<String> uses,
                             Set<Provides> provides,
                             Set<String> packages,
                             String mainClass)
    {
        assert version == null || rawVersionString == null;
        this.name = name;
        this.version = version;
        this.rawVersionString = rawVersionString;
        this.modifiers = Set.copyOf(modifiers);
        this.open = modifiers.contains(Modifier.OPEN);
        this.automatic = modifiers.contains(Modifier.AUTOMATIC);
        assert (requires.stream().map(Requires::name).distinct().count()
                == requires.size());
        this.requires = Set.copyOf(requires);
        this.exports = Set.copyOf(exports);
        this.opens = Set.copyOf(opens);
        this.uses = Set.copyOf(uses);
        this.provides = Set.copyOf(provides);

        this.packages = Set.copyOf(packages);
        this.mainClass = mainClass;
    }

    /**
     * Creates a module descriptor from its components.
     * The arguments are pre-validated and sets are unmodifiable sets.
     */
    ModuleDescriptor(String name,
                     Version version,
                     Set<Modifier> modifiers,
                     Set<Requires> requires,
                     Set<Exports> exports,
                     Set<Opens> opens,
                     Set<String> uses,
                     Set<Provides> provides,
                     Set<String> packages,
                     String mainClass,
                     int hashCode,
                     boolean unused) {
        this.name = name;
        this.version = version;
        this.rawVersionString = null;
        this.modifiers = modifiers;
        this.open = modifiers.contains(Modifier.OPEN);
        this.automatic = modifiers.contains(Modifier.AUTOMATIC);
        this.requires = requires;
        this.exports = exports;
        this.opens = opens;
        this.uses = uses;
        this.provides = provides;
        this.packages = packages;
        this.mainClass = mainClass;
        this.hash = hashCode;
    }

    /**
     * <p> Returns the module name. </p>
     *
     * @return The module name
     */
    public String name() {
        return name;
    }

    /**
     * <p> Returns the set of module modifiers. </p>
     *
     * @return A possibly-empty unmodifiable set of modifiers
     */
    public Set<Modifier> modifiers() {
        return modifiers;
    }

    /**
     * <p> Returns {@code true} if this is an open module. </p>
     *
     * <p> This method is equivalent to testing if the set of {@link #modifiers()
     * modifiers} contains the {@link Modifier#OPEN OPEN} modifier. </p>
     *
     * @return  {@code true} if this is an open module
     */
    public boolean isOpen() {
        return open;
    }

    /**
     * <p> Returns {@code true} if this is an automatic module. </p>
     *
     * <p> This method is equivalent to testing if the set of {@link #modifiers()
     * modifiers} contains the {@link Modifier#AUTOMATIC AUTOMATIC} modifier. </p>
     *
     * @return  {@code true} if this is an automatic module
     */
    public boolean isAutomatic() {
        return automatic;
    }

    /**
     * <p> Returns the set of {@code Requires} objects representing the module
     * dependences. </p>
     *
     * <p> The set includes a dependency on "{@code java.base}" when this
     * module is not named "{@code java.base}". If this module is an automatic
     * module then it does not have a dependency on any module other than
     * "{@code java.base}". </p>
     *
     * @return  A possibly-empty unmodifiable set of {@link Requires} objects
     */
    public Set<Requires> requires() {
        return requires;
    }

    /**
     * <p> Returns the set of {@code Exports} objects representing the exported
     * packages. </p>
     *
     * <p> If this module is an automatic module then the set of exports
     * is empty. </p>
     *
     * @return  A possibly-empty unmodifiable set of exported packages
     */
    public Set<Exports> exports() {
        return exports;
    }

    /**
     * <p> Returns the set of {@code Opens} objects representing the open
     * packages. </p>
     *
     * <p> If this module is an open module or an automatic module then the
     * set of open packages is empty. </p>
     *
     * @return  A possibly-empty unmodifiable set of open packages
     */
    public Set<Opens> opens() {
        return opens;
    }

    /**
     * <p> Returns the set of service dependences. </p>
     *
     * <p> If this module is an automatic module then the set of service
     * dependences is empty. </p>
     *
     * @return  A possibly-empty unmodifiable set of the fully qualified class
     *          names of the service types used
     */
    public Set<String> uses() {
        return uses;
    }

    /**
     * <p> Returns the set of {@code Provides} objects representing the
     * services that the module provides. </p>
     *
     * @return The possibly-empty unmodifiable set of the services that this
     *         module provides
     */
    public Set<Provides> provides() {
        return provides;
    }

    /**
     * <p> Returns the module version. </p>
     *
     * @return This module's version, or an empty {@code Optional} if the
     *         module does not have a version or the version is
     *         {@linkplain Version#parse(String) unparseable}
     */
    public Optional<Version> version() {
        return Optional.ofNullable(version);
    }

    /**
     * <p> Returns the string with the possibly-unparseable version of the
     * module. </p>
     *
     * @return The string containing the version of the module or an empty
     *         {@code Optional} if the module does not have a version
     *
     * @see #version()
     */
    public Optional<String> rawVersion() {
        if (version != null) {
            return Optional.of(version.toString());
        } else {
            return Optional.ofNullable(rawVersionString);
        }
    }

    /**
     * <p> Returns a string containing the module name and, if present, its
     * version. </p>
     *
     * @return A string containing the module name and, if present, its
     *         version
     */
    public String toNameAndVersion() {
        if (version != null) {
            return name() + "@" + version;
        } else {
            return name();
        }
    }

    /**
     * <p> Returns the module main class. </p>
     *
     * @return The fully qualified class name of the module's main class
     */
    public Optional<String> mainClass() {
        return Optional.ofNullable(mainClass);
    }

    /**
     * Returns the set of packages in the module.
     *
     * <p> The set of packages includes all exported and open packages, as well
     * as the packages of any service providers, and the package for the main
     * class. </p>
     *
     * @return A possibly-empty unmodifiable set of the packages in the module
     */
    public Set<String> packages() {
        return packages;
    }


    /**
     * A builder for building {@link ModuleDescriptor} objects.
     *
     * <p> {@code ModuleDescriptor} defines the {@link #newModule newModule},
     * {@link #newOpenModule newOpenModule}, and {@link #newAutomaticModule
     * newAutomaticModule} methods to create builders for building
     * <em>normal</em>, open, and automatic modules. </p>
     *
     * <p> The set of packages in the module are accumulated by the {@code
     * Builder} as the {@link ModuleDescriptor.Builder#exports(String) exports},
     * {@link ModuleDescriptor.Builder#opens(String) opens},
     * {@link ModuleDescriptor.Builder#packages(Set) packages},
     * {@link ModuleDescriptor.Builder#provides(String,List) provides}, and
     * {@link ModuleDescriptor.Builder#mainClass(String) mainClass} methods are
     * invoked. </p>
     *
     * <p> The module names, package names, and class names that are parameters
     * specified to the builder methods are the module names, package names,
     * and qualified names of classes (in named packages) as defined in the
     * <cite>The Java Language Specification</cite>. </p>
     *
     * <p> Example usage: </p>
     * <pre>{@code    ModuleDescriptor descriptor = ModuleDescriptor.newModule("stats.core")
     *         .requires("java.base")
     *         .exports("org.acme.stats.core.clustering")
     *         .exports("org.acme.stats.core.regression")
     *         .packages(Set.of("org.acme.stats.core.internal"))
     *         .build();
     * }</pre>
     *
     * @apiNote A {@code Builder} checks the components and invariants as
     * components are added to the builder. The rationale for this is to detect
     * errors as early as possible and not defer all validation to the
     * {@link #build build} method.
     *
     * @since 9
     */
    public static final class Builder {
        final String name;
        final boolean strict;
        final Set<Modifier> modifiers;
        final boolean open;
        final boolean automatic;
        final Set<String> packages = new HashSet<>();
        final Map<String, Requires> requires = new HashMap<>();
        final Map<String, Exports> exports = new HashMap<>();
        final Map<String, Opens> opens = new HashMap<>();
        final Set<String> uses = new HashSet<>();
        final Map<String, Provides> provides = new HashMap<>();
        Version version;
        String rawVersionString;
        String mainClass;

        /**
         * Initializes a new builder with the given module name.
         *
         * If {@code strict} is {@code true} then module, package, and class
         * names are checked to ensure they are legal names. In addition, the
         * {@link #build buid} method will add "{@code requires java.base}" if
         * the dependency is not declared.
         */
        Builder(String name, boolean strict, Set<Modifier> modifiers) {
            this.name = (strict) ? requireModuleName(name) : name;
            this.strict = strict;
            this.modifiers = modifiers;
            this.open = modifiers.contains(Modifier.OPEN);
            this.automatic = modifiers.contains(Modifier.AUTOMATIC);
            assert !open || !automatic;
        }

        /**
         * Returns a snapshot of the packages in the module.
         */
        /* package */ Set<String> packages() {
            return Collections.unmodifiableSet(packages);
        }

        /**
         * Adds a dependence on a module.
         *
         * @param  req
         *         The dependence
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the dependence is on the module that this builder was
         *         initialized to build
         * @throws IllegalStateException
         *         If the dependence on the module has already been declared
         *         or this builder is for an automatic module
         */
        public Builder requires(Requires req) {
            if (automatic)
                throw new IllegalStateException("Automatic modules cannot declare"
                                                + " dependences");
            String mn = req.name();
            if (name.equals(mn))
                throw new IllegalArgumentException("Dependence on self");
            if (requires.containsKey(mn))
                throw new IllegalStateException("Dependence upon " + mn
                                                + " already declared");
            requires.put(mn, req);
            return this;
        }

        /**
         * Adds a dependence on a module with the given (and possibly empty)
         * set of modifiers. The dependence includes the version of the
         * module that was recorded at compile-time.
         *
         * @param  ms
         *         The set of modifiers
         * @param  mn
         *         The module name
         * @param  compiledVersion
         *         The version of the module recorded at compile-time
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the module name is {@code null}, is not a legal module
         *         name, or is equal to the module name that this builder
         *         was initialized to build
         * @throws IllegalStateException
         *         If the dependence on the module has already been declared
         *         or this builder is for an automatic module
         */
        public Builder requires(Set<Requires.Modifier> ms,
                                String mn,
                                Version compiledVersion) {
            Objects.requireNonNull(compiledVersion);
            if (strict)
                mn = requireModuleName(mn);
            return requires(new Requires(ms, mn, compiledVersion, null));
        }

        /* package */Builder requires(Set<Requires.Modifier> ms,
                                      String mn,
                                      String rawCompiledVersion) {
            Requires r;
            try {
                Version v = Version.parse(rawCompiledVersion);
                r = new Requires(ms, mn, v, null);
            } catch (IllegalArgumentException e) {
                if (strict) throw e;
                r = new Requires(ms, mn, null, rawCompiledVersion);
            }
            return requires(r);
        }

        /**
         * Adds a dependence on a module with the given (and possibly empty)
         * set of modifiers.
         *
         * @param  ms
         *         The set of modifiers
         * @param  mn
         *         The module name
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the module name is {@code null}, is not a legal module
         *         name, or is equal to the module name that this builder
         *         was initialized to build
         * @throws IllegalStateException
         *         If the dependence on the module has already been declared
         *         or this builder is for an automatic module
         */
        public Builder requires(Set<Requires.Modifier> ms, String mn) {
            if (strict)
                mn = requireModuleName(mn);
            return requires(new Requires(ms, mn, null, null));
        }

        /**
         * Adds a dependence on a module with an empty set of modifiers.
         *
         * @param  mn
         *         The module name
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the module name is {@code null}, is not a legal module
         *         name, or is equal to the module name that this builder
         *         was initialized to build
         * @throws IllegalStateException
         *         If the dependence on the module has already been declared
         *         or this builder is for an automatic module
         */
        public Builder requires(String mn) {
            return requires(EnumSet.noneOf(Requires.Modifier.class), mn);
        }

        /**
         * Adds an exported package.
         *
         * @param  e
         *         The export
         *
         * @return This builder
         *
         * @throws IllegalStateException
         *         If the {@link Exports#source() package} is already declared as
         *         exported or this builder is for an automatic module
         */
        public Builder exports(Exports e) {
            if (automatic) {
                throw new IllegalStateException("Automatic modules cannot declare"
                                                 + " exported packages");
            }
            String source = e.source();
            if (exports.containsKey(source)) {
                throw new IllegalStateException("Exported package " + source
                                                 + " already declared");
            }
            exports.put(source, e);
            packages.add(source);
            return this;
        }

        /**
         * Adds an exported package with the given (and possibly empty) set of
         * modifiers. The package is exported to a set of target modules.
         *
         * @param  ms
         *         The set of modifiers
         * @param  pn
         *         The package name
         * @param  targets
         *         The set of target modules names
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the package name is {@code null} or is not a legal
         *         package name, the set of target modules is empty, or the set
         *         of target modules contains a name that is not a legal module
         *         name
         * @throws IllegalStateException
         *         If the package is already declared as exported
         *         or this builder is for an automatic module
         */
        public Builder exports(Set<Exports.Modifier> ms,
                               String pn,
                               Set<String> targets)
        {
            targets = new HashSet<>(targets);
            if (targets.isEmpty())
                throw new IllegalArgumentException("Empty target set");
            if (strict) {
                requirePackageName(pn);
                targets.forEach(Checks::requireModuleName);
            }
            Exports e = new Exports(ms, pn, targets);
            return exports(e);
        }

        /**
         * Adds an exported package with the given (and possibly empty) set of
         * modifiers. The package is exported to all modules.
         *
         * @param  ms
         *         The set of modifiers
         * @param  pn
         *         The package name
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the package name is {@code null} or is not a legal
         *         package name
         * @throws IllegalStateException
         *         If the package is already declared as exported
         *         or this builder is for an automatic module
         */
        public Builder exports(Set<Exports.Modifier> ms, String pn) {
            if (strict) {
                requirePackageName(pn);
            }
            Exports e = new Exports(ms, pn, Set.of());
            return exports(e);
        }

        /**
         * Adds an exported package. The package is exported to a set of target
         * modules.
         *
         * @param  pn
         *         The package name
         * @param  targets
         *         The set of target modules names
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the package name is {@code null} or is not a legal
         *         package name, the set of target modules is empty, or the set
         *         of target modules contains a name that is not a legal module
         *         name
         * @throws IllegalStateException
         *         If the package is already declared as exported
         *         or this builder is for an automatic module
         */
        public Builder exports(String pn, Set<String> targets) {
            return exports(Set.of(), pn, targets);
        }

        /**
         * Adds an exported package. The package is exported to all modules.
         *
         * @param  pn
         *         The package name
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the package name is {@code null} or is not a legal
         *         package name
         * @throws IllegalStateException
         *         If the package is already declared as exported
         *         or this builder is for an automatic module
         */
        public Builder exports(String pn) {
            return exports(Set.of(), pn);
        }

        /**
         * Adds an open package.
         *
         * @param  obj
         *         The {@code Opens} object
         *
         * @return This builder
         *
         * @throws IllegalStateException
         *         If the package is already declared as open, or this is a
         *         builder for an open module or automatic module
         */
        public Builder opens(Opens obj) {
            if (open || automatic) {
                throw new IllegalStateException("Open or automatic modules cannot"
                                                + " declare open packages");
            }
            String source = obj.source();
            if (opens.containsKey(source)) {
                throw new IllegalStateException("Open package " + source
                                                + " already declared");
            }
            opens.put(source, obj);
            packages.add(source);
            return this;
        }


        /**
         * Adds an open package with the given (and possibly empty) set of
         * modifiers. The package is open to a set of target modules.
         *
         * @param  ms
         *         The set of modifiers
         * @param  pn
         *         The package name
         * @param  targets
         *         The set of target modules names
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the package name is {@code null} or is not a legal
         *         package name, the set of target modules is empty, or the set
         *         of target modules contains a name that is not a legal module
         *         name
         * @throws IllegalStateException
         *         If the package is already declared as open, or this is a
         *         builder for an open module or automatic module
         */
        public Builder opens(Set<Opens.Modifier> ms,
                             String pn,
                             Set<String> targets)
        {
            targets = new HashSet<>(targets);
            if (targets.isEmpty())
                throw new IllegalArgumentException("Empty target set");
            if (strict) {
                requirePackageName(pn);
                targets.forEach(Checks::requireModuleName);
            }
            Opens opens = new Opens(ms, pn, targets);
            return opens(opens);
        }

        /**
         * Adds an open package with the given (and possibly empty) set of
         * modifiers. The package is open to all modules.
         *
         * @param  ms
         *         The set of modifiers
         * @param  pn
         *         The package name
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the package name is {@code null} or is not a legal
         *         package name
         * @throws IllegalStateException
         *         If the package is already declared as open, or this is a
         *         builder for an open module or automatic module
         */
        public Builder opens(Set<Opens.Modifier> ms, String pn) {
            if (strict) {
                requirePackageName(pn);
            }
            Opens e = new Opens(ms, pn, Set.of());
            return opens(e);
        }

        /**
         * Adds an open package. The package is open to a set of target modules.
         *
         * @param  pn
         *         The package name
         * @param  targets
         *         The set of target modules names
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the package name is {@code null} or is not a legal
         *         package name, the set of target modules is empty, or the set
         *         of target modules contains a name that is not a legal module
         *         name
         * @throws IllegalStateException
         *         If the package is already declared as open, or this is a
         *         builder for an open module or automatic module
         */
        public Builder opens(String pn, Set<String> targets) {
            return opens(Set.of(), pn, targets);
        }

        /**
         * Adds an open package. The package is open to all modules.
         *
         * @param  pn
         *         The package name
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the package name is {@code null} or is not a legal
         *         package name
         * @throws IllegalStateException
         *         If the package is already declared as open, or this is a
         *         builder for an open module or automatic module
         */
        public Builder opens(String pn) {
            return opens(Set.of(), pn);
        }

        /**
         * Adds a service dependence.
         *
         * @param  service
         *         The service type
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the service type is {@code null} or not a qualified name of
         *         a class in a named package
         * @throws IllegalStateException
         *         If a dependency on the service type has already been declared
         *         or this is a builder for an automatic module
         */
        public Builder uses(String service) {
            if (automatic)
                throw new IllegalStateException("Automatic modules can not declare"
                                                + " service dependences");
            if (uses.contains(requireServiceTypeName(service)))
                throw new IllegalStateException("Dependence upon service "
                                                + service + " already declared");
            uses.add(service);
            return this;
        }

        /**
         * Provides a service with one or more implementations. The package for
         * each {@link Provides#providers provider} (or provider factory) is
         * added to the module if not already added.
         *
         * @param  p
         *         The provides
         *
         * @return This builder
         *
         * @throws IllegalStateException
         *         If the providers for the service type have already been
         *         declared
         */
        public Builder provides(Provides p) {
            String service = p.service();
            if (provides.containsKey(service))
                throw new IllegalStateException("Providers of service "
                                                + service + " already declared");
            provides.put(service, p);
            p.providers().forEach(name -> packages.add(packageName(name)));
            return this;
        }

        /**
         * Provides implementations of a service. The package for each provider
         * (or provider factory) is added to the module if not already added.
         *
         * @param  service
         *         The service type
         * @param  providers
         *         The list of provider or provider factory class names
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If the service type or any of the provider class names is
         *         {@code null} or not a qualified name of a class in a named
         *         package, or the list of provider class names is empty
         * @throws IllegalStateException
         *         If the providers for the service type have already been
         *         declared
         */
        public Builder provides(String service, List<String> providers) {
            providers = new ArrayList<>(providers);
            if (providers.isEmpty())
                throw new IllegalArgumentException("Empty providers set");
            if (strict) {
                requireServiceTypeName(service);
                providers.forEach(Checks::requireServiceProviderName);
            } else {
                // Disallow service/providers in unnamed package
                String pn = packageName(service);
                if (pn.isEmpty()) {
                    throw new IllegalArgumentException(service
                                                       + ": unnamed package");
                }
                for (String name : providers) {
                    pn = packageName(name);
                    if (pn.isEmpty()) {
                        throw new IllegalArgumentException(name
                                                           + ": unnamed package");
                    }
                }
            }
            Provides p = new Provides(service, providers);
            return provides(p);
        }

        /**
         * Adds packages to the module. All packages in the set of package names
         * that are not in the module are added to module.
         *
         * @param  pns
         *         The (possibly empty) set of package names
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If any of the package names is {@code null} or is not a
         *         legal package name
         */
        public Builder packages(Set<String> pns) {
            if (strict) {
                pns = new HashSet<>(pns);
                pns.forEach(Checks::requirePackageName);
            }
            this.packages.addAll(pns);
            return this;
        }

        /**
         * Sets the module version.
         *
         * @param  v
         *         The version
         *
         * @return This builder
         */
        public Builder version(Version v) {
            version = requireNonNull(v);
            rawVersionString = null;
            return this;
        }

        /**
         * Sets the module version.
         *
         * @param  vs
         *         The version string to parse
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If {@code vs} is {@code null} or cannot be parsed as a
         *         version string
         *
         * @see Version#parse(String)
         */
        public Builder version(String vs) {
            try {
                version = Version.parse(vs);
                rawVersionString = null;
            } catch (IllegalArgumentException e) {
                if (strict) throw e;
                version = null;
                rawVersionString = vs;
            }
            return this;
        }

        /**
         * Sets the module main class. The package for the main class is added
         * to the module if not already added. In other words, this method is
         * equivalent to first invoking this builder's {@link #packages(Set)
         * packages} method to add the package name of the main class.
         *
         * @param  mc
         *         The module main class
         *
         * @return This builder
         *
         * @throws IllegalArgumentException
         *         If {@code mainClass} is {@code null} or not a qualified
         *         name of a class in a named package
         */
        public Builder mainClass(String mc) {
            String pn;
            if (strict) {
                mc = requireQualifiedClassName("main class name", mc);
                pn = packageName(mc);
                assert !pn.isEmpty();
            } else {
                // Disallow main class in unnamed package
                pn = packageName(mc);
                if (pn.isEmpty()) {
                    throw new IllegalArgumentException(mc + ": unnamed package");
                }
            }
            packages.add(pn);
            mainClass = mc;
            return this;
        }

        /**
         * Builds and returns a {@code ModuleDescriptor} from its components.
         *
         * <p> The module will require "{@code java.base}" even if the dependence
         * has not been declared (the exception is when building a module named
         * "{@code java.base}" as it cannot require itself). The dependence on
         * "{@code java.base}" will have the {@link
         * java.lang.module.ModuleDescriptor.Requires.Modifier#MANDATED MANDATED}
         * modifier if the dependence was not declared. </p>
         *
         * @return The module descriptor
         */
        public ModuleDescriptor build() {
            Set<Requires> requires = new HashSet<>(this.requires.values());
            Set<Exports> exports = new HashSet<>(this.exports.values());
            Set<Opens> opens = new HashSet<>(this.opens.values());

            // add dependency on java.base
            if (strict
                    && !name.equals("java.base")
                    && !this.requires.containsKey("java.base")) {
                requires.add(new Requires(Set.of(Requires.Modifier.MANDATED),
                                          "java.base",
                                          null,
                                          null));
            }

            Set<Provides> provides = new HashSet<>(this.provides.values());

            return new ModuleDescriptor(name,
                                        version,
                                        rawVersionString,
                                        modifiers,
                                        requires,
                                        exports,
                                        opens,
                                        uses,
                                        provides,
                                        packages,
                                        mainClass);
        }

    }

    /**
     * Compares this module descriptor to another.
     *
     * <p> Two {@code ModuleDescriptor} objects are compared by comparing their
     * module names lexicographically. Where the module names are equal then the
     * module versions are compared. When comparing the module versions then a
     * module descriptor with a version is considered to succeed a module
     * descriptor that does not have a version. If both versions are {@linkplain
     * Version#parse(String) unparseable} then the {@linkplain #rawVersion()
     * raw version strings} are compared lexicographically. Where the module names
     * are equal and the versions are equal (or not present in both), then the
     * set of modifiers are compared. Sets of modifiers are compared by comparing
     * a <em>binary value</em> computed for each set. If a modifier is present
     * in the set then the bit at the position of its ordinal is {@code 1}
     * in the binary value, otherwise {@code 0}. If the two set of modifiers
     * are also equal then the other components of the module descriptors are
     * compared in a manner that is consistent with {@code equals}. </p>
     *
     * @param  that
     *         The module descriptor to compare
     *
     * @return A negative integer, zero, or a positive integer if this module
     *         descriptor is less than, equal to, or greater than the given
     *         module descriptor
     */
    @Override
    public int compareTo(ModuleDescriptor that) {
        if (this == that) return 0;

        int c = this.name().compareTo(that.name());
        if (c != 0) return c;

        c = compare(this.version, that.version);
        if (c != 0) return c;

        c = compare(this.rawVersionString, that.rawVersionString);
        if (c != 0) return c;

        long v1 = modsValue(this.modifiers());
        long v2 = modsValue(that.modifiers());
        c = Long.compare(v1, v2);
        if (c != 0) return c;

        c = compare(this.requires, that.requires);
        if (c != 0) return c;

        c = compare(this.packages, that.packages);
        if (c != 0) return c;

        c = compare(this.exports, that.exports);
        if (c != 0) return c;

        c = compare(this.opens, that.opens);
        if (c != 0) return c;

        c = compare(this.uses, that.uses);
        if (c != 0) return c;

        c = compare(this.provides, that.provides);
        if (c != 0) return c;

        c = compare(this.mainClass, that.mainClass);
        if (c != 0) return c;

        return 0;
    }

    /**
     * Tests this module descriptor for equality with the given object.
     *
     * <p> If the given object is not a {@code ModuleDescriptor} then this
     * method returns {@code false}. Two module descriptors are equal if each
     * of their corresponding components is equal. </p>
     *
     * <p> This method satisfies the general contract of the {@link
     * java.lang.Object#equals(Object) Object.equals} method. </p>
     *
     * @param   ob
     *          the object to which this object is to be compared
     *
     * @return  {@code true} if, and only if, the given object is a module
     *          descriptor that is equal to this module descriptor
     */
    @Override
    public boolean equals(Object ob) {
        if (ob == this)
            return true;
        return (ob instanceof ModuleDescriptor that)
                && (name.equals(that.name)
                && modifiers.equals(that.modifiers)
                && requires.equals(that.requires)
                && Objects.equals(packages, that.packages)
                && exports.equals(that.exports)
                && opens.equals(that.opens)
                && uses.equals(that.uses)
                && provides.equals(that.provides)
                && Objects.equals(version, that.version)
                && Objects.equals(rawVersionString, that.rawVersionString)
                && Objects.equals(mainClass, that.mainClass));
    }

    /**
     * Computes a hash code for this module descriptor.
     *
     * <p> The hash code is based upon the components of the module descriptor,
     * and satisfies the general contract of the {@link Object#hashCode
     * Object.hashCode} method. </p>
     *
     * @return The hash-code value for this module descriptor
     */
    @Override
    public int hashCode() {
        int hc = hash;
        if (hc == 0) {
            hc = name.hashCode();
            hc = hc * 43 + Objects.hashCode(modifiers);
            hc = hc * 43 + requires.hashCode();
            hc = hc * 43 + Objects.hashCode(packages);
            hc = hc * 43 + exports.hashCode();
            hc = hc * 43 + opens.hashCode();
            hc = hc * 43 + uses.hashCode();
            hc = hc * 43 + provides.hashCode();
            hc = hc * 43 + Objects.hashCode(version);
            hc = hc * 43 + Objects.hashCode(rawVersionString);
            hc = hc * 43 + Objects.hashCode(mainClass);
            if (hc == 0)
                hc = -1;
            hash = hc;
        }
        return hc;
    }
    private transient int hash;  // cached hash code

    /**
     * <p> Returns a string describing the module. </p>
     *
     * @return A string describing the module
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (isOpen())
            sb.append("open ");
        sb.append("module { name: ").append(toNameAndVersion());
        if (!requires.isEmpty())
            sb.append(", ").append(requires);
        if (!uses.isEmpty())
            sb.append(", uses: ").append(uses);
        if (!exports.isEmpty())
            sb.append(", exports: ").append(exports);
        if (!opens.isEmpty())
            sb.append(", opens: ").append(opens);
        if (!provides.isEmpty()) {
            sb.append(", provides: ").append(provides);
        }
        sb.append(" }");
        return sb.toString();
    }


    /**
     * Instantiates a builder to build a module descriptor.
     *
     * @param  name
     *         The module name
     * @param  ms
     *         The set of module modifiers
     *
     * @return A new builder
     *
     * @throws IllegalArgumentException
     *         If the module name is {@code null} or is not a legal module
     *         name, or the set of modifiers contains {@link
     *         Modifier#AUTOMATIC AUTOMATIC} with other modifiers
     */
    public static Builder newModule(String name, Set<Modifier> ms) {
        Set<Modifier> mods = new HashSet<>(ms);
        if (mods.contains(Modifier.AUTOMATIC) && mods.size() > 1)
            throw new IllegalArgumentException("AUTOMATIC cannot be used with"
                                               + " other modifiers");

        return new Builder(name, true, mods);
    }

    /**
     * Instantiates a builder to build a module descriptor for a <em>normal</em>
     * module. This method is equivalent to invoking {@link #newModule(String,Set)
     * newModule} with an empty set of {@link ModuleDescriptor.Modifier modifiers}.
     *
     * @param  name
     *         The module name
     *
     * @return A new builder
     *
     * @throws IllegalArgumentException
     *         If the module name is {@code null} or is not a legal module
     *         name
     */
    public static Builder newModule(String name) {
        return new Builder(name, true, Set.of());
    }

    /**
     * Instantiates a builder to build a module descriptor for an open module.
     * This method is equivalent to invoking {@link #newModule(String,Set)
     * newModule} with the {@link ModuleDescriptor.Modifier#OPEN OPEN} modifier.
     *
     * <p> The builder for an open module cannot be used to declare any open
     * packages. </p>
     *
     * @param  name
     *         The module name
     *
     * @return A new builder that builds an open module
     *
     * @throws IllegalArgumentException
     *         If the module name is {@code null} or is not a legal module
     *         name
     */
    public static Builder newOpenModule(String name) {
        return new Builder(name, true, Set.of(Modifier.OPEN));
    }

    /**
     * Instantiates a builder to build a module descriptor for an automatic
     * module. This method is equivalent to invoking {@link #newModule(String,Set)
     * newModule} with the {@link ModuleDescriptor.Modifier#AUTOMATIC AUTOMATIC}
     * modifier.
     *
     * <p> The builder for an automatic module cannot be used to declare module
     * or service dependences. It also cannot be used to declare any exported
     * or open packages. </p>
     *
     * @param  name
     *         The module name
     *
     * @return A new builder that builds an automatic module
     *
     * @throws IllegalArgumentException
     *         If the module name is {@code null} or is not a legal module
     *         name
     *
     * @see ModuleFinder#of(Path[])
     */
    public static Builder newAutomaticModule(String name) {
        return new Builder(name, true, Set.of(Modifier.AUTOMATIC));
    }


    /**
     * Reads the binary form of a module declaration from an input stream
     * as a module descriptor.
     *
     * <p> If the descriptor encoded in the input stream does not indicate a
     * set of packages in the module then the {@code packageFinder} will be
     * invoked. The set of packages that the {@code packageFinder} returns
     * must include all the packages that the module exports, opens, as well
     * as the packages of the service implementations that the module provides,
     * and the package of the main class (if the module has a main class). If
     * the {@code packageFinder} throws an {@link UncheckedIOException} then
     * {@link IOException} cause will be re-thrown. </p>
     *
     * <p> If there are bytes following the module descriptor then it is
     * implementation specific as to whether those bytes are read, ignored,
     * or reported as an {@code InvalidModuleDescriptorException}. If this
     * method fails with an {@code InvalidModuleDescriptorException} or {@code
     * IOException} then it may do so after some, but not all, bytes have
     * been read from the input stream. It is strongly recommended that the
     * stream be promptly closed and discarded if an exception occurs. </p>
     *
     * @apiNote The {@code packageFinder} parameter is for use when reading
     * module descriptors from legacy module-artifact formats that do not
     * record the set of packages in the descriptor itself.
     *
     * @param  in
     *         The input stream
     * @param  packageFinder
     *         A supplier that can produce the set of packages
     *
     * @return The module descriptor
     *
     * @throws InvalidModuleDescriptorException
     *         If an invalid module descriptor is detected or the set of
     *         packages returned by the {@code packageFinder} does not include
     *         all of the packages obtained from the module descriptor
     * @throws IOException
     *         If an I/O error occurs reading from the input stream or {@code
     *         UncheckedIOException} is thrown by the package finder
     */
    public static ModuleDescriptor read(InputStream in,
                                        Supplier<Set<String>> packageFinder)
        throws IOException
    {
        return ModuleInfo.read(in, requireNonNull(packageFinder)).descriptor();
    }

    /**
     * Reads the binary form of a module declaration from an input stream as a
     * module descriptor. This method works exactly as specified by the 2-arg
     * {@link #read(InputStream,Supplier) read} method with the exception that
     * a package finder is not used to find additional packages when the
     * module descriptor read from the stream does not indicate the set of
     * packages.
     *
     * @param  in
     *         The input stream
     *
     * @return The module descriptor
     *
     * @throws InvalidModuleDescriptorException
     *         If an invalid module descriptor is detected
     * @throws IOException
     *         If an I/O error occurs reading from the input stream
     */
    public static ModuleDescriptor read(InputStream in) throws IOException {
        return ModuleInfo.read(in, null).descriptor();
    }

    /**
     * Reads the binary form of a module declaration from a byte buffer
     * as a module descriptor.
     *
     * <p> If the descriptor encoded in the byte buffer does not indicate a
     * set of packages in the module then the {@code packageFinder} will be
     * invoked. The set of packages that the {@code packageFinder} returns
     * must include all the packages that the module exports, opens, as well
     * as the packages of the service implementations that the module provides,
     * and the package of the main class (if the module has a main class). If
     * the {@code packageFinder} throws an {@link UncheckedIOException} then
     * {@link IOException} cause will be re-thrown. </p>
     *
     * <p> The module descriptor is read from the buffer starting at index
     * {@code p}, where {@code p} is the buffer's {@link ByteBuffer#position()
     * position} when this method is invoked. Upon return the buffer's position
     * will be equal to {@code p + n} where {@code n} is the number of bytes
     * read from the buffer. </p>
     *
     * <p> If there are bytes following the module descriptor then it is
     * implementation specific as to whether those bytes are read, ignored,
     * or reported as an {@code InvalidModuleDescriptorException}. If this
     * method fails with an {@code InvalidModuleDescriptorException} then it
     * may do so after some, but not all, bytes have been read. </p>
     *
     * @apiNote The {@code packageFinder} parameter is for use when reading
     * module descriptors from legacy module-artifact formats that do not
     * record the set of packages in the descriptor itself.
     *
     * @param  bb
     *         The byte buffer
     * @param  packageFinder
     *         A supplier that can produce the set of packages
     *
     * @return The module descriptor
     *
     * @throws InvalidModuleDescriptorException
     *         If an invalid module descriptor is detected or the set of
     *         packages returned by the {@code packageFinder} does not include
     *         all of the packages obtained from the module descriptor
     */
    public static ModuleDescriptor read(ByteBuffer bb,
                                        Supplier<Set<String>> packageFinder)
    {
        return ModuleInfo.read(bb, requireNonNull(packageFinder)).descriptor();
    }

    /**
     * Reads the binary form of a module declaration from a byte buffer as a
     * module descriptor. This method works exactly as specified by the 2-arg
     * {@link #read(ByteBuffer,Supplier) read} method with the exception that a
     * package finder is not used to find additional packages when the module
     * descriptor encoded in the buffer does not indicate the set of packages.
     *
     * @param  bb
     *         The byte buffer
     *
     * @return The module descriptor
     *
     * @throws InvalidModuleDescriptorException
     *         If an invalid module descriptor is detected
     */
    public static ModuleDescriptor read(ByteBuffer bb) {
        return ModuleInfo.read(bb, null).descriptor();
    }

    private static String packageName(String cn) {
        int index = cn.lastIndexOf('.');
        return (index == -1) ? "" : cn.substring(0, index);
    }

    /**
     * Returns a string containing the given set of modifiers and label.
     */
    private static <M> String toString(Set<M> mods, String what) {
        return (Stream.concat(mods.stream().map(e -> e.toString()
                                                      .toLowerCase(Locale.ROOT)),
                              Stream.of(what)))
                .collect(Collectors.joining(" "));
    }

    private static <T extends Object & Comparable<? super T>>
    int compare(T obj1, T obj2) {
        if (obj1 != null) {
            return (obj2 != null) ? obj1.compareTo(obj2) : 1;
        } else {
            return (obj2 == null) ? 0 : -1;
        }
    }

    /**
     * Compares two sets of {@code Comparable} objects.
     */
    @SuppressWarnings("unchecked")
    private static <T extends Object & Comparable<? super T>>
    int compare(Set<T> s1, Set<T> s2) {
        T[] a1 = (T[]) s1.toArray();
        T[] a2 = (T[]) s2.toArray();
        Arrays.sort(a1);
        Arrays.sort(a2);
        return Arrays.compare(a1, a2);
    }

    private static <E extends Enum<E>> long modsValue(Set<E> set) {
        long value = 0;
        for (Enum<E> e : set) {
            value += 1 << e.ordinal();
        }
        return value;
    }

    static {
        /**
         * Setup the shared secret to allow code in other packages access
         * private package methods in java.lang.module.
         */
        jdk.internal.access.SharedSecrets
            .setJavaLangModuleAccess(new jdk.internal.access.JavaLangModuleAccess() {
                @Override
                public Builder newModuleBuilder(String mn,
                                                boolean strict,
                                                Set<ModuleDescriptor.Modifier> modifiers) {
                    return new Builder(mn, strict, modifiers);
                }

                @Override
                public Set<String> packages(ModuleDescriptor.Builder builder) {
                    return builder.packages();
                }

                @Override
                public void requires(ModuleDescriptor.Builder builder,
                                     Set<Requires.Modifier> ms,
                                     String mn,
                                     String rawCompiledVersion) {
                    builder.requires(ms, mn, rawCompiledVersion);
                }

                @Override
                public Requires newRequires(Set<Requires.Modifier> ms, String mn, Version v) {
                    return new Requires(ms, mn, v, true);
                }

                @Override
                public Exports newExports(Set<Exports.Modifier> ms, String source) {
                    return new Exports(ms, source, Set.of(), true);
                }

                @Override
                public Exports newExports(Set<Exports.Modifier> ms,
                                          String source,
                                          Set<String> targets) {
                    return new Exports(ms, source, targets, true);
                }

                @Override
                public Opens newOpens(Set<Opens.Modifier> ms,
                                      String source,
                                      Set<String> targets) {
                    return new Opens(ms, source, targets, true);
                }

                @Override
                public Opens newOpens(Set<Opens.Modifier> ms, String source) {
                    return new Opens(ms, source, Set.of(), true);
                }

                @Override
                public Provides newProvides(String service, List<String> providers) {
                    return new Provides(service, providers, true);
                }

                @Override
                public ModuleDescriptor newModuleDescriptor(String name,
                                                            Version version,
                                                            Set<ModuleDescriptor.Modifier> modifiers,
                                                            Set<Requires> requires,
                                                            Set<Exports> exports,
                                                            Set<Opens> opens,
                                                            Set<String> uses,
                                                            Set<Provides> provides,
                                                            Set<String> packages,
                                                            String mainClass,
                                                            int hashCode) {
                    return new ModuleDescriptor(name,
                                                version,
                                                modifiers,
                                                requires,
                                                exports,
                                                opens,
                                                uses,
                                                provides,
                                                packages,
                                                mainClass,
                                                hashCode,
                                                false);
                }

                @Override
                public Configuration resolveAndBind(ModuleFinder finder,
                                                    Collection<String> roots,
                                                    PrintStream traceOutput)
                {
                    return Configuration.resolveAndBind(finder, roots, traceOutput);
                }

                @Override
                public Configuration newConfiguration(ModuleFinder finder,
                                                      Map<String, Set<String>> graph) {
                    return new Configuration(finder, graph);
                }
            });
    }

}
