/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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

package vm.runtime.defmeth.shared;

import java.util.List;
import java.util.ArrayList;
import static jdk.internal.org.objectweb.asm.Opcodes.*;

import vm.runtime.defmeth.shared.data.*;
import vm.runtime.defmeth.shared.data.method.*;
import vm.runtime.defmeth.shared.data.method.body.*;
import vm.runtime.defmeth.shared.data.method.param.*;
import vm.runtime.defmeth.shared.data.method.result.*;

import static java.lang.String.*;
import nsk.share.Pair;


/**
 * Construct text representation of a class.
 * Used to print generated class hierarchies.
 */
public class Printer implements Visitor {

    private StringBuilder sb = new StringBuilder();
    private Tester t;

    private String output() {
        return sb.toString();
    }

    static private String printAcc(int acc) {
        List<String> flags = new ArrayList<>();
        if ((acc & ACC_STATIC) != 0) {
            flags.add("static");
        }
        if ((acc & ACC_PUBLIC) != 0) {
            flags.add("public");
        }
        if ((acc & ACC_PRIVATE) != 0) {
            flags.add("private");
        }
        if ((acc & ACC_PROTECTED) != 0) {
            flags.add("protected");
        }
        if ((acc & ACC_PUBLIC) == 0 &&
            (acc & ACC_PRIVATE) == 0 &&
            (acc & ACC_PROTECTED) == 0) {
            flags.add("package");
        }
        if ((acc & ACC_SYNCHRONIZED) != 0) {
            flags.add("synchronized");
        }
        return Util.intersperse(" ", flags.toArray(new String[0]));
    }

    static public String print(Clazz clz) {
        Printer p = new Printer();
        clz.visit(p);
        return p.output();
    }

    static public String print(Method m) {
        Printer p = new Printer();
        m.visit(p);
        return p.output();
    }

    @Override
    public void visitClass(Clazz clz) {
        throw new IllegalStateException("More specific method should be called");
    }

    @Override
    public void visitMethod(Method m) {
        sb.append(String.format(
                "%s%s",
                m.name(), m.desc()));

        if (m.sig() != null) {
            sb.append("/* <").append(m.sig()).append("> */");
        }
    }

    @Override
    public void visitConcreteClass(ConcreteClass clz) {
        sb.append("class ").append(clz.name()).append(" ");

        if (!clz.parent().name().equals("java.lang.Object")) {
            sb.append("extends ").append(clz.parent().name()).append(" ");
        }

        if (clz.interfaces().length > 0) {
            sb.append("implements ");
            sb.append(Util.intersperse(", ", Util.asStrings(clz.interfaces())));
            sb.append(" ");
        }

        Method[] methods = clz.methods();

        sb.append("{");
        if (methods.length > 0) {
            for (Method m : methods) {
                sb.append("\n    ");
                m.visit(this);
            }
            sb.append("\n");
        }
        sb.append("}");
    }

    @Override
    public void visitInterface(Interface intf) {
        sb.append("interface ").append(intf.name())
                .append(" ");

        if (intf.parents().length > 0) {
            sb.append("extends ");
            sb.append(Util.intersperse(", ", Util.asStrings(intf.parents())));
            sb.append(" ");
        }

        Method[] methods = intf.methods();

        sb.append("{");
        if (methods.length > 0) {
            for (Method m : methods) {
                sb.append("\n    ");
                m.visit(this);
            }
            sb.append("\n");
        }
        sb.append("}");
    }
    @Override

    /* ====================================================================== */

    public void visitTester(Tester t) {
        this.t = t;

        //sb.append(t.name()).append(": ");
        sb.append("TEST: ");

        //t.getCall().visit(this);
        CallMethod call = t.getCall();

        // call.receiverClass() is null when a .staticCallSite() invoke is
        // used. There is a staticClass but no receiverClass.
        sb.append(format("%s o = new %s(); o.%s%s",
                call.staticClass().name(),
                (call.receiverClass() == null ? "" : call.receiverClass().name()),
                call.methodName(),
                call.methodDesc()));

        sb.append(" ");

        t.getResult().visit(this);

        this.t = null;
    }

    /* ====================================================================== */

    @Override
    public void visitAbstractMethod(AbstractMethod m) {
        Pair<String[],String> desc = Util.parseDesc(m.desc());

        sb.append(format(
                "abstract %s %s %s(%s);",
                printAcc(m.acc()),
                Util.decodeClassName(desc.second),
                m.name(),
                Util.intersperse(", ", desc.first)));

        if (m.sig() != null) {
            sb.append("<").append(m.sig()).append(">");
        }
    }

