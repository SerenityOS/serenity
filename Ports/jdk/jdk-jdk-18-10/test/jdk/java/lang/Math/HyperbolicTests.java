/*
 * Copyright (c) 2003, 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4851625 4900189 4939441
 * @summary Tests for {Math, StrictMath}.{sinh, cosh, tanh}
 * @author Joseph D. Darcy
 */

public class HyperbolicTests {
    private HyperbolicTests(){}

    static final double NaNd = Double.NaN;

    /**
     * Test accuracy of {Math, StrictMath}.sinh.  The specified
     * accuracy is 2.5 ulps.
     *
     * The defintion of sinh(x) is
     *
     * (e^x - e^(-x))/2
     *
     * The series expansion of sinh(x) =
     *
     * x + x^3/3! + x^5/5! + x^7/7! +...
     *
     * Therefore,
     *
     * 1. For large values of x sinh(x) ~= signum(x)*exp(|x|)/2
     *
     * 2. For small values of x, sinh(x) ~= x.
     *
     * Additionally, sinh is an odd function; sinh(-x) = -sinh(x).
     *
     */
    static int testSinh() {
        int failures = 0;
        /*
         * Array elements below generated using a quad sinh
         * implementation.  Rounded to a double, the quad result
         * *should* be correctly rounded, unless we are quite unlucky.
         * Assuming the quad value is a correctly rounded double, the
         * allowed error is 3.0 ulps instead of 2.5 since the quad
         * value rounded to double can have its own 1/2 ulp error.
         */
        double [][] testCases = {
            // x                sinh(x)
            {0.0625,            0.06254069805219182172183988501029229},
            {0.1250,            0.12532577524111545698205754229137154},
            {0.1875,            0.18860056562029018382047025055167585},
            {0.2500,            0.25261231680816830791412515054205787},
            {0.3125,            0.31761115611357728583959867611490292},
            {0.3750,            0.38385106791361456875429567642050245},
            {0.4375,            0.45159088610312053032509815226723017},
            {0.5000,            0.52109530549374736162242562641149155},
            {0.5625,            0.59263591611468777373870867338492247},
            {0.6250,            0.66649226445661608227260655608302908},
            {0.6875,            0.74295294580567543571442036910465007},
            {0.7500,            0.82231673193582998070366163444691386},
            {0.8125,            0.90489373856606433650504536421491368},
            {0.8750,            0.99100663714429475605317427568995231},
            {0.9375,            1.08099191569306394011007867453992548},
            {1.0000,            1.17520119364380145688238185059560082},
            {1.0625,            1.27400259579739321279181130344911907},
            {1.1250,            1.37778219077984075760379987065228373},
            {1.1875,            1.48694549961380717221109202361777593},
            {1.2500,            1.60191908030082563790283030151221415},
            {1.3125,            1.72315219460596010219069206464391528},
            {1.3750,            1.85111856355791532419998548438506416},
            {1.4375,            1.98631821852425112898943304217629457},
            {1.5000,            2.12927945509481749683438749467763195},
            {1.5625,            2.28056089740825247058075476705718764},
            {1.6250,            2.44075368098794353221372986997161132},
            {1.6875,            2.61048376261693140366028569794027603},
            {1.7500,            2.79041436627764265509289122308816092},
            {1.8125,            2.98124857471401377943765253243875520},
            {1.8750,            3.18373207674259205101326780071803724},
            {1.9375,            3.39865608104779099764440244167531810},
            {2.0000,            3.62686040784701876766821398280126192},
            {2.0625,            3.86923677050642806693938384073620450},
            {2.1250,            4.12673225993027252260441410537905269},
            {2.1875,            4.40035304533919660406976249684469164},
            {2.2500,            4.69116830589833069188357567763552003},
            {2.3125,            5.00031440855811351554075363240262157},
            {2.3750,            5.32899934843284576394645856548481489},
            {2.4375,            5.67850746906785056212578751630266858},
            {2.5000,            6.05020448103978732145032363835040319},
            {2.5625,            6.44554279850040875063706020260185553},
            {2.6250,            6.86606721451642172826145238779845813},
            {2.6875,            7.31342093738196587585692115636603571},
            {2.7500,            7.78935201149073201875513401029935330},
            {2.8125,            8.29572014785741787167717932988491961},
            {2.8750,            8.83450399097893197351853322827892144},
            {2.9375,            9.40780885043076394429977972921690859},
            {3.0000,            10.01787492740990189897459361946582867},
            {3.0625,            10.66708606836969224165124519209968368},
            {3.1250,            11.35797907995166028304704128775698426},
            {3.1875,            12.09325364161259019614431093344260209},
            {3.2500,            12.87578285468067003959660391705481220},
            {3.3125,            13.70862446906136798063935858393686525},
            {3.3750,            14.59503283146163690015482636921657975},
            {3.4375,            15.53847160182039311025096666980558478},
            {3.5000,            16.54262728763499762495673152901249743},
            {3.5625,            17.61142364906941482858466494889121694},
            {3.6250,            18.74903703113232171399165788088277979},
            {3.6875,            19.95991268283598684128844120984214675},
            {3.7500,            21.24878212710338697364101071825171163},
            {3.8125,            22.62068164929685091969259499078125023},
            {3.8750,            24.08097197661255803883403419733891573},
            {3.9375,            25.63535922523855307175060244757748997},
            {4.0000,            27.28991719712775244890827159079382096},
            {4.0625,            29.05111111351106713777825462100160185},
            {4.1250,            30.92582287788986031725487699744107092},
            {4.1875,            32.92137796722343190618721270937061472},
            {4.2500,            35.04557405638942942322929652461901154},
            {4.3125,            37.30671148776788628118833357170042385},
            {4.3750,            39.71362570500944929025069048612806024},
            {4.4375,            42.27572177772344954814418332587050658},
            {4.5000,            45.00301115199178562180965680564371424},
            {4.5625,            47.90615077031205065685078058248081891},
            {4.6250,            50.99648471383193131253995134526177467},
            {4.6875,            54.28608852959281437757368957713936555},
            {4.7500,            57.78781641599226874961859781628591635},
            {4.8125,            61.51535145084362283008545918273109379},
            {4.8750,            65.48325905829987165560146562921543361},
            {4.9375,            69.70704392356508084094318094283346381},
            {5.0000,            74.20321057778875897700947199606456364},
            {5.0625,            78.98932788987998983462810080907521151},
            {5.1250,            84.08409771724448958901392613147384951},
            {5.1875,            89.50742798369883598816307922895346849},
            {5.2500,            95.28051047011540739630959111303975956},
            {5.3125,            101.42590362176666730633859252034238987},
            {5.3750,            107.96762069594029162704530843962700133},
            {5.4375,            114.93122359426386042048760580590182604},
            {5.5000,            122.34392274639096192409774240457730721},
            {5.5625,            130.23468343534638291488502321709913206},
            {5.6250,            138.63433897999898233879574111119546728},
            {5.6875,            147.57571121692522056519568264304815790},
            {5.7500,            157.09373875244884423880085377625986165},
            {5.8125,            167.22561348600435888568183143777868662},
            {5.8750,            178.01092593829229887752609866133883987},
            {5.9375,            189.49181995209921964640216682906501778},
            {6.0000,            201.71315737027922812498206768797872263},
            {6.0625,            214.72269333437984291483666459592578915},
            {6.1250,            228.57126288889537420461281285729970085},
            {6.1875,            243.31297962030799867970551767086092471},
            {6.2500,            259.00544710710289911522315435345489966},
            {6.3125,            275.70998400700299790136562219920451185},
            {6.3750,            293.49186366095654566861661249898332253},
            {6.4375,            312.42056915013535342987623229485223434},
            {6.5000,            332.57006480258443156075705566965111346},
            {6.5625,            354.01908521044116928437570109827956007},
            {6.6250,            376.85144288706511933454985188849781703},
            {6.6875,            401.15635576625530823119100750634165252},
            {6.7500,            427.02879582326538080306830640235938517},
            {6.8125,            454.56986017986077163530945733572724452},
            {6.8750,            483.88716614351897894746751705315210621},
            {6.9375,            515.09527172439720070161654727225752288},
            {7.0000,            548.31612327324652237375611757601851598},
            {7.0625,            583.67953198942753384680988096024373270},
            {7.1250,            621.32368116099280160364794462812762880},
            {7.1875,            661.39566611888784148449430491465857519},
            {7.2500,            704.05206901515336623551137120663358760},
            {7.3125,            749.45957067108712382864538206200700256},
            {7.3750,            797.79560188617531521347351754559776282},
            {7.4375,            849.24903675279739482863565789325699416},
            {7.5000,            904.02093068584652953510919038935849651},
            {7.5625,            962.32530605113249628368993221570636328},
            {7.6250,            1024.38998846242707559349318193113614698},
            {7.6875,            1090.45749701500081956792547346904792325},
            {7.7500,            1160.78599193425808533255719118417856088},
            {7.8125,            1235.65028334242796895820912936318532502},
            {7.8750,            1315.34290508508890654067255740428824014},
            {7.9375,            1400.17525781352742299995139486063802583},
            {8.0000,            1490.47882578955018611587663903188144796},
            {8.0625,            1586.60647216744061169450001100145859236},
            {8.1250,            1688.93381781440241350635231605477507900},
            {8.1875,            1797.86070905726094477721128358866360644},
            {8.2500,            1913.81278009067446281883262689250118009},
            {8.3125,            2037.24311615199935553277163192983440062},
            {8.3750,            2168.63402396170125867037749369723761636},
            {8.4375,            2308.49891634734644432370720900969004306},
            {8.5000,            2457.38431841538268239359965370719928775},
            {8.5625,            2615.87200310986940554256648824234335262},
            {8.6250,            2784.58126450289932429469130598902487336},
            {8.6875,            2964.17133769964321637973459949999057146},
            {8.7500,            3155.34397481384944060352507473513108710},
            {8.8125,            3358.84618707947841898217318996045550438},
            {8.8750,            3575.47316381333288862617411467285480067},
            {8.9375,            3806.07137963459383403903729660349293583},
            {9.0000,            4051.54190208278996051522359589803425598},
            {9.0625,            4312.84391255878980330955246931164633615},
            {9.1250,            4590.99845434696991399363282718106006883},
            {9.1875,            4887.09242236403719571363798584676797558},
            {9.2500,            5202.28281022453561319352901552085348309},
            {9.3125,            5537.80123121853803935727335892054791265},
            {9.3750,            5894.95873086734181634245918412592155656},
            {9.4375,            6275.15090986233399457103055108344546942},
            {9.5000,            6679.86337740502119410058225086262108741},
            {9.5625,            7110.67755625726876329967852256934334025},
            {9.6250,            7569.27686218510919585241049433331592115},
            {9.6875,            8057.45328194243077504648484392156371121},
            {9.7500,            8577.11437549816065709098061006273039092},
            {9.8125,            9130.29072986829727910801024120918114778},
            {9.8750,            9719.14389367880274015504995181862860062},
            {9.9375,            10345.97482346383208590278839409938269134},
            {10.0000,           11013.23287470339337723652455484636420303},
        };

        for(int i = 0; i < testCases.length; i++) {
            double [] testCase = testCases[i];
            failures += testSinhCaseWithUlpDiff(testCase[0],
                                                testCase[1],
                                                3.0);
        }

        double [][] specialTestCases = {
            {0.0,                       0.0},
            {NaNd,                      NaNd},
            {Double.longBitsToDouble(0x7FF0000000000001L),      NaNd},
            {Double.longBitsToDouble(0xFFF0000000000001L),      NaNd},
            {Double.longBitsToDouble(0x7FF8555555555555L),      NaNd},
            {Double.longBitsToDouble(0xFFF8555555555555L),      NaNd},
            {Double.longBitsToDouble(0x7FFFFFFFFFFFFFFFL),      NaNd},
            {Double.longBitsToDouble(0xFFFFFFFFFFFFFFFFL),      NaNd},
            {Double.longBitsToDouble(0x7FFDeadBeef00000L),      NaNd},
            {Double.longBitsToDouble(0xFFFDeadBeef00000L),      NaNd},
            {Double.longBitsToDouble(0x7FFCafeBabe00000L),      NaNd},
            {Double.longBitsToDouble(0xFFFCafeBabe00000L),      NaNd},
            {Double.POSITIVE_INFINITY,  Double.POSITIVE_INFINITY}
        };

        for(int i = 0; i < specialTestCases.length; i++) {
            failures += testSinhCaseWithUlpDiff(specialTestCases[i][0],
                                                specialTestCases[i][1],
                                                0.0);
        }

        // For powers of 2 less than 2^(-27), the second and
        // subsequent terms of the Taylor series expansion will get
        // rounded away since |n-n^3| > 53, the binary precision of a
        // double significand.

        for(int i = DoubleConsts.MIN_SUB_EXPONENT; i < -27; i++) {
            double d = Math.scalb(2.0, i);

            // Result and expected are the same.
            failures += testSinhCaseWithUlpDiff(d, d, 2.5);
        }

        // For values of x larger than 22, the e^(-x) term is
        // insignificant to the floating-point result.  Util exp(x)
        // overflows around 709.8, sinh(x) ~= exp(x)/2; will will test
        // 10000 values in this range.

        long trans22 = Double.doubleToLongBits(22.0);
        // (approximately) largest value such that exp shouldn't
        // overflow
        long transExpOvfl = Double.doubleToLongBits(Math.nextDown(709.7827128933841));

        for(long i = trans22;
            i < transExpOvfl;
            i +=(transExpOvfl-trans22)/10000) {

            double d = Double.longBitsToDouble(i);

            // Allow 3.5 ulps of error to deal with error in exp.
            failures += testSinhCaseWithUlpDiff(d, StrictMath.exp(d)*0.5, 3.5);
        }

        // (approximately) largest value such that sinh shouldn't
        // overflow.
        long transSinhOvfl = Double.doubleToLongBits(710.4758600739439);

        // Make sure sinh(x) doesn't overflow as soon as exp(x)
        // overflows.

        /*
         * For large values of x, sinh(x) ~= 0.5*(e^x).  Therefore,
         *
         * sinh(x) ~= e^(ln 0.5) * e^x = e^(x + ln 0.5)
         *
         * So, we can calculate the approximate expected result as
         * exp(x + -0.693147186).  However, this sum suffers from
         * roundoff, limiting the accuracy of the approximation.  The
         * accuracy can be improved by recovering the rounded-off
         * information.  Since x is larger than ln(0.5), the trailing
         * bits of ln(0.5) get rounded away when the two values are
         * added.  However, high-order bits of ln(0.5) that
         * contribute to the sum can be found:
         *
         * offset = log(0.5);
         * effective_offset = (x + offset) - x; // exact subtraction
         * rounded_away_offset = offset - effective_offset; // exact subtraction
         *
         * Therefore, the product
         *
         * exp(x + offset)*exp(rounded_away_offset)
         *
         * will be a better approximation to the exact value of
         *
         * e^(x + offset)
         *
         * than exp(x+offset) alone.  (The expected result cannot be
         * computed as exp(x)*exp(offset) since exp(x) by itself would
         * overflow to infinity.)
         */
        double offset = StrictMath.log(0.5);
        for(long i = transExpOvfl+1; i < transSinhOvfl;
            i += (transSinhOvfl-transExpOvfl)/1000 ) {
            double input = Double.longBitsToDouble(i);

            double expected =
                StrictMath.exp(input + offset) *
                StrictMath.exp( offset - ((input + offset) - input) );

            failures += testSinhCaseWithUlpDiff(input, expected, 4.0);
        }

        // sinh(x) overflows for values greater than 710; in
        // particular, it overflows for all 2^i, i > 10.
        for(int i = 10; i <= Double.MAX_EXPONENT; i++) {
            double d = Math.scalb(2.0, i);

            // Result and expected are the same.
            failures += testSinhCaseWithUlpDiff(d,
                                                Double.POSITIVE_INFINITY, 0.0);
        }

        return failures;
    }

