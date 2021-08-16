/**
 * @test /nodynamiccopyright/
 * @bug 8161383
 * @summary javac is looking for operator symbols at the wrong place
 * @compile LookingForOperatorSymbolsAtWrongPlaceTest.java
 */

public class LookingForOperatorSymbolsAtWrongPlaceTest {
    class Base {
        protected int i = 1;
    }

    class Sub extends Base {
        void func(){
            Sub.super.i += 10;
        }
    }
}
