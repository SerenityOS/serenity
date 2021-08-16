/*
 * Copyright (c) 2002-2018, the original author or authors.
 *
 * This software is distributable under the BSD license. See the terms of the
 * BSD license in the documentation provided with this software.
 *
 * https://opensource.org/licenses/BSD-3-Clause
 */
package jdk.internal.org.jline.utils;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static jdk.internal.org.jline.terminal.TerminalBuilder.PROP_COLOR_DISTANCE;

public class Colors {

    /**
     * Default 256 colors palette
     */
    public static final int[] DEFAULT_COLORS_256 = {
            0x000000, 0x800000, 0x008000, 0x808000, 0x000080, 0x800080, 0x008080, 0xc0c0c0,
            0x808080, 0xff0000, 0x00ff00, 0xffff00, 0x0000ff, 0xff00ff, 0x00ffff, 0xffffff,

            0x000000, 0x00005f, 0x000087, 0x0000af, 0x0000d7, 0x0000ff, 0x005f00, 0x005f5f,
            0x005f87, 0x005faf, 0x005fd7, 0x005fff, 0x008700, 0x00875f, 0x008787, 0x0087af,
            0x0087d7, 0x0087ff, 0x00af00, 0x00af5f, 0x00af87, 0x00afaf, 0x00afd7, 0x00afff,
            0x00d700, 0x00d75f, 0x00d787, 0x00d7af, 0x00d7d7, 0x00d7ff, 0x00ff00, 0x00ff5f,
            0x00ff87, 0x00ffaf, 0x00ffd7, 0x00ffff, 0x5f0000, 0x5f005f, 0x5f0087, 0x5f00af,
            0x5f00d7, 0x5f00ff, 0x5f5f00, 0x5f5f5f, 0x5f5f87, 0x5f5faf, 0x5f5fd7, 0x5f5fff,
            0x5f8700, 0x5f875f, 0x5f8787, 0x5f87af, 0x5f87d7, 0x5f87ff, 0x5faf00, 0x5faf5f,
            0x5faf87, 0x5fafaf, 0x5fafd7, 0x5fafff, 0x5fd700, 0x5fd75f, 0x5fd787, 0x5fd7af,
            0x5fd7d7, 0x5fd7ff, 0x5fff00, 0x5fff5f, 0x5fff87, 0x5fffaf, 0x5fffd7, 0x5fffff,
            0x870000, 0x87005f, 0x870087, 0x8700af, 0x8700d7, 0x8700ff, 0x875f00, 0x875f5f,
            0x875f87, 0x875faf, 0x875fd7, 0x875fff, 0x878700, 0x87875f, 0x878787, 0x8787af,
            0x8787d7, 0x8787ff, 0x87af00, 0x87af5f, 0x87af87, 0x87afaf, 0x87afd7, 0x87afff,
            0x87d700, 0x87d75f, 0x87d787, 0x87d7af, 0x87d7d7, 0x87d7ff, 0x87ff00, 0x87ff5f,
            0x87ff87, 0x87ffaf, 0x87ffd7, 0x87ffff, 0xaf0000, 0xaf005f, 0xaf0087, 0xaf00af,
            0xaf00d7, 0xaf00ff, 0xaf5f00, 0xaf5f5f, 0xaf5f87, 0xaf5faf, 0xaf5fd7, 0xaf5fff,
            0xaf8700, 0xaf875f, 0xaf8787, 0xaf87af, 0xaf87d7, 0xaf87ff, 0xafaf00, 0xafaf5f,
            0xafaf87, 0xafafaf, 0xafafd7, 0xafafff, 0xafd700, 0xafd75f, 0xafd787, 0xafd7af,
            0xafd7d7, 0xafd7ff, 0xafff00, 0xafff5f, 0xafff87, 0xafffaf, 0xafffd7, 0xafffff,
            0xd70000, 0xd7005f, 0xd70087, 0xd700af, 0xd700d7, 0xd700ff, 0xd75f00, 0xd75f5f,
            0xd75f87, 0xd75faf, 0xd75fd7, 0xd75fff, 0xd78700, 0xd7875f, 0xd78787, 0xd787af,
            0xd787d7, 0xd787ff, 0xd7af00, 0xd7af5f, 0xd7af87, 0xd7afaf, 0xd7afd7, 0xd7afff,
            0xd7d700, 0xd7d75f, 0xd7d787, 0xd7d7af, 0xd7d7d7, 0xd7d7ff, 0xd7ff00, 0xd7ff5f,
            0xd7ff87, 0xd7ffaf, 0xd7ffd7, 0xd7ffff, 0xff0000, 0xff005f, 0xff0087, 0xff00af,
            0xff00d7, 0xff00ff, 0xff5f00, 0xff5f5f, 0xff5f87, 0xff5faf, 0xff5fd7, 0xff5fff,
            0xff8700, 0xff875f, 0xff8787, 0xff87af, 0xff87d7, 0xff87ff, 0xffaf00, 0xffaf5f,
            0xffaf87, 0xffafaf, 0xffafd7, 0xffafff, 0xffd700, 0xffd75f, 0xffd787, 0xffd7af,
            0xffd7d7, 0xffd7ff, 0xffff00, 0xffff5f, 0xffff87, 0xffffaf, 0xffffd7, 0xffffff,

            0x080808, 0x121212, 0x1c1c1c, 0x262626, 0x303030, 0x3a3a3a, 0x444444, 0x4e4e4e,
            0x585858, 0x626262, 0x6c6c6c, 0x767676, 0x808080, 0x8a8a8a, 0x949494, 0x9e9e9e,
            0xa8a8a8, 0xb2b2b2, 0xbcbcbc, 0xc6c6c6, 0xd0d0d0, 0xdadada, 0xe4e4e4, 0xeeeeee,
    };

