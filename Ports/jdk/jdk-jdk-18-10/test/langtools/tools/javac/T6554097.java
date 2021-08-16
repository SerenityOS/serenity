/*
 * @test /nodynamiccopyright/
 * @bug     6554097
 * @summary "final" confuses at-SuppressWarnings
 * @compile T6554097.java
 * @compile/fail/ref=T6554097.out -XDrawDiagnostics -Werror -Xlint:rawtypes T6554097.java
 */

import java.util.ArrayList;

class T6554097 {

    @SuppressWarnings("unchecked") final ArrayList[] v1 = { new ArrayList() {} };
    @SuppressWarnings("unchecked")       ArrayList[] v2 = { new ArrayList() {} };

    public static void m1() throws Throwable {
            @SuppressWarnings("unchecked") final ArrayList[] v3 = { new ArrayList() {} };
            @SuppressWarnings("unchecked")       ArrayList[] v4 = { new ArrayList() {} };
    }

    final ArrayList[] v5 = { new ArrayList() {} };
          ArrayList[] v6 = { new ArrayList() {} };

    public static void m2() throws Throwable {
        final ArrayList[] v7 = { new ArrayList() {} };
              ArrayList[] v8 = { new ArrayList() {} };
    }
}

