/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */


package nsk.jdi.TypeComponent.isStatic;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;


public class isstatic002a {
    public static void main (String argv[]) {
        ArgumentHandler argHandler = new ArgumentHandler(argv);
        Log log = new Log(System.err, argHandler);
        IOPipe pipe = argHandler.createDebugeeIOPipe(log);
        isstatic002aClassToCheck classToCheck = new isstatic002aClassToCheck();

        log.display("DEBUGEE> debugee started.");
        pipe.println("ready");
        String instruction = pipe.readln();
        if (instruction.equals("quit")) {
            log.display("DEBUGEE> \"quit\" signal recieved.");
            log.display("DEBUGEE> completed succesfully.");
            System.exit(95);
        }
        log.complain("DEBUGEE FAILURE> unexpected signal "
                         + "(no \"quit\") - " + instruction);
        log.complain("DEBUGEE FAILURE> TEST FAILED");
        System.exit(97);
    }
}

class isstatic002aClassToCheck {
    // User class and interface
    class Class {}
    interface Inter {}

    // Methods
    void        Mv() {};
    boolean     Mz(boolean z)      { return z; };
    boolean[]   Mz1(boolean z[])   { return z; };
    boolean[][] Mz2(boolean z[][]) { return z; };
    byte        Mb(byte b)         { return b; };
    byte[]      Mb1(byte b[])      { return b; };
    byte[][]    Mb2(byte b[][])    { return b; };
    char        Mc(char c)         { return c; };
    char[]      Mc1(char c[])      { return c; };
    char[][]    Mc2(char c[][])    { return c; };
    double      Md(double d)       { return d; };
    double[]    Md1(double d[])    { return d; };
    double[][]  Md2(double d[][])  { return d; };
    float       Mf(float f)        { return f; };
    float[]     Mf1(float f[])     { return f; };
    float[][]   Mf2(float f[][])   { return f; };
    int         Mi(int i)          { return i; };
    int[]       Mi1(int i[])       { return i; };
    int[][]     Mi2(int i[][])     { return i; };
    long        Ml(long l)         { return l; };
    long[]      Ml1(long l[])      { return l; };
    long[][]    Ml2(long l[][])    { return l; };
    short       Mr(short r)        { return r; };
    short[]     Mr1(short r[])     { return r; };
    short[][]   Mr2(short r[][])   { return r; };

    static void        MvS() {};
    static boolean     MzS(boolean z)      { return z; };
    static boolean[]   Mz1S(boolean z[])   { return z; };
    static boolean[][] Mz2S(boolean z[][]) { return z; };
    static byte        MbS(byte b)         { return b; };
    static byte[]      Mb1S(byte b[])      { return b; };
    static byte[][]    Mb2S(byte b[][])    { return b; };
    static char        McS(char c)         { return c; };
    static char[]      Mc1S(char c[])      { return c; };
    static char[][]    Mc2S(char c[][])    { return c; };
    static double      MdS(double d)       { return d; };
    static double[]    Md1S(double d[])    { return d; };
    static double[][]  Md2S(double d[][])  { return d; };
    static float       MfS(float f)        { return f; };
    static float[]     Mf1S(float f[])     { return f; };
    static float[][]   Mf2S(float f[][])   { return f; };
    static int         MiS(int i)          { return i; };
    static int[]       Mi1S(int i[])       { return i; };
    static int[][]     Mi2S(int i[][])     { return i; };
    static long        MlS(long l)         { return l; };
    static long[]      Ml1S(long l[])      { return l; };
    static long[][]    Ml2S(long l[][])    { return l; };
    static short       MrS(short r)        { return r; };
    static short[]     Mr1S(short r[])     { return r; };
    static short[][]   Mr2S(short r[][])   { return r; };

