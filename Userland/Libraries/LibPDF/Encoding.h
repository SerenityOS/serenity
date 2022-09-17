/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibPDF/ObjectDerivatives.h>

// Appendix D.2: Latin Character Set and Encodings
#define ENUMERATE_LATIN_CHARACTER_SET(FN)                           \
    FN("A", A, 65, 65, 65, 65)                                      \
    FN("Æ", AE, 225, 174, 198, 198)                                 \
    FN("Á", Aacute, -1, 231, 193, 193)                              \
    FN("Â", Acircumflex, -1, 229, 194, 194)                         \
    FN("Ä", Adieresis, -1, 128, 196, 196)                           \
    FN("À", Agrave, -1, 203, 192, 192)                              \
    FN("Å", Aring, -1, 129, 197, 197)                               \
    FN("Ã", Atilde, -1, 204, 195, 195)                              \
    FN("B", B, 66, 66, 66, 66)                                      \
    FN("C", C, 67, 67, 67, 67)                                      \
    FN("Ç", Ccedilla, -1, 130, 199, 199)                            \
    FN("D", D, 68, 68, 68, 68)                                      \
    FN("E", E, 69, 69, 69, 69)                                      \
    FN("É", Eacute, -1, 131, 201, 201)                              \
    FN("Ê", Ecircumflex, -1, 230, 202, 202)                         \
    FN("Ë", Edieresis, -1, 232, 203, 203)                           \
    FN("È", Egrave, -1, 233, 200, 200)                              \
    FN("Ð", Eth, -1, -1, 208, 208)                                  \
    FN("€", Euro, -1, -1, 128, 160) /* FIXME: Note 1 */             \
    FN("F", F, 70, 70, 70, 70)                                      \
    FN("G", G, 71, 71, 71, 71)                                      \
    FN("H", H, 72, 72, 72, 72)                                      \
    FN("I", I, 73, 73, 73, 73)                                      \
    FN("Í", Iacute, -1, 234, 205, 205)                              \
    FN("Î", Icircumflex, -1, 235, 206, 206)                         \
    FN("Ï", Idieresis, -1, 236, 207, 207)                           \
    FN("Ì", Igrave, -1, 237, 204, 204)                              \
    FN("J", J, 74, 74, 74, 74)                                      \
    FN("K", K, 75, 75, 75, 75)                                      \
    FN("L", L, 76, 76, 76, 76)                                      \
    FN("Ł", Lslash, 232, -1, -1, 149)                               \
    FN("M", M, 77, 77, 77, 77)                                      \
    FN("N", N, 78, 78, 78, 78)                                      \
    FN("Ñ", Ntilde, -1, 132, 209, 209)                              \
    FN("O", O, 79, 79, 79, 79)                                      \
    FN("Œ", OE, 234, 206, 140, 150)                                 \
    FN("Ó", Oacute, -1, 238, 211, 211)                              \
    FN("Ô", Ocircumflex, -1, 239, 212, 212)                         \
    FN("Ö", Odieresis, -1, 133, 214, 214)                           \
    FN("Ò", Ograve, -1, 241, 210, 210)                              \
    FN("Ø", Oslash, 233, 175, 216, 216)                             \
    FN("Õ", Otilde, -1, 205, 213, 213)                              \
    FN("P", P, 80, 80, 80, 80)                                      \
    FN("Q", Q, 81, 81, 81, 81)                                      \
    FN("R", R, 82, 82, 82, 82)                                      \
    FN("S", S, 83, 83, 83, 83)                                      \
    FN("Š", Scaron, -1, -1, 138, 151)                               \
    FN("T", T, 84, 84, 84, 84)                                      \
    FN("Þ", Thorn, -1, -1, 222, 222)                                \
    FN("U", U, 85, 85, 85, 85)                                      \
    FN("Ú", Uacute, -1, 242, 218, 218)                              \
    FN("Û", Ucircumflex, -1, 243, 219, 219)                         \
    FN("Ü", Udieresis, -1, 134, 220, 220)                           \
    FN("Ù", Ugrave, -1, 244, 217, 217)                              \
    FN("V", V, 86, 86, 86, 86)                                      \
    FN("W", W, 87, 87, 87, 87)                                      \
    FN("X", X, 88, 88, 88, 88)                                      \
    FN("Y", Y, 89, 89, 89, 89)                                      \
    FN("Ý", Yacute, -1, -1, 221, 221)                               \
    FN("Ÿ", Ydieresis, -1, 217, 159, 152)                           \
    FN("Z", Z, 90, 90, 90, 90)                                      \
    FN("Ž", Zcaron, -1, -1, 142, 153) /* FIXME: Note 2 */           \
    FN("a", a, 97, 97, 97, 97)                                      \
    FN("á", aacute, -1, 135, 225, 225)                              \
    FN("â", acircumflex, -1, 137, 226, 226)                         \
    FN("́", acute, 194, 171, 180, 180)                               \
    FN("ä", adieresis, -1, 138, 228, 228)                           \
    FN("æ", ae, 241, 190, 230, 230)                                 \
    FN("à", agrave, -1, 136, 224, 224)                              \
    FN("&", ampersand, 38, 38, 38, 38)                              \
    FN("å", aring, -1, 140, 229, 229)                               \
    FN("^", asciicircum, 94, 94, 94, 94)                            \
    FN("~", asciitilde, 126, 126, 126, 126)                         \
    FN("*", asterisk, 42, 42, 42, 42)                               \
    FN("@", at, 64, 64, 64, 64)                                     \
    FN("ã", atilde, -1, 139, 227, 227)                              \
    FN("b", b, 98, 98, 98, 98)                                      \
    FN("\\", backslash, 92, 92, 92, 92)                             \
    FN("|", bar, 124, 124, 124, 124)                                \
    FN("{", braceleft, 123, 123, 123, 123)                          \
    FN("}", braceright, 125, 125, 125, 125)                         \
    FN("[", bracketleft, 91, 91, 91, 91)                            \
    FN("]", bracketright, 93, 93, 93, 93)                           \
    FN(" ̆", breve, 198, 249, -1, 24)                                \
    FN("¦", brokenbar, -1, -1, 166, 166)                            \
    FN("•", bullet, 183, 165, 149, 128) /* FIXME: Note 3 */         \
    FN("c", c, 99, 99, 99, 99)                                      \
    FN("ˇ", caron, 207, 255, -1, 25)                                \
    FN("ç", ccedilla, -1, 141, 231, 231)                            \
    FN("̧", cedilla, 203, 252, 184, 184)                             \
    FN("¢", cent, 162, 162, 162, 162)                               \
    FN("ˆ", circumflex, 195, 246, 136, 26)                          \
    FN(":", colon, 58, 58, 58, 58)                                  \
    FN(",", comma, 44, 44, 44, 44)                                  \
    FN("©", copyright, -1, 169, 169, 169)                           \
    FN("¤", currency, 168, 219, 164, 164) /* FIXME: Note 1 */       \
    FN("d", d, 100, 100, 100, 100)                                  \
    FN("†", dagger, 178, 160, 134, 129)                             \
    FN("‡", daggerdbl, 179, 224, 135, 130)                          \
    FN("°", degree, -1, 161, 176, 176)                              \
    FN("̈", dieresis, 200, 172, 168, 168)                            \
    FN("÷", divide, -1, 214, 247, 247)                              \
    FN("$", dollar, 36, 36, 36, 36)                                 \
    FN("̇", dotaccent, 199, 250, -1, 27)                             \
    FN("ı", dotlessi, 245, 245, -1, 154)                            \
    FN("e", e, 101, 101, 101, 101)                                  \
    FN("é", eacute, -1, 142, 233, 233)                              \
    FN("ê", ecircumflex, -1, 144, 234, 234)                         \
    FN("ë", edieresis, -1, 145, 235, 235)                           \
    FN("è", egrave, -1, 143, 232, 232)                              \
    FN("8", eight, 56, 56, 56, 56)                                  \
    FN("…", ellipsis, 188, 201, 133, 131)                           \
    FN("—", emdash, 208, 209, 151, 132)                             \
    FN("–", endash, 177, 208, 150, 133)                             \
    FN("=", equal, 61, 61, 61, 61)                                  \
    FN("ð", eth, -1, -1, 240, 240)                                  \
    FN("!", exclam, 33, 33, 33, 33)                                 \
    FN("¡", exclamdown, 161, 193, 161, 161)                         \
    FN("f", f, 102, 102, 102, 102)                                  \
    FN("ﬁ", fi, 174, 222, -1, 147)                                  \
    FN("5", five, 53, 53, 53, 53)                                   \
    FN("ﬂ", fl, 175, 223, -1, 148)                                  \
    FN("ƒ", florin, 166, 196, 131, 134)                             \
    FN("4", four, 52, 52, 52, 52)                                   \
    FN("⁄", fraction, 164, 218, -1, 135)                            \
    FN("g", g, 103, 103, 103, 103)                                  \
    FN("ß", germandbls, 251, 167, 223, 223)                         \
    FN("`", grave, 193, 96, 96, 96)                                 \
    FN(">", greater, 62, 62, 62, 62)                                \
    FN("«", guillemotleft, 171, 199, 171, 171)  /* FIXME: Note 4 */ \
    FN("»", guillemotright, 187, 200, 187, 187) /* FIXME: Note 4 */ \
    FN("‹", guilsinglleft, 172, 220, 139, 136)                      \
    FN("›", guilsinglright, 173, 221, 155, 137)                     \
    FN("h", h, 104, 104, 104, 104)                                  \
    FN("̋", hungarumlaut, 205, 253, -1, 28)                          \
    FN("-", hyphen, 45, 45, 45, 45) /* FIXME: Note 5 */             \
    FN("i", i, 105, 105, 105, 105)                                  \
    FN("í", iacute, -1, 146, 237, 237)                              \
    FN("î", icircumflex, -1, 148, 238, 238)                         \
    FN("ï", idieresis, -1, 149, 239, 239)                           \
    FN("ì", igrave, -1, 147, 236, 236)                              \
    FN("j", j, 106, 106, 106, 106)                                  \
    FN("k", k, 107, 107, 107, 107)                                  \
    FN("l", l, 108, 108, 108, 108)                                  \
    FN("<", less, 60, 60, 60, 60)                                   \
    FN("¬", logicalnot, -1, 194, 172, 172)                          \
    FN("ł", lslash, 248, -1, -1, 155)                               \
    FN("m", m, 109, 109, 109, 109)                                  \
    FN("̄", macron, 197, 248, 175, 175)                              \
    FN("−", minus, -1, -1, -1, 138)                                 \
    FN("μ", mu, -1, 181, 181, 181)                                  \
    FN("×", multiply, -1, -1, 215, 215)                             \
    FN("n", n, 110, 110, 110, 110)                                  \
    FN("9", nine, 57, 57, 57, 57)                                   \
    FN("ñ", ntilde, -1, 150, 241, 241)                              \
    FN("#", numbersign, 35, 35, 35, 35)                             \
    FN("o", o, 111, 111, 111, 111)                                  \
    FN("ó", oacute, -1, 151, 243, 243)                              \
    FN("ô", ocircumflex, -1, 153, 244, 244)                         \
    FN("ö", odieresis, -1, 154, 246, 246)                           \
    FN("œ", oe, 250, 207, 156, 156)                                 \
    FN("̨", ogonek, 206, 254, -1, 29)                                \
    FN("ò", ograve, -1, 152, 242, 242)                              \
    FN("1", one, 49, 49, 49, 49)                                    \
    FN("½", onehalf, -1, -1, 189, 189)                              \
    FN("¼", onequarter, -1, -1, 188, 188)                           \
    FN("¹", onesuperior, -1, -1, 185, 185)                          \
    FN("ª", ordfeminine, 227, 187, 170, 170)                        \
    FN("º", ordmasculine, 235, 188, 186, 186)                       \
    FN("ø", oslash, 249, 191, 248, 248)                             \
    FN("õ", otilde, -1, 155, 245, 245)                              \
    FN("p", p, 112, 112, 112, 112)                                  \
    FN("¶", paragraph, 182, 166, 182, 182)                          \
    FN("(", parenleft, 40, 40, 40, 40)                              \
    FN(")", parenright, 41, 41, 41, 41)                             \
    FN("%", percent, 37, 37, 37, 37)                                \
    FN(".", period, 46, 46, 46, 46)                                 \
    FN("·", periodcentered, 180, 225, 183, 183)                     \
    FN("‰", perthousand, 189, 228, 137, 139)                        \
    FN("+", plus, 43, 43, 43, 43)                                   \
    FN("±", plusminus, -1, 177, 177, 177)                           \
    FN("q", q, 113, 113, 113, 113)                                  \
    FN("?", question, 63, 63, 63, 63)                               \
    FN("¿", questiondown, 191, 192, 191, 191)                       \
    FN("\"", quotedbl, 34, 34, 34, 34)                              \
    FN("„", quotedblbase, 185, 227, 132, 140)                       \
    FN("“", quotedblleft, 170, 210, 147, 141)                       \
    FN("”", quotedblright, 186, 211, 148, 142)                      \
    FN("‘", quoteleft, 96, 212, 145, 143)                           \
    FN("’", quoteright, 39, 213, 146, 144)                          \
    FN("‚", quotesinglbase, 184, 226, 130, 145)                     \
    FN(",", quotesingle, 169, 39, 39, 39)                           \
    FN("r", r, 114, 114, 114, 114)                                  \
    FN("®", registered, -1, 168, 174, 174)                          \
    FN("̊", ring, 202, 251, -1, 30)                                  \
    FN("s", s, 115, 115, 115, 115)                                  \
    FN("š", scaron, -1, -1, 154, 157)                               \
    FN("§", section, 167, 164, 167, 167)                            \
    FN(";", semicolon, 59, 59, 59, 59)                              \
    FN("7", seven, 55, 55, 55, 55)                                  \
    FN("6", six, 54, 54, 54, 54)                                    \
    FN("/", slash, 47, 47, 47, 47)                                  \
    FN(" ", space, 32, 32, 32, 32) /* FIXME: Note 6 */              \
    FN("£", sterling, 163, 163, 163, 163)                           \
    FN("t", t, 116, 116, 116, 116)                                  \
    FN("þ", thorn, -1, -1, 254, 254)                                \
    FN("3", three, 51, 51, 51, 51)                                  \
    FN("¾", threequarters, -1, -1, 190, 190)                        \
    FN("³", threesuperior, -1, -1, 179, 179)                        \
    FN("̃", tilde, 196, 247, 152, 31)                                \
    FN("™", trademark, -1, 170, 153, 146)                           \
    FN("2", two, 50, 50, 50, 50)                                    \
    FN("²", twosuperior, -1, -1, 178, 178)                          \
    FN("u", u, 117, 117, 117, 117)                                  \
    FN("ú", uacute, -1, 156, 250, 250)                              \
    FN("û", ucircumflex, -1, 158, 251, 251)                         \
    FN("ü", udieresis, -1, 159, 252, 252)                           \
    FN("ù", ugrave, -1, 157, 249, 249)                              \
    FN("_", underscore, 95, 95, 95, 95)                             \
    FN("v", v, 118, 118, 118, 118)                                  \
    FN("w", w, 119, 119, 119, 119)                                  \
    FN("x", x, 120, 120, 120, 120)                                  \
    FN("y", y, 121, 121, 121, 121)                                  \
    FN("ý", yacute, -1, -1, 253, 253)                               \
    FN("ÿ", ydieresis, -1, 216, 255, 255)                           \
    FN("¥", yen, 165, 180, 165, 165)                                \
    FN("z", z, 122, 122, 122, 122)                                  \
    FN("ž", zcaron, -1, -1, 158, 158) /* FIXME: Note 2 */           \
    FN("0", zero, 48, 48, 48, 48)

