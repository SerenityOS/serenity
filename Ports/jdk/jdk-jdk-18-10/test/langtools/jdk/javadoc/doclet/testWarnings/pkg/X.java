/* /nodynamiccopyright/ */

package pkg;

import java.io.Serializable;

/**
 * {@link com.package1.Class1#publicStaticMethod().
 * Any label would be ignored.
 */
public class X implements Serializable {

    private int f;

    private X(){}

    private void m() {}

    /** @see X#m() */
    private static class P implements Serializable {}

    /**
     * {@link #X()}<br/>
     * {@link #m()}<br/>
     * {@link #f}<br/>
     * {@link java.lang.String#toString()}<br/>
     * <hr/>
     * <p/>
     */
    public void foo() {}
}
