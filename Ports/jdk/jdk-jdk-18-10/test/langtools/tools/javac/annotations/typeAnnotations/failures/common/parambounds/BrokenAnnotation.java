/*
 * @test /nodynamiccopyright/
 * @bug 8006775
 * @summary Ensure unresolved upper bound annotation is handled correctly
 * @author Werner Dietl
 * @compile/fail/ref=BrokenAnnotation.out -XDrawDiagnostics BrokenAnnotation.java
 */

// No import, making the annotation @A invalid.
// import java.lang.annotation.*;

// Works: @Broke.A class...
// Works: class Broke<@Broke.A T> {
// Used to fail:
class BrokenAnnotation<T extends @BrokenAnnotation.A Object> {
    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
    @interface A { }
}

// If the Annotation is e.g. on the top-level class, we
// get something like this:
//
// Broke.java:6: cannot find symbol
// symbol  : class Target
// location: class Broke
//     @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
//      ^
// 1 error
//
// When the annotation is in the upper bound, one used to get
// the following stack trace:
//
// An exception has occurred in the compiler (1.7.0-jsr308-1.2.7). Please report this bug so we can fix it.  For instructions, see http://types.cs.washington.edu/checker-framework/current/README-jsr308.html#reporting-bugs .  Thank you.
// java.lang.NullPointerException
//      at com.sun.tools.javac.code.Type.isCompound(Type.java:346)
//      at com.sun.tools.javac.code.Types.getBounds(Types.java:1940)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter$1.visitTypeVar(RichDiagnosticFormatter.java:534)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter$1.visitTypeVar(RichDiagnosticFormatter.java:1)
//      at com.sun.tools.javac.code.Type$TypeVar.accept(Type.java:1049)
//      at com.sun.tools.javac.code.Types$UnaryVisitor.visit(Types.java:3809)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter$1.visit(RichDiagnosticFormatter.java:450)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter$1.visitClassType(RichDiagnosticFormatter.java:518)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter$1.visitClassType(RichDiagnosticFormatter.java:1)
//      at com.sun.tools.javac.code.Type$ClassType.accept(Type.java:596)
//      at com.sun.tools.javac.code.Types$UnaryVisitor.visit(Types.java:3809)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter.preprocessType(RichDiagnosticFormatter.java:442)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter.preprocessArgument(RichDiagnosticFormatter.java:172)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter.preprocessDiagnostic(RichDiagnosticFormatter.java:155)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter.preprocessArgument(RichDiagnosticFormatter.java:178)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter.preprocessDiagnostic(RichDiagnosticFormatter.java:155)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter.format(RichDiagnosticFormatter.java:111)
//      at com.sun.tools.javac.util.RichDiagnosticFormatter.format(RichDiagnosticFormatter.java:1)
//      at com.sun.tools.javac.util.Log.writeDiagnostic(Log.java:514)
//      at com.sun.tools.javac.util.Log.report(Log.java:496)
//      at com.sun.tools.javac.comp.Resolve.logResolveError(Resolve.java:2160)
//      at com.sun.tools.javac.comp.Resolve.access(Resolve.java:1553)
//      at com.sun.tools.javac.comp.Resolve.access(Resolve.java:1580)
//      at com.sun.tools.javac.comp.Resolve.access(Resolve.java:1592)
//      at com.sun.tools.javac.comp.Resolve.resolveIdent(Resolve.java:1653)
//      at com.sun.tools.javac.comp.Attr.visitIdent(Attr.java:2191)
//      at com.sun.tools.javac.tree.JCTree$JCIdent.accept(JCTree.java:1873)
//      at com.sun.tools.javac.comp.Attr.attribTree(Attr.java:467)
//      at com.sun.tools.javac.comp.Attr.attribType(Attr.java:503)
//      at com.sun.tools.javac.comp.Attr.attribType(Attr.java:496)
//      at com.sun.tools.javac.comp.Attr.attribAnnotationTypes(Attr.java:605)
//      at com.sun.tools.javac.comp.MemberEnter.complete(MemberEnter.java:944)
//      at com.sun.tools.javac.code.Symbol.complete(Symbol.java:432)
//      at com.sun.tools.javac.code.Symbol$ClassSymbol.complete(Symbol.java:832)
//      at com.sun.tools.javac.code.Symbol$ClassSymbol.flags(Symbol.java:775)
//      at com.sun.tools.javac.comp.Resolve.isAccessible(Resolve.java:350)
//      at com.sun.tools.javac.comp.Resolve.isAccessible(Resolve.java:346)
//      at com.sun.tools.javac.comp.Resolve.findMemberType(Resolve.java:1346)
//      at com.sun.tools.javac.comp.Resolve.findIdentInType(Resolve.java:1512)
//      at com.sun.tools.javac.comp.Attr.selectSym(Attr.java:2434)
//      at com.sun.tools.javac.comp.Attr.visitSelect(Attr.java:2312)
//      at com.sun.tools.javac.tree.JCTree$JCFieldAccess.accept(JCTree.java:1805)
//      at com.sun.tools.javac.comp.Attr.attribTree(Attr.java:467)
//      at com.sun.tools.javac.comp.Attr.attribType(Attr.java:503)
//      at com.sun.tools.javac.comp.Attr.attribType(Attr.java:496)
//      at com.sun.tools.javac.comp.Attr.attribAnnotationTypes(Attr.java:605)
//      at com.sun.tools.javac.comp.Attr.visitAnnotatedType(Attr.java:3016)
//      at com.sun.tools.javac.tree.JCTree$JCAnnotatedType.accept(JCTree.java:2253)
//      at com.sun.tools.javac.comp.Attr.attribTree(Attr.java:467)
//      at com.sun.tools.javac.comp.Attr.attribType(Attr.java:503)
//      at com.sun.tools.javac.comp.Attr.attribType(Attr.java:496)
//      at com.sun.tools.javac.comp.Attr.attribTypeVariables(Attr.java:569)
//      at com.sun.tools.javac.comp.MemberEnter.complete(MemberEnter.java:955)
//      at com.sun.tools.javac.code.Symbol.complete(Symbol.java:432)
//      at com.sun.tools.javac.code.Symbol$ClassSymbol.complete(Symbol.java:832)
//      at com.sun.tools.javac.comp.Enter.complete(Enter.java:500)
//      at com.sun.tools.javac.comp.Enter.main(Enter.java:478)
//      at com.sun.tools.javac.main.JavaCompiler.enterTrees(JavaCompiler.java:950)
//      at com.sun.tools.javac.main.JavaCompiler.compile(JavaCompiler.java:841)
//      at com.sun.tools.javac.main.Main.compile(Main.java:441)
//      at com.sun.tools.javac.main.Main.compile(Main.java:358)
//      at com.sun.tools.javac.main.Main.compile(Main.java:347)
//      at com.sun.tools.javac.main.Main.compile(Main.java:338)
//      at com.sun.tools.javac.Main.compile(Main.java:76)
//      at com.sun.tools.javac.Main.main(Main.java:61)
