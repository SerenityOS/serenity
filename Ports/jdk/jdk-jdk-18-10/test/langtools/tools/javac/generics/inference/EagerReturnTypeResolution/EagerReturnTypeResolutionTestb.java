/*
 * @test /nodynamiccopyright/
 * @bug 8030741 8078024
 * @summary Inference: implement eager resolution of return types, consistent with JDK-8028800
 * @compile/fail/ref=EagerReturnTypeResolutionTestb.out -XDrawDiagnostics EagerReturnTypeResolutionTestb.java
 * @author Dan Smith
 */

import java.util.List;

public class EagerReturnTypeResolutionTestb {
    interface I<S> {}
    interface J<S> extends I<S> {}
    interface K extends I<String> {}
    interface L<S> extends I {}

    <T> T lower(List<? extends T> l) { return null; }
    <T> T lower2(List<? extends T> l1, List<? extends T> l2) { return null; }

    <T> T upper(List<? super T> l) { return null; }
    <T> T upper2(List<? super T> l1, List<? super T> l2) { return null; }

    <T> T eq(List<T> l) { return null; }
    <T> T eq2(List<T> l1, List<T> l2) { return null; }

    <X> void takeI(I<X> i) {}
    void takeIString(I<String> i) {}
    I<String> iStringField;

    void takeLong(long arg) {}
    long longField;

    void testSimpleCaptureOK(List<I<?>> i1) {
        takeI(lower(i1)); // ok*
        takeI(eq(i1)); // ok*
        takeI(upper(i1)); // ok, no capture
        takeIString(upper(i1)); // ok
        iStringField = upper(i1); // ok
    }

    void testSimpleCaptureKO(List<I<?>> i1) {
        takeIString(lower(i1)); // ERROR
        takeIString(eq(i1)); // ERROR
        iStringField = lower(i1); // ERROR
        iStringField = eq(i1); // ERROR
    }

    void testMultiCaptureOK(List<I<String>> i1, List<I<Integer>> i2, List<I<?>> i3,
                          List<J<String>> j1, List<J<Integer>> j2, List<K> k1) {
        /* Lines marked with JDK-8029002 should be uncommented once this bug is
         * fixed
         */
        takeI(lower2(i1, i2)); // ok*
        takeI(lower2(i1, i3)); // ok*
        takeI(upper2(i1, i3)); // ok, no capture*  JDK-8029002

        takeIString(upper2(i1, i3)); // ok, no capture
        iStringField = upper2(i1, i3); // ok, no capture

        takeI(lower2(j1, j2)); // ok*
        takeI(lower2(j1, k1)); // ok, no capture
        takeI(upper2(j1, k1)); // ok, no capture*  JDK-8029002

        takeIString(lower2(j1, k1)); // ok, no capture
        takeIString(upper2(j1, k1)); // ok, no capture

        iStringField = lower2(j1, k1); // ok, no capture
        iStringField = upper2(j1, k1); // ok, no capture
        takeI(lower2(j2, k1)); // ok*
    }

