/**@test /nodynamiccopyright/
 * @compile/fail/ref=Test.out -Xplugin:coding_rules -XDrawDiagnostics Test.java
 */

package com.sun.tools.javac;

public class Test {
    public static String mutable;
}
