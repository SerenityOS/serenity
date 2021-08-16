/* /nodynamiccopyright/ */

package p;

class Inner extends Outer.Super {
    Inner(Outer t) {
        t.super();
    }

    Outer this$0 = null;

    public void foo() {
        this$0 = new Outer();
    }
}
