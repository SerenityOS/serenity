/*
 * @test /nodynamiccopyright/
 * @bug 4249111
 * @summary 'new' of inner class should not be allowed unless outer is constructed
 *
 * @compile/fail/ref=NewBeforeOuterConstructed.out -XDrawDiagnostics  NewBeforeOuterConstructed.java
 */

import java.io.*;

public class NewBeforeOuterConstructed extends PrintStream {
      private class NullOutputStream extends OutputStream {
              public NullOutputStream() {
                      super();
              }
              public void write(int b) { }
              public void write(byte b[]) { }
              public void write(byte b[], int off, int len) { }
              public void flush() { }
              public void close() { }
      }
       public NewBeforeOuterConstructed() {
                // The 'new' below is illegal, as the outer
                // constructor has not been called when the
                // implicit reference to 'this' is evaluated
                // during the new instance expression.
              super(new NullOutputStream());
      }
}
