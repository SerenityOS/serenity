/* /nodynamiccopyright/ */

public class C
{
    // U+10400 (\ud801\udc00): DESERET CAPITAL LETTER LONG I (can be start or part)
    // U+1D17B (\ud834\udd7b): MUSICAL SYMBOL COMBINING ACCENT (can only be part)
    // U+1D100 (\ud834\udd00): MUSICAL SYMBOL SINGLE BARLINE (can be none of start nor part)

    // valid tags

    /**
     * @see C#\ud801\udc00method()
     */
    public void \ud801\udc00method() {};

    /**
     * @see C#method\ud801\udc00()
     */
    public void method\ud801\udc00() {};

    /**
     * @see C#method\ud834\udd7b()
     */
    public void method\ud834\udd7b() {};

    /**
     * @serialField \ud801\udc00field int
     * @serialField field\ud801\udc00 int
     * @serialField field\ud834\udd7b int
     */
    public void method1() {};

    // invalid tags - should generate warnings

    /**
     * @see C#method\ud834\udd00()
     */
    public void method2() {};

    /**
     * @serialField field\ud801\ud801 int
     * @serialField \ud834\udd7bfield int
     */
    public void method3() {};
}
