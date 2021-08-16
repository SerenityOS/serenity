/* @test /nodynamiccopyright/
 * @compile HasBrokenConstantValue.jcod
 * @compile/fail/ref=BrokenConstantValue.out -XDrawDiagnostics BrokenConstantValue.java
 */
public class BrokenConstantValue {
     void t() {
         String s = HasBrokenConstantValue.VALUE;
     }
}
