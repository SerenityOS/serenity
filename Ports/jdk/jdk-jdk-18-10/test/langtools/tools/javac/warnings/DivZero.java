/**
 * @test  /nodynamiccopyright/
 * @bug 4759494 4986256
 * @compile/ref=DivZero.noLint.out                   -XDrawDiagnostics DivZero.java
 * @compile/ref=DivZero.lint.out    -Xlint:divzero   -XDrawDiagnostics DivZero.java
 * @compile/ref=DivZero.lint.out    -Xlint:all,-path -XDrawDiagnostics DivZero.java
 */

class DivZero
{
    public static final int ONE = 1;

    public int i1 = 1 / 0;
    public static final int i2 = 1 / 0;

    public int i3 = 1 % 0;
    public static final int i4 = 1 % 0;

    public static final int i5 = 1 / (1 - 1);
    public static final int i6 = 1 / (ONE - 1);
    public static final int i7 = 1 / (ONE - ONE);

    public  static final int i8 = 1 % (1 - 1);
    public  static final int i9 = 1 % (ONE - 1);
    public  static final int i10 = 1 % (ONE - ONE);

    public static final long ONEL = 1L;

    public long l1 = 1L / 0L;
    public static final long l2 = 1L / 0L;

    public long l3 = 1L % 0L;
    public static final long l4 = 1L % 0L;

    public static final long l5 = 1L / (1L - 1L);
    public static final long l6 = 1L / (ONEL - 1L);
    public static final long l7 = 1L / (ONEL - ONEL);

    public  static final long l8 = 1L % (1L - 1L);
    public  static final long l9 = 1L % (ONEL - 1L);
    public  static final long l10 = 1L % (ONEL - ONEL);


    static void m() {
        int mi1 = 1 / 0;
        int mi2 = 1 % 0;

        mi1 /= 0;
        mi1 %= 0;

        long ml1 = 1L / 0L;
        long ml2 = 1L % 0L;

        ml1 /= 0L;
        ml1 %= 0L;
    }
}

@SuppressWarnings("divzero")
class DivZero2
{
    public static final int ONE = 1;

    public int i1 = 1 / 0;
    public static final int i2 = 1 / 0;

    public int i3 = 1 % 0;
    public static final int i4 = 1 % 0;

    public static final int i5 = 1 / (1 - 1);
    public static final int i6 = 1 / (ONE - 1);
    public static final int i7 = 1 / (ONE - ONE);

    public  static final int i8 = 1 % (1 - 1);
    public  static final int i9 = 1 % (ONE - 1);
    public  static final int i10 = 1 % (ONE - ONE);

    public static final long ONEL = 1L;

    public long l1 = 1L / 0L;
    public static final long l2 = 1L / 0L;

    public long l3 = 1L % 0L;
    public static final long l4 = 1L % 0L;

    public static final long l5 = 1L / (1L - 1L);
    public static final long l6 = 1L / (ONEL - 1L);
    public static final long l7 = 1L / (ONEL - ONEL);

    public  static final long l8 = 1L % (1L - 1L);
    public  static final long l9 = 1L % (ONEL - 1L);
    public  static final long l10 = 1L % (ONEL - ONEL);


    static void m() {
        int mi1 = 1 / 0;
        int mi2 = 1 % 0;

        mi1 /= 0;
        mi1 %= 0;

        long ml1 = 1L / 0L;
        long ml2 = 1L % 0L;

        ml1 /= 0L;
        ml1 %= 0L;
    }
}

class DivZero3
{
    @SuppressWarnings("divzero")
    public int i1 = 1 / 0;
    @SuppressWarnings("divzero")
    public static final int i2 = 1 / 0;

    @SuppressWarnings("divzero")
    public int i3 = 1 % 0;
    @SuppressWarnings("divzero")
    public static final int i4 = 1 % 0;

    @SuppressWarnings("divzero")
    public long l1 = 1L / 0L;
    @SuppressWarnings("divzero")
    public static final long l2 = 1L / 0L;

    @SuppressWarnings("divzero")
    public long l3 = 1L % 0L;
    @SuppressWarnings("divzero")
    public static final long l4 = 1L % 0L;


    @SuppressWarnings("divzero")
    static void m() {
        int mi1 = 1 / 0;
        int mi2 = 1 % 0;

        mi1 /= 0;
        mi1 %= 0;

        long ml1 = 1L / 0L;
        long ml2 = 1L % 0L;

        ml1 /= 0L;
        ml1 %= 0L;
    }
}