    public static int testSinhCaseWithTolerance(double input,
                                                double expected,
                                                double tolerance) {
        int failures = 0;
        failures += Tests.testTolerance("Math.sinh(double)",
                                        input, Math.sinh(input),
                                        expected, tolerance);
        failures += Tests.testTolerance("Math.sinh(double)",
                                        -input, Math.sinh(-input),
                                        -expected, tolerance);

        failures += Tests.testTolerance("StrictMath.sinh(double)",
                                        input, StrictMath.sinh(input),
                                        expected, tolerance);
        failures += Tests.testTolerance("StrictMath.sinh(double)",
                                        -input, StrictMath.sinh(-input),
                                        -expected, tolerance);
        return failures;
    }

    public static int testSinhCaseWithUlpDiff(double input,
                                              double expected,
                                              double ulps) {
        int failures = 0;
        failures += Tests.testUlpDiff("Math.sinh(double)",
                                      input, Math.sinh(input),
                                      expected, ulps);
        failures += Tests.testUlpDiff("Math.sinh(double)",
                                      -input, Math.sinh(-input),
                                      -expected, ulps);

        failures += Tests.testUlpDiff("StrictMath.sinh(double)",
                                      input, StrictMath.sinh(input),
                                      expected, ulps);
        failures += Tests.testUlpDiff("StrictMath.sinh(double)",
                                      -input, StrictMath.sinh(-input),
                                      -expected, ulps);
        return failures;
    }


