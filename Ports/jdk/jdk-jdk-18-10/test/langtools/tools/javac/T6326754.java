/*
 * @test /nodynamiccopyright/
 * @bug 6326754
 * @summary Compiler will fail to handle -Xmaxerrs with -ve numbers
 *
 * @compile/fail/ref=T6326754.out -XDrawDiagnostics -Xmaxerrs -1 T6326754.java
 * @compile/fail/ref=T6326754.out -XDrawDiagnostics -Xmaxerrs  0 T6326754.java
 * @compile/fail/ref=T6326754.out -XDrawDiagnostics -Xmaxerrs 10 T6326754.java
 * @compile/fail/ref=T6326754.out -XDrawDiagnostics              T6326754.java
 */
class TestConstructor<T,K>{
    T t;
    K k;
    public TestConstructor(T t,K k){
        this.t =t;
    }
    public TestConstructor(K k){
        this.k = k;
        this.t = null;
    }
    public TestConstructor(T t){
        this.t=t;
        this.k=null;
    }
    public void setT(T t){
        this.t=t;
        this.k=null;
    }
    public void setT(K k){
        this.k = k;
        this.t = null;
    }
    public void setT(T t,K k){
        this.t = t;
        this.k = k;
    }
}
class TestC<T>{
    T t;
    public <T>void setT(T t){
        this.t = t;
    }
}
public class T6326754{
    public static void main(String... arg){
        TestC tC =new TestC();
        tC.setT();
        TestConstructor tc = new TestConstructor("saaa");
        tc.setT("sasa");
        TestC<Integer> tC1 = new TestC();
        tC1.setT(545);
    }
}