    @Override
    public void visitConcreteMethod(ConcreteMethod m) {
        Pair<String[],String> desc = Util.parseDesc(m.desc());

        sb.append(format(
                "%s %s %s(%s)",
                printAcc(m.acc()),
                Util.decodeClassName(desc.second),
                m.name(),
                Util.intersperse(", ", desc.first)));

        if (m.sig() != null) {
            sb.append("<").append(m.sig()).append(">");
        }

        sb.append(" ");

        sb.append(" { ");
        m.body().visit(this);
        sb.append(" }");
    }

    @Override
    public void visitDefaultMethod(DefaultMethod m) {
        Pair<String[],String> desc = Util.parseDesc(m.desc());

        sb.append(format(
                "default %s %s %s(%s)",
                printAcc(m.acc()),
                Util.decodeClassName(desc.second),
                m.name(),
                Util.intersperse(", ", desc.first)));

        if (m.sig() != null) {
            sb.append("<").append(m.sig()).append(">");
        }

        sb.append(" { ");
        m.body().visit(this);
        sb.append(" }");
    }

    /* ====================================================================== */

    @Override
    public void visitThrowExBody(ThrowExBody body) {
        sb.append(String.format(
                "throw new %s();", body.getExc().name()));
    }

    @Override
    public void visitReturnIntBody(ReturnIntBody body) {
        sb.append(String.format(
                "return %d;", body.getValue()));
    }

    @Override
    public void visitReturnNullBody(ReturnNullBody body) {
        sb.append("return null;");
    }

    @Override
    public void visitEmptyBody(EmptyBody aThis) {
    }

    /* ====================================================================== */

    @Override
    public void visitResultIgnore() {
        sb.append("/* result ignored */");
    }

    @Override
    public void visitResultInt(IntResult res) {
        sb.append("== ").append(res.getExpected());
    }

    @Override
    public void visitResultThrowExc(ThrowExResult res) {
        sb.append(String.format(
                "throws %s%s",
                abbreviateExcName(res.getExc().name()),
                res.getMessage() != null ? "(\"" + res.getMessage() + "\")"  : ""));
    }

    private String abbreviateExcName(String name) {
        switch(name) {
            case "java.lang.AbstractMethodError" : return "AME";
            case "java.lang.NoSuchMethodError"   : return "NSME";
            default: return name.replaceAll("java\\.lang\\.", "");
        }
    }
    /* ====================================================================== */

    @Override
    public void visitParamInt(IntParam param) {
        sb.append(param.value());
    }

    @Override
    public void visitParamString(StringParam param) {
        sb.append(param.value());
    }

    @Override
    public void visitParamNull() {
        sb.append("null");
    }
    @Override
    public void visitParamLong(LongParam param) {
        sb.append(param.value());
    }

    @Override
    public void visitParamFloat(FloatParam param) {
        sb.append(param.value());
    }

    @Override
    public void visitParamDouble(DoubleParam param) {
        sb.append(param.value());
    }

    @Override
    public void visitParamNewInstance(NewInstanceParam param) {
        sb.append(String.format(
                "new %s()",
                param.clazz().name()));
    }

    @Override
    public void visitCallMethod(CallMethod call) {
        String[] paramTypes = Util.parseDesc(call.methodDesc()).first;

        if (paramTypes.length != call.params().length) {
            throw new IllegalStateException();
        }

        //sb.append("{ ");
        if (!call.popReturnValue()) {
            sb.append("return ");
        }

        switch (call.invokeInsn()) {
            case VIRTUAL: case INTERFACE:
                sb.append(String.format(
                        "((%s)%s).%s(",
                        call.staticClass().name(),
                        call.receiverClass() != null ? call.receiverClass().name() : "this",
                        call.methodName()));
                break;
            case STATIC:
                sb.append(String.format(
                        "%s.%s(",
                        call.staticClass().name(),
                        call.methodName()));
                break;
            case SPECIAL:
                sb.append(String.format(
                        "%s.super.%s(",
                        call.staticClass().name(),
                        call.methodName()));
                break;
            default:
                throw new IllegalStateException();
        }

        for (int i = 0; i<paramTypes.length; i++) {
            sb.append(String.format(
                    "(%s)", paramTypes[i]));

            call.params()[i].visit(this);

            if (i+1 < paramTypes.length) {
                sb.append(", ");
            }
        }

        //sb.append("); }");
        sb.append(");");
    }

    @Override
    public void visitReturnNewInstanceBody(ReturnNewInstanceBody body) {
        sb.append(String.format(
                        "return new %s();",
                        body.getType().name()));
    }
}