    final        void MvF()                {};
    final        long MlF(long l)          { return l; };
    final        long[] MlF1(long l[])     { return l; };
    final        long[][] MlF2(long l[][]) { return l; };
    native       void MvN();
    native       long MlN(long l);
    native       long[] MlN1(long l[]);
    native       long[][] MlN2(long l[][]);
    strictfp     void MvI() {};
    strictfp     long MlI(long l)          { return l; };
    strictfp     long[] MlI1(long l[])     { return l; };
    strictfp     long[][] MlI2(long l[][]) { return l; };
    synchronized void MvY() {};
    synchronized long MlY(long l)          { return l; };
    synchronized long[] MlY1(long l[])     { return l; };
    synchronized long[][] MlY2(long l[][]) { return l; };
    public       void MvU() {};
    public       long MlU(long l)          { return l; };
    public       long[] MlU1(long l[])     { return l; };
    public       long[][] MlU2(long l[][]) { return l; };
    protected    void MvR() {};
    protected    long MlR(long l)          { return l; };
    protected    long[] MlR1(long l[])     { return l; };
    protected    long[][] MlR2(long l[][]) { return l; };
    private      void MvP() {};
    private      long MlP(long l)          { return l; };
    private      long[] MlP1(long l[])     { return l; };
    private      long[][] MlP2(long l[][]) { return l; };

    static final        void MvFS()                {};
    static final        long MlFS(long l)          { return l; };
    static final        long[] MlF1S(long l[])     { return l; };
    static final        long[][] MlF2S(long l[][]) { return l; };
    static native       void MvNS();
    static native       long MlNS(long l);
    static native       long[] MlN1S(long l[]);
    static native       long[][] MlN2S(long l[][]);
    static strictfp     void MvIS() {};
    static strictfp     long MlIS(long l)          { return l; };
    static strictfp     long[] MlI1S(long l[])     { return l; };
    static strictfp     long[][] MlI2S(long l[][]) { return l; };
    static synchronized void MvYS() {};
    static synchronized long MlYS(long l)          { return l; };
    static synchronized long[] MlY1S(long l[])     { return l; };
    static synchronized long[][] MlY2S(long l[][]) { return l; };
    static public       void MvUS() {};
    static public       long MlUS(long l)          { return l; };
    static public       long[] MlU1S(long l[])     { return l; };
    static public       long[][] MlU2S(long l[][]) { return l; };
    static protected    void MvRS() {};
    static protected    long MlRS(long l)          { return l; };
    static protected    long[] MlR1S(long l[])     { return l; };
    static protected    long[][] MlR2S(long l[][]) { return l; };
    static private      void MvPS() {};
    static private      long MlPS(long l)          { return l; };
    static private      long[] MlP1S(long l[])     { return l; };
    static private      long[][] MlP2S(long l[][]) { return l; };

    Class         MX(Class X)          { return X; };
    Class[]       MX1(Class X[])       { return X; };
    Class[][]     MX2(Class X[][])     { return X; };
    Object        MO(Object O)         { return O; };
    Object[]      MO1(Object[] O)      { return O; };
    Object[][]    MO2(Object[][] O)    { return O; };

    static Class         MXS(Class X)          { return X; };
    static Class[]       MX1S(Class X[])       { return X; };
    static Class[][]     MX2S(Class X[][])     { return X; };
    static Object        MOS(Object O)         { return O; };
    static Object[]      MO1S(Object[] O)      { return O; };
    static Object[][]    MO2S(Object[][] O)    { return O; };

    final        Long     MLF(Long L)      { return L; };
    final        Long[]   MLF1(Long[] L)   { return L; };
    final        Long[][] MLF2(Long[][] L) { return L; };
    native       Long     MLN(Long L);
    native       Long[]   MLN1(Long L[]);
    native       Long[][] MLN2(Long L[][]);
    strictfp     Long     MLI(Long L)      { return L; };
    strictfp     Long[]   MLI1(Long L[])   { return L; };
    strictfp     Long[][] MLI2(Long L[][]) { return L; };
    synchronized Long     MLY(Long L)      { return L; };
    synchronized Long[]   MLY1(Long[] L)   { return L; };
    synchronized Long[][] MLY2(Long[][] L) { return L; };
    public       Long     MLU(Long L)      { return L; };
    public       Long[]   MLU1(Long[] L)   { return L; };
    public       Long[][] MLU2(Long[][] L) { return L; };
    protected    Long     MLR(Long L)      { return L; };
    protected    Long[]   MLR1(Long[] L)   { return L; };
    protected    Long[][] MLR2(Long[][] L) { return L; };
    private      Long     MLP(Long L)      { return L; };
    private      Long[]   MLP1(Long[] L)   { return L; };
    private      Long[][] MLP2(Long[][] L) { return L; };

