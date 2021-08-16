/*
 * Copyright (c) 2012, 2013, Oracle and/or its affiliates. All rights reserved.
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

package separate;

import java.util.*;
import java.io.StringWriter;
import java.io.PrintWriter;

public class SourceModel {

    public static final String stdMethodName = "m";

    public static interface SourceProcessor {
        // Called with a generated source file
        void process(String name, String content);
    }

    public static abstract class Element {

        protected abstract void generate(PrintWriter pw);

        public String toString() {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            generate(pw);
            return sw.toString();
        }
    };

    public static class AccessFlag extends Element {
        private String flag;

        public AccessFlag(String name) { flag = name; }

        protected void generate(PrintWriter pw) {
            pw.print(flag);
        }

        public String toString() { return flag; }

        public static final AccessFlag PUBLIC = new AccessFlag("public");
        public static final AccessFlag PRIVATE = new AccessFlag("private");
        public static final AccessFlag PROTECTED = new AccessFlag("protected");
        public static final AccessFlag STATIC = new AccessFlag("static");
        public static final AccessFlag FINAL = new AccessFlag("final");
        public static final AccessFlag SYNCHRONIZED = new AccessFlag("synchronized");
        public static final AccessFlag VOLATILE = new AccessFlag("volatile");
        public static final AccessFlag NATIVE = new AccessFlag("native");
        public static final AccessFlag ABSTRACT = new AccessFlag("abstract");
        public static final AccessFlag STRICTFP = new AccessFlag("strictfp");
        public static final AccessFlag DEFAULT = new AccessFlag("default");
    }

    public static class TypeParameter extends Element {
        private String parameter;

        public TypeParameter(String str) {
            this.parameter = str;
        }

        protected void generate(PrintWriter pw) {
            pw.print(parameter);
        }
    }

    public static class TypeArgument extends Element {
        private String argument;

        public TypeArgument(String str) {
            this.argument = str;
        }

        protected void generate(PrintWriter pw) {
            pw.print(argument);
        }
    }

    public static class MethodParameter extends Element {
        private String type;
        private String name;

        public MethodParameter(String type, String name) {
            this.type = type;
            this.name = name;
        }

        protected void generate(PrintWriter pw) {
            pw.printf("%s %s", this.type, this.name);
        }

        public String toString() { return type + " " + name; }
    }

    public static abstract class Type extends Element {
        private String name;
        private List<AccessFlag> accessFlags;
        private List<TypeParameter> parameters;
        private List<Extends> supertypes;
        private List<Method> methods;

        // methods from superclasses that are required for compilation
        // (and thus will be present in stubs)
        private Set<Method> methodDependencies;
        private List<Type> typeDependencies;

        protected Type(String name,
                List<AccessFlag> flags, List<TypeParameter> params,
                List<Extends> ifaces, List<Method> methods) {
            this.name = name;
            this.accessFlags = flags == null ? new ArrayList<>() : flags;
            this.parameters = params == null ? new ArrayList<>() : params;
            this.supertypes = ifaces == null ? new ArrayList<>() : ifaces;
            this.methods = methods == null ? new ArrayList<>() : methods;
            this.methodDependencies = new HashSet<>();
            this.typeDependencies = new ArrayList<>();
        }

        public String getName() { return this.name; }
        public List<AccessFlag> getAccessFlags() { return this.accessFlags; }
        public List<TypeParameter> getParameters() { return this.parameters; }
        public List<Extends> getSupertypes() { return this.supertypes; }
        public List<Method> getMethods() { return this.methods; }
        public Set<Method> methodDependencies() {
            return this.methodDependencies;
        }

        public Class getSuperclass() { return null; }
        protected abstract void setSuperClass(Extends supertype);

        public void addSuperType(Extends sup) {
            assert sup.getType() instanceof Interface : "Must be an interface";
            this.supertypes.add(sup);
        }
        public void addSuperType(Interface iface) {
            this.supertypes.add(new Extends(iface));
        }

        public void addMethod(Method m) {
            this.methods.add(m);
        }

        public void addAccessFlag(AccessFlag f) {
            this.accessFlags.add(f);
        }

        // Convenience method for creation.  Parameters are interpreted
        // according to their type.  Class (or Extends with a Class type) is
        // considered a superclass (only one allowed).  TypeParameters are
        // generic parameter names.  Interface (or Extends with an Interface
        // type) is an implemented supertype.  Methods are methods (duh!).
        protected void addComponent(Element p) {
            if (p instanceof Class) {
                setSuperClass(new Extends((Class)p));
            } else if (p instanceof Extends) {
                Extends ext = (Extends)p;
                if (ext.supertype instanceof Class) {
                    setSuperClass(ext);
                } else if (ext.supertype instanceof Interface) {
                    addSuperType(ext);
                } else {
                    assert false : "What is this thing?";
                }
            } else if (p instanceof Interface) {
                addSuperType((Interface)p);
            } else if (p instanceof TypeParameter) {
                this.parameters.add((TypeParameter)p);
            } else if (p instanceof Method) {
                addMethod((Method)p);
            } else if (p instanceof AccessFlag) {
                addAccessFlag((AccessFlag)p);
            } else {
                assert false : "What is this thing?";
            }
        }

        // Find and return the first method that has name 'name'
        public Method findMethod(String name) {
            for (Method m : methods) {
                if (m.name.equals(name)) {
                    return m;
                }
            }
            return null;
        }

        public void addCompilationDependency(Type t) {
            typeDependencies.add(t);
        }

        public void addCompilationDependency(Method m) {
            methodDependencies.add(m);
        }

        // Convenience method for creating an Extends object using this
        // class and specified type arguments.
        public Extends with(String ... args) {
            return new Extends(this, args);
        }

        public abstract void generate(SourceProcessor sp);
        public abstract void generateAsDependency(
            SourceProcessor sp, Set<Method> neededMethods);

        protected void generateName(PrintWriter pw) {
            pw.print(this.name);
            StringJoiner joiner = new StringJoiner(",", "<", ">").setEmptyValue("");
            this.parameters.stream().map(Element::toString).forEach(joiner::add);
            pw.print(joiner.toString());
            pw.print(" ");
        }

        protected void generateBody(PrintWriter pw, String superSpec) {
            StringJoiner joiner = new StringJoiner(",", superSpec + " ", " ")
                    .setEmptyValue("");
            supertypes.stream().map(Element::toString).forEach(joiner::add);
            pw.print(joiner.toString());
            pw.println("{ ");
            joiner = new StringJoiner("\n    ", "\n    ", "\n").setEmptyValue("");
            methods.stream().map(Element::toString).forEach(joiner::add);
            pw.print(joiner.toString());
            pw.println("}");
        }

        protected void generateAccessFlags(PrintWriter pw) {
            StringJoiner joiner = new StringJoiner(" ", "", " ");
            accessFlags.stream().map(AccessFlag::toString).forEach(joiner::add);
            pw.print(joiner.toString());
        }

        protected void generateBodyAsDependency(
            PrintWriter pw, Set<Method> neededMethods) {
            pw.println(" {");
            for (Method m : this.methods) {
                if (neededMethods.contains(m)) {
                    pw.print("    ");
                    m.generate(pw);
                    pw.println();
                }
            }
            pw.println("}");
        }

        public Collection<Type> typeDependencies() {
            HashMap<String,Type> dependencies = new HashMap<>();
            Type superclass = getSuperclass();
            if (superclass != null) {
                dependencies.put(superclass.getName(), superclass);
            }
            for (Extends e : getSupertypes())
                dependencies.put(e.getType().getName(), e.getType());
            // Do these last so that they override
            for (Type t : this.typeDependencies)
                dependencies.put(t.getName(), t);
            return dependencies.values();
        }
    }

    public static class Class extends Type {
        private Extends superClass;

        public Class(String name, List<AccessFlag> flags,
                List<TypeParameter> params, Extends sprClass,
                List<Extends> interfaces, List<Method> methods) {
            super(name, flags, params, interfaces, methods);
            this.superClass = sprClass;
            addAccessFlag(AccessFlag.PUBLIC); // should remove this
        }

        public Class(String name, Element ... components) {
            super(name, null, null, null, null);
            this.superClass = null;

            for (Element p : components) {
                addComponent(p);
            }
            addAccessFlag(AccessFlag.PUBLIC); // should remove this
        }

        public boolean isAbstract() {
            for (AccessFlag flag : getAccessFlags()) {
                if (flag == AccessFlag.ABSTRACT) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public void setSuperClass(Extends ext) {
            assert this.superClass == null : "Multiple superclasses defined";
            assert ext.getType() instanceof Class : "Must be a class";
            this.superClass = ext;
        }

        public void setSuperClass(Class c) {
            setSuperClass(new Extends(c));
        }

        @Override
        public Class getSuperclass() {
            return superClass == null ? null : (Class)superClass.supertype;
        }

        public void generate(SourceProcessor processor) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            generate(pw);
            processor.process(getName(), sw.toString());
        }

        public void generate(PrintWriter pw) {
            generateAccessFlags(pw);
            pw.print("class ");
            generateName(pw);
            if (superClass != null) {
                pw.print("extends ");
                superClass.generate(pw);
                pw.print(" ");
            }
            generateBody(pw, "implements");
        }

        public void generateAsDependency(
                SourceProcessor processor, Set<Method> neededMethods) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            generateAccessFlags(pw);
            pw.print("class ");
            generateName(pw);
            pw.print(" ");
            generateBodyAsDependency(pw, neededMethods);

            processor.process(getName(), sw.toString());
        }
    }

    public static class Interface extends Type {

        public Interface(String name,
                  List<AccessFlag> flags, List<TypeParameter> params,
                  List<Extends> interfaces, List<Method> methods) {
            super(name, flags, params, interfaces, methods);
        }

        public Interface(String name, Element ... components) {
            super(name, null, null, null, null);
            for (Element c : components) {
                addComponent(c);
            }
        }

        protected void setSuperClass(Extends ext) {
            assert false : "Interfaces cannot have Class supertypes";
        }

        public void generate(SourceProcessor processor) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            generate(pw);
            processor.process(getName(), sw.toString());
        }

        public void generate(PrintWriter pw) {
            generateAccessFlags(pw);
            pw.print("interface ");
            generateName(pw);
            pw.print(" ");
            generateBody(pw, "extends");
        }

        public void generateAsDependency(
                SourceProcessor processor, Set<Method> neededMethods) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);

            generateAccessFlags(pw);
            pw.print("interface ");
            generateName(pw);
            pw.print(" ");
            generateBodyAsDependency(pw, neededMethods);

            processor.process(getName(), sw.toString());
        }
    }

    /**
     * Represents a type extension that might contain type arguments
     */
    public static class Extends extends Element {
        private final Type supertype;
        private final List<TypeArgument> arguments;

        public Type getType() { return supertype; }
        public List<TypeArgument> getArguments() {
            return arguments;
        }

        public Extends(Type supertype, String ... args) {
            assert supertype != null : "Null supertype";
            this.supertype = supertype;
            this.arguments = new ArrayList<>();
            for (String arg : args) {
                this.arguments.add(new TypeArgument(arg));
            }
        }

        public void generate(PrintWriter pw) {
            StringJoiner joiner = new StringJoiner(",", "<", ">").setEmptyValue("");
            getArguments().stream().map(Element::toString).forEach(joiner::add);
            pw.print(supertype.getName());
            pw.print(joiner.toString());
        }
    }

    public static abstract class Method extends Element {
        private String name;
        private String returnType;
        private List<AccessFlag> accessFlags;
        private List<MethodParameter> parameters;
        private boolean emitSuppressWarnings;

        protected Method(String ret, String name, Element ... params) {
            this.name = name;
            this.returnType = ret;
            this.accessFlags = new ArrayList<>();
            this.parameters = new ArrayList<>();
            this.emitSuppressWarnings = false;

            Arrays.asList(params).stream()
                .filter(x -> x instanceof MethodParameter)
                .map(x -> (MethodParameter)x)
                .forEach(this.parameters::add);
            Arrays.asList(params).stream()
                .filter(x -> x instanceof AccessFlag)
                .map(x -> (AccessFlag)x)
                .forEach(this.accessFlags::add);
            assert accessFlags.size() + parameters.size() == params.length :
                   "Non method parameters or access flags in constructor";
        }

        public String getName() { return this.name; }
        public String getReturnType() { return this.returnType; }
        public List<MethodParameter> getParameters() {
            return this.parameters;
        }
        public List<AccessFlag> getAccessFlags() {
            return this.accessFlags;
        }
        public Element[] getElements() {
            ArrayList<Element> elements = new ArrayList<>();
            getParameters().stream().forEach(elements::add);
            getAccessFlags().stream().forEach(elements::add);
            return elements.toArray(new Element[elements.size()]);
        }

        public void suppressWarnings() { this.emitSuppressWarnings = true; }

        public void generateWarningSuppression(PrintWriter pw) {
            if (this.emitSuppressWarnings) {
                pw.printf("@SuppressWarnings(\"unchecked\")\n    ");
            }
        }

        protected void generateDecl(PrintWriter pw) {
            generateWarningSuppression(pw);
            StringJoiner joiner = new StringJoiner(" ", "", " ");
            accessFlags.stream().map(AccessFlag::toString).forEach(joiner::add);
            pw.print(joiner.toString());
            joiner = new StringJoiner(",");
            pw.printf("%s %s(", returnType, name);
            parameters.stream().map(MethodParameter::toString).forEach(joiner::add);
            pw.print(joiner.toString());
            pw.print(")");
        }
    }

    public static class AbstractMethod extends Method {
        public AbstractMethod(
                String ret, String name, Element ... params) {
            super(ret, name, params);
            this.getAccessFlags().add(AccessFlag.ABSTRACT);
        }

        public void generate(PrintWriter pw) {
            generateDecl(pw);
            pw.print(";");
        }

        public static AbstractMethod std() {
            return new AbstractMethod(
                "int", SourceModel.stdMethodName, AccessFlag.PUBLIC);
        }
    }

    public static class ConcreteMethod extends Method {
        protected String body;

        public ConcreteMethod(String ret, String name,
                String body, Element ... params) {
            super(ret, name, params);
            this.body = body;
        }

        public void generate(PrintWriter pw) {
            generateDecl(pw);
            pw.printf(" { %s }", this.body);
        }

        public static ConcreteMethod std(String value) {
            return new ConcreteMethod(
                "int", SourceModel.stdMethodName, "return " + value + ";",
                AccessFlag.PUBLIC);
        }
    }

    // When the default method flag gets moved into the traditional
    // access flags location, we can remove this class completely and
    // use a ConcreteMethod with an AccessFlag("default") in the constructor
    public static class DefaultMethod extends Method {
        protected String body;

        public DefaultMethod(String ret, String name, String body,
                Element ... params) {
            super(ret, name, params);
            this.body = body;
            this.getAccessFlags().add(AccessFlag.DEFAULT);
        }

        public void generate(PrintWriter pw) {
            generateDecl(pw);
            pw.printf(" { %s }", this.body);
        }

        public static DefaultMethod std(String value) {
            return new DefaultMethod(
                "int", SourceModel.stdMethodName, "return " + value + ";");
        }
    }
}