    /**
     * Test accuracy of {Math, StrictMath}.cosh.  The specified
     * accuracy is 2.5 ulps.
     *
     * The defintion of cosh(x) is
     *
     * (e^x + e^(-x))/2
     *
     * The series expansion of cosh(x) =
     *
     * 1 + x^2/2! + x^4/4! + x^6/6! +...
     *
     * Therefore,
     *
     * 1. For large values of x cosh(x) ~= exp(|x|)/2
     *
     * 2. For small values of x, cosh(x) ~= 1.
     *
     * Additionally, cosh is an even function; cosh(-x) = cosh(x).
     *
     */
    static int testCosh() {
        int failures = 0;
        /*
         * Array elements below generated using a quad cosh
         * implementation.  Rounded to a double, the quad result
         * *should* be correctly rounded, unless we are quite unlucky.
         * Assuming the quad value is a correctly rounded double, the
         * allowed error is 3.0 ulps instead of 2.5 since the quad
         * value rounded to double can have its own 1/2 ulp error.
         */
        double [][] testCases = {
            // x                cosh(x)
            {0.0625,            1.001953760865667607841550709632597376},
            {0.1250,            1.007822677825710859846949685520422223},
            {0.1875,            1.017629683800690526835115759894757615},
            {0.2500,            1.031413099879573176159295417520378622},
            {0.3125,            1.049226785060219076999158096606305793},
            {0.3750,            1.071140346704586767299498015567016002},
            {0.4375,            1.097239412531012567673453832328262160},
            {0.5000,            1.127625965206380785226225161402672030},
            {0.5625,            1.162418740845610783505338363214045218},
            {0.6250,            1.201753692975606324229229064105075301},
            {0.6875,            1.245784523776616395403056980542275175},
            {0.7500,            1.294683284676844687841708185390181730},
            {0.8125,            1.348641048647144208352285714214372703},
            {0.8750,            1.407868656822803158638471458026344506},
            {0.9375,            1.472597542369862933336886403008640891},
            {1.0000,            1.543080634815243778477905620757061497},
            {1.0625,            1.619593348374367728682469968448090763},
            {1.1250,            1.702434658138190487400868008124755757},
            {1.1875,            1.791928268324866464246665745956119612},
            {1.2500,            1.888423877161015738227715728160051696},
            {1.3125,            1.992298543335143985091891077551921106},
            {1.3750,            2.103958159362661802010972984204389619},
            {1.4375,            2.223839037619709260803023946704272699},
            {1.5000,            2.352409615243247325767667965441644201},
            {1.5625,            2.490172284559350293104864895029231913},
            {1.6250,            2.637665356192137582275019088061812951},
            {1.6875,            2.795465162524235691253423614360562624},
            {1.7500,            2.964188309728087781773608481754531801},
            {1.8125,            3.144494087167972176411236052303565201},
            {1.8750,            3.337087043587520514308832278928116525},
            {1.9375,            3.542719740149244276729383650503145346},
            {2.0000,            3.762195691083631459562213477773746099},
            {2.0625,            3.996372503438463642260225717607554880},
            {2.1250,            4.246165228196992140600291052990934410},
            {2.1875,            4.512549935859540340856119781585096760},
            {2.2500,            4.796567530460195028666793366876218854},
            {2.3125,            5.099327816921939817643745917141739051},
            {2.3750,            5.422013837643509250646323138888569746},
            {2.4375,            5.765886495263270945949271410819116399},
            {2.5000,            6.132289479663686116619852312817562517},
            {2.5625,            6.522654518468725462969589397439224177},
            {2.6250,            6.938506971550673190999796241172117288},
            {2.6875,            7.381471791406976069645686221095397137},
            {2.7500,            7.853279872697439591457564035857305647},
            {2.8125,            8.355774815752725814638234943192709129},
            {2.8750,            8.890920130482709321824793617157134961},
            {2.9375,            9.460806908834119747071078865866737196},
            {3.0000,            10.067661995777765841953936035115890343},
            {3.0625,            10.713856690753651225304006562698007312},
            {3.1250,            11.401916013575067700373788969458446177},
            {3.1875,            12.134528570998387744547733730974713055},
            {3.2500,            12.914557062512392049483503752322408761},
            {3.3125,            13.745049466398732213877084541992751273},
            {3.3750,            14.629250949773302934853381428660210721},
            {3.4375,            15.570616549147269180921654324879141947},
            {3.5000,            16.572824671057316125696517821376119469},
            {3.5625,            17.639791465519127930722105721028711044},
            {3.6250,            18.775686128468677200079039891415789429},
            {3.6875,            19.984947192985946987799359614758598457},
            {3.7500,            21.272299872959396081877161903352144126},
            {3.8125,            22.642774526961913363958587775566619798},
            {3.8750,            24.101726314486257781049388094955970560},
            {3.9375,            25.654856121347151067170940701379544221},
            {4.0000,            27.308232836016486629201989612067059978},
            {4.0625,            29.068317063936918520135334110824828950},
            {4.1250,            30.941986372478026192360480044849306606},
            {4.1875,            32.936562165180269851350626768308756303},
            {4.2500,            35.059838290298428678502583470475012235},
            {4.3125,            37.320111495433027109832850313172338419},
            {4.3750,            39.726213847251883288518263854094284091},
            {4.4375,            42.287547242982546165696077854963452084},
            {4.5000,            45.014120148530027928305799939930642658},
            {4.5625,            47.916586706774825161786212701923307169},
            {4.6250,            51.006288368867753140854830589583165950},
            {4.6875,            54.295298211196782516984520211780624960},
            {4.7500,            57.796468111195389383795669320243166117},
            {4.8125,            61.523478966332915041549750463563672435},
            {4.8750,            65.490894152518731617237739112888213645},
            {4.9375,            69.714216430810089539924900313140922323},
            {5.0000,            74.209948524787844444106108044487704798},
            {5.0625,            78.995657605307475581204965926043112946},
            {5.1250,            84.090043934600961683400343038519519678},
            {5.1875,            89.513013937957834087706670952561002466},
            {5.2500,            95.285757988514588780586084642381131013},
            {5.3125,            101.430833209098212357990123684449846912},
            {5.3750,            107.972251614673824873137995865940755392},
            {5.4375,            114.935573939814969189535554289886848550},
            {5.5000,            122.348009517829425991091207107262038316},
            {5.5625,            130.238522601820409078244923165746295574},
            {5.6250,            138.637945543134998069351279801575968875},
            {5.6875,            147.579099269447055276899288971207106581},
            {5.7500,            157.096921533245353905868840194264636395},
            {5.8125,            167.228603431860671946045256541679445836},
            {5.8750,            178.013734732486824390148614309727161925},
            {5.9375,            189.494458570056311567917444025807275896},
            {6.0000,            201.715636122455894483405112855409538488},
            {6.0625,            214.725021906554080628430756558271312513},
            {6.1250,            228.573450380013557089736092321068279231},
            {6.1875,            243.315034578039208138752165587134488645},
            {6.2500,            259.007377561239126824465367865430519592},
            {6.3125,            275.711797500835732516530131577254654076},
            {6.3750,            293.493567280752348242602902925987643443},
            {6.4375,            312.422169552825597994104814531010579387},
            {6.5000,            332.571568241777409133204438572983297292},
            {6.5625,            354.020497560858198165985214519757890505},
            {6.6250,            376.852769667496146326030849450983914197},
            {6.6875,            401.157602161123700280816957271992998156},
            {6.7500,            427.029966702886171977469256622451185850},
            {6.8125,            454.570960119471524953536004647195906721},
            {6.8750,            483.888199441157626584508920036981010995},
            {6.9375,            515.096242417696720610477570797503766179},
            {7.0000,            548.317035155212076889964120712102928484},
            {7.0625,            583.680388623257719787307547662358502345},
            {7.1250,            621.324485894002926216918634755431456031},
            {7.1875,            661.396422095589629755266517362992812037},
            {7.2500,            704.052779189542208784574955807004218856},
            {7.3125,            749.460237818184878095966335081928645934},
            {7.3750,            797.796228612873763671070863694973560629},
            {7.4375,            849.249625508044731271830060572510241864},
            {7.5000,            904.021483770216677368692292389446994987},
            {7.5625,            962.325825625814651122171697031114091993},
            {7.6250,            1024.390476557670599008492465853663578558},
            {7.6875,            1090.457955538048482588540574008226583335},
            {7.7500,            1160.786422676798661020094043586456606003},
            {7.8125,            1235.650687987597295222707689125107720568},
            {7.8750,            1315.343285214046776004329388551335841550},
            {7.9375,            1400.175614911635999247504386054087931958},
            {8.0000,            1490.479161252178088627715460421007179728},
            {8.0625,            1586.606787305415349050508956232945539108},
            {8.1250,            1688.934113859132470361718199038326340668},
            {8.1875,            1797.860987165547537276364148450577336075},
            {8.2500,            1913.813041349231764486365114317586148767},
            {8.3125,            2037.243361581700856522236313401822532385},
            {8.3750,            2168.634254521568851112005905503069409349},
            {8.4375,            2308.499132938297821208734949028296170563},
            {8.5000,            2457.384521883751693037774022640629666294},
            {8.5625,            2615.872194250713123494312356053193077854},
            {8.6250,            2784.581444063104750127653362960649823247},
            {8.6875,            2964.171506380845754878370650565756538203},
            {8.7500,            3155.344133275174556354775488913749659006},
            {8.8125,            3358.846335940117183452010789979584950102},
            {8.8750,            3575.473303654961482727206202358956274888},
            {8.9375,            3806.071511003646460448021740303914939059},
            {9.0000,            4051.542025492594047194773093534725371440},
            {9.0625,            4312.844028491571841588188869958240355518},
            {9.1250,            4590.998563255739769060078863130940205710},
            {9.1875,            4887.092524674358252509551443117048351290},
            {9.2500,            5202.282906336187674588222835339193136030},
            {9.3125,            5537.801321507079474415176386655744387251},
            {9.3750,            5894.958815685577062811620236195525504885},
            {9.4375,            6275.150989541692149890530417987358096221},
            {9.5000,            6679.863452256851081801173722051940058824},
            {9.5625,            7110.677626574055535297758456126491707647},
            {9.6250,            7569.276928241617224537226019600213961572},
            {9.6875,            8057.453343996777301036241026375049070162},
            {9.7500,            8577.114433792824387959788368429252257664},
            {9.8125,            9130.290784631065880205118262838330689429},
            {9.8750,            9719.143945123662919857326995631317996715},
            {9.9375,            10345.974871791805753327922796701684092861},
            {10.0000,           11013.232920103323139721376090437880844591},
        };

        for(int i = 0; i < testCases.length; i++) {
            double [] testCase = testCases[i];
            failures += testCoshCaseWithUlpDiff(testCase[0],
                                                testCase[1],
                                                3.0);
        }


        double [][] specialTestCases = {
            {0.0,                       1.0},
            {NaNd,                      NaNd},
            {Double.longBitsToDouble(0x7FF0000000000001L),      NaNd},
            {Double.longBitsToDouble(0xFFF0000000000001L),      NaNd},
            {Double.longBitsToDouble(0x7FF8555555555555L),      NaNd},
            {Double.longBitsToDouble(0xFFF8555555555555L),      NaNd},
            {Double.longBitsToDouble(0x7FFFFFFFFFFFFFFFL),      NaNd},
            {Double.longBitsToDouble(0xFFFFFFFFFFFFFFFFL),      NaNd},
            {Double.longBitsToDouble(0x7FFDeadBeef00000L),      NaNd},
            {Double.longBitsToDouble(0xFFFDeadBeef00000L),      NaNd},
            {Double.longBitsToDouble(0x7FFCafeBabe00000L),      NaNd},
            {Double.longBitsToDouble(0xFFFCafeBabe00000L),      NaNd},
            {Double.POSITIVE_INFINITY,  Double.POSITIVE_INFINITY}
        };

        for(int i = 0; i < specialTestCases.length; i++ ) {
            failures += testCoshCaseWithUlpDiff(specialTestCases[i][0],
                                                specialTestCases[i][1],
                                                0.0);
        }

        // For powers of 2 less than 2^(-27), the second and
        // subsequent terms of the Taylor series expansion will get
        // rounded.

        for(int i = DoubleConsts.MIN_SUB_EXPONENT; i < -27; i++) {
            double d = Math.scalb(2.0, i);

            // Result and expected are the same.
            failures += testCoshCaseWithUlpDiff(d, 1.0, 2.5);
        }

        // For values of x larger than 22, the e^(-x) term is
        // insignificant to the floating-point result.  Util exp(x)
        // overflows around 709.8, cosh(x) ~= exp(x)/2; will will test
        // 10000 values in this range.

        long trans22 = Double.doubleToLongBits(22.0);
        // (approximately) largest value such that exp shouldn't
        // overflow
        long transExpOvfl = Double.doubleToLongBits(Math.nextDown(709.7827128933841));

        for(long i = trans22;
            i < transExpOvfl;
            i +=(transExpOvfl-trans22)/10000) {

            double d = Double.longBitsToDouble(i);

            // Allow 3.5 ulps of error to deal with error in exp.
            failures += testCoshCaseWithUlpDiff(d, StrictMath.exp(d)*0.5, 3.5);
        }

        // (approximately) largest value such that cosh shouldn't
        // overflow.
        long transCoshOvfl = Double.doubleToLongBits(710.4758600739439);

        // Make sure sinh(x) doesn't overflow as soon as exp(x)
        // overflows.

        /*
         * For large values of x, cosh(x) ~= 0.5*(e^x).  Therefore,
         *
         * cosh(x) ~= e^(ln 0.5) * e^x = e^(x + ln 0.5)
         *
         * So, we can calculate the approximate expected result as
         * exp(x + -0.693147186).  However, this sum suffers from
         * roundoff, limiting the accuracy of the approximation.  The
         * accuracy can be improved by recovering the rounded-off
         * information.  Since x is larger than ln(0.5), the trailing
         * bits of ln(0.5) get rounded away when the two values are
         * added.  However, high-order bits of ln(0.5) that
         * contribute to the sum can be found:
         *
         * offset = log(0.5);
         * effective_offset = (x + offset) - x; // exact subtraction
         * rounded_away_offset = offset - effective_offset; // exact subtraction
         *
         * Therefore, the product
         *
         * exp(x + offset)*exp(rounded_away_offset)
         *
         * will be a better approximation to the exact value of
         *
         * e^(x + offset)
         *
         * than exp(x+offset) alone.  (The expected result cannot be
         * computed as exp(x)*exp(offset) since exp(x) by itself would
         * overflow to infinity.)
         */
        double offset = StrictMath.log(0.5);
        for(long i = transExpOvfl+1; i < transCoshOvfl;
            i += (transCoshOvfl-transExpOvfl)/1000 ) {
            double input = Double.longBitsToDouble(i);

            double expected =
                StrictMath.exp(input + offset) *
                StrictMath.exp( offset - ((input + offset) - input) );

            failures += testCoshCaseWithUlpDiff(input, expected, 4.0);
        }

        // cosh(x) overflows for values greater than 710; in
        // particular, it overflows for all 2^i, i > 10.
        for(int i = 10; i <= Double.MAX_EXPONENT; i++) {
            double d = Math.scalb(2.0, i);

            // Result and expected are the same.
            failures += testCoshCaseWithUlpDiff(d,
                                                Double.POSITIVE_INFINITY, 0.0);
        }
        return failures;
    }

