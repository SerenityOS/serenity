/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibPDF/ObjectDerivatives.h>

// Appendix D.2: Latin Character Set and Encodings
#define ENUMERATE_LATIN_CHARACTER_SET(V)                           \
    V("A", A, 65, 65, 65, 65)                                      \
    V("Æ", AE, 225, 174, 198, 198)                                 \
    V("Á", Aacute, -1, 231, 193, 193)                              \
    V("Â", Acircumflex, -1, 229, 194, 194)                         \
    V("Ä", Adieresis, -1, 128, 196, 196)                           \
    V("À", Agrave, -1, 203, 192, 192)                              \
    V("Å", Aring, -1, 129, 197, 197)                               \
    V("Ã", Atilde, -1, 204, 195, 195)                              \
    V("B", B, 66, 66, 66, 66)                                      \
    V("C", C, 67, 67, 67, 67)                                      \
    V("Ç", Ccedilla, -1, 130, 199, 199)                            \
    V("D", D, 68, 68, 68, 68)                                      \
    V("E", E, 69, 69, 69, 69)                                      \
    V("É", Eacute, -1, 131, 201, 201)                              \
    V("Ê", Ecircumflex, -1, 230, 202, 202)                         \
    V("Ë", Edieresis, -1, 232, 203, 203)                           \
    V("È", Egrave, -1, 233, 200, 200)                              \
    V("Ð", Eth, -1, -1, 208, 208)                                  \
    V("€", Euro, -1, -1, 128, 160) /* FIXME: Note 1 */             \
    V("F", F, 70, 70, 70, 70)                                      \
    V("G", G, 71, 71, 71, 71)                                      \
    V("H", H, 72, 72, 72, 72)                                      \
    V("I", I, 73, 73, 73, 73)                                      \
    V("Í", Iacute, -1, 234, 205, 205)                              \
    V("Î", Icircumflex, -1, 235, 206, 206)                         \
    V("Ï", Idieresis, -1, 236, 207, 207)                           \
    V("Ì", Igrave, -1, 237, 204, 204)                              \
    V("J", J, 74, 74, 74, 74)                                      \
    V("K", K, 75, 75, 75, 75)                                      \
    V("L", L, 76, 76, 76, 76)                                      \
    V("Ł", Lslash, 232, -1, -1, 149)                               \
    V("M", M, 77, 77, 77, 77)                                      \
    V("N", N, 78, 78, 78, 78)                                      \
    V("Ñ", Ntilde, -1, 132, 209, 209)                              \
    V("O", O, 79, 79, 79, 79)                                      \
    V("Œ", OE, 234, 206, 140, 150)                                 \
    V("Ó", Oacute, -1, 238, 211, 211)                              \
    V("Ô", Ocircumflex, -1, 239, 212, 212)                         \
    V("Ö", Odieresis, -1, 133, 214, 214)                           \
    V("Ò", Ograve, -1, 241, 210, 210)                              \
    V("Ø", Oslash, 233, 175, 216, 216)                             \
    V("Õ", Otilde, -1, 205, 213, 213)                              \
    V("P", P, 80, 80, 80, 80)                                      \
    V("Q", Q, 81, 81, 81, 81)                                      \
    V("R", R, 82, 82, 82, 82)                                      \
    V("S", S, 83, 83, 83, 83)                                      \
    V("Š", Scaron, -1, -1, 138, 151)                               \
    V("T", T, 84, 84, 84, 84)                                      \
    V("Þ", Thorn, -1, -1, 222, 222)                                \
    V("U", U, 85, 85, 85, 85)                                      \
    V("Ú", Uacute, -1, 242, 218, 218)                              \
    V("Û", Ucircumflex, -1, 243, 219, 219)                         \
    V("Ü", Udieresis, -1, 134, 220, 220)                           \
    V("Ù", Ugrave, -1, 244, 217, 217)                              \
    V("V", V, 86, 86, 86, 86)                                      \
    V("W", W, 87, 87, 87, 87)                                      \
    V("X", X, 88, 88, 88, 88)                                      \
    V("Y", Y, 89, 89, 89, 89)                                      \
    V("Ý", Yacute, -1, -1, 221, 221)                               \
    V("Ÿ", Ydieresis, -1, 217, 159, 152)                           \
    V("Z", Z, 90, 90, 90, 90)                                      \
    V("Ž", Zcaron, -1, -1, 142, 153) /* FIXME: Note 2 */           \
    V("a", a, 97, 97, 97, 97)                                      \
    V("á", aacute, -1, 135, 225, 225)                              \
    V("â", acircumflex, -1, 137, 226, 226)                         \
    V("́", acute, 194, 171, 180, 180)                               \
    V("ä", adieresis, -1, 138, 228, 228)                           \
    V("æ", ae, 241, 190, 230, 230)                                 \
    V("à", agrave, -1, 136, 224, 224)                              \
    V("&", ampersand, 38, 38, 38, 38)                              \
    V("å", aring, -1, 140, 229, 229)                               \
    V("^", asciicircum, 94, 94, 94, 94)                            \
    V("~", asciitilde, 126, 126, 126, 126)                         \
    V("*", asterisk, 42, 42, 42, 42)                               \
    V("@", at, 64, 64, 64, 64)                                     \
    V("ã", atilde, -1, 139, 227, 227)                              \
    V("b", b, 98, 98, 98, 98)                                      \
    V("\\", backslash, 92, 92, 92, 92)                             \
    V("|", bar, 124, 124, 124, 124)                                \
    V("{", braceleft, 123, 123, 123, 123)                          \
    V("}", braceright, 125, 125, 125, 125)                         \
    V("[", bracketleft, 91, 91, 91, 91)                            \
    V("]", bracketright, 93, 93, 93, 93)                           \
    V(" ̆", breve, 198, 249, -1, 24)                                \
    V("¦", brokenbar, -1, -1, 166, 166)                            \
    V("•", bullet, 183, 165, 149, 128) /* FIXME: Note 3 */         \
    V("c", c, 99, 99, 99, 99)                                      \
    V("ˇ", caron, 207, 255, -1, 25)                                \
    V("ç", ccedilla, -1, 141, 231, 231)                            \
    V("̧", cedilla, 203, 252, 184, 184)                             \
    V("¢", cent, 162, 162, 162, 162)                               \
    V("ˆ", circumflex, 195, 246, 136, 26)                          \
    V(":", colon, 58, 58, 58, 58)                                  \
    V(",", comma, 44, 44, 44, 44)                                  \
    V("©", copyright, -1, 169, 169, 169)                           \
    V("¤", currency, 168, 219, 164, 164) /* FIXME: Note 1 */       \
    V("d", d, 100, 100, 100, 100)                                  \
    V("†", dagger, 178, 160, 134, 129)                             \
    V("‡", daggerdbl, 179, 224, 135, 130)                          \
    V("°", degree, -1, 161, 176, 176)                              \
    V("̈", dieresis, 200, 172, 168, 168)                            \
    V("÷", divide, -1, 214, 247, 247)                              \
    V("$", dollar, 36, 36, 36, 36)                                 \
    V("̇", dotaccent, 199, 250, -1, 27)                             \
    V("ı", dotlessi, 245, 245, -1, 154)                            \
    V("e", e, 101, 101, 101, 101)                                  \
    V("é", eacute, -1, 142, 233, 233)                              \
    V("ê", ecircumflex, -1, 144, 234, 234)                         \
    V("ë", edieresis, -1, 145, 235, 235)                           \
    V("è", egrave, -1, 143, 232, 232)                              \
    V("8", eight, 56, 56, 56, 56)                                  \
    V("…", ellipsis, 188, 201, 133, 131)                           \
    V("—", emdash, 208, 209, 151, 132)                             \
    V("–", endash, 177, 208, 150, 133)                             \
    V("=", equal, 61, 61, 61, 61)                                  \
    V("ð", eth, -1, -1, 240, 240)                                  \
    V("!", exclam, 33, 33, 33, 33)                                 \
    V("¡", exclamdown, 161, 193, 161, 161)                         \
    V("f", f, 102, 102, 102, 102)                                  \
    V("ﬁ", fi, 174, 222, -1, 147)                                  \
    V("5", five, 53, 53, 53, 53)                                   \
    V("ﬂ", fl, 175, 223, -1, 148)                                  \
    V("ƒ", florin, 166, 196, 131, 134)                             \
    V("4", four, 52, 52, 52, 52)                                   \
    V("⁄", fraction, 164, 218, -1, 135)                            \
    V("g", g, 103, 103, 103, 103)                                  \
    V("ß", germandbls, 251, 167, 223, 223)                         \
    V("`", grave, 193, 96, 96, 96)                                 \
    V(">", greater, 62, 62, 62, 62)                                \
    V("«", guillemotleft, 171, 199, 171, 171)  /* FIXME: Note 4 */ \
    V("»", guillemotright, 187, 200, 187, 187) /* FIXME: Note 4 */ \
    V("‹", guilsinglleft, 172, 220, 139, 136)                      \
    V("›", guilsinglright, 173, 221, 155, 137)                     \
    V("h", h, 104, 104, 104, 104)                                  \
    V("̋", hungarumlaut, 205, 253, -1, 28)                          \
    V("-", hyphen, 45, 45, 45, 45) /* FIXME: Note 5 */             \
    V("i", i, 105, 105, 105, 105)                                  \
    V("í", iacute, -1, 146, 237, 237)                              \
    V("î", icircumflex, -1, 148, 238, 238)                         \
    V("ï", idieresis, -1, 149, 239, 239)                           \
    V("ì", igrave, -1, 147, 236, 236)                              \
    V("j", j, 106, 106, 106, 106)                                  \
    V("k", k, 107, 107, 107, 107)                                  \
    V("l", l, 108, 108, 108, 108)                                  \
    V("<", less, 60, 60, 60, 60)                                   \
    V("¬", logicalnot, -1, 194, 172, 172)                          \
    V("ł", lslash, 248, -1, -1, 155)                               \
    V("m", m, 109, 109, 109, 109)                                  \
    V("̄", macron, 197, 248, 175, 175)                              \
    V("−", minus, -1, -1, -1, 138)                                 \
    V("μ", mu, -1, 181, 181, 181)                                  \
    V("×", multiply, -1, -1, 215, 215)                             \
    V("n", n, 110, 110, 110, 110)                                  \
    V("9", nine, 57, 57, 57, 57)                                   \
    V("ñ", ntilde, -1, 150, 241, 241)                              \
    V("#", numbersign, 35, 35, 35, 35)                             \
    V("o", o, 111, 111, 111, 111)                                  \
    V("ó", oacute, -1, 151, 243, 243)                              \
    V("ô", ocircumflex, -1, 153, 244, 244)                         \
    V("ö", odieresis, -1, 154, 246, 246)                           \
    V("œ", oe, 250, 207, 156, 156)                                 \
    V("̨", ogonek, 206, 254, -1, 29)                                \
    V("ò", ograve, -1, 152, 242, 242)                              \
    V("1", one, 49, 49, 49, 49)                                    \
    V("½", onehalf, -1, -1, 189, 189)                              \
    V("¼", onequarter, -1, -1, 188, 188)                           \
    V("¹", onesuperior, -1, -1, 185, 185)                          \
    V("ª", ordfeminine, 227, 187, 170, 170)                        \
    V("º", ordmasculine, 235, 188, 186, 186)                       \
    V("ø", oslash, 249, 191, 248, 248)                             \
    V("õ", otilde, -1, 155, 245, 245)                              \
    V("p", p, 112, 112, 112, 112)                                  \
    V("¶", paragraph, 182, 166, 182, 182)                          \
    V("(", parenleft, 40, 40, 40, 40)                              \
    V(")", parenright, 41, 41, 41, 41)                             \
    V("%", percent, 37, 37, 37, 37)                                \
    V(".", period, 46, 46, 46, 46)                                 \
    V("·", periodcentered, 180, 225, 183, 183)                     \
    V("‰", perthousand, 189, 228, 137, 139)                        \
    V("+", plus, 43, 43, 43, 43)                                   \
    V("±", plusminus, -1, 177, 177, 177)                           \
    V("q", q, 113, 113, 113, 113)                                  \
    V("?", question, 63, 63, 63, 63)                               \
    V("¿", questiondown, 191, 192, 191, 191)                       \
    V("\"", quotedbl, 34, 34, 34, 34)                              \
    V("„", quotedblbase, 185, 227, 132, 140)                       \
    V("“", quotedblleft, 170, 210, 147, 141)                       \
    V("”", quotedblright, 186, 211, 148, 142)                      \
    V("‘", quoteleft, 96, 212, 145, 143)                           \
    V("’", quoteright, 39, 213, 146, 144)                          \
    V("‚", quotesinglbase, 184, 226, 130, 145)                     \
    V(",", quotesingle, 169, 39, 39, 39)                           \
    V("r", r, 114, 114, 114, 114)                                  \
    V("®", registered, -1, 168, 174, 174)                          \
    V("̊", ring, 202, 251, -1, 30)                                  \
    V("s", s, 115, 115, 115, 115)                                  \
    V("š", scaron, -1, -1, 154, 157)                               \
    V("§", section, 167, 164, 167, 167)                            \
    V(";", semicolon, 59, 59, 59, 59)                              \
    V("7", seven, 55, 55, 55, 55)                                  \
    V("6", six, 54, 54, 54, 54)                                    \
    V("/", slash, 47, 47, 47, 47)                                  \
    V(" ", space, 32, 32, 32, 32) /* FIXME: Note 6 */              \
    V("£", sterling, 163, 163, 163, 163)                           \
    V("t", t, 116, 116, 116, 116)                                  \
    V("þ", thorn, -1, -1, 254, 254)                                \
    V("3", three, 51, 51, 51, 51)                                  \
    V("¾", threequarters, -1, -1, 190, 190)                        \
    V("³", threesuperior, -1, -1, 179, 179)                        \
    V("̃", tilde, 196, 247, 152, 31)                                \
    V("™", trademark, -1, 170, 153, 146)                           \
    V("2", two, 50, 50, 50, 50)                                    \
    V("²", twosuperior, -1, -1, 178, 178)                          \
    V("u", u, 117, 117, 117, 117)                                  \
    V("ú", uacute, -1, 156, 250, 250)                              \
    V("û", ucircumflex, -1, 158, 251, 251)                         \
    V("ü", udieresis, -1, 159, 252, 252)                           \
    V("ù", ugrave, -1, 157, 249, 249)                              \
    V("_", underscore, 95, 95, 95, 95)                             \
    V("v", v, 118, 118, 118, 118)                                  \
    V("w", w, 119, 119, 119, 119)                                  \
    V("x", x, 120, 120, 120, 120)                                  \
    V("y", y, 121, 121, 121, 121)                                  \
    V("ý", yacute, -1, -1, 253, 253)                               \
    V("ÿ", ydieresis, -1, 216, 255, 255)                           \
    V("¥", yen, 165, 180, 165, 165)                                \
    V("z", z, 122, 122, 122, 122)                                  \
    V("ž", zcaron, -1, -1, 158, 158) /* FIXME: Note 2 */           \
    V("0", zero, 48, 48, 48, 48)

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