    /** D50 illuminant for CAM color spaces */
    public static final double[] D50 = new double[] { 96.422f, 100.0f,  82.521f };
    /** D65 illuminant for CAM color spaces */
    public static final double[] D65 = new double[] { 95.047, 100.0, 108.883 };

    /** Average surrounding for CAM color spaces */
    public static final double[] averageSurrounding = new double[] { 1.0, 0.690, 1.0 };
    /** Dim surrounding for CAM color spaces */
    public static final double[] dimSurrounding =     new double[] { 0.9, 0.590, 0.9 };
    /** Dark surrounding for CAM color spaces */
    public static final double[] darkSurrounding =    new double[] { 0.8, 0.525, 0.8 };

    /** sRGB encoding environment */
    public static final double[] sRGB_encoding_environment = vc(D50,  64.0,  64/5, dimSurrounding);
    /** sRGB typical environment */
    public static final double[] sRGB_typical_environment  = vc(D50, 200.0, 200/5, averageSurrounding);
    /** Adobe RGB environment */
    public static final double[] AdobeRGB_environment      = vc(D65, 160.0, 160/5, averageSurrounding);

    private static int[] COLORS_256 = DEFAULT_COLORS_256;

    private static Map<String, Integer> COLOR_NAMES;

    public static void setRgbColors(int[] colors) {
        if (colors == null || colors.length != 256) {
            throw new IllegalArgumentException();
        }
        COLORS_256 = colors;
    }

    public static int rgbColor(int col) {
        return COLORS_256[col];
    }

    public static Integer rgbColor(String name) {
        if (COLOR_NAMES == null) {
            Map<String, Integer> colors = new LinkedHashMap<>();
            try (InputStream is = InfoCmp.class.getResourceAsStream("colors.txt");
                 BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                br.lines().map(String::trim)
                        .filter(s -> !s.startsWith("#"))
                        .filter(s -> !s.isEmpty())
                        .forEachOrdered(s -> {
                            colors.put(s, colors.size());
                        });
                COLOR_NAMES = colors;
            } catch (IOException e) {
                throw new IOError(e);
            }
        }
        return COLOR_NAMES.get(name);
    }

    public static int roundColor(int col, int max) {
        return roundColor(col, max, null);
    }

    public static int roundColor(int col, int max, String dist) {
        if (col >= max) {
            int c = COLORS_256[col];
            col = roundColor(c, COLORS_256, max, dist);
        }
        return col;
    }

    public static int roundRgbColor(int r, int g, int b, int max) {
        return roundColor((r << 16) + (g << 8) + b, COLORS_256, max, (String) null);
    }

    private static int roundColor(int color, int[] colors, int max, String dist) {
        return roundColor(color, colors, max, getDistance(dist));
    }

    private interface Distance {
        double compute(int c1, int c2);
    }