    public static int testCoshCaseWithTolerance(double input,
                                                double expected,
                                                double tolerance) {
        int failures = 0;
        failures += Tests.testTolerance("Math.cosh(double)",
                                        input, Math.cosh(input),
                                        expected, tolerance);
        failures += Tests.testTolerance("Math.cosh(double)",
                                        -input, Math.cosh(-input),
                                        expected, tolerance);

        failures += Tests.testTolerance("StrictMath.cosh(double)",
                                        input, StrictMath.cosh(input),
                                        expected, tolerance);
        failures += Tests.testTolerance("StrictMath.cosh(double)",
                                        -input, StrictMath.cosh(-input),
                                        expected, tolerance);
        return failures;
    }

    public static int testCoshCaseWithUlpDiff(double input,
                                              double expected,
                                              double ulps) {
        int failures = 0;
        failures += Tests.testUlpDiff("Math.cosh(double)",
                                      input, Math.cosh(input),
                                      expected, ulps);
        failures += Tests.testUlpDiff("Math.cosh(double)",
                                      -input, Math.cosh(-input),
                                      expected, ulps);

        failures += Tests.testUlpDiff("StrictMath.cosh(double)",
                                      input, StrictMath.cosh(input),
                                      expected, ulps);
        failures += Tests.testUlpDiff("StrictMath.cosh(double)",
                                      -input, StrictMath.cosh(-input),
                                      expected, ulps);
        return failures;
    }


