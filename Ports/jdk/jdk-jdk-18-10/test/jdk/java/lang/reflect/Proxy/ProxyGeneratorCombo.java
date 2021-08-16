/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Proxy Generator Combo tests
 * @library /test/langtools/tools/javac/lib .
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build combo.ComboTestHelper
 * @run main/othervm ProxyGeneratorCombo
 */

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import combo.ComboInstance;
import combo.ComboParameter;
import combo.ComboTask.Result;
import combo.ComboTestHelper;

public class ProxyGeneratorCombo extends ComboInstance<ProxyGeneratorCombo> {

    // The unique number to qualify interface names, unique across multiple runs
    private static int uniqueId = 0;

    /**
     * Class Access kinds.
     */
    enum ClassAccessKind implements ComboParameter {
        PUBLIC("public"),
        PACKAGE(""),
        ;

        String classAccessTemplate;

        ClassAccessKind(String classAccessTemplate) {
            this.classAccessTemplate = classAccessTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return classAccessTemplate;
        }
    }

    /**
     * Signatures of methods to be tested.
     */
    enum MethodsKind implements ComboParameter {
        NONE(""),
        ZERO("#{METHODACCESS} void zero() #{EXCEPTION};"),
        ONE("#{METHODACCESS} void one(#{ARG[0]} a) #{EXCEPTION};"),
        TWO("#{METHODACCESS} void one(#{ARG[0]} b);\n" +
                "#{METHODACCESS} void two(#{ARG[0]} a, #{ARG[1]} b);"),
        THREE("#{METHODACCESS} void one(#{ARG[0]} a);\n" +
                "#{METHODACCESS} void two(#{ARG[0]} a, #{ARG[1]} b);\n" +
                "#{METHODACCESS} void three(#{ARG[0]} a, #{ARG[1]} b, #{ARG[0]} c);");

        String methodsKindTemplate;

        MethodsKind(String methodsKindTemplate) {
            this.methodsKindTemplate = methodsKindTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return methodsKindTemplate;
        }
    }

    /**
     * Type of arguments to insert in method signatures
     */
    enum ArgumentKind implements ComboParameter {
        BOOLEAN("boolean"),
        BYTE("byte"),
        CHAR("char"),
        SHORT("short"),
        INT("int"),
        LONG("long"),
        FLOAT("float"),
        DOUBLE("double"),
        STRING("String");

        String argumentsKindTemplate;

        ArgumentKind(String argumentsKindTemplate) {
            this.argumentsKindTemplate = argumentsKindTemplate;
        }

        @Override
        public String expand(String optParameter) {
            return argumentsKindTemplate;
        }
    }

    /**
     * Exceptions to be added to zero and one methods.
     */
    enum ExceptionKind implements ComboParameter {
        NONE(null),
        EXCEPTION(java.lang.Exception.class),
        RUNTIME_EXCEPTION(java.lang.RuntimeException.class),
        ILLEGAL_ARGUMENT_EXCEPTION(java.lang.IllegalArgumentException.class),
        IOEXCEPTION(java.io.IOException.class),
        /**
         * Used only for throw testing, is empty for throws clause in the source,
         */
        UNDECLARED_EXCEPTION(Exception1.class),
        ;

        Class<? extends Throwable> exceptionKindClass;

        ExceptionKind(Class<? extends Throwable> exceptionKindClass) {
            this.exceptionKindClass = exceptionKindClass;
        }

        @Override
        public String expand(String optParameter) {
            return exceptionKindClass == null || exceptionKindClass == Exception1.class
                    ? "" : "throws " + exceptionKindClass.getName();
        }
    }

    /**
     * Extra interfaces to be added.
     */
    enum MultiInterfacesKind implements ComboParameter {
        NONE(new Class<?>[0]),
        INTERFACE_WITH_EXCEPTION(new Class<?>[] {InterfaceWithException.class}),
        ;

        Class<?>[] multiInterfaceClasses;

        MultiInterfacesKind(Class<?>[] multiInterfaceClasses) {
            this.multiInterfaceClasses = multiInterfaceClasses;
        }

        @Override
        // Not used for expansion only execution
        public String expand(String optParameter) {
            throw new RuntimeException("NYI");
        }

        Class<?>[] classes() {
            return multiInterfaceClasses;
        }
    }

    @Override
    public int id() {
        return ++uniqueId;
    }

    protected void fail(String msg, Throwable thrown) {
        super.fail(msg);
        thrown.printStackTrace();
    }