    void testMultiCaptureKO(List<I<String>> i1, List<I<Integer>> i2, List<I<?>> i3,
                          List<J<String>> j1, List<J<Integer>> j2, List<K> k1) {
        takeI(eq2(i1, i2)); // ERROR, bad bounds
        takeI(upper2(i1, i2)); // ERROR, bad bounds

        takeIString(lower2(i1, i2)); // ERROR
        takeIString(eq2(i1, i2)); // ERROR, bad bounds
        takeIString(upper2(i1, i2)); // ERROR, bad bounds

        iStringField = lower2(i1, i2); // ERROR
        iStringField = eq2(i1, i2); // ERROR, bad bounds
        iStringField = upper2(i1, i2); // ERROR, bad bounds

        takeI(eq2(i1, i3)); // ERROR, bad bounds
        takeIString(lower2(i1, i3)); // ERROR
        takeIString(eq2(i1, i3)); // ERROR, bad bounds

        iStringField = lower2(i1, i3); // ERROR
        iStringField = eq2(i1, i3); // ERROR, bad bounds
        takeI(eq2(j1, j2)); // ERROR, bad bounds
        takeI(upper2(j1, j2)); // ERROR, bad bounds

        takeIString(lower2(j1, j2)); // ERROR
        takeIString(eq2(j1, j2)); // ERROR, bad bounds
        takeIString(upper2(j1, j2)); // ERROR, bad bounds

        iStringField = lower2(j1, j2); // ERROR
        iStringField = eq2(j1, j2); // ERROR, bad bounds
        iStringField = upper2(j1, j2); // ERROR, bad bounds

        takeI(eq2(j1, k1)); // ERROR, bad bounds
        takeIString(eq2(j1, k1)); // ERROR, bad bounds
        iStringField = eq2(j1, k1); // ERROR, bad bounds
        takeI(eq2(j2, k1)); // ERROR, bad bounds
        takeI(upper2(j2, k1)); // ERROR, bad bounds; actual: no error, see JDK-8037474

        takeIString(lower2(j2, k1)); // ERROR
        takeIString(eq2(j2, k1)); // ERROR, bad bounds
        takeIString(upper2(j2, k1)); // ERROR, bad bounds

        iStringField = lower2(j2, k1); // ERROR
        iStringField = eq2(j2, k1); // ERROR, bad bounds
        iStringField = upper2(j2, k1); // ERROR, bad bounds
    }

    void testRawOK(List<I> i1, List<J> j1, List<L<String>> l1) {
        takeI(lower(i1)); // ok, unchecked
        takeI(eq(i1)); // ok, unchecked
        takeI(upper(i1)); // ok, no capture, not unchecked

        takeIString(lower(i1)); // ok, unchecked
        takeIString(eq(i1)); // ok, unchecked
        takeIString(upper(i1)); // ok, no capture, not unchecked

        iStringField = lower(i1); // ok, unchecked
        iStringField = eq(i1); // ok, unchecked
        iStringField = upper(i1); // ok, no capture, not unchecked

        takeI(lower(j1)); // ok, unchecked
        takeI(eq(j1)); // ok, unchecked
        takeI(upper(j1)); // bad bounds? -- spec is unclear

        takeIString(lower(j1)); // ok, unchecked
        takeIString(eq(j1)); // ok, unchecked
        takeIString(upper(j1)); // bad bounds? -- spec is unclear

        iStringField = lower(j1); // ok, unchecked
        iStringField = eq(j1); // ok, unchecked
        iStringField = upper(j1); // bad bounds? -- spec is unclear

        takeI(lower(l1)); // ok, unchecked
        takeI(eq(l1)); // ok, unchecked
        takeI(upper(l1)); // bad bounds? -- spec is unclear

        takeIString(lower(l1)); // ok, unchecked
        takeIString(eq(l1)); // ok, unchecked
        takeIString(upper(l1)); // bad bounds? -- spec is unclear

        iStringField = lower(l1); // ok, unchecked
        iStringField = eq(l1); // ok, unchecked
        iStringField = upper(l1); // bad bounds? -- spec is unclear
    }

    void testPrimOK(List<Integer> i1, List<Long> l1, List<Double> d1) {
        takeLong(lower(i1)); // ok
        takeLong(eq(i1)); // ok
        takeLong(upper(i1)); // ok*

        longField = lower(i1); // ok
        longField = eq(i1); // ok
        longField = upper(i1); // ok*

        takeLong(lower(l1)); // ok
        takeLong(eq(l1)); // ok
        takeLong(upper(l1)); // ok

        longField = lower(l1); // ok
        longField = eq(l1); // ok
        longField = upper(l1); // ok
    }

    void testPrimKO(List<Integer> i1, List<Long> l1, List<Double> d1) {
        takeLong(lower(d1)); // ERROR
        takeLong(eq(d1)); // ERROR
        takeLong(upper(d1)); // ERROR

        longField = lower(d1); // ERROR
        longField = eq(d1); // ERROR
        longField = upper(d1); // ERROR
    }
}