    /**
     * Test accuracy of {Math, StrictMath}.tanh.  The specified
     * accuracy is 2.5 ulps.
     *
     * The defintion of tanh(x) is
     *
     * (e^x - e^(-x))/(e^x + e^(-x))
     *
     * The series expansion of tanh(x) =
     *
     * x - x^3/3 + 2x^5/15 - 17x^7/315 + ...
     *
     * Therefore,
     *
     * 1. For large values of x tanh(x) ~= signum(x)
     *
     * 2. For small values of x, tanh(x) ~= x.
     *
     * Additionally, tanh is an odd function; tanh(-x) = -tanh(x).
     *
     */
    static int testTanh() {
        int failures = 0;
        /*
         * Array elements below generated using a quad sinh
         * implementation.  Rounded to a double, the quad result
         * *should* be correctly rounded, unless we are quite unlucky.
         * Assuming the quad value is a correctly rounded double, the
         * allowed error is 3.0 ulps instead of 2.5 since the quad
         * value rounded to double can have its own 1/2 ulp error.
         */
        double [][] testCases = {
            // x                tanh(x)
            {0.0625,            0.06241874674751251449014289119421133},
            {0.1250,            0.12435300177159620805464727580589271},
            {0.1875,            0.18533319990813951753211997502482787},
            {0.2500,            0.24491866240370912927780113149101697},
            {0.3125,            0.30270972933210848724239738970991712},
            {0.3750,            0.35835739835078594631936023155315807},
            {0.4375,            0.41157005567402245143207555859415687},
            {0.5000,            0.46211715726000975850231848364367256},
            {0.5625,            0.50982997373525658248931213507053130},
            {0.6250,            0.55459972234938229399903909532308371},
            {0.6875,            0.59637355547924233984437303950726939},
            {0.7500,            0.63514895238728731921443435731249638},
            {0.8125,            0.67096707420687367394810954721913358},
            {0.8750,            0.70390560393662106058763026963135371},
            {0.9375,            0.73407151960434149263991588052503660},
            {1.0000,            0.76159415595576488811945828260479366},
            {1.0625,            0.78661881210869761781941794647736081},
            {1.1250,            0.80930107020178101206077047354332696},
            {1.1875,            0.82980190998595952708572559629034476},
            {1.2500,            0.84828363995751289761338764670750445},
            {1.3125,            0.86490661772074179125443141102709751},
            {1.3750,            0.87982669965198475596055310881018259},
            {1.4375,            0.89319334040035153149249598745889365},
            {1.5000,            0.90514825364486643824230369645649557},
            {1.5625,            0.91582454416876231820084311814416443},
            {1.6250,            0.92534622531174107960457166792300374},
            {1.6875,            0.93382804322259173763570528576138652},
            {1.7500,            0.94137553849728736226942088377163687},
            {1.8125,            0.94808528560440629971240651310180052},
            {1.8750,            0.95404526017994877009219222661968285},
            {1.9375,            0.95933529331468249183399461756952555},
            {2.0000,            0.96402758007581688394641372410092317},
            {2.0625,            0.96818721657637057702714316097855370},
            {2.1250,            0.97187274591350905151254495374870401},
            {2.1875,            0.97513669829362836159665586901156483},
            {2.2500,            0.97802611473881363992272924300618321},
            {2.3125,            0.98058304703705186541999427134482061},
            {2.3750,            0.98284502917257603002353801620158861},
            {2.4375,            0.98484551746427837912703608465407824},
            {2.5000,            0.98661429815143028888127603923734964},
            {2.5625,            0.98817786228751240824802592958012269},
            {2.6250,            0.98955974861288320579361709496051109},
            {2.6875,            0.99078085564125158320311117560719312},
            {2.7500,            0.99185972456820774534967078914285035},
            {2.8125,            0.99281279483715982021711715899682324},
            {2.8750,            0.99365463431502962099607366282699651},
            {2.9375,            0.99439814606575805343721743822723671},
            {3.0000,            0.99505475368673045133188018525548849},
            {3.0625,            0.99563456710930963835715538507891736},
            {3.1250,            0.99614653067334504917102591131792951},
            {3.1875,            0.99659855517712942451966113109487039},
            {3.2500,            0.99699763548652601693227592643957226},
            {3.3125,            0.99734995516557367804571991063376923},
            {3.3750,            0.99766097946988897037219469409451602},
            {3.4375,            0.99793553792649036103161966894686844},
            {3.5000,            0.99817789761119870928427335245061171},
            {3.5625,            0.99839182812874152902001617480606320},
            {3.6250,            0.99858065920179882368897879066418294},
            {3.6875,            0.99874733168378115962760304582965538},
            {3.7500,            0.99889444272615280096784208280487888},
            {3.8125,            0.99902428575443546808677966295308778},
            {3.8750,            0.99913888583735077016137617231569011},
            {3.9375,            0.99924003097049627100651907919688313},
            {4.0000,            0.99932929973906704379224334434172499},
            {4.0625,            0.99940808577297384603818654530731215},
            {4.1250,            0.99947761936180856115470576756499454},
            {4.1875,            0.99953898655601372055527046497863955},
            {4.2500,            0.99959314604388958696521068958989891},
            {4.3125,            0.99964094406130644525586201091350343},
            {4.3750,            0.99968312756179494813069349082306235},
            {4.4375,            0.99972035584870534179601447812936151},
            {4.5000,            0.99975321084802753654050617379050162},
            {4.5625,            0.99978220617994689112771768489030236},
            {4.6250,            0.99980779516900105210240981251048167},
            {4.6875,            0.99983037791655283849546303868853396},
            {4.7500,            0.99985030754497877753787358852000255},
            {4.8125,            0.99986789571029070417475400133989992},
            {4.8750,            0.99988341746867772271011794614780441},
            {4.9375,            0.99989711557251558205051185882773206},
            {5.0000,            0.99990920426259513121099044753447306},
            {5.0625,            0.99991987261554158551063867262784721},
            {5.1250,            0.99992928749851651137225712249720606},
            {5.1875,            0.99993759617721206697530526661105307},
            {5.2500,            0.99994492861777083305830639416802036},
            {5.3125,            0.99995139951851344080105352145538345},
            {5.3750,            0.99995711010315817210152906092289064},
            {5.4375,            0.99996214970350792531554669737676253},
            {5.5000,            0.99996659715630380963848952941756868},
            {5.5625,            0.99997052203605101013786592945475432},
            {5.6250,            0.99997398574306704793434088941484766},
            {5.6875,            0.99997704246374583929961850444364696},
            {5.7500,            0.99997974001803825215761760428815437},
            {5.8125,            0.99998212060739040166557477723121777},
            {5.8750,            0.99998422147482750993344503195672517},
            {5.9375,            0.99998607548749972326220227464612338},
            {6.0000,            0.99998771165079557056434885235523206},
            {6.0625,            0.99998915556205996764518917496149338},
            {6.1250,            0.99999042981101021976277974520745310},
            {6.1875,            0.99999155433311068015449574811497719},
            {6.2500,            0.99999254672143162687722782398104276},
            {6.3125,            0.99999342250186907900400800240980139},
            {6.3750,            0.99999419537602957780612639767025158},
            {6.4375,            0.99999487743557848265406225515388994},
            {6.5000,            0.99999547935140419285107893831698753},
            {6.5625,            0.99999601054055694588617385671796346},
            {6.6250,            0.99999647931357331502887600387959900},
            {6.6875,            0.99999689300449080997594368612277442},
            {6.7500,            0.99999725808558628431084200832778748},
            {6.8125,            0.99999758026863294516387464046135924},
            {6.8750,            0.99999786459425991170635407313276785},
            {6.9375,            0.99999811551081218572759991597586905},
            {7.0000,            0.99999833694394467173571641595066708},
            {7.0625,            0.99999853235803894918375164252059190},
            {7.1250,            0.99999870481040359014665019356422927},
            {7.1875,            0.99999885699910593255108365463415411},
            {7.2500,            0.99999899130518359709674536482047025},
            {7.3125,            0.99999910982989611769943303422227663},
            {7.3750,            0.99999921442759946591163427422888252},
            {7.4375,            0.99999930673475777603853435094943258},
            {7.5000,            0.99999938819554614875054970643513124},
            {7.5625,            0.99999946008444508183970109263856958},
            {7.6250,            0.99999952352618001331402589096040117},
            {7.6875,            0.99999957951331792817413683491979752},
            {7.7500,            0.99999962892179632633374697389145081},
            {7.8125,            0.99999967252462750190604116210421169},
            {7.8750,            0.99999971100399253750324718031574484},
            {7.9375,            0.99999974496191422474977283863588658},
            {8.0000,            0.99999977492967588981001883295636840},
            {8.0625,            0.99999980137613348259726597081723424},
            {8.1250,            0.99999982471505097353529823063673263},
            {8.1875,            0.99999984531157382142423402736529911},
            {8.2500,            0.99999986348794179107425910499030547},
            {8.3125,            0.99999987952853049895833839645847571},
            {8.3750,            0.99999989368430056302584289932834041},
            {8.4375,            0.99999990617672396471542088609051728},
            {8.5000,            0.99999991720124905211338798152800748},
            {8.5625,            0.99999992693035839516545287745322387},
            {8.6250,            0.99999993551626733394129009365703767},
            {8.6875,            0.99999994309330543951799157347876934},
            {8.7500,            0.99999994978001814614368429416607424},
            {8.8125,            0.99999995568102143535399207289008504},
            {8.8750,            0.99999996088863858914831986187674522},
            {8.9375,            0.99999996548434461974481685677429908},
            {9.0000,            0.99999996954004097447930211118358244},
            {9.0625,            0.99999997311918045901919121395899372},
            {9.1250,            0.99999997627775997868467948564005257},
            {9.1875,            0.99999997906519662964368381583648379},
            {9.2500,            0.99999998152510084671976114264303159},
            {9.3125,            0.99999998369595870397054673668361266},
            {9.3750,            0.99999998561173404286033236040150950},
            {9.4375,            0.99999998730239984852716512979473289},
            {9.5000,            0.99999998879440718770812040917618843},
            {9.5625,            0.99999999011109904501789298212541698},
            {9.6250,            0.99999999127307553219220251303121960},
            {9.6875,            0.99999999229851618412119275358396363},
            {9.7500,            0.99999999320346438410630581726217930},
            {9.8125,            0.99999999400207836827291739324060736},
            {9.8750,            0.99999999470685273619047001387577653},
            {9.9375,            0.99999999532881393331131526966058758},
            {10.0000,           0.99999999587769276361959283713827574},
        };

        for(int i = 0; i < testCases.length; i++) {
            double [] testCase = testCases[i];
            failures += testTanhCaseWithUlpDiff(testCase[0],
                                                testCase[1],
                                                3.0);
        }


        double [][] specialTestCases = {
            {0.0,                       0.0},
            {NaNd,                      NaNd},
            {Double.longBitsToDouble(0x7FF0000000000001L),      NaNd},
            {Double.longBitsToDouble(0xFFF0000000000001L),      NaNd},
            {Double.longBitsToDouble(0x7FF8555555555555L),      NaNd},
            {Double.longBitsToDouble(0xFFF8555555555555L),      NaNd},
            {Double.longBitsToDouble(0x7FFFFFFFFFFFFFFFL),      NaNd},
            {Double.longBitsToDouble(0xFFFFFFFFFFFFFFFFL),      NaNd},
            {Double.longBitsToDouble(0x7FFDeadBeef00000L),      NaNd},
            {Double.longBitsToDouble(0xFFFDeadBeef00000L),      NaNd},
            {Double.longBitsToDouble(0x7FFCafeBabe00000L),      NaNd},
            {Double.longBitsToDouble(0xFFFCafeBabe00000L),      NaNd},
            {Double.POSITIVE_INFINITY,  1.0}
        };

        for(int i = 0; i < specialTestCases.length; i++) {
            failures += testTanhCaseWithUlpDiff(specialTestCases[i][0],
                                                specialTestCases[i][1],
                                                0.0);
        }

        // For powers of 2 less than 2^(-27), the second and
        // subsequent terms of the Taylor series expansion will get
        // rounded away since |n-n^3| > 53, the binary precision of a
        // double significand.

        for(int i = DoubleConsts.MIN_SUB_EXPONENT; i < -27; i++) {
            double d = Math.scalb(2.0, i);

            // Result and expected are the same.
            failures += testTanhCaseWithUlpDiff(d, d, 2.5);
        }

        // For values of x larger than 22, tanh(x) is 1.0 in double
        // floating-point arithmetic.

        for(int i = 22; i < 32; i++) {
            failures += testTanhCaseWithUlpDiff(i, 1.0, 2.5);
        }

        for(int i = 5; i <= Double.MAX_EXPONENT; i++) {
            double d = Math.scalb(2.0, i);

            failures += testTanhCaseWithUlpDiff(d, 1.0, 2.5);
        }

        return failures;
    }

