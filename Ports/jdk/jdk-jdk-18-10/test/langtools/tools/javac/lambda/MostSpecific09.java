/*
 * @test /nodynamiccopyright/
 * @bug 8029718 8065800
 * @summary Should always use lambda body structure to disambiguate overload resolution
 * @compile/fail/ref=MostSpecific09.out -XDrawDiagnostics --should-stop=ifError=ATTR --should-stop=ifNoError=ATTR --debug=verboseResolution=applicable,success MostSpecific09.java
 */

class MostSpecific09 {

    interface I {
        String xoo(String x);
    }

    interface J {
        void xoo(int x);
    }

    static void foo(I i) {}
    static void foo(J j) {}

    static void moo(I i) {}
    static void moo(J j) {}

    void m() {
        foo((x) -> { return x += 1; });
        foo((x) -> { return ""; });
        foo((x) -> { System.out.println(""); });
        foo((x) -> { return ""; System.out.println(""); });
        foo((x) -> { throw new RuntimeException(); });
        foo((x) -> { while (true); });

        foo((x) -> x += 1);
        foo((x) -> "");
    }

    /* any return statement that is not in the body of the lambda but in an
     * inner class or another lambda should be ignored for value void compatibility
     * determination.
     */
    void m1() {
        boolean cond = true;
        foo((x) -> {
            if (cond) {
                return "";
            }
            System.out.println("");
        });

        foo((x)->{
            class Bar {
                String m() {
                    return "from Bar.m()";
                }
            }
            class Boo {
                Bar b = new Bar (){
                    String m() {
                        return "from Bar$1.m()";
                    }
                };
            }
            moo((y) -> { return ""; });
            return;
        });

        foo((x)->{
            class Bar {
                void m() {}
            }
            class Boo {
                Bar b = new Bar (){
                    void m() {
                        return;
                    }
                };
            }
            moo((y) -> { System.out.println(""); });
            return "";
        });
    }
}