    /**
     * Test interface with a "one(int)" method.
     */
    interface InterfaceWithException {
        // The signature must match the ONE MethodsKind above
        void one(int a) throws RuntimeException, IOException;
    }


    /**
     * Main to generate combinations and run the tests.
     * @param args unused
     * @throws Exception In case of failure
     */
    public static void main(String... args) throws Exception {

        // Test variations of access declarations
        new ComboTestHelper<ProxyGeneratorCombo>()
                .withDimension("CLASSACCESS", ClassAccessKind.values())
                .withDimension("METHODACCESS", new ClassAccessKind[]{ClassAccessKind.PUBLIC})
                .withDimension("METHODS", ProxyGeneratorCombo::saveMethod,
                        new MethodsKind[] {MethodsKind.NONE, MethodsKind.ZERO, MethodsKind.ONE})
                .withDimension("ARG[0]", new ArgumentKind[] {ArgumentKind.INT})
                .withDimension("EXCEPTION", ProxyGeneratorCombo::saveException,
                        new ExceptionKind[]{ExceptionKind.NONE})
                .run(ProxyGeneratorCombo::new);

        // Test variations of argument types
        new ComboTestHelper<ProxyGeneratorCombo>()
                .withDimension("CLASSACCESS", new ClassAccessKind[]{ClassAccessKind.PUBLIC})
                .withDimension("METHODACCESS", new ClassAccessKind[]{ClassAccessKind.PUBLIC})
                .withDimension("METHODS", ProxyGeneratorCombo::saveMethod,
                        MethodsKind.values())
                .withArrayDimension("ARG", ProxyGeneratorCombo::saveArg, 2,
                        ArgumentKind.values())
                .withDimension("EXCEPTION", ProxyGeneratorCombo::saveException,
                        new ExceptionKind[]{ExceptionKind.NONE})
                .withFilter(ProxyGeneratorCombo::filter)
                .run(ProxyGeneratorCombo::new);

        // Test for conflicts in Exceptions on methods with the same signatures
        new ComboTestHelper<ProxyGeneratorCombo>()
                .withDimension("CLASSACCESS", new ClassAccessKind[]{ClassAccessKind.PUBLIC})
                .withDimension("METHODACCESS", new ClassAccessKind[]{ClassAccessKind.PUBLIC})
                .withDimension("METHODS", ProxyGeneratorCombo::saveMethod, new MethodsKind[] {
                        MethodsKind.ZERO})
                .withDimension("EXCEPTION", ProxyGeneratorCombo::saveException,
                        ExceptionKind.values())
                .withDimension("MULTI_INTERFACES", ProxyGeneratorCombo::saveInterface,
                        new MultiInterfacesKind[] {MultiInterfacesKind.NONE})
                .run(ProxyGeneratorCombo::new);
    }

    /**
     * Basic template.
     */
    String template = "#{CLASSACCESS} interface #{TESTNAME} {\n" +
            "#{METHODS}" +
            "}";

    // Saved values of Combo values
    private MultiInterfacesKind currInterface = MultiInterfacesKind.NONE;
    private MethodsKind currMethod = MethodsKind.NONE;
    private ExceptionKind currException = ExceptionKind.NONE;
    private ArgumentKind[] currArgs = new ArgumentKind[0];

    void saveInterface(ComboParameter s) {
        currInterface = (MultiInterfacesKind)s;
    }

    void saveMethod(ComboParameter s) {
        currMethod = (MethodsKind)s;
    }

    void saveException(ComboParameter s) {
        currException = (ExceptionKind)s;
    }

    void saveArg(ComboParameter s, int index) {
        if (index >= currArgs.length) {
            currArgs = Arrays.copyOf(currArgs, index + 1);
        }
        currArgs[index] = (ArgumentKind)s;
    }

    /**
     * Filter out needless tests (mostly with more variations of arguments than needed).
     * @return true to run the test, false if not
     */
    boolean filter() {
        if ((currMethod == MethodsKind.NONE || currMethod == MethodsKind.ZERO) &&
                currArgs.length >= 2) {
            return currArgs[0] == ArgumentKind.INT &&
                currArgs[1] == ArgumentKind.INT;
        }
        if (currMethod == MethodsKind.ONE &&
                currArgs.length >= 2 ) {
            return currArgs[0] == currArgs[1];
        }
        return true;
    }

