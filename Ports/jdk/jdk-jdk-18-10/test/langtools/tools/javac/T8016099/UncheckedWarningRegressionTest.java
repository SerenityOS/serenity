/*
 * @test /nodynamiccopyright/
 * @bug     8016099
 * @summary Some SuppressWarnings annotations ignored ( unchecked, rawtypes )
 * @compile UncheckedWarningRegressionTest.java
 * @compile/fail/ref=UncheckedWarningRegressionTest.out -XDrawDiagnostics -Werror -Xlint:unchecked UncheckedWarningRegressionTest.java
 */

public class UncheckedWarningRegressionTest {
    <T> void suppressedWarningsFinalInitializer() {
        @SuppressWarnings("unchecked")
        T[] tt = (T[]) FINAL_EMPTY_ARRAY;
    }

    final Object[] FINAL_EMPTY_ARRAY = {};

    <T> void finalInitializer() {
        T[] tt = (T[]) FINAL_EMPTY_ARRAY;
    }

    <T> void suppressedWarningsNonFinalInitializer() {
        @SuppressWarnings("unchecked")
        T[] tt = (T[]) NON_FINAL_EMPTY_ARRAY;
    }

    Object[] NON_FINAL_EMPTY_ARRAY = {};

    <T> void nonFinalInitializer() {
        T[] tt = (T[]) NON_FINAL_EMPTY_ARRAY;
    }

}