    private static int roundColor(int color, int[] colors, int max, Distance distance) {
        double best_distance = Integer.MAX_VALUE;
        int best_index = Integer.MAX_VALUE;
        for (int idx = 0; idx < max; idx++) {
            double d = distance.compute(color, colors[idx]);
            if (d <= best_distance) {
                best_index = idx;
                best_distance = d;
            }
        }
        return best_index;
    }

    private static Distance getDistance(String dist) {
        if (dist == null) {
            dist = System.getProperty(PROP_COLOR_DISTANCE, "cie76");
        }
        return doGetDistance(dist);
    }

    private static Distance doGetDistance(String dist) {
        if (dist.equals("rgb")) {
            return (p1, p2) -> {
                // rgb: see https://www.compuphase.com/cmetric.htm
                double[] c1 = rgb(p1);
                double[] c2 = rgb(p2);
                double rmean = (c1[0] + c2[0]) / 2.0;
                double[] w = { 2.0 + rmean, 4.0, 3.0 - rmean };
                return scalar(c1, c2, w);
            };
        }
        if (dist.matches("rgb\\(([0-9]+(\\.[0-9]+)?),([0-9]+(\\.[0-9]+)?),([0-9]+(\\.[0-9]+)?)\\)")) {
            return (p1, p2) -> scalar(rgb(p1), rgb(p2), getWeights(dist));
        }
        if (dist.equals("lab") || dist.equals("cie76")) {
            return (p1, p2) -> scalar(rgb2cielab(p1), rgb2cielab(p2));
        }
        if (dist.matches("lab\\(([0-9]+(\\.[0-9]+)?),([0-9]+(\\.[0-9]+)?)\\)")) {
            double[] w = getWeights(dist);
            return (p1, p2) -> scalar(rgb2cielab(p1), rgb2cielab(p2), new double[] { w[0], w[1], w[1] });
        }
        if (dist.equals("cie94")) {
            return (p1, p2) -> cie94(rgb2cielab(p1), rgb2cielab(p2));
        }
        if (dist.equals("cie00") || dist.equals("cie2000")) {
            return (p1, p2) -> cie00(rgb2cielab(p1), rgb2cielab(p2));
        }
        if (dist.equals("cam02")) {
            return (p1, p2) -> cam02(p1, p2, sRGB_typical_environment);
        }
        if (dist.equals("camlab")) {
            return (p1, p2) -> {
                double[] c1 = camlab(p1, sRGB_typical_environment);
                double[] c2 = camlab(p2, sRGB_typical_environment);
                return scalar(c1, c2);
            };
        }
        if (dist.matches("camlab\\(([0-9]+(\\.[0-9]+)?),([0-9]+(\\.[0-9]+)?)\\)")) {
            return (p1, p2) -> {
                double[] c1 = camlab(p1, sRGB_typical_environment);
                double[] c2 = camlab(p2, sRGB_typical_environment);
                double[] w = getWeights(dist);
                return scalar(c1, c2, new double[] { w[0], w[1], w[1] });
            };
        }
        if (dist.matches("camlch")) {
            return (p1, p2) -> {
                double[] c1 = camlch(p1, sRGB_typical_environment);
                double[] c2 = camlch(p2, sRGB_typical_environment);
                return camlch(c1, c2);
            };
        }
        if (dist.matches("camlch\\(([0-9]+(\\.[0-9]+)?),([0-9]+(\\.[0-9]+)?),([0-9]+(\\.[0-9]+)?)\\)")) {
            return (p1, p2) -> {
                double[] c1 = camlch(p1, sRGB_typical_environment);
                double[] c2 = camlch(p2, sRGB_typical_environment);
                double[] w = getWeights(dist);
                return camlch(c1, c2, w);
            };
        }
        throw new IllegalArgumentException("Unsupported distance function: " + dist);
    }

    private static double[] getWeights(String dist) {
        String[] weights = dist.substring(dist.indexOf('(') + 1, dist.length() - 1).split(",");
        return Stream.of(weights).mapToDouble(Double::parseDouble).toArray();
    }

    private static double scalar(double[] c1, double[] c2, double[] w) {
        return sqr((c1[0] - c2[0]) * w[0])
             + sqr((c1[1] - c2[1]) * w[1])
             + sqr((c1[2] - c2[2]) * w[2]);
    }

