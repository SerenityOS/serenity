/*
 * @test /nodynamiccopyright/
 * @bug 4767128 5048557 5048773 8078559
 * @summary diagnose encoding errors in Java source files
 * @author gafter
 *
 * @compile/fail/ref=Unmappable.out -XDrawDiagnostics  -encoding ascii Unmappable.java
 */

// example from 4766897
public class Unmappable {
    String s = "zähler";
}
