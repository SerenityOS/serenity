/* @test  /nodynamiccopyright/
   @bug 4037020 4785453
   @summary Verify that ClassModifier "synchronized" is not allowed.
   @author dps

   @compile/fail/ref=SynchronizedClass.out -XDrawDiagnostics SynchronizedClass.java
*/

public synchronized class SynchronizedClass { } // ERROR
