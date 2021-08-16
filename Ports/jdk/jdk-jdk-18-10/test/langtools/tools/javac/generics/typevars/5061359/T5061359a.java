/*
 * @test    /nodynamiccopyright/
 * @bug     5061359
 * @summary No error for ambiguous member of intersection
 * @clean   Base Intf
 * @compile/fail/ref=T5061359a.out -XDrawDiagnostics  T5061359a.java
 * @clean   Base Intf T5061359a
 * @compile/fail/ref=T5061359a.out -XDrawDiagnostics  Base.java Intf.java T5061359a.java
 * @clean   Base Intf T5061359a
 * @compile/fail/ref=T5061359a.out -XDrawDiagnostics  T5061359a.java Base.java Intf.java
 */

import java.util.*;

public class T5061359a {
    public static class Test<T extends Base & Intf> {}
}