    private static double scalar(double[] c1, double[] c2) {
        return sqr(c1[0] - c2[0])
             + sqr(c1[1] - c2[1])
             + sqr(c1[2] - c2[2]);
    }

    private static final int L = 0;
    private static final int A = 1;
    private static final int B = 2;
    private static final int X = 0;
    private static final int Y = 1;
    private static final int Z = 2;
    private static final double kl = 2.0;
    private static final double kc = 1.0;
    private static final double kh = 1.0;
    private static final double k1 = 0.045;
    private static final double k2 = 0.015;

    private static double cie94(double[] lab1, double[] lab2) {
        double dl = lab1[L] - lab2[L];
        double da = lab1[A] - lab2[A];
        double db = lab1[B] - lab2[B];
        double c1 = Math.sqrt(lab1[A] * lab1[A] + lab1[B] * lab1[B]);
        double c2 = Math.sqrt(lab2[A] * lab2[A] + lab2[B] * lab2[B]);
        double dc = c1 - c2;
        double dh = da * da + db * db - dc * dc;
        dh = dh < 0.0 ? 0.0 : Math.sqrt(dh);
        double sl = 1.0;
        double sc = 1.0 + k1 * c1;
        double sh = 1.0 + k2 * c1;
        double dLKlsl = dl / (kl * sl);
        double dCkcsc = dc / (kc * sc);
        double dHkhsh = dh / (kh * sh);
        return dLKlsl * dLKlsl + dCkcsc * dCkcsc + dHkhsh * dHkhsh;
    }

    private static double cie00(double[] lab1, double[] lab2) {
        double c_star_1_ab = Math.sqrt(lab1[A] * lab1[A] + lab1[B] * lab1[B]);
        double c_star_2_ab = Math.sqrt(lab2[A] * lab2[A] + lab2[B] * lab2[B]);
        double c_star_average_ab = (c_star_1_ab + c_star_2_ab) / 2.0;
        double c_star_average_ab_pot_3 = c_star_average_ab * c_star_average_ab * c_star_average_ab;
        double c_star_average_ab_pot_7 = c_star_average_ab_pot_3 * c_star_average_ab_pot_3 * c_star_average_ab;
        double G = 0.5 * (1.0 - Math.sqrt(c_star_average_ab_pot_7 / (c_star_average_ab_pot_7 + 6103515625.0))); //25^7
        double a1_prime = (1.0 + G) * lab1[A];
        double a2_prime = (1.0 + G) * lab2[A];
        double C_prime_1 = Math.sqrt(a1_prime * a1_prime + lab1[B] * lab1[B]);
        double C_prime_2 = Math.sqrt(a2_prime * a2_prime + lab2[B] * lab2[B]);
        double h_prime_1 = (Math.toDegrees(Math.atan2(lab1[B], a1_prime)) + 360.0) % 360.0;
        double h_prime_2 = (Math.toDegrees(Math.atan2(lab2[B], a2_prime)) + 360.0) % 360.0;
        double delta_L_prime = lab2[L] - lab1[L];
        double delta_C_prime = C_prime_2 - C_prime_1;
        double h_bar = Math.abs(h_prime_1 - h_prime_2);
        double delta_h_prime;
        if (C_prime_1 * C_prime_2 == 0.0) {
            delta_h_prime = 0.0;
        } else if (h_bar <= 180.0) {
            delta_h_prime = h_prime_2 - h_prime_1;
        } else if (h_prime_2 <= h_prime_1) {
            delta_h_prime = h_prime_2 - h_prime_1 + 360.0;
        } else {
            delta_h_prime = h_prime_2 - h_prime_1 - 360.0;
        }
        double delta_H_prime = 2.0 * Math.sqrt(C_prime_1 * C_prime_2) * Math.sin(Math.toRadians(delta_h_prime / 2.0));
        double L_prime_average = (lab1[L] + lab2[L]) / 2.0;
        double C_prime_average = (C_prime_1 + C_prime_2) / 2.0;
        double h_prime_average;
        if (C_prime_1 * C_prime_2 == 0.0) {
            h_prime_average = 0.0;
        } else if (h_bar <= 180.0) {
            h_prime_average = (h_prime_1 + h_prime_2) / 2.0;
        } else if ((h_prime_1 + h_prime_2) < 360.0) {
            h_prime_average = (h_prime_1 + h_prime_2 + 360.0) / 2.0;
        } else {
            h_prime_average = (h_prime_1 + h_prime_2 - 360.0) / 2.0;
        }
        double L_prime_average_minus_50 = L_prime_average - 50.0;
        double L_prime_average_minus_50_square = L_prime_average_minus_50 * L_prime_average_minus_50;
        double T = 1.0
                - 0.17 * Math.cos(Math.toRadians(h_prime_average - 30.0))
                + 0.24 * Math.cos(Math.toRadians(h_prime_average * 2.0))
                + 0.32 * Math.cos(Math.toRadians(h_prime_average * 3.0 + 6.0))
                - 0.20 * Math.cos(Math.toRadians(h_prime_average * 4.0 - 63.0));
        double S_L = 1.0 + ((0.015 * L_prime_average_minus_50_square) / Math.sqrt(20.0 + L_prime_average_minus_50_square));
        double S_C = 1.0 + 0.045 * C_prime_average;
        double S_H = 1.0 + 0.015 * T * C_prime_average;
        double h_prime_average_minus_275_div_25 = (h_prime_average - 275.0) / (25.0);
        double h_prime_average_minus_275_div_25_square = h_prime_average_minus_275_div_25 * h_prime_average_minus_275_div_25;
        double delta_theta = 30.0 * Math.exp(-h_prime_average_minus_275_div_25_square);
        double C_prime_average_pot_3 = C_prime_average * C_prime_average * C_prime_average;
        double C_prime_average_pot_7 = C_prime_average_pot_3 * C_prime_average_pot_3 * C_prime_average;
        double R_C = 2.0 * Math.sqrt(C_prime_average_pot_7 / (C_prime_average_pot_7 + 6103515625.0)); //25^7
        double R_T = - Math.sin(Math.toRadians(2.0 * delta_theta)) * R_C;
        double dLKlsl = delta_L_prime / (kl * S_L);
        double dCkcsc = delta_C_prime / (kc * S_C);
        double dHkhsh = delta_H_prime / (kh * S_H);
        return dLKlsl * dLKlsl + dCkcsc * dCkcsc + dHkhsh * dHkhsh + R_T * dCkcsc * dHkhsh;
    }

