/*
 * Copyright (c) 2016 Red Hat Inc.
 *
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

/**
 *  @test
 *  @bug 8153711
 *  @summary JDWP: Memory Leak (global references not deleted after invokeMethod).
 *
 *  @author Severin Gehwolf <sgehwolf@redhat.com>
 *
 *  @requires vm.gc != "Z"
 *  @library ..
 *  @run build TestScaffold VMConnection TargetListener TargetAdapter
 *  @run compile -g OomDebugTest.java
 *  @run main OomDebugTest OomDebugTestTarget test1
 *  @run main OomDebugTest OomDebugTestTarget test2
 *  @run main OomDebugTest OomDebugTestTarget test3
 *  @run main OomDebugTest OomDebugTestTarget test4
 *  @run main OomDebugTest OomDebugTestTarget test5
 */
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.InvocationException;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.VMOutOfMemoryException;
import com.sun.jdi.Value;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ExceptionEvent;

/***************** Target program **********************/

class OomDebugTestTarget {

    OomDebugTestTarget() {
        System.out.println("DEBUG: invoked constructor");
    }
    static class FooCls {
        @SuppressWarnings("unused")
        private byte[] bytes = new byte[3000000];
    };

    FooCls fooCls = new FooCls();
    byte[] byteArray = new byte[0];

    void testMethod(FooCls foo) {
        System.out.println("DEBUG: invoked 'void testMethod(FooCls)', foo == " + foo);
    }

    void testPrimitive(byte[] foo) {
        System.out.println("DEBUG: invoked 'void testPrimitive(byte[])', foo == " + foo);
    }

    byte[] testPrimitiveArrRetval() {
        System.out.println("DEBUG: invoked 'byte[] testPrimitiveArrRetval()'");
        return new byte[3000000];
    }

    FooCls testFooClsRetval() {
        System.out.println("DEBUG: invoked 'FooCls testFooClsRetval()'");
        return new FooCls();
    }

    public void entry() {}

    public static void main(String[] args){
        System.out.println("DEBUG: OomDebugTestTarget.main");
        new OomDebugTestTarget().entry();
    }
}

/***************** Test program ************************/

public class OomDebugTest extends TestScaffold {

    private static final String[] ALL_TESTS = new String[] {
            "test1", "test2", "test3", "test4", "test5"
    };
    private static final Set<String> ALL_TESTS_SET = new HashSet<String>();
    static {
        ALL_TESTS_SET.addAll(Arrays.asList(ALL_TESTS));
    }
    private static final String TEST_CLASSES = System.getProperty("test.classes", ".");
    private static final File RESULT_FILE = new File(TEST_CLASSES, "results.properties");
    private static final String LAST_TEST = ALL_TESTS[ALL_TESTS.length - 1];
    private ReferenceType targetClass;
    private ObjectReference thisObject;
    private int failedTests;
    private final String testMethod;

    public OomDebugTest(String[] args) {
        super(args);
        if (args.length != 2) {
            throw new RuntimeException("Test failed unexpectedly.");
        }
        this.testMethod = args[1];
    }

    @Override
    protected void runTests() throws Exception {
        try {
            addListener(new TargetAdapter() {

                @Override
                public void exceptionThrown(ExceptionEvent event) {
                    String name = event.exception().referenceType().name();
                    System.err.println("DEBUG: Exception thrown in debuggee was: " + name);
                }
            });
            /*
             * Get to the top of entry()
             * to determine targetClass and mainThread
             */
            BreakpointEvent bpe = startTo("OomDebugTestTarget", "entry", "()V");
            targetClass = bpe.location().declaringType();

            mainThread = bpe.thread();

            StackFrame frame = mainThread.frame(0);
            thisObject = frame.thisObject();
            java.lang.reflect.Method m = findTestMethod();
            m.invoke(this);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            failure();
        } catch (SecurityException e) {
            e.printStackTrace();
            failure();
        }
        /*
         * resume the target, listening for events
         */
        listenUntilVMDisconnect();
    }