    /**
     * Generate the source file and compile.
     * Generate a proxy for the interface and test the resulting Proxy
     * for the methods, exceptions and handling of a thrown exception
     * @throws IOException catch all IOException
     */
    @Override
    public void doWork() throws IOException {
        String cp = System.getProperty("test.classes");
        String ifaceName = "Interface_" + this.id();
        newCompilationTask()
                .withSourceFromTemplate(ifaceName, template.replace("#{TESTNAME}", ifaceName))
                .withOption("-d")
                .withOption(cp)
                .generate(this::checkCompile);
        try {
            ClassLoader loader = ClassLoader.getSystemClassLoader();
            Class<?> tc = Class.forName(ifaceName);
            InvocationHandler handler =
                    new ProxyHandler(currException.exceptionKindClass);

            // Construct array of interfaces for the proxy
            Class<?>[] interfaces = new Class<?>[currInterface.classes().length + 1];
            interfaces[0] = tc;
            System.arraycopy(currInterface.classes(), 0,
                    interfaces, 1,
                    currInterface.classes().length);

            Object proxy = Proxy.newProxyInstance(loader, interfaces, handler);
            if (!Proxy.isProxyClass(proxy.getClass())) {
                fail("generated proxy is not a proxy class");
                return;
            }
            for (Class<?> i : interfaces) {
                if (!i.isAssignableFrom(proxy.getClass())) {
                    fail("proxy is not assignable to " + i.getName());
                }
            }
            try {
                String s = proxy.toString();
            } catch (Exception ex) {
                ex.printStackTrace();
                fail("proxy.toString() threw an exception");
            }

            checkDeclaredProxyExceptions(proxy, interfaces);

            if (currMethod == MethodsKind.ZERO && currException != ExceptionKind.NONE) {
                checkThrowsException(proxy, interfaces);
            }

        } catch (Exception ex) {
            throw new RuntimeException("doWork unexpected", ex);
        }
    }

    /**
     * Check that the exceptions declared on the proxy match the declarations for
     * exceptions from the interfaces.
     *
     * @param proxy a proxy object
     * @param interfaces the interfaces that defined it
     */
    void checkDeclaredProxyExceptions(Object proxy, Class<?>[] interfaces) {
        var allMethods = allMethods(Arrays.asList(interfaces));
        Method[] methods = proxy.getClass().getDeclaredMethods();
        for (Method m : methods) {
            String sig = toShortSignature(m);
            var imethods = allMethods.get(sig);
            if (imethods != null) {
                var expectedEx = Set.copyOf(Arrays.asList(m.getExceptionTypes()));
                var exs = Set.copyOf(extractExceptions(imethods));
                if (!expectedEx.equals(exs)) {
                    System.out.printf("mismatch on exceptions for method %s:%nExpected: " +
                                    "%s%nActual:  %s%n",
                            sig, expectedEx, exs);
                    fail("Exceptions declared on proxy don't match interface methods");
                }
            }
        }
    }

    void checkThrowsException(Object proxy, Class<?>[] interfaces) {
        ProxyHandler ph = (ProxyHandler)(Proxy.getInvocationHandler(proxy));
        try {
            Method m = proxy.getClass().getDeclaredMethod("zero");
            m.invoke(proxy);
            fail("Missing exception: " + ph.exceptionClass);
        } catch (NoSuchMethodException nsme) {
            System.out.printf("No method 'zero()' to test exceptions with%n");
            for (var cl : interfaces) {
                System.out.printf("     i/f %s: %s%n", cl, Arrays.toString(cl.getMethods()));
            }
            Method[] methods = proxy.getClass().getMethods();
            System.out.printf("    Proxy methods: %s%n", Arrays.toString(methods));
            fail("No such method test bug", nsme);
        } catch (InvocationTargetException actual) {
            ph.checkThrownException(actual.getTargetException());
        } catch (IllegalAccessException iae) {
            fail("IllegalAccessException", iae);
        }
    }