// https://help.adobe.com/en_US/framemaker/2015/using/using-framemaker-2015/frm_references_re/frm_character_sets_cs/Symbol_and_ZapfDingbats_character_sets-.htm
#define ENUMERATE_SYMBOL_CHARACTER_SET(V)                        \
    V("Α", Alpha, 65)                                            \
    V("Β", Beta, 66)                                             \
    V("Χ", Chi, 67)                                              \
    V("Δ", Delta, 68)                                            \
    V("Ε", Epsilon, 69)                                          \
    V("Η", Eta, 72)                                              \
    V("€", Euro, 160)                                            \
    V("Γ", Gamma, 71)                                            \
    V("ℑ", Ifraktur, 193)                                        \
    V("Ι", Iota, 73)                                             \
    V("Κ", Kappa, 75)                                            \
    V("Λ", Lambda, 76)                                           \
    V("Μ", Mu, 77)                                               \
    V("Ν", Nu, 78)                                               \
    V("Ω", Omega, 87)                                            \
    V("Ο", Omicron, 79)                                          \
    V("Φ", Phi, 70)                                              \
    V("Π", Pi, 80)                                               \
    V("Ψ", Psi, 89)                                              \
    V("ℜ", Rfraktur, 194)                                        \
    V("Ρ", Rho, 82)                                              \
    V("Σ", Sigma, 83)                                            \
    V("Τ", Tau, 84)                                              \
    V("Θ", Theta, 81)                                            \
    V("Υ", Upsilon, 85)                                          \
    V("ϒ", Upsilon1, 161)                                        \
    V("Ξ", Xi, 88)                                               \
    V("Ζ", Zeta, 90)                                             \
    V("ℵ", aleph, 192)                                           \
    V("α", alpha, 97)                                            \
    V("&", ampersand, 38)                                        \
    V("∠", angle, 208)                                           \
    V("〈", angleleft, 225)                                      \
    V("〉", angleright, 241)                                     \
    V("≈", approxequal, 187)                                     \
    V("↔", arrowboth, 171)                                       \
    V("⇔", arrowdblboth, 219)                                    \
    V("⇓", arrowdbldown, 223)                                    \
    V("⇐", arrowdblleft, 220)                                    \
    V("⇒", arrowdblright, 222)                                   \
    V("⇑", arrowdblup, 221)                                      \
    V("↓", arrowdown, 175)                                       \
    V("", arrowhorizex, 190)                                    \
    V("←", arrowleft, 172)                                       \
    V("→", arrowright, 174)                                      \
    V("↑", arrowup, 173)                                         \
    V("", arrowvertex, 189)                                     \
    V("∗", asteriskmath, 42)                                     \
    V("|", bar, 124)                                             \
    V("β", beta, 98)                                             \
    V("{", braceleft, 123)                                       \
    V("}", braceright, 125)                                      \
    V("", bracelefttp, 236)                                     \
    V("", braceleftmid, 237)                                    \
    V("", braceleftbt, 238)                                     \
    V("", bracerighttp, 252)                                    \
    V("", bracerightmid, 253)                                   \
    V("", bracerightbt, 254)                                    \
    V("", braceex, 239)                                         \
    V("[", bracketleft, 91)                                      \
    V("]", bracketright, 93)                                     \
    V("", bracketlefttp, 233)                                   \
    V("", bracketleftex, 234)                                   \
    V("", bracketleftbt, 235)                                   \
    V("", bracketrighttp, 249)                                  \
    V("", bracketrightex, 250)                                  \
    V("", bracketrightbt, 251)                                  \
    V("•", bullet, 183)                                          \
    V("↵", carriagereturn, 191)                                  \
    V("χ", chi, 99)                                              \
    V("⊗", circlemultiply, 196)                                  \
    V("⊕", circleplus, 197)                                      \
    V("♣", club, 167)                                            \
    V(":", colon, 58)                                            \
    V(",", comma, 44)                                            \
    V("≅", congruent, 64)                                        \
    V("", copyrightsans, 227)                                   \
    V("©", copyrightserif, 211)                                  \
    V("°", degree, 176)                                          \
    V("δ", delta, 100)                                           \
    V("♦", diamond, 168)                                         \
    V("÷", divide, 184)                                          \
    V("⋅", dotmath, 215)                                         \
    V("8", eight, 56)                                            \
    V("∈", element, 206)                                         \
    V("…", ellipsis, 188)                                        \
    V("∅", emptyset, 198)                                        \
    V("ε", epsilon, 101)                                         \
    V("=", equal, 61)                                            \
    V("≡", equivalence, 186)                                     \
    V("η", eta, 104)                                             \
    V("!", exclam, 33)                                           \
    V("∃", existential, 36)                                      \
    V("5", five, 53)                                             \
    V("ƒ", florin, 166)                                          \
    V("4", four, 52)                                             \
    V("⁄", fraction, 164)                                        \
    V("γ", gamma, 103)                                           \
    V("∇", gradient, 209)                                        \
    V(">", greater, 62)                                          \
    V("≥", greaterequal, 179)                                    \
    V("♥", heart, 169)                                           \
    V("∞", infinity, 165)                                        \
    V("`", integral, 242)                                        \
    V("∫", integraltp, 243)                                      \
    V("", integralex, 244)                                      \
    V("⌡", integralbt, 245)                                      \
    V("∩", intersection, 199)                                    \
    V("ι", iota, 105)                                            \
    V("κ", kappa, 107)                                           \
    V("λ", lambda, 108)                                          \
    V("<", less, 60)                                             \
    V("≤", lessequal, 163)                                       \
    V("∧", logicaland, 217)                                      \
    V("¬", logicalnot, 216)                                      \
    V("∨", logicalor, 218)                                       \
    V("⋄", lozenge, 224)                                         \
    V("−", minus, 45)                                            \
    V("′", minute, 162)                                          \
    V("μ", mu, 109)                                              \
    V("×", multiply, 180)                                        \
    V("9", nine, 57)                                             \
    V("∉", notelement, 207)                                      \
    V("≠", notequal, 185)                                        \
    V("⊄", notsubset, 203)                                       \
    V("ν", nu, 110)                                              \
    V("#", numbersign, 35)                                       \
    V("ω", omega, 119)                                           \
    V("v", omega1, 118)                                          \
    V("ϖ", omicron, 111)                                         \
    V("1", one, 49)                                              \
    V("(", parenleft, 40)                                        \
    V(")", parenright, 41)                                       \
    V("", parenlefttp, 230)                                     \
    V("", parenleftex, 231)                                     \
    V("", parenleftbt, 232)                                     \
    V("", parenrighttp, 246)                                    \
    V("", parenrightex, 247)                                    \
    V("", parenrightbt, 248)                                    \
    V("∂", partialdiff, 182)                                     \
    V("%", percent, 37)                                          \
    V(".", period, 46)                                           \
    V("⊥", perpendicular, 94)                                    \
    V("φ", phi, 102)                                             \
    V("ϕ", phi1, 106)                                            \
    V("π", pi, 112)                                              \
    V("+", plus, 43)                                             \
    V("±", plusminus, 177)                                       \
    V("∏", product, 213)                                         \
    V("⊂", propersubset, 204)                                    \
    V("⊃", propersuperset, 201)                                  \
    V("∝", proportional, 181)                                    \
    V("ψ", psi, 121)                                             \
    V("?", question, 63)                                         \
    V("√", radical, 214)                                         \
    V("?", radicalex, 96) /* FIXME: What is this character, ? */ \
    V("⊆", reflexsubset, 205)                                    \
    V("⊇", reflexsuperset, 202)                                  \
    V("", registersans, 226)                                    \
    V("®", registerserif, 210)                                   \
    V("ρ", rho, 114)                                             \
    V("″", second, 178)                                          \
    V(";", semicolon, 59)                                        \
    V("7", seven, 55)                                            \
    V("σ", sigma, 115)                                           \
    V("ς", sigma1, 86)                                           \
    V("∼", similar, 126)                                         \
    V("6", six, 54)                                              \
    V("/", slash, 47)                                            \
    V(" ", space, 32)                                            \
    V("¤", spade, 170)                                           \
    V("∋", suchthat, 39)                                         \
    V("∑", summation, 229)                                       \
    V("τ", tau, 116)                                             \
    V("∴", therefore, 92)                                        \
    V("θ", theta, 113)                                           \
    V("ϑ", theta1, 74)                                           \
    V("3", three, 51)                                            \
    V("", trademarksans, 228)                                   \
    V("™", trademarkserif, 212)                                  \
    V("2", two, 50)                                              \
    V("_", underscore, 95)                                       \
    V("∪", union, 200)                                           \
    V("∀", universal, 34)                                        \
    V("υ", upsilon, 117)                                         \
    V("℘", weierstrass, 195)                                     \
    V("ξ", xi, 120)                                              \
    V("0", zero, 48)                                             \
    V("ζ", zeta, 15)

