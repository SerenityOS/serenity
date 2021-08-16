/*
 * @test    /nodynamiccopyright/
 * @bug     6214959
 * @summary Compiler fails to produce error message with ODD number of import static
 * @compile/fail/ref=Estatico4.out -XDrawDiagnostics Estatico4.java
 */


import static javax.swing.JLabel.*;
import static java.awt.FlowLayout.*;
import static javax.swing.JLabel.*;

public class Estatico4 {

  public static void main(String[] s) {
    System.out.println(CENTER);
  }

}