    private static double cam02(int p1, int p2, double[] vc) {
        double[] c1 = jmh2ucs(camlch(p1, vc));
        double[] c2 = jmh2ucs(camlch(p2, vc));
        return scalar(c1, c2);
    }

    private static double[] jmh2ucs(double[] lch) {
        double sJ = ((1.0 + 100 * 0.007) * lch[0]) / (1.0 + 0.007 * lch[0]);
        double sM = ((1.0 / 0.0228) * Math.log(1.0 + 0.0228 * lch[1]));
        double a = sM * Math.cos(Math.toRadians(lch[2]));
        double b = sM * Math.sin(Math.toRadians(lch[2]));
        return new double[] {sJ, a, b };
    }

    static double camlch(double[] c1, double[] c2) {
        return camlch(c1, c2, new double[] { 1.0, 1.0, 1.0 });
    }

    static double camlch(double[] c1, double[] c2, double[] w) {
        // normalize weights to correlate range
        double lightnessWeight = w[0] / 100.0;
        double colorfulnessWeight = w[1] / 120.0;
        double hueWeight = w[2] / 360.0;
        // calculate sort-of polar distance
        double dl = (c1[0] - c2[0]) * lightnessWeight;
        double dc = (c1[1] - c2[1]) * colorfulnessWeight;
        double dh = hueDifference(c1[2], c2[2], 360.0) * hueWeight;
        return dl * dl + dc * dc + dh * dh;
    }

    private static double hueDifference(double hue1, double hue2, double c) {
        double difference = (hue2 - hue1) % c;
        double ch = c / 2;
        if (difference > ch)
            difference -= c;
        if (difference < -ch)
            difference += c;
        return difference;
    }