    private java.lang.reflect.Method findTestMethod()
            throws NoSuchMethodException, SecurityException {
        return OomDebugTest.class.getDeclaredMethod(testMethod);
    }

    private void failure() {
        failedTests++;
    }

    /*
     * Test case: Object reference as method parameter.
     */
    @SuppressWarnings("unused") // called via reflection
    private void test1() throws Exception {
        System.out.println("DEBUG: ------------> Running test1");
        try {
            Field field = targetClass.fieldByName("fooCls");
            ClassType clsType = (ClassType)field.type();
            Method constructor = getConstructorForClass(clsType);
            for (int i = 0; i < 15; i++) {
                @SuppressWarnings({ "rawtypes", "unchecked" })
                ObjectReference objRef = clsType.newInstance(mainThread,
                                                             constructor,
                                                             new ArrayList(0),
                                                             ObjectReference.INVOKE_NONVIRTUAL);
                if (objRef.isCollected()) {
                    System.out.println("DEBUG: Object got GC'ed before we can use it. NO-OP.");
                    continue;
                }
                invoke("testMethod", "(LOomDebugTestTarget$FooCls;)V", objRef);
            }
        } catch (InvocationException e) {
            handleFailure(e);
        }
    }

    /*
     * Test case: Array reference as method parameter.
     */
    @SuppressWarnings("unused") // called via reflection
    private void test2() throws Exception {
        System.out.println("DEBUG: ------------> Running test2");
        try {
            Field field = targetClass.fieldByName("byteArray");
            ArrayType arrType = (ArrayType)field.type();

            for (int i = 0; i < 15; i++) {
                ArrayReference byteArrayVal = arrType.newInstance(3000000);
                if (byteArrayVal.isCollected()) {
                    System.out.println("DEBUG: Object got GC'ed before we can use it. NO-OP.");
                    continue;
                }
                invoke("testPrimitive", "([B)V", byteArrayVal);
            }
        } catch (VMOutOfMemoryException e) {
            defaultHandleOOMFailure(e);
        }
    }

    /*
     * Test case: Array reference as return value.
     */
    @SuppressWarnings("unused") // called via reflection
    private void test3() throws Exception {
        System.out.println("DEBUG: ------------> Running test3");
        try {
            for (int i = 0; i < 15; i++) {
                invoke("testPrimitiveArrRetval",
                       "()[B",
                       Collections.EMPTY_LIST,
                       vm().mirrorOfVoid());
            }
        } catch (InvocationException e) {
            handleFailure(e);
        }
    }

    /*
     * Test case: Object reference as return value.
     */
    @SuppressWarnings("unused") // called via reflection
    private void test4() throws Exception {
        System.out.println("DEBUG: ------------> Running test4");
        try {
            for (int i = 0; i < 15; i++) {
                invoke("testFooClsRetval",
                       "()LOomDebugTestTarget$FooCls;",
                       Collections.EMPTY_LIST,
                       vm().mirrorOfVoid());
            }
        } catch (InvocationException e) {
            handleFailure(e);
        }
    }

    /*
     * Test case: Constructor
     */
    @SuppressWarnings({ "unused", "unchecked", "rawtypes" }) // called via reflection
    private void test5() throws Exception {
        System.out.println("DEBUG: ------------> Running test5");
        try {
            ClassType type = (ClassType)thisObject.type();
            for (int i = 0; i < 15; i++) {
                type.newInstance(mainThread,
                                 findMethod(targetClass, "<init>", "()V"),
                                 new ArrayList(0),
                                 ObjectReference.INVOKE_NONVIRTUAL);
            }
        } catch (InvocationException e) {
            handleFailure(e);
        }
    }

    private Method getConstructorForClass(ClassType clsType) {
        List<Method> methods = clsType.methodsByName("<init>");
        if (methods.size() != 1) {
            throw new RuntimeException("FAIL. Expected only one, the default, constructor");
        }
        return methods.get(0);
    }