    static final        Long     MLFS(Long L)      { return L; };
    static final        Long[]   MLF1S(Long[] L)   { return L; };
    static final        Long[][] MLF2S(Long[][] L) { return L; };
    static native       Long     MLNS(Long L);
    static native       Long[]   MLN1S(Long[] L);
    static native       Long[][] MLN2S(Long[][] L);
    static strictfp     Long     MLIS(Long L)      { return L; };
    static strictfp     Long[]   MLI1S(Long[] L)   { return L; };
    static strictfp     Long[][] MLI2S(Long[][] L) { return L; };
    static synchronized Long     MLYS(Long L)      { return L; };
    static synchronized Long[]   MLY1S(Long[] L)   { return L; };
    static synchronized Long[][] MLY2S(Long[][] L) { return L; };
    static public       Long     MLUS(Long L)      { return L; };
    static public       Long[]   MLU1S(Long[] L)   { return L; };
    static public       Long[][] MLU2S(Long[][] L) { return L; };
    static protected    Long     MLRS(Long L)      { return L; };
    static protected    Long[]   MLR1S(Long[] L)   { return L; };
    static protected    Long[][] MLR2S(Long[][] L) { return L; };
    static private      Long     MLPS(Long L)      { return L; };
    static private      Long[]   MLP1S(Long[] L)   { return L; };
    static private      Long[][] MLP2S(Long[][] L) { return L; };

    Inter     ME(Inter E)      { return E; };
    Inter[]   ME1(Inter[] E)   { return E; };
    Inter[][] ME2(Inter[][] E) { return E; };
    static Inter     MES(Inter E)      { return E; };
    static Inter[]   ME1S(Inter[] E)   { return E; };
    static Inter[][] ME2S(Inter[][] E) { return E; };

    final        Inter     MEF(Inter E)      { return E; };
    final        Inter[]   MEF1(Inter[] E)   { return E; };
    final        Inter[][] MEF2(Inter[][] E) { return E; };
    native       Inter     MEN(Inter E);
    native       Inter[]   MEN1(Inter[] E);
    native       Inter[][] MEN2(Inter[][] E);
    strictfp     Inter     MEI(Inter E)      { return E; };
    strictfp     Inter[]   MEI1(Inter[] E)   { return E; };
    strictfp     Inter[][] MEI2(Inter[][] E) { return E; };
    synchronized Inter     MEY(Inter E)      { return E; };
    synchronized Inter[]   MEY1(Inter[] E)   { return E; };
    synchronized Inter[][] MEY2(Inter[][] E) { return E; };
    public       Inter     MEU(Inter E)      { return E; };
    public       Inter[]   MEU1(Inter[] E)   { return E; };
    public       Inter[][] MEU2(Inter[][] E) { return E; };
    protected    Inter     MER(Inter E)      { return E; };
    protected    Inter[]   MER1(Inter[] E)   { return E; };
    protected    Inter[][] MER2(Inter[][] E) { return E; };
    private      Inter     MEP(Inter E)      { return E; };
    private      Inter[]   MEP1(Inter[] E)   { return E; };
    private      Inter[][] MEP2(Inter[][] E) { return E; };

    static final        Inter     MEFS(Inter E)      { return E; };
    static final        Inter[]   MEF1S(Inter[] E)   { return E; };
    static final        Inter[][] MEF2S(Inter[][] E) { return E; };
    static native       Inter     MENS(Inter E);
    static native       Inter[]   MEN1S(Inter[] E);
    static native       Inter[][] MEN2S(Inter[][] E);
    static strictfp     Inter     MEIS(Inter E)      { return E; };
    static strictfp     Inter[]   MEI1S(Inter[] E)   { return E; };
    static strictfp     Inter[][] MEI2S(Inter[][] E) { return E; };
    static synchronized Inter     MEYS(Inter E)      { return E; };
    static synchronized Inter[]   MEY1S(Inter[] E)   { return E; };
    static synchronized Inter[][] MEY2S(Inter[][] E) { return E; };
    static public       Inter     MEUS(Inter E)      { return E; };
    static public       Inter[]   MEU1S(Inter[] E)   { return E; };
    static public       Inter[][] MEU2S(Inter[][] E) { return E; };
    static protected    Inter     MERS(Inter E)      { return E; };
    static protected    Inter[]   MER1S(Inter[] E)   { return E; };
    static protected    Inter[][] MER2S(Inter[][] E) { return E; };
    static private      Inter     MEPS(Inter E)      { return E; };
    static private      Inter[]   MEP1S(Inter[] E)   { return E; };
    static private      Inter[][] MEP2S(Inter[][] E) { return E; };
}
