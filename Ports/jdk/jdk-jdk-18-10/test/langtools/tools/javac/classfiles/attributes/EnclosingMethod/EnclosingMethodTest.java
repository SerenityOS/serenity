/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8042931 8215470
 * @summary Checking EnclosingMethod attribute of anonymous/local class.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @run main EnclosingMethodTest
 */

import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.EnclosingMethod_attribute;

import java.io.File;
import java.io.FilenameFilter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * The test checks the enclosing method attribute of anonymous/local classes.
 * The top-level class contains the anonymous and local classes to be tested. The test examines
 * each inner class and determine whether the class should have the EnclosingMethod attribute or not.
 * Golden information about enclosing methods are held in annotation {@code ExpectedEnclosingMethod}.
 *
 * The test assumes that a class must have the EnclosingMethod attribute if the class is annotated or
 * if its parent class is annotated in case of anonymous class. In addition, classes
 * named {@code VariableInitializer} are introduced to test variable initializer cases. These classes
 * must not have the enclosing method attribute, but its anonymous derived class must.
 * After classification of classes, the test checks whether classes contain the correct enclosing
 * method attribute in case of anonymous/local class, or checks whether classes do not contain
 * the EnclosingMethod attribute, otherwise.
 *
 * Test cases:
 *   top-level class as enclosing class:
 *     1. anonymous and local classes in static initializer;
 *     2. anonymous and local classes in instance initializer;
 *     3. anonymous and local classes in lambda;
 *     4. anonymous and local classes in constructor;
 *     5. anonymous and local classes in method;
 *     6. static and instance variable initializer.
 *
 *   inner class as enclosing class:
 *     1. anonymous and local classes in static initializer;
 *     2. anonymous and local classes in instance initializer;
 *     3. anonymous and local classes in lambda;
 *     4. anonymous and local classes in constructor;
 *     5. anonymous and local classes in method;
 *     6. static and instance variable initializer.
 *
 *   enum as enclosing class:
 *     1. anonymous and local classes in static initializer;
 *     2. anonymous and local classes in instance initializer;
 *     3. anonymous and local classes in lambda;
 *     4. anonymous and local classes in constructor;
 *     5. anonymous and local classes in method;
 *     6. static and instance variable initializer.
 *
 *   interface as enclosing class:
 *     1. anonymous and local classes in lambda;
 *     2. anonymous and local classes in static method;
 *     3. anonymous and local classes in default method;
 *     4. static variable initializer.
 *
 *   annotation as enclosing class:
 *     1. anonymous and local classes in lambda;
 *     2. static variable initializer.
 */
public class EnclosingMethodTest extends TestResult {

    private final Map<Class<?>, ExpectedEnclosingMethod> class2EnclosingMethod = new HashMap<>();
    private final Set<Class<?>> noEnclosingMethod = new HashSet<>();

    public EnclosingMethodTest() throws ClassNotFoundException {
        Class<EnclosingMethodTest> outerClass = EnclosingMethodTest.class;
        String outerClassName = outerClass.getSimpleName();
        File testClasses = getClassDir();
        FilenameFilter filter = (dir, name) -> name.matches(outerClassName + ".*\\.class");

        for (File file : testClasses.listFiles(filter)) {
            Class<?> clazz = Class.forName(file.getName().replace(".class", ""));
            if (clazz.isAnonymousClass()) {
                // anonymous class cannot be annotated, information is in its parent class.
                ExpectedEnclosingMethod declaredAnnotation =
                        clazz.getSuperclass().getDeclaredAnnotation(ExpectedEnclosingMethod.class);
                class2EnclosingMethod.put(clazz, declaredAnnotation);
            } else {
                ExpectedEnclosingMethod enclosingMethod = clazz.getDeclaredAnnotation(ExpectedEnclosingMethod.class);
                // if class is annotated and it does not contain information for variable initializer cases,
                // then it must have the enclosing method attribute.
                if (enclosingMethod != null && !clazz.getSimpleName().contains("VariableInitializer")) {
                    class2EnclosingMethod.put(clazz, enclosingMethod);
                } else {
                    noEnclosingMethod.add(clazz);
                }
            }
        }
    }