    private void handleFailure(InvocationException e) {
        // There is no good way to see the OOME diagnostic message in the target since the
        // TestScaffold might throw an exception while trying to print the stack trace. I.e
        // it might get a a VMDisconnectedException before the stack trace printing finishes.
        System.err.println("FAILURE: InvocationException thrown. Trying to determine cause...");
        defaultHandleOOMFailure(e);
    }

    private void defaultHandleOOMFailure(Exception e) {
        e.printStackTrace();
        failure();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void invoke(String methodName, String methodSig, Value value)
            throws Exception {
        List args = new ArrayList(1);
        args.add(value);
        invoke(methodName, methodSig, args, value);
    }

    void invoke(String methodName,
                String methodSig,
                @SuppressWarnings("rawtypes") List args,
                Value value) throws Exception {
        Method method = findMethod(targetClass, methodName, methodSig);
        if ( method == null) {
            failure("FAILED: Can't find method: "
                    + methodName  + " for class = " + targetClass);
            return;
        }
        invoke(method, args, value);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    void invoke(Method method, List args, Value value) throws Exception {
        thisObject.invokeMethod(mainThread, method, args, 0);
        System.out.println("DEBUG: Done invoking method via debugger.");
    }

    Value fieldValue(String fieldName) {
        Field field = targetClass.fieldByName(fieldName);
        return thisObject.getValue(field);
    }

    // Determine the pass/fail status on some heuristic and don't fail the
    // test if < 3 of the total number of tests (currently 5) fail. This also
    // has the nice side effect that all tests are first attempted and only
    // all tests ran an overall pass/fail status is determined.
    private static void determineOverallTestStatus(OomDebugTest oomTest)
                                   throws IOException, FileNotFoundException {
        Properties resultProps = new Properties();
        if (!RESULT_FILE.exists()) {
            RESULT_FILE.createNewFile();
        }
        FileInputStream fin = null;
        try {
            fin = new FileInputStream(RESULT_FILE);
            resultProps.load(fin);
            resultProps.put(oomTest.testMethod,
                            Integer.toString(oomTest.failedTests));
        } finally {
            if (fin != null) {
                fin.close();
            }
        }
        System.out.println("DEBUG: Finished running test '"
                           + oomTest.testMethod + "'.");
        if (LAST_TEST.equals(oomTest.testMethod)) {
            System.out.println("DEBUG: Determining overall test status.");
            Set<String> actualTestsRun = new HashSet<String>();
            int totalTests = ALL_TESTS.length;
            int failedTests = 0;
            for (Object key: resultProps.keySet()) {
                actualTestsRun.add((String)key);
                Object propVal = resultProps.get(key);
                int value = Integer.parseInt((String)propVal);
                failedTests += value;
            }
            if (!ALL_TESTS_SET.equals(actualTestsRun)) {
                String errorMsg = "Test failed! Expected to run tests '"
                        + ALL_TESTS_SET + "', but only these were run '"
                        + actualTestsRun + "'";
                throw new RuntimeException(errorMsg);
            }
            if (failedTests >= 3) {
                String errorMsg = "Test failed. Expected < 3 sub-tests to fail "
                                  + "for a pass. Got " + failedTests
                                  + " failed tests out of " + totalTests + ".";
                throw new RuntimeException(errorMsg);
            }
            RESULT_FILE.delete();
            System.out.println("All " + totalTests + " tests passed.");
        } else {
            System.out.println("DEBUG: More tests to run. Coninuing.");
            FileOutputStream fout = null;
            try {
                fout = new FileOutputStream(RESULT_FILE);
                resultProps.store(fout, "Storing results after test "
                                         + oomTest.testMethod);
            } finally {
                if (fout != null) {
                    fout.close();
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("test.vm.opts", "-Xmx40m"); // Set debuggee VM option
        OomDebugTest oomTest = new OomDebugTest(args);
        try {
            oomTest.startTests();
        } catch (Throwable e) {
            System.out.println("DEBUG: Got exception for test run. " + e);
            e.printStackTrace();
            oomTest.failure();
        }
        determineOverallTestStatus(oomTest);
    }

}
