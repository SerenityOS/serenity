/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.classfile;

import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import com.sun.tools.classfile.Dependency.Filter;
import com.sun.tools.classfile.Dependency.Finder;
import com.sun.tools.classfile.Dependency.Location;
import com.sun.tools.classfile.Type.ArrayType;
import com.sun.tools.classfile.Type.ClassSigType;
import com.sun.tools.classfile.Type.ClassType;
import com.sun.tools.classfile.Type.MethodType;
import com.sun.tools.classfile.Type.SimpleType;
import com.sun.tools.classfile.Type.TypeParamType;
import com.sun.tools.classfile.Type.WildcardType;

import static com.sun.tools.classfile.ConstantPool.*;

/**
 * A framework for determining {@link Dependency dependencies} between class files.
 *
 * A {@link Dependency.Finder finder} is used to identify the dependencies of
 * individual classes. Some finders may return subtypes of {@code Dependency} to
 * further characterize the type of dependency, such as a dependency on a
 * method within a class.
 *
 * A {@link Dependency.Filter filter} may be used to restrict the set of
 * dependencies found by a finder.
 *
 * Dependencies that are found may be passed to a {@link Dependencies.Recorder
 * recorder} so that the dependencies can be stored in a custom data structure.
 */
public class Dependencies {
    /**
     * Thrown when a class file cannot be found.
     */
    public static class ClassFileNotFoundException extends Exception {
        private static final long serialVersionUID = 3632265927794475048L;

        public ClassFileNotFoundException(String className) {
            super(className);
            this.className = className;
        }

        public ClassFileNotFoundException(String className, Throwable cause) {
            this(className);
            initCause(cause);
        }

        public final String className;
    }

    /**
     * Thrown when an exception is found processing a class file.
     */
    public static class ClassFileError extends Error {
        private static final long serialVersionUID = 4111110813961313203L;

        public ClassFileError(Throwable cause) {
            initCause(cause);
        }
    }

    /**
     * Service provider interface to locate and read class files.
     */
    public interface ClassFileReader {
        /**
         * Get the ClassFile object for a specified class.
         * @param className the name of the class to be returned.
         * @return the ClassFile for the given class
         * @throws Dependencies.ClassFileNotFoundException if the classfile cannot be
         *   found
         */
        public ClassFile getClassFile(String className)
                throws ClassFileNotFoundException;
    }

    /**
     * Service provide interface to handle results.
     */
    public interface Recorder {
        /**
         * Record a dependency that has been found.
         * @param d
         */
        public void addDependency(Dependency d);
    }

    /**
     * Get the  default finder used to locate the dependencies for a class.
     * @return the default finder
     */
    public static Finder getDefaultFinder() {
        return new APIDependencyFinder(AccessFlags.ACC_PRIVATE);
    }

    /**
     * Get a finder used to locate the API dependencies for a class.
     * These include the superclass, superinterfaces, and classes referenced in
     * the declarations of fields and methods.  The fields and methods that
     * are checked can be limited according to a specified access.
     * The access parameter must be one of {@link AccessFlags#ACC_PUBLIC ACC_PUBLIC},
     * {@link AccessFlags#ACC_PRIVATE ACC_PRIVATE},
     * {@link AccessFlags#ACC_PROTECTED ACC_PROTECTED}, or 0 for
     * package private access. Members with greater than or equal accessibility
     * to that specified will be searched for dependencies.
     * @param access the access of members to be checked
     * @return an API finder
     */
    public static Finder getAPIFinder(int access) {
        return new APIDependencyFinder(access);
    }

    /**
     * Get a finder to do class dependency analysis.
     *
     * @return a Class dependency finder
     */
    public static Finder getClassDependencyFinder() {
        return new ClassDependencyFinder();
    }

    /**
     * Get the finder used to locate the dependencies for a class.
     * @return the finder
     */
    public Finder getFinder() {
        if (finder == null)
            finder = getDefaultFinder();
        return finder;
    }

    /**
     * Set the finder used to locate the dependencies for a class.
     * @param f the finder
     */
    public void setFinder(Finder f) {
        finder = Objects.requireNonNull(f);
    }

    /**
     * Get the default filter used to determine included when searching
     * the transitive closure of all the dependencies.
     * Unless overridden, the default filter accepts all dependencies.
     * @return the default filter.
     */
    public static Filter getDefaultFilter() {
        return DefaultFilter.instance();
    }

