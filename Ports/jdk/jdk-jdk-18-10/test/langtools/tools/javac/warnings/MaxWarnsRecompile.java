import java.util.List;

/*
 * @test /nodynamiccopyright/
 * @summary suggest recompiling with -Xmaxwarns
 * @compile/ref=MaxWarnsRecompile.max1.out -XDrawDiagnostics -Xlint:all -Xmaxwarns 1 MaxWarnsRecompile.java
 * @compile/ref=MaxWarnsRecompile.all.out -XDrawDiagnostics -Xlint:all -Xmaxwarns 4 MaxWarnsRecompile.java
 */
public class MaxWarnsRecompile {

  List x1;
  List x2;
  List x3;
  List x4;
}
