/*
 * @test  /nodynamiccopyright/
 * @bug     5003235
 * @summary Private inner class accessible from subclasses
 * @author  Peter von der Ah\u00e9
 * @compile/fail/ref=T5003235a.out -XDrawDiagnostics T5003235a.java
 */

class Super {
    Inner i;
    private class Inner {
        void defaultM() {}
        protected void protectedM() {}
        public void publicM() {}
        private void privateM() {}
    }
}

class Sub extends Super {
    void foo() {
        i.defaultM();
        i.protectedM();
        i.publicM();
        i.privateM();
    }
}