    /**
     * Get a filter which uses a regular expression on the target's class name
     * to determine if a dependency is of interest.
     * @param pattern the pattern used to match the target's class name
     * @return a filter for matching the target class name with a regular expression
     */
    public static Filter getRegexFilter(Pattern pattern) {
        return new TargetRegexFilter(pattern);
    }

    /**
     * Get a filter which checks the package of a target's class name
     * to determine if a dependency is of interest. The filter checks if the
     * package of the target's class matches any of a set of given package
     * names. The match may optionally match subpackages of the given names as well.
     * @param packageNames the package names used to match the target's class name
     * @param matchSubpackages whether or not to match subpackages as well
     * @return a filter for checking the target package name against a list of package names
     */
    public static Filter getPackageFilter(Set<String> packageNames, boolean matchSubpackages) {
        return new TargetPackageFilter(packageNames, matchSubpackages);
    }

    /**
     * Get the filter used to determine the dependencies included when searching
     * the transitive closure of all the dependencies.
     * Unless overridden, the default filter accepts all dependencies.
     * @return the filter
     */
    public Filter getFilter() {
        if (filter == null)
            filter = getDefaultFilter();
        return filter;
    }

    /**
     * Set the filter used to determine the dependencies included when searching
     * the transitive closure of all the dependencies.
     * @param f the filter
     */
    public void setFilter(Filter f) {
        filter = Objects.requireNonNull(f);
    }

    /**
     * Find the dependencies of a class, using the current
     * {@link Dependencies#getFinder finder} and
     * {@link Dependencies#getFilter filter}.
     * The search may optionally include the transitive closure of all the
     * filtered dependencies, by also searching in the classes named in those
     * dependencies.
     * @param classFinder a finder to locate class files
     * @param rootClassNames the names of the root classes from which to begin
     *      searching
     * @param transitiveClosure whether or not to also search those classes
     *      named in any filtered dependencies that are found.
     * @return the set of dependencies that were found
     * @throws ClassFileNotFoundException if a required class file cannot be found
     * @throws ClassFileError if an error occurs while processing a class file,
     *      such as an error in the internal class file structure.
     */
    public Set<Dependency> findAllDependencies(
            ClassFileReader classFinder, Set<String> rootClassNames,
            boolean transitiveClosure)
            throws ClassFileNotFoundException {
        final Set<Dependency> results = new HashSet<>();
        Recorder r = results::add;
        findAllDependencies(classFinder, rootClassNames, transitiveClosure, r);
        return results;
    }

    /**
     * Find the dependencies of a class, using the current
     * {@link Dependencies#getFinder finder} and
     * {@link Dependencies#getFilter filter}.
     * The search may optionally include the transitive closure of all the
     * filtered dependencies, by also searching in the classes named in those
     * dependencies.
     * @param classFinder a finder to locate class files
     * @param rootClassNames the names of the root classes from which to begin
     *      searching
     * @param transitiveClosure whether or not to also search those classes
     *      named in any filtered dependencies that are found.
     * @param recorder a recorder for handling the results
     * @throws ClassFileNotFoundException if a required class file cannot be found
     * @throws ClassFileError if an error occurs while processing a class file,
     *      such as an error in the internal class file structure.
     */
    public void findAllDependencies(
            ClassFileReader classFinder, Set<String> rootClassNames,
            boolean transitiveClosure, Recorder recorder)
            throws ClassFileNotFoundException {
        Set<String> doneClasses = new HashSet<>();

        getFinder();  // ensure initialized
        getFilter();  // ensure initialized

        // Work queue of names of classfiles to be searched.
        // Entries will be unique, and for classes that do not yet have
        // dependencies in the results map.
        Deque<String> deque = new LinkedList<>(rootClassNames);

        String className;
        while ((className = deque.poll()) != null) {
            assert (!doneClasses.contains(className));
            doneClasses.add(className);

            ClassFile cf = classFinder.getClassFile(className);

            // The following code just applies the filter to the dependencies
            // followed for the transitive closure.
            for (Dependency d: finder.findDependencies(cf)) {
                recorder.addDependency(d);
                if (transitiveClosure && filter.accepts(d)) {
                    String cn = d.getTarget().getClassName();
                    if (!doneClasses.contains(cn))
                        deque.add(cn);
                }
            }
        }
    }