#define ENUMERATE_ZAPF_DINGBATS_CHARACTER_SET(V) \
    V(" ", space, 040)                           \
    V("✁", a1, 041)                              \
    V("✂", a2, 042)                              \
    V("✃", a202, 043)                            \
    V("✄", a3, 044)                              \
    V("☎", a4, 045)                              \
    V("✆", a5, 046)                              \
    V("✇", a119, 047)                            \
    V("✈", a118, 050)                            \
    V("✉", a117, 051)                            \
    V("☛", a11, 052)                             \
    V("☞", a12, 053)                             \
    V("✌", a13, 054)                             \
    V("✍", a14, 055)                             \
    V("✎", a15, 056)                             \
    V("✏", a16, 057)                             \
    V("✐", a105, 060)                            \
    V("✑", a17, 061)                             \
    V("✒", a18, 062)                             \
    V("✓", a19, 063)                             \
    V("✔", a20, 064)                             \
    V("✕", a21, 065)                             \
    V("✖", a22, 066)                             \
    V("✗", a23, 067)                             \
    V("✘", a24, 070)                             \
    V("✙", a25, 071)                             \
    V("✚", a26, 072)                             \
    V("✛", a27, 073)                             \
    V("✜", a28, 074)                             \
    V("✝", a6, 075)                              \
    V("✞", a7, 076)                              \
    V("✟", a8, 077)                              \
    V("✠", a9, 100)                              \
    V("✡", a10, 101)                             \
    V("✢", a29, 102)                             \
    V("✣", a30, 103)                             \
    V("✤", a31, 104)                             \
    V("✥", a32, 105)                             \
    V("✦", a33, 106)                             \
    V("✧", a34, 107)                             \
    V("★", a35, 110)                             \
    V("✩", a36, 111)                             \
    V("✪", a37, 112)                             \
    V("✫", a38, 113)                             \
    V("✬", a39, 114)                             \
    V("✭", a40, 115)                             \
    V("✮", a41, 116)                             \
    V("✯", a42, 117)                             \
    V("✰", a43, 120)                             \
    V("✱", a44, 121)                             \
    V("✲", a45, 122)                             \
    V("✳", a46, 123)                             \
    V("✴", a47, 124)                             \
    V("✵", a48, 125)                             \
    V("✶", a49, 126)                             \
    V("✷", a50, 127)                             \
    V("✸", a51, 130)                             \
    V("✹", a52, 131)                             \
    V("✺", a53, 132)                             \
    V("✻", a54, 133)                             \
    V("✼", a55, 134)                             \
    V("✽", a56, 135)                             \
    V("✾", a57, 136)                             \
    V("✿", a58, 137)                             \
    V("❀", a59, 140)                             \
    V("❁", a60, 141)                             \
    V("❂", a61, 142)                             \
    V("❃", a62, 143)                             \
    V("❄", a63, 144)                             \
    V("❅", a64, 145)                             \
    V("❆", a65, 146)                             \
    V("❇", a66, 147)                             \
    V("❈", a67, 150)                             \
    V("❉", a68, 151)                             \
    V("❊", a69, 152)                             \
    V("❋", a70, 153)                             \
    V("●", a71, 154)                             \
    V("❍", a72, 155)                             \
    V("■", a73, 156)                             \
    V("❏", a74, 157)                             \
    V("❐", a203, 160)                            \
    V("❑", a75, 161)                             \
    V("❒", a204, 162)                            \
    V("▲", a76, 163)                             \
    V("▼", a77, 164)                             \
    V("◆", a78, 165)                             \
    V("❖", a79, 166)                             \
    V("◗", a81, 167)                             \
    V("❘", a82, 170)                             \
    V("❙", a83, 171)                             \
    V("❚", a84, 172)                             \
    V("❛", a97, 173)                             \
    V("❜", a98, 174)                             \
    V("❝", a99, 175)                             \
    V("❞", a100, 176)                            \
    V("❡", a101, 241)                            \
    V("❢", a102, 242)                            \
    V("❣", a103, 243)                            \
    V("❤", a104, 244)                            \
    V("❥", a106, 245)                            \
    V("❦", a107, 246)                            \
    V("❧", a108, 247)                            \
    V("♣", a112, 250)                            \
    V("♦", a111, 251)                            \
    V("♥", a110, 252)                            \
    V("♠", a109, 253)                            \
    V("①", a120, 254)                            \
    V("②", a121, 255)                            \
    V("③", a122, 256)                            \
    V("④", a123, 257)                            \
    V("⑤", a124, 260)                            \
    V("⑥", a125, 261)                            \
    V("⑦", a126, 262)                            \
    V("⑧", a127, 263)                            \
    V("⑨", a128, 264)                            \
    V("⑩", a129, 265)                            \
    V("❶", a130, 266)                            \
    V("❷", a131, 267)                            \
    V("❸", a132, 270)                            \
    V("❹", a133, 271)                            \
    V("❺", a134, 272)                            \
    V("❻", a135, 273)                            \
    V("❼", a136, 274)                            \
    V("❽", a137, 275)                            \
    V("❾", a138, 276)                            \
    V("❿", a139, 277)                            \
    V("➀", a140, 300)                            \
    V("➁", a141, 301)                            \
    V("➂", a142, 302)                            \
    V("➃", a143, 303)                            \
    V("➄", a144, 304)                            \
    V("➅", a145, 305)                            \
    V("➆", a146, 306)                            \
    V("➇", a147, 307)                            \
    V("➈", a148, 310)                            \
    V("➉", a149, 311)                            \
    V("➊", a150, 312)                            \
    V("➋", a151, 313)                            \
    V("➌", a152, 314)                            \
    V("➍", a153, 315)                            \
    V("➎", a154, 316)                            \
    V("➏", a155, 317)                            \
    V("➐", a156, 320)                            \
    V("➑", a157, 321)                            \
    V("➒", a158, 322)                            \
    V("➓", a159, 323)                            \
    V("➔", a160, 324)                            \
    V("→", a161, 325)                            \
    V("↔", a163, 326)                            \
    V("↕", a164, 327)                            \
    V("➘", a196, 330)                            \
    V("➙", a165, 331)                            \
    V("➚", a192, 332)                            \
    V("➛", a166, 333)                            \
    V("➜", a167, 334)                            \
    V("➝", a168, 335)                            \
    V("➞", a169, 336)                            \
    V("➟", a170, 337)                            \
    V("➠", a171, 340)                            \
    V("➡", a172, 341)                            \
    V("➢", a173, 342)                            \
    V("➣", a162, 343)                            \
    V("➤", a174, 344)                            \
    V("➥", a175, 345)                            \
    V("➦", a176, 346)                            \
    V("➧", a177, 347)                            \
    V("➨", a178, 350)                            \
    V("➩", a179, 351)                            \
    V("➪", a193, 352)                            \
    V("➫", a180, 353)                            \
    V("➬", a199, 354)                            \
    V("➭", a181, 355)                            \
    V("➮", a200, 356)                            \
    V("➯", a182, 357)                            \
    V("➱", a201, 361)                            \
    V("➲", a183, 362)                            \
    V("➳", a184, 363)                            \
    V("➴", a197, 364)                            \
    V("➵", a185, 365)                            \
    V("➶", a194, 366)                            \
    V("➷", a198, 367)                            \
    V("➸", a186, 370)                            \
    V("➹", a195, 371)                            \
    V("➺", a187, 372)                            \
    V("➻", a188, 373)                            \
    V("➼", a189, 374)                            \
    V("➽", a190, 375)                            \
    V("➾", a191, 376)

namespace PDF {

struct CharDescriptor {
    String name;
    u32 code_point;
};

class Encoding : public RefCounted<Encoding> {
public:
    static PDFErrorOr<NonnullRefPtr<Encoding>> from_object(Document*, NonnullRefPtr<Object> const&);

    static NonnullRefPtr<Encoding> standard_encoding();
    static NonnullRefPtr<Encoding> mac_encoding();
    static NonnullRefPtr<Encoding> windows_encoding();
    static NonnullRefPtr<Encoding> pdf_doc_encoding();
    static NonnullRefPtr<Encoding> symbol_encoding();
    static NonnullRefPtr<Encoding> zapf_encoding();

    HashMap<u16, CharDescriptor> const& descriptors() const { return m_descriptors; }

    CharDescriptor const& get_char_code_descriptor(u16 char_code) const;

protected:
    HashMap<u16, CharDescriptor> m_descriptors;
    HashMap<String, u16> m_name_mapping;
};

}
