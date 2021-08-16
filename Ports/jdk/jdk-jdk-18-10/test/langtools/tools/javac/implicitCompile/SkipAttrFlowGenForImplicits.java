/*
 * @test /nodynamiccopyright/
 * @bug 8030714
 * @summary make sure attribute and flow is skipped for implicit classes
 * @compile/ref=SkipAttrFlowGenForImplicits.out -XDverboseCompilePolicy -implicit:none SkipAttrFlowGenForImplicits.java
 */
class Explicit {
    Implicit implicit;
}
