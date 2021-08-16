/*
 * @test    /nodynamiccopyright/
 * @bug     5009484
 * @summary Compiler fails to resolve appropriate type for outer member
 * @author  Philippe P Mulet
 * @compile/fail/ref=X.out -XDrawDiagnostics  X.java
 */

public class X<T> {
   private T t;
   X(T t) {
       this.t = t;
   }
   public static void main(String[] args) {
       new X<String>("OUTER").bar();
   }
   void bar() {
       new X<X>(this) {     // #1
           void run() {
               new Object() {  // #2
                   void run() {
                       X x = t;        // #3 <--- which t is bound ?
                   System.out.println(x);
                   }
               }.run();
           }
       }.run();
   }
}
