/*
 * @test /nodynamiccopyright/
 * @bug 6356530
 * @summary -Xlint:serial does not flag abstract classes with concrete methods/members
 * @compile/fail/ref=FinalVariableAssignedToInCatchBlockTest.out -XDrawDiagnostics FinalVariableAssignedToInCatchBlockTest.java
 */

import java.io.IOException;

public class FinalVariableAssignedToInCatchBlockTest {
    public void m1(int o)
    {
        final int i;
        try {
            if (o == 1) {
                throw new IOException();
            } else if (o == 2) {
                throw new InterruptedException();
            } else {
                throw new Exception();
            }
        } catch (IOException e) {
            i = 1;
        } catch (InterruptedException ie) {
            i = 2;
        } catch (Exception e) {
            i = 3;
        } finally {
            i = 4;
        }
    }

    public void m2(int o)
    {
        final int i;
        try {
            if (o == 1) {
                throw new IOException();
            } else if (o == 2) {
                throw new InterruptedException();
            } else {
                throw new Exception();
            }
        } catch (IOException e) {
            i = 1;
        } catch (InterruptedException ie) {
            i = 2;
        } catch (Exception e) {
            i = 3;
        }
    }

    public void m3(int o) throws Exception
    {
        final int i;
        try {
            if (o == 1) {
                throw new IOException();
            } else if (o == 2) {
                throw new InterruptedException();
            } else {
                throw new Exception();
            }
        } catch (IOException e) {
            i = 1;
        } catch (InterruptedException ie) {
            i = 2;
        }
        i = 3;
    }
}
