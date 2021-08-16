/*
 * Copyright (c) 2002, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4509267
 * @summary generics: parametric exception type versus overriding
 * @author gafter
 *
 * @compile  ParametricException.java
 */

import java.io.*;

abstract class AChurchBoolean {
    public abstract <Return, Parameter, Throws extends Throwable>
    Return accept(IVisitor<Return, Parameter, Throws> visitor, Parameter parameter) throws Throws;

    public interface IVisitor<Return, Parameter, Throws extends Throwable> {
        public Return caseTrue(Parameter parameter) throws Throws;
        public Return caseFalse(Parameter parameter) throws Throws;
    }
}

class TrueChurchBoolean extends AChurchBoolean {
    private static TrueChurchBoolean instance = new TrueChurchBoolean();
    private TrueChurchBoolean() {}
    public static TrueChurchBoolean singleton() {
        return instance;
    }
    public <Return, Parameter, Throws extends Throwable>
    Return accept(IVisitor<Return, Parameter, Throws> visitor, Parameter parameter) throws Throws {
        return visitor.caseTrue(parameter);
    }
}

class FalseChurchBoolean extends AChurchBoolean {
    private static FalseChurchBoolean instance = new FalseChurchBoolean();
    private FalseChurchBoolean() {}
    public static FalseChurchBoolean singleton() {
        return instance;
    }
    public <Return, Parameter, Throws extends Throwable>
    Return accept(IVisitor<Return, Parameter, Throws> visitor, Parameter parameter) throws Throws {
        return visitor.caseFalse(parameter);
    }
}

class Pair<T,U> {
    private T first;
    private U second;
    Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }
    T getFirst() {
        return first;
    }
    U getSecond() {
        return second;
    }
}

// Perhaps a bit of a toy example, but relevant nonetheless.
class ChurchBooleanTest {
    private AChurchBoolean bool;
    public ChurchBooleanTest(AChurchBoolean bool) {
        this.bool = bool;
    }
    public AChurchBoolean readIf(File file, byte[] output) throws IOException {
        return bool.accept(new AChurchBoolean.IVisitor<AChurchBoolean, Pair<File, byte[]>, IOException>() {
            public AChurchBoolean caseTrue(Pair<File, byte[]> parameter) throws IOException {
                FileInputStream input = new FileInputStream(parameter.getFirst()); // throws
                input.read(parameter.getSecond()); // throws
                input.close(); // throws
                return TrueChurchBoolean.singleton();
            }
            public AChurchBoolean caseFalse(Pair<File, byte[]> parameter) throws IOException {
                return FalseChurchBoolean.singleton();
            }
        }, new Pair<File, byte[]>(file, output));
    }
}
