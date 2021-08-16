/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8042251
 * @summary Test that inner classes have in its inner classes attribute enclosing classes and its immediate members.
 * @library /tools/lib /tools/javac/lib ../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox InMemoryFileManager TestResult TestBase
 * @run main InnerClassesHierarchyTest
 */

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

import com.sun.tools.classfile.*;
import com.sun.tools.classfile.InnerClasses_attribute.Info;

public class InnerClassesHierarchyTest extends TestResult {

    private final Map<String, Set<String>> innerClasses;
    private final String outerClassName;

    public InnerClassesHierarchyTest() throws IOException, ConstantPoolException {
        innerClasses = new HashMap<>();
        outerClassName = InnerClassesHierarchyTest.class.getSimpleName();
        File classDir = getClassDir();
        FilenameFilter filter =
                (dir, name) -> name.matches(outerClassName + ".*\\.class");
        for (File file : Arrays.asList(classDir.listFiles(filter))) {
            ClassFile classFile = readClassFile(file);
            String className = classFile.getName();
            for (ConstantPool.CPInfo info : classFile.constant_pool.entries()) {
                if (info instanceof ConstantPool.CONSTANT_Class_info) {
                    ConstantPool.CONSTANT_Class_info classInfo =
                            (ConstantPool.CONSTANT_Class_info) info;
                    String cpClassName = classInfo.getBaseName();
                    if (isInnerClass(cpClassName)) {
                        get(className).add(cpClassName);
                    }
                }
            }
        }
    }

    private boolean isInnerClass(String cpClassName) {
        return cpClassName.contains("$");
    }

    private Set<String> get(String className) {
        if (!innerClasses.containsKey(className)) {
            innerClasses.put(className, new HashSet<>());
        }
        return innerClasses.get(className);
    }

    public static void main(String[] args) throws IOException, ConstantPoolException, TestFailedException {
        new InnerClassesHierarchyTest().test();
    }

    private void test() throws TestFailedException {
        addTestCase("Source file is InnerClassesHierarchyTest.java");
        try {
            Queue<String> queue = new LinkedList<>();
            Set<String> visitedClasses = new HashSet<>();
            queue.add(outerClassName);
            while (!queue.isEmpty()) {
                String currentClassName = queue.poll();
                if (!currentClassName.startsWith(outerClassName)) {
                    continue;
                }
                ClassFile cf = readClassFile(currentClassName);
                InnerClasses_attribute attr = (InnerClasses_attribute)
                        cf.getAttribute(Attribute.InnerClasses);
                checkNotNull(attr, "Class should not contain "
                        + "inner classes attribute : " + currentClassName);
                checkTrue(innerClasses.containsKey(currentClassName),
                        "map contains class name : " + currentClassName);
                Set<String> setClasses = innerClasses.get(currentClassName);
                if (setClasses == null) {
                    continue;
                }
                checkEquals(attr.number_of_classes,
                        setClasses.size(),
                        "Check number of inner classes : " + setClasses);
                for (Info info : attr.classes) {
                    String innerClassName = info
                            .getInnerClassInfo(cf.constant_pool).getBaseName();
                    checkTrue(setClasses.contains(innerClassName),
                            currentClassName + " contains inner class : "
                                    + innerClassName);
                    if (visitedClasses.add(innerClassName)) {
                        queue.add(innerClassName);
                    }
                }
            }
            Set<String> allClasses = innerClasses.entrySet().stream()
                    .flatMap(entry -> entry.getValue().stream())
                    .collect(Collectors.toSet());

            Set<String> a_b = removeAll(visitedClasses, allClasses);
            Set<String> b_a = removeAll(allClasses, visitedClasses);
            checkEquals(visitedClasses, allClasses,
                    "All classes are found\n"
                            + "visited - all classes : " + a_b
                            + "\nall classes - visited : " + b_a);
        } catch (Exception e) {
            addFailure(e);
        } finally {
            checkStatus();
        }
    }

    private Set<String> removeAll(Set<String> set1, Set<String> set2) {
        Set<String> set = new HashSet<>(set1);
        set.removeAll(set2);
        return set;
    }

    public static class A1 {

        public class B1 {
        }

        public enum B2 {
        }

        public interface B3 {
        }

        public @interface B4 {
        }

        public void f() {
            new B1() {
            };
            new B3() {
            };
            new B4() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
            class B5 {
            }
        }

        Runnable r = () -> {
            new B1() {
            };
            new B3() {
            };
            new B4() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
            class B5 {
            }
        };
    }

    public enum A2 {;

        public class B1 {
        }

        public enum B2 {
        }

        public interface B3 {
        }

        public @interface B4 {
        }

        public void a2() {
            new B1() {
            };
            new B3() {
            };
            new B4() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
            class B5 {
            }
        }

        Runnable r = () -> {
            new B1() {
            };
            new B3() {
            };
            new B4() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
            class B5 {
            }
        };
    }

    public interface A3 {

        public class B1 {
        }

        public enum B2 {
        }

        public interface B3 {
        }

        public @interface B4 {
        }

        default void a1() {
            new B1() {
            };
            new B3() {
            };
            new B4() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
            class B5 {
            }
        }

        static void a2() {
            new B1() {
            };
            new B3() {
            };
            new B4() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
            class B5 {
            }
        }
    }

    public @interface A4 {

        public class B1 {
        }

        public enum B2 {
        }

        public interface B3 {
        }

        public @interface B4 {
        }
    }

    {
        new A1() {
            class B1 {
            }

            public void a2() {
                new B1() {
                };
                class B5 {
                }
            }
        };
        new A3() {
            class B1 {
            }

            public void a3() {
                new B1() {
                };
                class B5 {
                }
            }
        };
        new A4() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            class B1 {
            }

            public void a4() {
                new B1() {
                };
                class B5 {
                }
            }
        };
        Runnable r = () -> {
            new A1() {
            };
            new A3() {
            };
            new A4() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
            class B5 {
            }
        };
    }

    static {
        new A1() {
            class B1 {
            }

            public void a2() {
                new B1() {
                };
                class B5 {
                }
            }
        };
        new A3() {
            class B1 {
            }

            public void a3() {
                new B1() {
                };
                class B5 {
                }
            }
        };
        new A4() {
            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            class B1 {
            }

            public void a4() {
                new B1() {
                };
                class B5 {
                }
            }
        };
        Runnable r = () -> {
            new A1() {
            };
            new A3() {
            };
            new A4() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
            class B5 {
            }
        };
    }

    public void a5() {
        class A5 {

            class B1 {
            }

            public void a5() {
                new B1() {
                };

                class B5 {
                }
            }
        }
        Runnable r = () -> {
            new A1() {
            };
            new A3() {
            };
            new A4() {
                @Override
                public Class<? extends Annotation> annotationType() {
                    return null;
                }
            };
            class B5 {
            }
        };
    }
}
