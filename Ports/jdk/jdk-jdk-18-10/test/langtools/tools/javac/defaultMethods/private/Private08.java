/* @test   /nodynamiccopyright/
 * @bug    8071453
 * @author sadayapalam
 * @summary Test various JLS changes made for supporting private interface methods.
 * @compile/fail/ref=Private08.out -XDrawDiagnostics Private08.java
 */
class Private08 {
    interface I {
        private void poo() {}
        private int foo() { return 0; }
        int goo();
        default int doo() { return foo(); }
        private public int bad(); // 9.4 illegal combination of modifiers
        private abstract int verybad(); // 9.4 illegal combination of modifiers
        private default int alsobad() { return foo(); } // 9.4 illegal combination of modifiers
        protected void blah();
        private void missingBody(); // private methods are not abstract.
    }
}

class Private08_01 {
    int y = ((Private08.I) null).foo();   // 9.4 test that private methods are not implicitly public.
    interface J extends Private08.I {
        default void foo() { // foo not inherited from super, change of return type is OK.
            super.foo();  // super in static context - Error.
        }
        private int doo() { return 0; } // private cannot override public.
    };

    Private08.I i = new Private08.I () {
        public void foo() { // foo not inherited from super, change of return type is OK.
            super.foo();  // super's foo not inherited, NOT OK.
        }
        private int doo() { return 0; } // private cannot override public.
    }; // should not complain about poo() not being implemented.
}

