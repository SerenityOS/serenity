/*
 * @test /nodynamiccopyright/
 * @summary suggest recompiling with -Xmaxerrs
 * @compile/fail/ref=MaxDiagsRecompile.max1.out -XDrawDiagnostics -Xmaxerrs 1 MaxDiagsRecompile.java
 * @compile/fail/ref=MaxDiagsRecompile.all.out -XDrawDiagnostics -Xmaxerrs 4 MaxDiagsRecompile.java
 */
public class MaxDiagsRecompile {

  NoSuchSymbol1 f1;
  NoSuchSymbol2 f2;
  NoSuchSymbol3 f3;
  NoSuchSymbol4 f4;
}
