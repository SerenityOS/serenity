/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collection;
import java.util.stream.Collectors;

public enum  ClassType {
    CLASS("class"),
    INTERFACE("interface") {
        @Override
        public String methodToString(TestCase.TestMethodInfo method) {
            String modifiers = method.mods.stream()
                    .collect(Collectors.joining(" "));
            boolean hasBody = modifiers.contains("static") || modifiers.contains("default");
            String parameters = method.parameters.stream()
                    .map(TestCase.TestMemberInfo::generateSource)
                    .collect(Collectors.joining(", "));
            return String.format("%s %s %s(%s) %s",
                    method.indention() + modifiers,
                    "int",
                    method.getName(),
                    parameters,
                    hasBody ? "{return 0;}" : ";");
        }
    },
    ANNOTATION("@interface") {
        @Override
        public String methodToString(TestCase.TestMethodInfo method) {
            String modifiers = method.mods.stream()
                    .collect(Collectors.joining(" "));
            return String.format("%s %s %s() %s",
                    method.indention() + modifiers,
                    "int",
                    method.getName(),
                    ";");
        }
    },
    ENUM("enum") {
        @Override
        public String fieldToString(TestCase.TestFieldInfo field) {
            return field.indention() + field.name;
        }

        @Override
        public String collectFields(Collection<TestCase.TestFieldInfo> fields) {
            return fields.stream()
                    .map(TestCase.TestMemberInfo::generateSource)
                    .collect(Collectors.joining(",\n")) + ";\n";
        }
    };

    private final String classType;

    ClassType(String classType) {
        this.classType = classType;
    }

    private String collectSrc(Collection<? extends TestCase.TestMemberInfo> members) {
        String src = members.stream()
                .map(TestCase.TestMemberInfo::generateSource)
                .collect(Collectors.joining("\n"));
        return src.trim().isEmpty() ? "" : src + "\n\n";
    }

    public String collectInnerClasses(Collection<TestCase.TestClassInfo> innerClasses) {
        return collectSrc(innerClasses);
    }

    public String collectFields(Collection<TestCase.TestFieldInfo> fields) {
        return collectSrc(fields);
    }

    public String collectMethods(Collection<TestCase.TestMethodInfo> methods) {
        return collectSrc(methods);
    }

    public String methodToString(TestCase.TestMethodInfo method) {
        String modifiers = method.mods.stream()
                .collect(Collectors.joining(" "));
        String parameters = method.parameters.stream()
                .map(TestCase.TestMemberInfo::generateSource)
                .collect(Collectors.joining(", "));
        String localClasses = collectInnerClasses(method.localClasses.values());
        String methodBody = modifiers.contains("abstract") ? ";" :
                String.format("{%n%s%s%n%s}",
                        localClasses,
                        method.isConstructor
                                ? ""
                                : method.indention() + "return false;",
                        method.indention());
        return String.format("%s %s %s(%s) %s",
                method.indention() + modifiers,
                method.isConstructor ? "" : "boolean",
                method.getName(),
                parameters,
                methodBody);
    }

    public String fieldToString(TestCase.TestFieldInfo field) {
        String modifiers = field.mods.stream()
                .collect(Collectors.joining(" "));
        return String.format("%s int %s = 0;",
                field.indention() + modifiers,
                field.name);
    }

    public String getDescription() {
        return classType;
    }
}