    public static int testTanhCaseWithTolerance(double input,
                                                double expected,
                                                double tolerance) {
        int failures = 0;
        failures += Tests.testTolerance("Math.tanh(double",
                                        input, Math.tanh(input),
                                        expected, tolerance);
        failures += Tests.testTolerance("Math.tanh(double",
                                        -input, Math.tanh(-input),
                                        -expected, tolerance);

        failures += Tests.testTolerance("StrictMath.tanh(double",
                                        input, StrictMath.tanh(input),
                                        expected, tolerance);
        failures += Tests.testTolerance("StrictMath.tanh(double",
                                        -input, StrictMath.tanh(-input),
                                        -expected, tolerance);
        return failures;
    }

    public static int testTanhCaseWithUlpDiff(double input,
                                              double expected,
                                              double ulps) {
        int failures = 0;

        failures += Tests.testUlpDiffWithAbsBound("Math.tanh(double)",
                                                  input, Math.tanh(input),
                                                  expected, ulps, 1.0);
        failures += Tests.testUlpDiffWithAbsBound("Math.tanh(double)",
                                                  -input, Math.tanh(-input),
                                                  -expected, ulps, 1.0);

        failures += Tests.testUlpDiffWithAbsBound("StrictMath.tanh(double)",
                                                  input, StrictMath.tanh(input),
                                                  expected, ulps, 1.0);
        failures += Tests.testUlpDiffWithAbsBound("StrictMath.tanh(double)",
                                                  -input, StrictMath.tanh(-input),
                                                  -expected, ulps, 1.0);
        return failures;
    }


    public static void main(String argv[]) {
        int failures = 0;

        failures += testSinh();
        failures += testCosh();
        failures += testTanh();

        if (failures > 0) {
            System.err.println("Testing the hyperbolic functions incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }

}
