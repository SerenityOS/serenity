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

import java.lang.StackWalker.Option;
import java.lang.StackWalker.StackFrame;
import java.util.*;

/**
 * Utility class for recording a stack trace for later comparison to
 * StackWalker results.
 *
 * StackTraceElement comparison does not include line number, isNativeMethod
 */
public class StackRecorderUtil implements Iterable<StackRecorderUtil.TestFrame> {
    private List<TestFrame> testFrames = new LinkedList();

    private boolean compareClasses;
    private boolean compareClassNames = true;
    private boolean compareMethodNames = true;
    private boolean compareSTEs;

    public StackRecorderUtil(Set<StackWalker.Option> swOptions) {
        compareClasses = swOptions.contains(Option.RETAIN_CLASS_REFERENCE);
        compareSTEs = true;
    }

    /**
     * Add a method call to this recorded stack.
     */
    public void add(Class declaringClass, String methodName, String fileName) {
        testFrames.add(0, new TestFrame(declaringClass, methodName, fileName));
    }

    public int frameCount() { return testFrames.size(); }

    /**
     * Compare the given StackFrame returned from the StackWalker to the
     * recorded frame at the given index.
     *
     * Tests for equality, as well as functional correctness with respect to
     * the StackWalker's options (e.g. throws or doesn't throw exceptions)
     */
    public void compareFrame(int index, StackFrame sf) {
        TestFrame tf = testFrames.get(index);
        if (compareClasses) {
            if (!tf.declaringClass.equals(sf.getDeclaringClass())) {
                throw new RuntimeException("Expected class: " +
                  tf.declaringClass.toString() + ", but got: " +
                  sf.getDeclaringClass().toString());
            }
        } else {
            boolean caught = false;
            try {
                sf.getDeclaringClass();
            } catch (UnsupportedOperationException e) {
                caught = true;
            }
            if (!caught) {
                throw new RuntimeException("StackWalker did not have " +
                  "RETAIN_CLASS_REFERENCE Option, but did not throw " +
                  "UnsupportedOperationException");
            }
        }

        if (compareClassNames && !tf.className().equals(sf.getClassName())) {
            throw new RuntimeException("Expected class name: " + tf.className() +
                    ", but got: " + sf.getClassName());
        }
        if (compareMethodNames && !tf.methodName.equals(sf.getMethodName())) {
            throw new RuntimeException("Expected method name: " + tf.methodName +
                    ", but got: " + sf.getMethodName());
        }
        if (compareSTEs) {
            StackTraceElement ste = sf.toStackTraceElement();
            if (!(ste.getClassName().equals(tf.className()) &&
                  ste.getMethodName().equals(tf.methodName)) &&
                  ste.getFileName().equals(tf.fileName)) {
                throw new RuntimeException("Expected StackTraceElement info: " +
                        tf + ", but got: " + ste);
            }
            if (!Objects.equals(ste.getClassName(), sf.getClassName())
                || !Objects.equals(ste.getMethodName(), sf.getMethodName())
                || !Objects.equals(ste.getFileName(), sf.getFileName())
                || !Objects.equals(ste.getLineNumber(), sf.getLineNumber())
                || !Objects.equals(ste.isNativeMethod(), sf.isNativeMethod())) {
                throw new RuntimeException("StackFrame and StackTraceElement differ: " +
                        "sf=" + sf + ", ste=" + ste);
            }
        }
    }

    public Iterator<TestFrame> iterator() {
        return testFrames.iterator();
    }

    /**
     * Class used to record stack frame information.
     */
    public static class TestFrame {
        public Class declaringClass;
        public String methodName;
        public String fileName = null;

        public TestFrame (Class declaringClass, String methodName, String fileName) {
            this.declaringClass = declaringClass;
            this.methodName = methodName;
            this.fileName = fileName;
        }
        public String className() {
            return declaringClass.getName();
        }
        public String toString() {
            return "TestFrame: " + className() + "." + methodName +
                    (fileName == null ? "" : "(" + fileName + ")");
        }
    }
}
