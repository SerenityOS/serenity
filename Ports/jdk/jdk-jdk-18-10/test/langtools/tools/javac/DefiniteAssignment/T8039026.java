/*
 * @test /nodynamiccopyright/
 * @bug 8039026
 * @summary Definitely unassigned field can be accessed
 * @compile/fail/ref=T8039026.out -XDrawDiagnostics T8039026.java
 */

public class T8039026 {
    final int x,y,z;
    final int a = this.y;  // <- error
    {
        int b = true ? this.x : 0;  // <- error
        System.out.println(this.x); // <- error
        this.y = 1;
    }
    T8039026() {
        this.x = 1;      // <- no error!
        this.y = 1;      // <- error
        this.z = this.x; // <- no error
    }
}