    private Filter filter;
    private Finder finder;

    /**
     * A location identifying a class.
     */
    static class SimpleLocation implements Location {
        public SimpleLocation(String name) {
            this.name = name;
            this.className = name.replace('/', '.');
        }

        public String getName() {
            return name;
        }

        public String getClassName() {
            return className;
        }

        public String getPackageName() {
            int i = name.lastIndexOf('/');
            return (i > 0) ? name.substring(0, i).replace('/', '.') : "";
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof SimpleLocation))
                return false;
            return (name.equals(((SimpleLocation) other).name));
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }

        private String name;
        private String className;
    }

    /**
     * A dependency of one class on another.
     */
    static class SimpleDependency implements Dependency {
        public SimpleDependency(Location origin, Location target) {
            this.origin = origin;
            this.target = target;
        }

        public Location getOrigin() {
            return origin;
        }

        public Location getTarget() {
            return target;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other)
                return true;
            if (!(other instanceof SimpleDependency))
                return false;
            SimpleDependency o = (SimpleDependency) other;
            return (origin.equals(o.origin) && target.equals(o.target));
        }

        @Override
        public int hashCode() {
            return origin.hashCode() * 31 + target.hashCode();
        }

        @Override
        public String toString() {
            return origin + ":" + target;
        }

        private Location origin;
        private Location target;
    }


    /**
     * This class accepts all dependencies.
     */
    static class DefaultFilter implements Filter {
        private static DefaultFilter instance;

        static DefaultFilter instance() {
            if (instance == null)
                instance = new DefaultFilter();
            return instance;
        }

        public boolean accepts(Dependency dependency) {
            return true;
        }
    }

    /**
     * This class accepts those dependencies whose target's class name matches a
     * regular expression.
     */
    static class TargetRegexFilter implements Filter {
        TargetRegexFilter(Pattern pattern) {
            this.pattern = pattern;
        }

        public boolean accepts(Dependency dependency) {
            return pattern.matcher(dependency.getTarget().getClassName()).matches();
        }

        private final Pattern pattern;
    }

    /**
     * This class accepts those dependencies whose class name is in a given
     * package.
     */
    static class TargetPackageFilter implements Filter {
        TargetPackageFilter(Set<String> packageNames, boolean matchSubpackages) {
            for (String pn: packageNames) {
                if (pn.length() == 0) // implies null check as well
                    throw new IllegalArgumentException();
            }
            this.packageNames = packageNames;
            this.matchSubpackages = matchSubpackages;
        }

        public boolean accepts(Dependency dependency) {
            String pn = dependency.getTarget().getPackageName();
            if (packageNames.contains(pn))
                return true;

            if (matchSubpackages) {
                for (String n: packageNames) {
                    if (pn.startsWith(n + "."))
                        return true;
                }
            }

            return false;
        }

        private final Set<String> packageNames;
        private final boolean matchSubpackages;
    }

    /**
     * This class identifies class names directly or indirectly in the constant pool.
     */
    static class ClassDependencyFinder extends BasicDependencyFinder {
        public Iterable<? extends Dependency> findDependencies(ClassFile classfile) {
            Visitor v = new Visitor(classfile);
            for (CPInfo cpInfo: classfile.constant_pool.entries()) {
                v.scan(cpInfo);
            }
            try {
                v.addClass(classfile.super_class);
                v.addClasses(classfile.interfaces);
                v.scan(classfile.attributes);

                for (Field f : classfile.fields) {
                    v.scan(f.descriptor, f.attributes);
                }
                for (Method m : classfile.methods) {
                    v.scan(m.descriptor, m.attributes);
                    Exceptions_attribute e =
                        (Exceptions_attribute)m.attributes.get(Attribute.Exceptions);
                    if (e != null) {
                        v.addClasses(e.exception_index_table);
                    }
                }
            } catch (ConstantPoolException e) {
                throw new ClassFileError(e);
            }

            return v.deps;
        }
    }

    /**
     * This class identifies class names in the signatures of classes, fields,
     * and methods in a class.
     */
    static class APIDependencyFinder extends BasicDependencyFinder {
        APIDependencyFinder(int access) {
            switch (access) {
                case AccessFlags.ACC_PUBLIC:
                case AccessFlags.ACC_PROTECTED:
                case AccessFlags.ACC_PRIVATE:
                case 0:
                    showAccess = access;
                    break;
                default:
                    throw new IllegalArgumentException("invalid access 0x"
                            + Integer.toHexString(access));
            }
        }

        public Iterable<? extends Dependency> findDependencies(ClassFile classfile) {
            try {
                Visitor v = new Visitor(classfile);
                v.addClass(classfile.super_class);
                v.addClasses(classfile.interfaces);
                // inner classes?
                for (Field f : classfile.fields) {
                    if (checkAccess(f.access_flags))
                        v.scan(f.descriptor, f.attributes);
                }
                for (Method m : classfile.methods) {
                    if (checkAccess(m.access_flags)) {
                        v.scan(m.descriptor, m.attributes);
                        Exceptions_attribute e =
                                (Exceptions_attribute) m.attributes.get(Attribute.Exceptions);
                        if (e != null)
                            v.addClasses(e.exception_index_table);
                    }
                }
                return v.deps;
            } catch (ConstantPoolException e) {
                throw new ClassFileError(e);
            }
        }

        boolean checkAccess(AccessFlags flags) {
            // code copied from javap.Options.checkAccess
            boolean isPublic = flags.is(AccessFlags.ACC_PUBLIC);
            boolean isProtected = flags.is(AccessFlags.ACC_PROTECTED);
            boolean isPrivate = flags.is(AccessFlags.ACC_PRIVATE);
            boolean isPackage = !(isPublic || isProtected || isPrivate);

            if ((showAccess == AccessFlags.ACC_PUBLIC) && (isProtected || isPrivate || isPackage))
                return false;
            else if ((showAccess == AccessFlags.ACC_PROTECTED) && (isPrivate || isPackage))
                return false;
            else if ((showAccess == 0) && (isPrivate))
                return false;
            else
                return true;
        }

        private int showAccess;
    }

    static abstract class BasicDependencyFinder implements Finder {
        private Map<String,Location> locations = new ConcurrentHashMap<>();

        Location getLocation(String className) {
            return locations.computeIfAbsent(className, SimpleLocation::new);
        }

        class Visitor implements ConstantPool.Visitor<Void,Void>, Type.Visitor<Void, Void> {
            private ConstantPool constant_pool;
            private Location origin;
            Set<Dependency> deps;

            Visitor(ClassFile classFile) {
                try {
                    constant_pool = classFile.constant_pool;
                    origin = getLocation(classFile.getName());
                    deps = new HashSet<>();
                } catch (ConstantPoolException e) {
                    throw new ClassFileError(e);
                }
            }

            void scan(Descriptor d, Attributes attrs) {
                try {
                    scan(new Signature(d.index).getType(constant_pool));
                    scan(attrs);
                } catch (ConstantPoolException e) {
                    throw new ClassFileError(e);
                }
            }

            void scan(CPInfo cpInfo) {
                cpInfo.accept(this, null);
            }

            void scan(Type t) {
                t.accept(this, null);
            }

            void scan(Attributes attrs) {
                try {
                    Signature_attribute sa = (Signature_attribute)attrs.get(Attribute.Signature);
                    if (sa != null)
                        scan(sa.getParsedSignature().getType(constant_pool));

                    scan((RuntimeVisibleAnnotations_attribute)
                            attrs.get(Attribute.RuntimeVisibleAnnotations));
                    scan((RuntimeVisibleParameterAnnotations_attribute)
                            attrs.get(Attribute.RuntimeVisibleParameterAnnotations));
                } catch (ConstantPoolException e) {
                    throw new ClassFileError(e);
                }
            }

            private void scan(RuntimeAnnotations_attribute attr) throws ConstantPoolException {
                if (attr == null) {
                    return;
                }
                for (int i = 0; i < attr.annotations.length; i++) {
                    int index = attr.annotations[i].type_index;
                    scan(new Signature(index).getType(constant_pool));
                }
            }

            private void scan(RuntimeParameterAnnotations_attribute attr) throws ConstantPoolException {
                if (attr == null) {
                    return;
                }
                for (int param = 0; param < attr.parameter_annotations.length; param++) {
                    for (int i = 0; i < attr.parameter_annotations[param].length; i++) {
                        int index = attr.parameter_annotations[param][i].type_index;
                        scan(new Signature(index).getType(constant_pool));
                    }
                }
            }

            void addClass(int index) throws ConstantPoolException {
                if (index != 0) {
                    String name = constant_pool.getClassInfo(index).getBaseName();
                    if (name != null)
                        addDependency(name);
                }
            }

            void addClasses(int[] indices) throws ConstantPoolException {
                for (int i: indices)
                    addClass(i);
            }

            private void addDependency(String name) {
                deps.add(new SimpleDependency(origin, getLocation(name)));
            }

            // ConstantPool.Visitor methods

            public Void visitClass(CONSTANT_Class_info info, Void p) {
                try {
                    if (info.getName().startsWith("["))
                        new Signature(info.name_index).getType(constant_pool).accept(this, null);
                    else
                        addDependency(info.getBaseName());
                    return null;
                } catch (ConstantPoolException e) {
                    throw new ClassFileError(e);
                }
            }

            public Void visitDouble(CONSTANT_Double_info info, Void p) {
                return null;
            }

            public Void visitFieldref(CONSTANT_Fieldref_info info, Void p) {
                return visitRef(info, p);
            }

            public Void visitFloat(CONSTANT_Float_info info, Void p) {
                return null;
            }

            public Void visitInteger(CONSTANT_Integer_info info, Void p) {
                return null;
            }

            public Void visitInterfaceMethodref(CONSTANT_InterfaceMethodref_info info, Void p) {
                return visitRef(info, p);
            }

            public Void visitInvokeDynamic(CONSTANT_InvokeDynamic_info info, Void p) {
                return null;
            }

            @Override
            public Void visitDynamicConstant(CONSTANT_Dynamic_info info, Void aVoid) {
                return null;
            }

            public Void visitLong(CONSTANT_Long_info info, Void p) {
                return null;
            }

            public Void visitMethodHandle(CONSTANT_MethodHandle_info info, Void p) {
                return null;
            }

            public Void visitMethodType(CONSTANT_MethodType_info info, Void p) {
                return null;
            }

            public Void visitMethodref(CONSTANT_Methodref_info info, Void p) {
                return visitRef(info, p);
            }

            public Void visitModule(CONSTANT_Module_info info, Void p) {
                return null;
            }

            public Void visitNameAndType(CONSTANT_NameAndType_info info, Void p) {
                try {
                    new Signature(info.type_index).getType(constant_pool).accept(this, null);
                    return null;
                } catch (ConstantPoolException e) {
                    throw new ClassFileError(e);
                }
            }

            public Void visitPackage(CONSTANT_Package_info info, Void p) {
                return null;
            }

            public Void visitString(CONSTANT_String_info info, Void p) {
                return null;
            }

            public Void visitUtf8(CONSTANT_Utf8_info info, Void p) {
                return null;
            }

            private Void visitRef(CPRefInfo info, Void p) {
                try {
                    visitClass(info.getClassInfo(), p);
                    return null;
                } catch (ConstantPoolException e) {
                    throw new ClassFileError(e);
                }
            }

            // Type.Visitor methods

            private void findDependencies(Type t) {
                if (t != null)
                    t.accept(this, null);
            }

            private void findDependencies(List<? extends Type> ts) {
                if (ts != null) {
                    for (Type t: ts)
                        t.accept(this, null);
                }
            }

            public Void visitSimpleType(SimpleType type, Void p) {
                return null;
            }

            public Void visitArrayType(ArrayType type, Void p) {
                findDependencies(type.elemType);
                return null;
            }

            public Void visitMethodType(MethodType type, Void p) {
                findDependencies(type.paramTypes);
                findDependencies(type.returnType);
                findDependencies(type.throwsTypes);
                findDependencies(type.typeParamTypes);
                return null;
            }

            public Void visitClassSigType(ClassSigType type, Void p) {
                findDependencies(type.superclassType);
                findDependencies(type.superinterfaceTypes);
                return null;
            }

            public Void visitClassType(ClassType type, Void p) {
                findDependencies(type.outerType);
                addDependency(type.getBinaryName());
                findDependencies(type.typeArgs);
                return null;
            }

            public Void visitTypeParamType(TypeParamType type, Void p) {
                findDependencies(type.classBound);
                findDependencies(type.interfaceBounds);
                return null;
            }

            public Void visitWildcardType(WildcardType type, Void p) {
                findDependencies(type.boundType);
                return null;
            }
        }
    }
}