    /**
     * Exceptions known to be supported by all methods with the same signature.
     * @return a list of universal exception types
     */
    private static List<Class<?>> extractExceptions(List<Method> methods) {
        // for all methods with the same signature
        // start with the exceptions from the first method
        // while there are any exceptions remaining
        // look at the next method
        List<Class<?>> exceptions = null;
        for (Method m : methods) {
            var e = m.getExceptionTypes();
            if (e.length == 0)
                return emptyClassList();
            List<Class<?>> elist = Arrays.asList(e);
            if (exceptions == null) {
                exceptions = elist;    // initialize to first method exceptions
            } else {
                // for each exception
                // if it is compatible (both ways) with any of the existing exceptions continue
                //    else remove the current exception
                var okExceptions = new HashSet<Class<?>>();
                for (int j = 0; j < exceptions.size(); j++) {
                    var ex = exceptions.get(j);
                    for (int i = 0; i < elist.size();i++) {
                        var ci = elist.get(i);

                        if (ci.isAssignableFrom(ex)) {
                            okExceptions.add(ex);
                        }
                        if (ex.isAssignableFrom(ci)) {
                            okExceptions.add(ci);
                        }
                    }
                }
                if (exceptions.isEmpty()) {
                    // The empty set terminates the search for a common set of exceptions
                    return emptyClassList();
                }
                // Use the new set for the next iteration
                exceptions = List.copyOf(okExceptions);
            }
        }
        return (exceptions == null) ? emptyClassList() : exceptions;
    }

    /**
     * An empty correctly typed list of classes.
     * @return An empty typed list of classes
     */
    @SuppressWarnings("unchecked")
    static List<Class<?>> emptyClassList() {
        return Collections.EMPTY_LIST;
    }

    /**
     * Accumulate all of the unique methods.
     *
     * @param interfaces a list of interfaces
     * @return a map from signature to List of methods, unique by signature
     */
    private static Map<String, List<Method>> allMethods(List<Class<?>> interfaces) {
        Map<String, List<Method>> methods = new HashMap<>();
        for (Class<?> c : interfaces) {
            for (Method m : c.getMethods()) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    String sig = toShortSignature(m);
                    methods.computeIfAbsent(sig, s -> new ArrayList<Method>())
                            .add(m);
                }
            }
        }
        return methods;
    }

    /**
     * The signature of a method without the return type.
     * @param m a Method
     * @return the signature with method name and parameters
     */
    static String toShortSignature(Method m) {
        StringJoiner sj = new StringJoiner(",", m.getName() + "(", ")");
        for (Class<?> parameterType : m.getParameterTypes()) {
            sj.add(parameterType.getTypeName());
        }
        return sj.toString();
    }

    /**
     * Report any compilation errors.
     * @param res the result
     */
    void checkCompile(Result<?> res) {
        if (res.hasErrors()) {
            fail("invalid diagnostics for source:\n" +
                    res.compilationInfo() +
                    "\nFound error: " + res.hasErrors());
        }
    }

    /**
     * The Handler for the proxy includes the method to invoke the proxy
     * and the expected exception, if any.
     */
    class ProxyHandler implements InvocationHandler {

        private final Class<? extends Throwable> exceptionClass;

        ProxyHandler(Class<? extends Throwable> throwable) {
            this.exceptionClass = throwable;
        }

        /**
         * Invoke a method on the proxy or return a value.
         * @param   proxy the proxy instance that the method was invoked on
         * @param   method a method
         * @param   args some args
         * @return
         * @throws Throwable a throwable
         */
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (method.getName().equals("toString")) {
                return "Proxy" + System.identityHashCode(proxy);
            }
            if (method.getName().equals("zero")) {
                if (exceptionClass != null) {
                    throw exceptionClass.getDeclaredConstructor().newInstance();
                }
            }
            return "meth: " + method.toString();
        }

        /**
         * Check that the expected exception was thrown.
         * Special case is handled for Exception1 which does not appear in the
         * throws clause of the method so UndeclaredThrowableException is expected.
         */
        void checkThrownException(Throwable thrown) {
            if (exceptionClass == Exception1.class &&
                    thrown instanceof UndeclaredThrowableException &&
                    ((UndeclaredThrowableException)thrown).getUndeclaredThrowable() instanceof Exception1) {
                // Exception1 caused UndeclaredThrowableException
                return;
            } else if (exceptionClass == Exception1.class) {
                fail("UndeclaredThrowableException", thrown);
            }

            if (exceptionClass != null &&
                    !exceptionClass.equals(thrown.getClass())) {
                throw new RuntimeException("Wrong exception thrown: expected: " + exceptionClass +
                        ", actual: " + thrown.getClass());
            }
        }
    }

    /**
     * Exception to be thrown as a test of InvocationTarget.
     */
    static class Exception1 extends Exception {
        private static final long serialVersionUID = 1L;
        Exception1() {}
    }
}
