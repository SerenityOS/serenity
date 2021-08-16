/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4902952 4905407 4916149 8057793
 * @summary Tests that the scale of zero is propagated properly and has the
 * proper effect and that setting the scale to zero does not mutate the
 * BigDecimal.
 * @author Joseph D. Darcy
 */

import java.math.*;
import java.util.*;

public class ZeroScalingTests {

    static MathContext longEnough = new MathContext(50, RoundingMode.UNNECESSARY);

    static BigDecimal[]  zeros = new BigDecimal[23];
    static {
        for(int i = 0; i < 21; i++) {
            zeros[i] = new BigDecimal(BigInteger.ZERO, i-10);
        }
        zeros[21] = new BigDecimal(BigInteger.ZERO, Integer.MIN_VALUE);
        zeros[22] = new BigDecimal(BigInteger.ZERO, Integer.MAX_VALUE);
    }

    static BigDecimal element = BigDecimal.valueOf(100, -2);

    static MathContext contexts[] = {
        new MathContext(0, RoundingMode.UNNECESSARY),
        new MathContext(100, RoundingMode.UNNECESSARY),
        new MathContext(5, RoundingMode.UNNECESSARY),
        new MathContext(4, RoundingMode.UNNECESSARY),
        new MathContext(3, RoundingMode.UNNECESSARY),
        new MathContext(2, RoundingMode.UNNECESSARY),
        new MathContext(1, RoundingMode.UNNECESSARY),
    };


