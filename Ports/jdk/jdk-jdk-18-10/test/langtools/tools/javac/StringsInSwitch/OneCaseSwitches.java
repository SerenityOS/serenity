/*
 * @test /nodynamiccopyright/
 * @bug 6827009 8078561
 * @summary Positive tests for strings in switch with few alternatives.
 * @compile          OneCaseSwitches.java
 * @run main OneCaseSwitches
 * @author  Joseph D. Darcy
 */

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;
import static java.lang.annotation.RetentionPolicy.*;

public class OneCaseSwitches {
    @Retention(RUNTIME)
    @interface TestMeForNull {}

    @TestMeForNull
    public static int zeroCasesNoDefault(String s, Set<String> stringSet, boolean expected) {
        int failures = 0;
        switch(s) {
        }
        return failures;
    }

    @TestMeForNull
    public static int zeroCasesWithDefault(String s, Set<String> stringSet, boolean expected) {
        int failures = 2;
        boolean addResult;

        switch(s) {
        default:
            failures = 0;
            addResult = stringSet.add(s);
            if (addResult != expected) {
                failures++;
                System.err.println("zeroCaseWithDefault: Expectedly got add result of " + addResult +
                                   " on string " + s);
            }
        }

        return failures;
    }

    @TestMeForNull
    public static int zeroCasesWithDefaultBreak(String s, Set<String> stringSet, boolean expected) {
        int failures = 2;
        boolean addResult;

        switch(s) {
        default:
            failures = zeroCasesWithDefault(s, stringSet, expected);
            break;
        }

        return failures;
    }

    @TestMeForNull
    public static int oneCaseNoDefault(String s, Set<String> stringSet, boolean expected) {
        int failures = 2;
        boolean addResult;

        switch(s) {
        case "foo":
            failures = 0;
            addResult = stringSet.add(s);
            if (addResult != expected) {
                failures++;
                System.err.println("oneCaseNoDefault: Unexpectedly got add result of " + addResult +
                                   " on string " + s);
            }
        }

        return failures;
    }

    @TestMeForNull
    public static int oneCaseNoDefaultBreak(String s, Set<String> stringSet, boolean expected) {
        int failures = 2;
        boolean addResult;

        switch(s) {
        case "foo":
            failures = oneCaseNoDefaultBreak(s, stringSet, expected);
            break;
        }

        return failures;
    }

    @TestMeForNull
    public static int oneCaseWithDefault(String s, Set<String> stringSet, boolean expected) {
        int failures = 2;
        boolean addResult;;

        switch(s) {
        case "foo":
            failures = 0;
            addResult = stringSet.add(s);
            if (addResult != expected) {
                failures++;
                System.err.println("oneCaseNoDefault: Expectedly got add result of " + addResult +
                                   " on string " + s);
            }
            break;
        default:
            break;
        }

        return failures;
    }

    @TestMeForNull
    public static int oneCaseBreakOnly(String s, Set<String> stringSet, boolean expected) {
        int failures = 1;
        switch(s) {
        case "foo":
            break;
        }
        failures = 0;
        return failures;
    }

    @TestMeForNull
    public static int oneCaseDefaultBreakOnly(String s, Set<String> stringSet, boolean expected) {
        int failures = 1;
        switch(s) {
        default:
            break;
        }
        failures = 0;
        return failures;
    }


    static int testNullBehavior() {
        int failures = 0;
        int count = 0;

        Method[] methods = OneCaseSwitches.class.getDeclaredMethods();

        try {
            for(Method method : methods) {
                count++;
                try {
                    if (method.isAnnotationPresent(TestMeForNull.class)) {
                        System.out.println("Testing method " + method);
                        method.invoke(null, (String)null, emptyStringSet, false);
                        failures++;
                        System.err.println("Didn't get NPE as expected from " + method);
                    }
                } catch (InvocationTargetException ite) { // Expected
                    Throwable targetException = ite.getTargetException();
                    if (! (targetException instanceof NullPointerException)) {
                        failures++; // Wrong exception thrown
                        System.err.println("Didn't get expected target exception NPE, got " +
                                           ite.getClass().getName());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (count == 0) {
            failures++;
            System.err.println("Did not find any annotated methods.");
        }
        return failures;
    }

    static int testZeroCases() {
        int failures = 0;
        Set<String> noDefaultSet = new HashSet<String>();
        Set<String> defaultSet   = new HashSet<String>();

        zeroCasesNoDefault(FOO, noDefaultSet, false);
        for(String word : words) {
            zeroCasesNoDefault(word, noDefaultSet, false);
        }

        if (!noDefaultSet.isEmpty()) {
            failures++;
            System.err.println("Non-empty set after zeroCasesNoDefault");
        }

        for(String word : words) {
            zeroCasesWithDefault(word, defaultSet, true);
        }
        if (defaultSet.size() != words.length) {
            failures++;
            System.err.println("Missing strings after zeroCasesWithDefault");
        }

        return failures;
    }

    static int testOneCaseNoDefault() {
        int failures = 0;
        Set<String> s = new HashSet<String>();
        s.add("foo");
        Set<String> fooSet = Collections.unmodifiableSet(s);
        Set<String> testSet   = new HashSet<String>();

        oneCaseNoDefault(FOO, testSet, true);
        if (!testSet.equals(fooSet)) {
            failures++;
            System.err.println("Unexpected result from oneCaseNoDefault: didn't get {\"Foo\"}");
        }

        for(String word : words) {
            oneCaseNoDefault(word, testSet, false);
        }
        if (!testSet.equals(fooSet)) {
            failures++;
            System.err.println("Unexpected result from oneCaseNoDefault: didn't get {\"Foo\"}");
        }

        return failures;
    }

    static int testBreakOnly() {
        int failures = 0;

        for(String word : words) {
            failures += oneCaseBreakOnly(word, emptyStringSet, true);
            failures += oneCaseDefaultBreakOnly(word, emptyStringSet, true);
        }

        return failures;
    }

    static int testExpressionEval() {
        String s = "a";
        int errors = 2;

        System.out.println("Testing expression evaluation.");

        switch (s + s) {
        case "aa":
            errors = 0;
            break;

        case "aaaa":
            errors = 1;
            System.err.println("Suspected bad expression evaluation.");
            break;

        default:
             throw new RuntimeException("Should not reach here.");
        }
        return errors;
    }

    static final String FOO = "foo";

    static final String[] words = {"baz",
                                   "quux",
                                   "wombat",
                                   "\u0ccc\u0012"}; // hash collision with "foo"

    final static Set<String> emptyStringSet = Collections.emptySet();

    public static void main(String... args) {
        int failures = 0;

        failures += testNullBehavior();
        failures += testZeroCases();
        failures += testOneCaseNoDefault();
        failures += testBreakOnly();
        failures += testExpressionEval();

        if (failures > 0) {
            throw new RuntimeException();
        }
    }
}
