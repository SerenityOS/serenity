/**
 * @test /nodynamiccopyright/
 * @bug 8022765
 * @summary javac should not crash for incorrect attribute values
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 * @build VerifyAnnotationsAttributed
 * @run main VerifyAnnotationsAttributed T8022765.java
 * @compile/fail/ref=T8022765.out -XDrawDiagnostics T8022765.java
 */
@Ann(@Override)
@Primitive(@Override)
@Str(@Override)
@En(@Override)
@ArrAnn(@Override)
@ArrPrimitive(@Override)
@ArrStr(@Override)
@ArrEn(@Override)
class AnnC { }

class PrimitiveC {
    private static final int C = 1;
    @Ann(C)
    @Primitive(C)
    @Str(C)
    @En(C)
    @ArrAnn(C)
    @ArrPrimitive(C)
    @ArrStr(C)
    @ArrEn(C)
    class I {
    }
}

class StringC {

    private static final String C = "";

    @Ann(C)
    @Primitive(C)
    @Str(C)
    @En(C)
    @ArrAnn(C)
    @ArrPrimitive(C)
    @ArrStr(C)
    @ArrEn(C)
    class I {
    }
}

@Ann(E.A)
@Primitive(E.A)
@Str(E.A)
@En(E.A)
@ArrAnn(E.A)
@ArrPrimitive(E.A)
@ArrStr(E.A)
@ArrEn(E.A)
class EnC { }

@Ann({@Override})
@Primitive({@Override})
@Str({@Override})
@En({@Override})
@ArrAnn({@Override})
@ArrPrimitive({@Override})
@ArrStr({@Override})
@ArrEn({@Override})
class ArrAnnC { }

class ArrPrimitiveC {
    private static final int C = 1;
    @Ann({C})
    @Primitive({C})
    @Str({C})
    @En({C})
    @ArrAnn({C})
    @ArrPrimitive({C})
    @ArrStr({C})
    @ArrEn({C})
    class I {
    }
}

class ArrStringC {
    private static final String C = "";
    @Ann({C})
    @Primitive({C})
    @Str({C})
    @En({C})
    @ArrAnn({C})
    @ArrPrimitive({C})
    @ArrStr({C})
    @ArrEn({C})
    class I {
    }
}

@Ann({E.A})
@Primitive({E.A})
@Str({E.A})
@En({E.A})
@ArrAnn({E.A})
@ArrPrimitive({E.A})
@ArrStr({E.A})
@ArrEn({E.A})
class ArrEnC { }

@interface Ann {
    Override value();
}

@interface Primitive {
    int value();
}

@interface Str {
    String value();
}

@interface En {
    E value();
}

@interface ArrAnn {
    Override[] value();
}

@interface ArrPrimitive {
    int[] value();
}

@interface ArrStr {
    String[] value();
}

@interface ArrEn {
    E[] value();
}

enum E {
    A;
}