    static int addTests() {
        int failures = 0;

        for(BigDecimal zero1: zeros) {
            for(BigDecimal zero2: zeros) {
                BigDecimal expected = new BigDecimal(BigInteger.ZERO,
                                                     Math.max(zero1.scale(), zero2.scale()));
                BigDecimal result;

                if(! (result=zero1.add(zero2)).equals(expected) ) {
                    failures++;
                    System.err.println("For classic exact add, expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

                if(! (result=zero1.add(zero2, MathContext.UNLIMITED)).equals(expected) ) {
                    failures++;
                    System.err.println("For UNLIMITED math context add," +
                                       " expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

                if(! (result=zero1.add(zero2, longEnough)).equals(expected) ) {
                    failures++;
                    System.err.println("For longEnough math context add," +
                                       " expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

            }
        }

        // Test effect of adding zero to a nonzero value.
        for (MathContext mc: contexts) {
            for (BigDecimal zero: zeros) {
                if (Math.abs((long)zero.scale()) < 100 ) {

                    int preferredScale = Math.max(zero.scale(), element.scale());
                    if (mc.getPrecision() != 0) {
                        if (preferredScale < -4 )
                            preferredScale = -4;
                        else if (preferredScale > -(5 - mc.getPrecision())) {
                            preferredScale = -(5 - mc.getPrecision());
                        }
                    }


                    /*
                      System.err.println("\n    " + element + " +\t" + zero + " =\t" + result);

                      System.err.println("scales" + element.scale() + " \t" + zero.scale() +
                      "  \t " + result.scale() + "\t precison = " + mc.getPrecision());
                      System.err.println("expected scale = " + preferredScale);
                    */

                    BigDecimal result = element.add(zero, mc);
                    if (result.scale() != preferredScale ||
                            result.compareTo(element) != 0) {
                        failures++;
                        System.err.println("Expected scale  " + preferredScale +
                                           " result scale was " + result.scale() +
                                           " ; value was " + result);
                    }

                    result = zero.add(element, mc);
                    if (result.scale() != preferredScale ||
                            result.compareTo(element) != 0) {
                        failures++;
                        System.err.println("Expected scale  " + preferredScale +
                                           " result scale was " + result.scale() +
                                           " ; value was " + result);
                    }

                    result = element.negate().add(zero, mc);
                    if (result.scale() != preferredScale ||
                            result.compareTo(element.negate()) != 0) {
                        failures++;
                        System.err.println("Expected scale  " + preferredScale +
                                           " result scale was " + result.scale() +
                                           " ; value was " + result);
                    }

                    result = zero.add(element.negate(), mc);
                    if (result.scale() != preferredScale ||
                            result.compareTo(element.negate()) != 0) {
                        failures++;
                        System.err.println("Expected scale  " + preferredScale +
                                           " result scale was " + result.scale() +
                                           " ; value was " + result);
                    }

                }
            }
        }

        return failures;
    }

    static int subtractTests() {
        int failures = 0;

        for(BigDecimal zero1: zeros) {
            for(BigDecimal zero2: zeros) {
                BigDecimal expected = new BigDecimal(BigInteger.ZERO,
                                                     Math.max(zero1.scale(), zero2.scale()));
                BigDecimal result;

                if(! (result=zero1.subtract(zero2)).equals(expected) ) {
                    failures++;
                    System.err.println("For classic exact subtract, expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

                if(! (result=zero1.subtract(zero2, MathContext.UNLIMITED)).equals(expected) ) {
                    failures++;
                    System.err.println("For UNLIMITED math context subtract," +
                                       " expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

                if(! (result=zero1.subtract(zero2, longEnough)).equals(expected) ) {
                    failures++;
                    System.err.println("For longEnough math context subtract," +
                                       " expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

            }
        }


        // Test effect of adding zero to a nonzero value.
        for (MathContext mc: contexts) {
            for (BigDecimal zero: zeros) {
                if (Math.abs((long)zero.scale()) < 100 ) {

                    int preferredScale = Math.max(zero.scale(), element.scale());
                    if (mc.getPrecision() != 0) {
                        if (preferredScale < -4 )
                            preferredScale = -4;
                        else if (preferredScale > -(5 - mc.getPrecision())) {
                            preferredScale = -(5 - mc.getPrecision());
                        }
                    }


                    /*
                      System.err.println("\n    " + element + " +\t" + zero + " =\t" + result);

                      System.err.println("scales" + element.scale() + " \t" + zero.scale() +
                      "  \t " + result.scale() + "\t precison = " + mc.getPrecision());
                      System.err.println("expected scale = " + preferredScale);
                    */

                    BigDecimal result = element.subtract(zero, mc);
                    if (result.scale() != preferredScale ||
                            result.compareTo(element) != 0) {
                        failures++;
                        System.err.println("Expected scale  " + preferredScale +
                                           " result scale was " + result.scale() +
                                           " ; value was " + result);
                    }

                    result = zero.subtract(element, mc);
                    if (result.scale() != preferredScale ||
                            result.compareTo(element.negate()) != 0) {
                        failures++;
                        System.err.println("Expected scale  " + preferredScale +
                                           " result scale was " + result.scale() +
                                           " ; value was " + result);
                    }

                    result = element.negate().subtract(zero, mc);
                    if (result.scale() != preferredScale ||
                            result.compareTo(element.negate()) != 0) {
                        failures++;
                        System.err.println("Expected scale  " + preferredScale +
                                           " result scale was " + result.scale() +
                                           " ; value was " + result);
                    }

                    result = zero.subtract(element.negate(), mc);
                    if (result.scale() != preferredScale ||
                            result.compareTo(element) != 0) {
                        failures++;
                        System.err.println("Expected scale  " + preferredScale +
                                           " result scale was " + result.scale() +
                                           " ; value was " + result);
                    }

                }
            }
        }

        return failures;
    }

    static int multiplyTests() {
        int failures = 0;

        BigDecimal ones[] = {
            BigDecimal.valueOf(1, 0),
            BigDecimal.valueOf(10, 1),
            BigDecimal.valueOf(1000, 3),
            BigDecimal.valueOf(100000000, 8),
        };

        List<BigDecimal> values = new LinkedList<BigDecimal>();
        values.addAll(Arrays.asList(zeros));
        values.addAll(Arrays.asList(ones));

        for(BigDecimal zero1: zeros) {
            for(BigDecimal value: values) {
                BigDecimal expected = new BigDecimal(BigInteger.ZERO,
                                                     (int)Math.min(Math.max((long)zero1.scale()+value.scale(),
                                                                            Integer.MIN_VALUE ),
                                                                   Integer.MAX_VALUE ) );
                BigDecimal result;

                if(! (result=zero1.multiply(value)).equals(expected) ) {
                    failures++;
                    System.err.println("For classic exact multiply, expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

                if(! (result=zero1.multiply(value, MathContext.UNLIMITED)).equals(expected) ) {
                    failures++;
                    System.err.println("For UNLIMITED math context multiply," +
                                       " expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

                if(! (result=zero1.multiply(value, longEnough)).equals(expected) ) {
                    failures++;
                    System.err.println("For longEnough math context multiply," +
                                       " expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

            }
        }

        return failures;
    }

    static int divideTests() {
        int failures = 0;

        BigDecimal [] ones = {
            BigDecimal.valueOf(1, 0),
            BigDecimal.valueOf(10, -1),
            BigDecimal.valueOf(100, -2),
            BigDecimal.valueOf(1000, -3),
            BigDecimal.valueOf(1000000, -5),
        };

        for(BigDecimal one: ones) {
            for(BigDecimal zero: zeros) {
                BigDecimal expected = new BigDecimal(BigInteger.ZERO,
                                                     (int)Math.min(Math.max((long)zero.scale() - one.scale(),
                                                                            Integer.MIN_VALUE ),
                                                                   Integer.MAX_VALUE ) );
                BigDecimal result;

                if(! (result=zero.divide(one)).equals(expected) ) {
                    failures++;
                    System.err.println("For classic exact divide, expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

                if(! (result=zero.divide(one, MathContext.UNLIMITED)).equals(expected) ) {
                    failures++;
                    System.err.println("For UNLIMITED math context divide," +
                                       " expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

                if(! (result=zero.divide(one, longEnough)).equals(expected) ) {
                    failures++;
                    System.err.println("For longEnough math context divide," +
                                       " expected scale of " +
                                       expected.scale() + "; got " +
                                       result.scale() + ".");
                }

            }
        }

        return failures;
    }

    static int setScaleTests() {
        int failures = 0;

        int scales[] = {
            Integer.MIN_VALUE,
            Integer.MIN_VALUE+1,
            -10000000,
            -3,
            -2,
            -1,
            0,
            1,
            2,
            3,
            10,
            10000000,
            Integer.MAX_VALUE-1,
            Integer.MAX_VALUE
        };

        for(BigDecimal zero: zeros) {
            for(int scale: scales) {
                try {
                    BigDecimal bd = zero.setScale(scale);
                }
                catch (ArithmeticException e) {
                    failures++;
                    System.err.println("Exception when trying to set a scale of " + scale +
                                       " on " + zero);
                }
            }
        }

        return failures;
    }

    static int toEngineeringStringTests() {
        int failures = 0;

        String [][] testCases  = {
            {"0E+10",   "0.00E+12"},
            {"0E+9",    "0E+9"},
            {"0E+8",    "0.0E+9"},
            {"0E+7",    "0.00E+9"},

            {"0E-10",   "0.0E-9"},
            {"0E-9",    "0E-9"},
            {"0E-8",    "0.00E-6"},
            {"0E-7",    "0.0E-6"},
        };

        for(String[] testCase: testCases) {
            BigDecimal bd = new BigDecimal(testCase[0]);
            String result = bd.toEngineeringString();

            if (!result.equals(testCase[1]) ||
                !bd.equals(new BigDecimal(result))) {
                failures++;
                System.err.println("From input ``" + testCase[0] + ",'' " +
                                   " bad engineering string output ``" + result +
                                   "''; expected ``" + testCase[1] + ".''");
            }

        }

        return failures;
    }

    static int ulpTests() {
        int failures = 0;

        for(BigDecimal zero: zeros) {
            BigDecimal result;
            BigDecimal expected = BigDecimal.valueOf(1, zero.scale());

            if (! (result=zero.ulp()).equals(expected) ) {
                failures++;
                System.err.println("Unexpected ulp value for zero value " +
                                   zero + "; expected " + expected +
                                   ", got " + result);
            }
        }

        return failures;
    }

    static int setScaleDoesNotMutateTest() {
        BigDecimal total = new BigDecimal("258815507198903607775511093103396443816569106750031264155319238473795838680758514810110764742309284477206138527975952150289602995045050194333030191178778772026538699925775139201970526695485362661420908248887297829319881475178467494779683293036572059595504702727301324759997409522995072582369210284334718757260859794972695026582432867589093687280300148141501712013226636373167978223780290547640482160818746599330924736802844173226042389174403401903999447463440670236056324929325189403433689"
                + ".426167432065785331444814035799717606745777287606858873045971898862329763544687891847664736523584843544347118836628373041412918374550458884706686730726101338872517021688769782894793734049819222924171842793485919753186993388451909096042127903835765393729547730953942175461146061715108701615615142134282261293656760570061554783195726716403304101469782303957325142638493327692352838806741611887655695029948975509680496573999174402058593454203190963443179532640446352828089016874853634851387762579319853267317320515941105912189838719919259277721994880193541634872882180184303434360412344059435559680494807415573269199203376126242271766939666939316648575065702750502798973418978204972336924254702551350654650573582614211506856383897692911422458286912085339575875324832979140870119455620532272318122103640233069115700020760625493816902806241630788230268031695140687964931377988962507263990468276009750998066442971308866347136022907166625330623130307555914930120150437900510530537258665172619821272937026713977709974434967165159545592482710663639966781678268622620229577009317698254134914742098420792313931843709810905414336383757407675429663714210967924767434203021205270369316797752411974617662200898086335322218191674846795163102021505555508444216708745911194321674887527227200297039471799580744303346354057273540730643842091810899490590914195225087593013834388801018488174855060306804024894292757613618190472234110859436472645203753139820658279559340251226992556744343475086923568365637919479462424794554522865559888240039662899509652221329892034706445253487898044421278283079233226845124525434586324657471286953226255430662125870993375281512713207125720748163498642795960457639954616530163959004770092547297392499137383176609646505351001304840762905826237024982330597805063521162285806541220110524989649256399233792799406995068469271941269511818994954109392839548141262324660472253632382325038836831429045617036015122388070240133760858500132713255407855625837956886349324981003917084922808187223285051144454915441134217743066575863563572152133978905444998209075763950909784148142018992367290485890072303179512881131769414783097454103103347826517701720263541869335631166977965013552647906729408522950996105479525445916501155305220090853891226367184989434453290788068397817927893708837722255115237672194162924260945492012622891770365546831236789867922136747819364833843397165107825773447549885351449899330007200651144003961228091210630807333236718793283427788965479074476288255387824982443633190938302785760754436525586544523339170400053128503337395428393881357669568532722167493096151221381017320147344991331421789379785964440840684363041795410525097564979585773948558651896834067324427900848255265001498890329859444233861478388742393060996236783742654761350763876989363052609107226398858310051497856931093693697981165801539060516895227818925342535261227134364063673285588256280386915163875872231395348293505967057794409379709079685798908660258077792158532257603211711587587586356431658240229896344639704");
        if (total.setScale(0, RoundingMode.DOWN).equals(total.setScale(0, RoundingMode.DOWN))) {
            return 0;
        } else {
            return 1;
        }
    }

    public static void main(String argv[]) {
        int failures = 0;

        failures += addTests();
        failures += subtractTests();
        failures += multiplyTests();
        failures += divideTests();
        failures += setScaleTests();
        failures += toEngineeringStringTests();
        failures += ulpTests();
        failures += setScaleDoesNotMutateTest();

        if (failures > 0 ) {
            throw new RuntimeException("Incurred " + failures + " failures" +
                                       " testing the preservation of zero scales.");
        }
    }
}