    public void test() throws TestFailedException {
        try {
            testEnclosingMethodAttribute();
            testLackOfEnclosingMethodAttribute();
        } finally {
            checkStatus();
        }
    }

    private void testLackOfEnclosingMethodAttribute() {
        for (Class<?> clazz : noEnclosingMethod) {
            try {
                addTestCase("Class should not have EnclosingMethod attribute : " + clazz);
                ClassFile classFile = readClassFile(clazz);
                checkEquals(countEnclosingMethodAttributes(classFile),
                        0l, "number of the EnclosingMethod attribute in the class is zero : "
                                + classFile.getName());
            } catch (Exception e) {
                addFailure(e);
            }
        }
    }

    private void testEnclosingMethodAttribute() {
        class2EnclosingMethod.forEach((clazz, enclosingMethod) -> {
            try {
                String info = enclosingMethod.info() + " "
                        + (clazz.isAnonymousClass() ? "anonymous" : "local");
                addTestCase(info);
                printf("Testing test case : %s\n", info);
                ClassFile classFile = readClassFile(clazz);
                String className = clazz.getName();
                checkEquals(countEnclosingMethodAttributes(classFile), 1l,
                        "number of the EnclosingMethod attribute in the class is one : "
                                + clazz);
                EnclosingMethod_attribute attr = (EnclosingMethod_attribute)
                        classFile.getAttribute(Attribute.EnclosingMethod);

                if (!checkNotNull(attr, "the EnclosingMethod attribute is not null : " + className)) {
                    // stop checking, attr is null. test case failed
                    return;
                }
                checkEquals(classFile.constant_pool.getUTF8Value(attr.attribute_name_index),
                        "EnclosingMethod",
                        "attribute_name_index of EnclosingMethod attribute in the class : " + className);
                checkEquals(attr.attribute_length, 4,
                        "attribute_length of EnclosingMethod attribute in the class : " + className);
                String expectedClassName = enclosingMethod.enclosingClazz().getName();
                checkEquals(classFile.constant_pool.getClassInfo(attr.class_index).getName(),
                        expectedClassName, String.format(
                        "enclosing class of EnclosingMethod attribute in the class %s is %s",
                                className, expectedClassName));

                String expectedMethodName = enclosingMethod.enclosingMethod();
                if (expectedMethodName.isEmpty()) {
                    // class does not have an enclosing method
                    checkEquals(attr.method_index, 0, String.format(
                            "enclosing method of EnclosingMethod attribute in the class %s is null", className));
                } else {
                    String methodName = classFile.constant_pool.getNameAndTypeInfo(attr.method_index).getName();
                    checkTrue(methodName.startsWith(expectedMethodName), String.format(
                            "enclosing method of EnclosingMethod attribute in the class %s" +
                                    " is method name %s" +
                                    ", actual method name is %s",
                            className, expectedMethodName, methodName));
                }
            } catch (Exception e) {
                addFailure(e);
            }
        });
    }