    private static double[] rgb(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >>  8) & 0xFF;
        int b = (color >>  0) & 0xFF;
        return new double[] { r / 255.0, g / 255.0, b / 255.0 };
    }

    static double[] rgb2xyz(int color) {
        return rgb2xyz(rgb(color));
    }

    static double[] rgb2cielab(int color) {
        return rgb2cielab(rgb(color));
    }

    static double[] camlch(int color) {
        return camlch(color, sRGB_typical_environment);
    }

    static double[] camlch(int color, double[] vc) {
        return xyz2camlch(rgb2xyz(color), vc);
    }

    static double[] camlab(int color) {
        return camlab(color, sRGB_typical_environment);
    }

    static double[] camlab(int color, double[] vc) {
        return lch2lab(camlch(color, vc));
    }

    static double[] lch2lab(double[] lch) {
        double toRad = Math.PI / 180;
        return new double[] { lch[0], lch[1] * Math.cos(lch[2] * toRad), lch[1] * Math.sin(lch[2] * toRad) };
    }

    private static double[] xyz2camlch(double[] xyz, double[] vc) {
        double[] XYZ = new double[] {xyz[0] * 100.0, xyz[1] * 100.0, xyz[2] * 100.0};
        double[] cam = forwardTransform(XYZ, vc);
        return new double[] { cam[J], cam[M], cam[h] };
    }

    /** Lightness */
    public static final int J = 0;
    /** Brightness */
    public static final int Q = 1;
    /** Chroma */
    public static final int C = 2;
    /** Colorfulness */
    public static final int M = 3;
    /** Saturation */
    public static final int s = 4;
    /** Hue Composition / Hue Quadrature */
    public static final int H = 5;
    /** Hue */
    public static final int h = 6;


    /** CIECAM02 appearance correlates */
    private static double[] forwardTransform(double[] XYZ, double[] vc) {
        // calculate sharpened cone response
        double[] RGB = forwardPreAdaptationConeResponse(XYZ);
        // calculate corresponding (sharpened) cone response considering various luminance level and surround conditions in D
        double[] RGB_c = forwardPostAdaptationConeResponse(RGB, vc);
        // calculate HPE equal area cone fundamentals
        double[] RGBPrime = CAT02toHPE(RGB_c);
        // calculate response-compressed postadaptation cone response
        double[] RGBPrime_a = forwardResponseCompression(RGBPrime, vc);
        // calculate achromatic response
        double A = (2.0 * RGBPrime_a[0] + RGBPrime_a[1] + RGBPrime_a[2] / 20.0 - 0.305) * vc[VC_N_BB];
        // calculate lightness
        double J = 100.0 * Math.pow(A / vc[VC_A_W], vc[VC_Z] * vc[VC_C]);
        // calculate redness-greenness and yellowness-blueness color opponent values
        double a = RGBPrime_a[0] + (-12.0 * RGBPrime_a[1] + RGBPrime_a[2]) / 11.0;
        double b = (RGBPrime_a[0] + RGBPrime_a[1] - 2.0 * RGBPrime_a[2]) / 9.0;
        // calculate hue angle
        double h = (Math.toDegrees(Math.atan2(b, a)) + 360.0) % 360.0;
        // calculate eccentricity
        double e = ((12500.0 / 13.0) * vc[VC_N_C] * vc[VC_N_CB]) * (Math.cos(Math.toRadians(h) + 2.0) + 3.8);
        // get t
        double t = e * Math.sqrt(Math.pow(a, 2.0) + Math.pow(b, 2.0)) / (RGBPrime_a[0] + RGBPrime_a[1] + 1.05 * RGBPrime_a[2]);
        // calculate brightness
        double Q = (4.0 / vc[VC_C]) * Math.sqrt(J / 100.0) * (vc[VC_A_W] + 4.0) * Math.pow(vc[VC_F_L], 0.25);
        // calculate the correlates of chroma, colorfulness, and saturation
        double C = Math.signum(t) * Math.pow(Math.abs(t), 0.9) * Math.sqrt(J / 100.0) * Math.pow(1.64- Math.pow(0.29, vc[VC_N]), 0.73);
        double M = C * Math.pow(vc[VC_F_L], 0.25);
        double s = 100.0 * Math.sqrt(M / Q);
        // calculate hue composition
        double H = calculateH(h);
        return new double[] { J, Q, C, M, s, H, h };
    }

    private static double calculateH(double h) {
        if (h < 20.14)
            h = h + 360;
        double i;
        if (h >= 20.14 && h < 90.0) {  // index i = 1
            i = (h - 20.14) / 0.8;
            return 100.0 * i / (i + (90 - h) / 0.7);
        } else if (h < 164.25) { // index i = 2
            i = (h - 90) / 0.7;
            return 100.0 + 100.0 * i / (i + (164.25 - h) / 1);
        } else if (h < 237.53) {  // index i = 3
            i = (h - 164.25) / 1.0;
            return 200.0 + 100.0 * i / (i + (237.53 - h) / 1.2);
        } else if (h <= 380.14) {  // index i = 4
            i = (h - 237.53) / 1.2;
            double H = 300.0 + 100.0 * i / (i + (380.14 - h) / 0.8);
            // don't use 400 if we can use 0
            if (H <= 400.0 && H >= 399.999)
                H = 0;
            return H;
        } else {
            throw new IllegalArgumentException("h outside assumed range 0..360: " + Double.toString(h));
        }
    }

    private static double[] forwardResponseCompression(double[] RGB, double[] vc) {
        double[] result = new double[3];
        for(int channel = 0; channel < RGB.length; channel++) {
            if(RGB[channel] >= 0) {
                double n = Math.pow(vc[VC_F_L] * RGB[channel] / 100.0, 0.42);
                result[channel] = 400.0 * n / (n + 27.13) + 0.1;
            } else {
                double n = Math.pow(-1.0 * vc[VC_F_L] * RGB[channel] / 100.0, 0.42);
                result[channel] = -400.0 * n / (n + 27.13) + 0.1;
            }
        }
        return result;
    }

    private static double[] forwardPostAdaptationConeResponse(double[] RGB, double[] vc) {
        return new double[] { vc[VC_D_RGB_R] * RGB[0], vc[VC_D_RGB_G] * RGB[1], vc[VC_D_RGB_B] * RGB[2] };
    }

    public static double[] CAT02toHPE(double[] RGB) {
        double[] RGBPrime = new double[3];
        RGBPrime[0] =  0.7409792 * RGB[0] + 0.2180250 * RGB[1] + 0.0410058 * RGB[2];
        RGBPrime[1] =  0.2853532 * RGB[0] + 0.6242014 * RGB[1] + 0.0904454 * RGB[2];
        RGBPrime[2] = -0.0096280 * RGB[0] - 0.0056980 * RGB[1] + 1.0153260 * RGB[2];
        return RGBPrime;
    }

    private static double[] forwardPreAdaptationConeResponse(double[] XYZ) {
        double[] RGB = new double[3];
        RGB[0] =  0.7328 * XYZ[0] + 0.4296 * XYZ[1] - 0.1624 * XYZ[2];
        RGB[1] = -0.7036 * XYZ[0] + 1.6975 * XYZ[1] + 0.0061 * XYZ[2];
        RGB[2] =  0.0030 * XYZ[0] + 0.0136 * XYZ[1] + 0.9834 * XYZ[2];
        return RGB;
    }

    static final int SUR_F = 0;
    static final int SUR_C = 1;
    static final int SUR_N_C = 2;

    static final int VC_X_W = 0;
    static final int VC_Y_W = 1;
    static final int VC_Z_W = 2;
    static final int VC_L_A = 3;
    static final int VC_Y_B = 4;
    static final int VC_F =   5;
    static final int VC_C =   6;
    static final int VC_N_C = 7;

    static final int VC_Z = 8;
    static final int VC_N = 9;
    static final int VC_N_BB = 10;
    static final int VC_N_CB = 11;
    static final int VC_A_W = 12;
    static final int VC_F_L = 13;
    static final int VC_D_RGB_R = 14;
    static final int VC_D_RGB_G = 15;
    static final int VC_D_RGB_B = 16;

    static double[] vc(double[] xyz_w, double L_A, double Y_b, double[] surrounding) {
        double[] vc = new double[17];
        vc[VC_X_W] = xyz_w[0];
        vc[VC_Y_W] = xyz_w[1];
        vc[VC_Z_W] = xyz_w[2];
        vc[VC_L_A] = L_A;
        vc[VC_Y_B] = Y_b;
        vc[VC_F] = surrounding[SUR_F];
        vc[VC_C] = surrounding[SUR_C];
        vc[VC_N_C] = surrounding[SUR_N_C];

        double[] RGB_w = forwardPreAdaptationConeResponse(xyz_w);
        double D = Math.max(0.0, Math.min(1.0, vc[VC_F] * (1.0 - (1.0 / 3.6) * Math.pow(Math.E, (-L_A - 42.0) / 92.0))));
        double Yw = xyz_w[1];
        double[] RGB_c = new double[] {
                (D * Yw / RGB_w[0]) + (1.0 - D),
                (D * Yw / RGB_w[1]) + (1.0 - D),
                (D * Yw / RGB_w[2]) + (1.0 - D),
        };

        // calculate increase in brightness and colorfulness caused by brighter viewing environments
        double L_Ax5 = 5.0 * L_A;
        double k = 1.0 / (L_Ax5 + 1.0);
        double kpow4 = Math.pow(k, 4.0);
        vc[VC_F_L] = 0.2 * kpow4 * (L_Ax5) + 0.1 * Math.pow(1.0 - kpow4, 2.0) * Math.pow(L_Ax5, 1.0/3.0);

        // calculate response compression on J and C caused by background lightness.
        vc[VC_N] = Y_b / Yw;
        vc[VC_Z] = 1.48 + Math.sqrt(vc[VC_N]);

        vc[VC_N_BB] = 0.725 * Math.pow(1.0 / vc[VC_N], 0.2);
        vc[VC_N_CB] = vc[VC_N_BB]; // chromatic contrast factors (calculate increase in J, Q, and C caused by dark backgrounds)

        // calculate achromatic response to white
        double[] RGB_wc = new double[] {RGB_c[0] * RGB_w[0], RGB_c[1] * RGB_w[1], RGB_c[2] * RGB_w[2]};
        double[] RGBPrime_w = CAT02toHPE(RGB_wc);
        double[] RGBPrime_aw = new double[3];
        for(int channel = 0; channel < RGBPrime_w.length; channel++) {
            if(RGBPrime_w[channel] >= 0) {
                double n = Math.pow(vc[VC_F_L] * RGBPrime_w[channel] / 100.0, 0.42);
                RGBPrime_aw[channel] = 400.0 * n / (n + 27.13) + 0.1;
            } else {
                double n = Math.pow(-1.0 * vc[VC_F_L] * RGBPrime_w[channel] / 100.0, 0.42);
                RGBPrime_aw[channel] = -400.0 * n / (n + 27.13) + 0.1;
            }
        }
        vc[VC_A_W] = (2.0 * RGBPrime_aw[0] + RGBPrime_aw[1] + RGBPrime_aw[2] / 20.0 - 0.305) * vc[VC_N_BB];
        vc[VC_D_RGB_R] = RGB_c[0];
        vc[VC_D_RGB_G] = RGB_c[1];
        vc[VC_D_RGB_B] = RGB_c[2];
        return vc;
    }

    public static double[] rgb2cielab(double[] rgb) {
        return xyz2lab(rgb2xyz(rgb));
    }

    private static double[] rgb2xyz(double[] rgb) {
        double vr = pivotRgb(rgb[0]);
        double vg = pivotRgb(rgb[1]);
        double vb = pivotRgb(rgb[2]);
        // http://www.brucelindbloom.com/index.html?Eqn_RGB_XYZ_Matrix.html
        double x = vr * 0.4124564 + vg * 0.3575761 + vb * 0.1804375;
        double y = vr * 0.2126729 + vg * 0.7151522 + vb * 0.0721750;
        double z = vr * 0.0193339 + vg * 0.1191920 + vb * 0.9503041;
        return new double[] { x, y, z };
    }

    private static double pivotRgb(double n) {
        return n > 0.04045 ? Math.pow((n + 0.055) / 1.055, 2.4) : n / 12.92;
    }

    private static double[] xyz2lab(double[] xyz) {
        double fx = pivotXyz(xyz[0]);
        double fy = pivotXyz(xyz[1]);
        double fz = pivotXyz(xyz[2]);
        double l = 116.0 * fy - 16.0;
        double a = 500.0 * (fx - fy);
        double b = 200.0 * (fy - fz);
        return new double[] { l, a, b };
    }

    private static final double epsilon = 216.0 / 24389.0;
    private static final double kappa = 24389.0 / 27.0;
    private static double pivotXyz(double n) {
        return n > epsilon ? Math.cbrt(n) : (kappa * n + 16) / 116;
    }

    private static double sqr(double n) {
        return n * n;
    }

}
