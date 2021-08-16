/*
 * @test  /nodynamiccopyright/
 * @bug 6247324
 * @compile/fail/ref=T6247324.out -XDrawDiagnostics -Xlint -Xlint:-path T6247324.java
 */
class Pair<X,Y> {
    private X x;
    private Y y;

    public Pair(X x, Y y){
        this.x = x;
        this.y = y;
    }

    public X getX(){
        return x;
    }
    @Seetharam // Undefined annotation...
    public Y getY(){
        return y;
    }
}

public class T6247324{
    public void myMethod(){
        Pair<Integer, String> pair = new Pair<Integer, String>(0, "I am not sure");
        int intValue = pair.getX();
        String strValue = pair.getY();
    }
}