    private long countEnclosingMethodAttributes(ClassFile classFile) {
        return Stream.of(classFile.attributes.attrs)
                .filter(x -> x instanceof EnclosingMethod_attribute)
                .count();
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface ExpectedEnclosingMethod {
        String info();
        Class<?> enclosingClazz();
        String enclosingMethod() default "";
    }

    public static void main(String[] args) throws ClassNotFoundException, TestFailedException {
        new EnclosingMethodTest().test();
    }

    // Test cases: enclosing class is a top-level class
    static {
        // anonymous and local classes in static initializer
        @ExpectedEnclosingMethod(
                info = "EnclosingStaticInitialization in EnclosingMethodTest",
                enclosingClazz = EnclosingMethodTest.class
        )
        class EnclosingStaticInitialization {
        }
        new EnclosingStaticInitialization() {
        };
    }

    {
        // anonymous and local classes in instance initializer
        @ExpectedEnclosingMethod(
                info = "EnclosingInitialization in EnclosingMethodTest",
                enclosingClazz = EnclosingMethodTest.class
        )
        class EnclosingInitialization {
        }
        new EnclosingInitialization() {
        };
    }

    Runnable lambda = () -> {
        // anonymous and local classes in lambda
        @ExpectedEnclosingMethod(
                info = "EnclosingLambda in EnclosingMethodTest",
                enclosingMethod = "<init>",
                enclosingClazz = EnclosingMethodTest.class
        )
        class EnclosingLambda {
        }
        new EnclosingLambda() {
        };
    };

    EnclosingMethodTest(int i) {
        // anonymous and local classes in constructor
        @ExpectedEnclosingMethod(
                info = "EnclosingConstructor in EnclosingMethodTest",
                enclosingMethod = "<init>",
                enclosingClazz = EnclosingMethodTest.class
        )
        class EnclosingConstructor {
        }
        new EnclosingConstructor() {
        };
    }

    void method() {
        // anonymous and local classes in method
        @ExpectedEnclosingMethod(
                info = "EnclosingMethod in EnclosingMethodTest",
                enclosingMethod = "method",
                enclosingClazz = EnclosingMethodTest.class
        )
        class EnclosingMethod {
        }
        new EnclosingMethod() {
        };
    }

    @ExpectedEnclosingMethod(
            info = "VariableInitializer in EnclosingMethodTest",
            enclosingClazz = EnclosingMethodTest.class
    )
    static class VariableInitializer {
    }

    // static variable initializer
    private static final VariableInitializer cvi = new VariableInitializer() {
    };

    // instance variable initializer
    private final VariableInitializer ivi = new VariableInitializer() {
    };

    // Test cases: enclosing class is an inner class
    public static class notEnclosing01 {
        static {
            // anonymous and local classes in static initializer
            @ExpectedEnclosingMethod(
                    info = "EnclosingStaticInitialization in notEnclosing01",
                    enclosingClazz = notEnclosing01.class
            )
            class EnclosingStaticInitialization {
            }
            new EnclosingStaticInitialization() {
            };
        }

        {
            // anonymous and local classes in instance initializer
            @ExpectedEnclosingMethod(
                    info = "EnclosingInitialization in notEnclosing01",
                    enclosingClazz = notEnclosing01.class
            )
            class EnclosingInitialization {
            }
            new EnclosingInitialization() {
            };
        }

        Runnable lambda = () -> {
            // anonymous and local classes in lambda
            @ExpectedEnclosingMethod(
                    info = "EnclosingLambda in notEnclosing01",
                    enclosingMethod = "<init>",
                    enclosingClazz = notEnclosing01.class
            )
            class EnclosingLambda {
            }
            new EnclosingLambda() {
            };
        };

        notEnclosing01() {
            // anonymous and local classes in constructor
            @ExpectedEnclosingMethod(
                    info = "EnclosingConstructor in notEnclosing01",
                    enclosingMethod = "<init>",
                    enclosingClazz = notEnclosing01.class
            )
            class EnclosingConstructor {
            }
            new EnclosingConstructor() {
            };
        }

        void method() {
            // anonymous and local classes in method
            @ExpectedEnclosingMethod(
                    info = "EnclosingMethod in notEnclosing01",
                    enclosingMethod = "method",
                    enclosingClazz = notEnclosing01.class
            )
            class EnclosingMethod {
            }
            new EnclosingMethod() {
            };
        }

        @ExpectedEnclosingMethod(
                info = "VariableInitializer in notEnclosing01",
                enclosingClazz = notEnclosing01.class
        )
        static class VariableInitializer {
        }

        // static variable initializer
        private static final VariableInitializer cvi = new VariableInitializer() {
        };

        // instance variable initializer
        private final VariableInitializer ivi = new VariableInitializer() {
        };
    }

    // Test cases: enclosing class is an interface
    public interface notEnclosing02 {
        Runnable lambda = () -> {
            // anonymous and local classes in lambda
            @ExpectedEnclosingMethod(
                    info = "EnclosingLambda in notEnclosing02",
                    enclosingMethod = "<clinit>",
                    enclosingClazz = notEnclosing02.class
            )
            class EnclosingLambda {
            }
            new EnclosingLambda() {
            };
        };

        static void staticMethod() {
            // anonymous and local classes in static method
            @ExpectedEnclosingMethod(
                    info = "EnclosingMethod in notEnclosing02",
                    enclosingMethod = "staticMethod",
                    enclosingClazz = notEnclosing02.class
            )
            class EnclosingMethod {
            }
            new EnclosingMethod() {
            };
        }

        default void defaultMethod() {
            // anonymous and local classes in default method
            @ExpectedEnclosingMethod(
                    info = "EnclosingMethod in notEnclosing02",
                    enclosingMethod = "defaultMethod",
                    enclosingClazz = notEnclosing02.class
            )
            class EnclosingMethod {
            }
            new EnclosingMethod() {
            };
        }

        @ExpectedEnclosingMethod(
                info = "VariableInitializer in notEnclosing02",
                enclosingClazz = notEnclosing02.class
        )
        static class VariableInitializer {
        }

        // static variable initializer
        VariableInitializer cvi = new VariableInitializer() {
        };
    }

    // Test cases: enclosing class is an enum
    public enum notEnclosing03 {;

        static {
            // anonymous and local classes in static initializer
            @ExpectedEnclosingMethod(
                    info = "EnclosingStaticInitialization in notEnclosing03",
                    enclosingClazz = notEnclosing03.class
            )
            class EnclosingStaticInitialization {
            }
            new EnclosingStaticInitialization() {
            };
        }

        {
            // anonymous and local classes in instance initializer
            @ExpectedEnclosingMethod(
                    info = "EnclosingInitialization in notEnclosing03",
                    enclosingClazz = notEnclosing03.class
            )
            class EnclosingInitialization {
            }
            new EnclosingInitialization() {
            };
        }

        Runnable lambda = () -> {
            // anonymous and local classes in lambda
            @ExpectedEnclosingMethod(
                    info = "EnclosingLambda in notEnclosing03",
                    enclosingMethod = "<init>",
                    enclosingClazz = notEnclosing03.class
            )
            class EnclosingLambda {
            }
            new EnclosingLambda() {
            };
        };

        notEnclosing03() {
            // anonymous and local classes in constructor
            @ExpectedEnclosingMethod(
                    info = "EnclosingConstructor in notEnclosing03",
                    enclosingMethod = "<init>",
                    enclosingClazz = notEnclosing03.class
            )
            class EnclosingConstructor {
            }
            new EnclosingConstructor() {
            };
        }

        void method() {
            // anonymous and local classes in method
            @ExpectedEnclosingMethod(
                    info = "EnclosingMethod in notEnclosing03",
                    enclosingMethod = "method",
                    enclosingClazz = notEnclosing03.class
            )
            class EnclosingMethod {
            }
            new EnclosingMethod() {
            };
        }

        @ExpectedEnclosingMethod(
                info = "VariableInitializer in notEnclosing03",
                enclosingClazz = notEnclosing03.class
        )
        static class VariableInitializer {
        }

        // static variable initializer
        private static final VariableInitializer cvi = new VariableInitializer() {
        };

        // instance variable initializer
        private final VariableInitializer ivi = new VariableInitializer() {
        };
    }

    // Test cases: enclosing class is an annotation
    public @interface notEnclosing04 {
        Runnable lambda = () -> {
            // anonymous and local classes in lambda
            @ExpectedEnclosingMethod(
                    info = "EnclosingLambda in notEnclosing04",
                    enclosingMethod = "<clinit>",
                    enclosingClazz = notEnclosing04.class
            )
            class EnclosingLambda {
            }
            new EnclosingLambda() {
            };
        };

        @ExpectedEnclosingMethod(
                info = "VariableInitializer in notEnclosing04",
                enclosingClazz = notEnclosing04.class
        )
        static class VariableInitializer {
        }

        // static variable initializer
        VariableInitializer cvi = new VariableInitializer() {
        };
    }
}
