/*
 * @test /nodynamiccopyright/
 * @bug 8238735
 * @summary javac should fail without throwing NPE
 * @compile/fail/ref=T8238735.out -XDrawDiagnostics T8238735.java
 */

class T8238735 {
     public static void main(String[] args) {
        boolean first = true;
        first = first ? false : (boolean)(() -> false) ;
    }
}
