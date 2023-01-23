/*
 * Copyright (c) 2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/HashMap.h>
#include <LibPDF/ObjectDerivatives.h>

// Appendix D.2: Latin Character Set and Encodings
#define ENUMERATE_LATIN_CHARACTER_SET(FN)                      \
    FN(A, 65, 65, 65, 65)                                      \
    FN(AE, 225, 174, 198, 198)                                 \
    FN(Aacute, -1, 231, 193, 193)                              \
    FN(Acircumflex, -1, 229, 194, 194)                         \
    FN(Adieresis, -1, 128, 196, 196)                           \
    FN(Agrave, -1, 203, 192, 192)                              \
    FN(Aring, -1, 129, 197, 197)                               \
    FN(Atilde, -1, 204, 195, 195)                              \
    FN(B, 66, 66, 66, 66)                                      \
    FN(C, 67, 67, 67, 67)                                      \
    FN(Ccedilla, -1, 130, 199, 199)                            \
    FN(D, 68, 68, 68, 68)                                      \
    FN(E, 69, 69, 69, 69)                                      \
    FN(Eacute, -1, 131, 201, 201)                              \
    FN(Ecircumflex, -1, 230, 202, 202)                         \
    FN(Edieresis, -1, 232, 203, 203)                           \
    FN(Egrave, -1, 233, 200, 200)                              \
    FN(Eth, -1, -1, 208, 208)                                  \
    FN(Euro, -1, -1, 128, 160) /* FIXME: Note 1 */             \
    FN(F, 70, 70, 70, 70)                                      \
    FN(G, 71, 71, 71, 71)                                      \
    FN(H, 72, 72, 72, 72)                                      \
    FN(I, 73, 73, 73, 73)                                      \
    FN(Iacute, -1, 234, 205, 205)                              \
    FN(Icircumflex, -1, 235, 206, 206)                         \
    FN(Idieresis, -1, 236, 207, 207)                           \
    FN(Igrave, -1, 237, 204, 204)                              \
    FN(J, 74, 74, 74, 74)                                      \
    FN(K, 75, 75, 75, 75)                                      \
    FN(L, 76, 76, 76, 76)                                      \
    FN(Lslash, 232, -1, -1, 149)                               \
    FN(M, 77, 77, 77, 77)                                      \
    FN(N, 78, 78, 78, 78)                                      \
    FN(Ntilde, -1, 132, 209, 209)                              \
    FN(O, 79, 79, 79, 79)                                      \
    FN(OE, 234, 206, 140, 150)                                 \
    FN(Oacute, -1, 238, 211, 211)                              \
    FN(Ocircumflex, -1, 239, 212, 212)                         \
    FN(Odieresis, -1, 133, 214, 214)                           \
    FN(Ograve, -1, 241, 210, 210)                              \
    FN(Oslash, 233, 175, 216, 216)                             \
    FN(Otilde, -1, 205, 213, 213)                              \
    FN(P, 80, 80, 80, 80)                                      \
    FN(Q, 81, 81, 81, 81)                                      \
    FN(R, 82, 82, 82, 82)                                      \
    FN(S, 83, 83, 83, 83)                                      \
    FN(Scaron, -1, -1, 138, 151)                               \
    FN(T, 84, 84, 84, 84)                                      \
    FN(Thorn, -1, -1, 222, 222)                                \
    FN(U, 85, 85, 85, 85)                                      \
    FN(Uacute, -1, 242, 218, 218)                              \
    FN(Ucircumflex, -1, 243, 219, 219)                         \
    FN(Udieresis, -1, 134, 220, 220)                           \
    FN(Ugrave, -1, 244, 217, 217)                              \
    FN(V, 86, 86, 86, 86)                                      \
    FN(W, 87, 87, 87, 87)                                      \
    FN(X, 88, 88, 88, 88)                                      \
    FN(Y, 89, 89, 89, 89)                                      \
    FN(Yacute, -1, -1, 221, 221)                               \
    FN(Ydieresis, -1, 217, 159, 152)                           \
    FN(Z, 90, 90, 90, 90)                                      \
    FN(Zcaron, -1, -1, 142, 153) /* FIXME: Note 2 */           \
    FN(a, 97, 97, 97, 97)                                      \
    FN(aacute, -1, 135, 225, 225)                              \
    FN(acircumflex, -1, 137, 226, 226)                         \
    FN(acute, 194, 171, 180, 180)                              \
    FN(adieresis, -1, 138, 228, 228)                           \
    FN(ae, 241, 190, 230, 230)                                 \
    FN(agrave, -1, 136, 224, 224)                              \
    FN(ampersand, 38, 38, 38, 38)                              \
    FN(aring, -1, 140, 229, 229)                               \
    FN(asciicircum, 94, 94, 94, 94)                            \
    FN(asciitilde, 126, 126, 126, 126)                         \
    FN(asterisk, 42, 42, 42, 42)                               \
    FN(at, 64, 64, 64, 64)                                     \
    FN(atilde, -1, 139, 227, 227)                              \
    FN(b, 98, 98, 98, 98)                                      \
    FN(backslash, 92, 92, 92, 92)                              \
    FN(bar, 124, 124, 124, 124)                                \
    FN(braceleft, 123, 123, 123, 123)                          \
    FN(braceright, 125, 125, 125, 125)                         \
    FN(bracketleft, 91, 91, 91, 91)                            \
    FN(bracketright, 93, 93, 93, 93)                           \
    FN(breve, 198, 249, -1, 24)                                \
    FN(brokenbar, -1, -1, 166, 166)                            \
    FN(bullet, 183, 165, 149, 128)                             \
    FN(c, 99, 99, 99, 99)                                      \
    FN(caron, 207, 255, -1, 25)                                \
    FN(ccedilla, -1, 141, 231, 231)                            \
    FN(cedilla, 203, 252, 184, 184)                            \
    FN(cent, 162, 162, 162, 162)                               \
    FN(circumflex, 195, 246, 136, 26)                          \
    FN(colon, 58, 58, 58, 58)                                  \
    FN(comma, 44, 44, 44, 44)                                  \
    FN(copyright, -1, 169, 169, 169)                           \
    FN(currency, 168, 219, 164, 164) /* FIXME: Note 1 */       \
    FN(d, 100, 100, 100, 100)                                  \
    FN(dagger, 178, 160, 134, 129)                             \
    FN(daggerdbl, 179, 224, 135, 130)                          \
    FN(degree, -1, 161, 176, 176)                              \
    FN(dieresis, 200, 172, 168, 168)                           \
    FN(divide, -1, 214, 247, 247)                              \
    FN(dollar, 36, 36, 36, 36)                                 \
    FN(dotaccent, 199, 250, -1, 27)                            \
    FN(dotlessi, 245, 245, -1, 154)                            \
    FN(e, 101, 101, 101, 101)                                  \
    FN(eacute, -1, 142, 233, 233)                              \
    FN(ecircumflex, -1, 144, 234, 234)                         \
    FN(edieresis, -1, 145, 235, 235)                           \
    FN(egrave, -1, 143, 232, 232)                              \
    FN(eight, 56, 56, 56, 56)                                  \
    FN(ellipsis, 188, 201, 133, 131)                           \
    FN(emdash, 208, 209, 151, 132)                             \
    FN(endash, 177, 208, 150, 133)                             \
    FN(equal, 61, 61, 61, 61)                                  \
    FN(eth, -1, -1, 240, 240)                                  \
    FN(exclam, 33, 33, 33, 33)                                 \
    FN(exclamdown, 161, 193, 161, 161)                         \
    FN(f, 102, 102, 102, 102)                                  \
    FN(fi, 174, 222, -1, 147)                                  \
    FN(five, 53, 53, 53, 53)                                   \
    FN(fl, 175, 223, -1, 148)                                  \
    FN(florin, 166, 196, 131, 134)                             \
    FN(four, 52, 52, 52, 52)                                   \
    FN(fraction, 164, 218, -1, 135)                            \
    FN(g, 103, 103, 103, 103)                                  \
    FN(germandbls, 251, 167, 223, 223)                         \
    FN(grave, 193, 96, 96, 96)                                 \
    FN(greater, 62, 62, 62, 62)                                \
    FN(guillemotleft, 171, 199, 171, 171)  /* FIXME: Note 4 */ \
    FN(guillemotright, 187, 200, 187, 187) /* FIXME: Note 4 */ \
    FN(guilsinglleft, 172, 220, 139, 136)                      \
    FN(guilsinglright, 173, 221, 155, 137)                     \
    FN(h, 104, 104, 104, 104)                                  \
    FN(hungarumlaut, 205, 253, -1, 28)                         \
    FN(hyphen, 45, 45, 45, 45) /* FIXME: Note 5 */             \
    FN(i, 105, 105, 105, 105)                                  \
    FN(iacute, -1, 146, 237, 237)                              \
    FN(icircumflex, -1, 148, 238, 238)                         \
    FN(idieresis, -1, 149, 239, 239)                           \
    FN(igrave, -1, 147, 236, 236)                              \
    FN(j, 106, 106, 106, 106)                                  \
    FN(k, 107, 107, 107, 107)                                  \
    FN(l, 108, 108, 108, 108)                                  \
    FN(less, 60, 60, 60, 60)                                   \
    FN(logicalnot, -1, 194, 172, 172)                          \
    FN(lslash, 248, -1, -1, 155)                               \
    FN(m, 109, 109, 109, 109)                                  \
    FN(macron, 197, 248, 175, 175)                             \
    FN(minus, -1, -1, -1, 138)                                 \
    FN(mu, -1, 181, 181, 181)                                  \
    FN(multiply, -1, -1, 215, 215)                             \
    FN(n, 110, 110, 110, 110)                                  \
    FN(nine, 57, 57, 57, 57)                                   \
    FN(ntilde, -1, 150, 241, 241)                              \
    FN(numbersign, 35, 35, 35, 35)                             \
    FN(o, 111, 111, 111, 111)                                  \
    FN(oacute, -1, 151, 243, 243)                              \
    FN(ocircumflex, -1, 153, 244, 244)                         \
    FN(odieresis, -1, 154, 246, 246)                           \
    FN(oe, 250, 207, 156, 156)                                 \
    FN(ogonek, 206, 254, -1, 29)                               \
    FN(ograve, -1, 152, 242, 242)                              \
    FN(one, 49, 49, 49, 49)                                    \
    FN(onehalf, -1, -1, 189, 189)                              \
    FN(onequarter, -1, -1, 188, 188)                           \
    FN(onesuperior, -1, -1, 185, 185)                          \
    FN(ordfeminine, 227, 187, 170, 170)                        \
    FN(ordmasculine, 235, 188, 186, 186)                       \
    FN(oslash, 249, 191, 248, 248)                             \
    FN(otilde, -1, 155, 245, 245)                              \
    FN(p, 112, 112, 112, 112)                                  \
    FN(paragraph, 182, 166, 182, 182)                          \
    FN(parenleft, 40, 40, 40, 40)                              \
    FN(parenright, 41, 41, 41, 41)                             \
    FN(percent, 37, 37, 37, 37)                                \
    FN(period, 46, 46, 46, 46)                                 \
    FN(periodcentered, 180, 225, 183, 183)                     \
    FN(perthousand, 189, 228, 137, 139)                        \
    FN(plus, 43, 43, 43, 43)                                   \
    FN(plusminus, -1, 177, 177, 177)                           \
    FN(q, 113, 113, 113, 113)                                  \
    FN(question, 63, 63, 63, 63)                               \
    FN(questiondown, 191, 192, 191, 191)                       \
    FN(quotedbl, 34, 34, 34, 34)                               \
    FN(quotedblbase, 185, 227, 132, 140)                       \
    FN(quotedblleft, 170, 210, 147, 141)                       \
    FN(quotedblright, 186, 211, 148, 142)                      \
    FN(quoteleft, 96, 212, 145, 143)                           \
    FN(quoteright, 39, 213, 146, 144)                          \
    FN(quotesinglbase, 184, 226, 130, 145)                     \
    FN(quotesingle, 169, 39, 39, 39)                           \
    FN(r, 114, 114, 114, 114)                                  \
    FN(registered, -1, 168, 174, 174)                          \
    FN(ring, 202, 251, -1, 30)                                 \
    FN(s, 115, 115, 115, 115)                                  \
    FN(scaron, -1, -1, 154, 157)                               \
    FN(section, 167, 164, 167, 167)                            \
    FN(semicolon, 59, 59, 59, 59)                              \
    FN(seven, 55, 55, 55, 55)                                  \
    FN(six, 54, 54, 54, 54)                                    \
    FN(slash, 47, 47, 47, 47)                                  \
    FN(space, 32, 32, 32, 32) /* FIXME: Note 6 */              \
    FN(sterling, 163, 163, 163, 163)                           \
    FN(t, 116, 116, 116, 116)                                  \
    FN(thorn, -1, -1, 254, 254)                                \
    FN(three, 51, 51, 51, 51)                                  \
    FN(threequarters, -1, -1, 190, 190)                        \
    FN(threesuperior, -1, -1, 179, 179)                        \
    FN(tilde, 196, 247, 152, 31)                               \
    FN(trademark, -1, 170, 153, 146)                           \
    FN(two, 50, 50, 50, 50)                                    \
    FN(twosuperior, -1, -1, 178, 178)                          \
    FN(u, 117, 117, 117, 117)                                  \
    FN(uacute, -1, 156, 250, 250)                              \
    FN(ucircumflex, -1, 158, 251, 251)                         \
    FN(udieresis, -1, 159, 252, 252)                           \
    FN(ugrave, -1, 157, 249, 249)                              \
    FN(underscore, 95, 95, 95, 95)                             \
    FN(v, 118, 118, 118, 118)                                  \
    FN(w, 119, 119, 119, 119)                                  \
    FN(x, 120, 120, 120, 120)                                  \
    FN(y, 121, 121, 121, 121)                                  \
    FN(yacute, -1, -1, 253, 253)                               \
    FN(ydieresis, -1, 216, 255, 255)                           \
    FN(yen, 165, 180, 165, 165)                                \
    FN(z, 122, 122, 122, 122)                                  \
    FN(zcaron, -1, -1, 158, 158) /* FIXME: Note 2 */           \
    FN(zero, 48, 48, 48, 48)

// https://help.adobe.com/en_US/framemaker/2015/using/using-framemaker-2015/frm_references_re/frm_character_sets_cs/Symbol_and_ZapfDingbats_character_sets-.htm
#define ENUMERATE_SYMBOL_CHARACTER_SET(V)                   \
    V(Alpha, 65)                                            \
    V(Beta, 66)                                             \
    V(Chi, 67)                                              \
    V(Delta, 68)                                            \
    V(Epsilon, 69)                                          \
    V(Eta, 72)                                              \
    V(Euro, 160)                                            \
    V(Gamma, 71)                                            \
    V(Ifraktur, 193)                                        \
    V(Iota, 73)                                             \
    V(Kappa, 75)                                            \
    V(Lambda, 76)                                           \
    V(Mu, 77)                                               \
    V(Nu, 78)                                               \
    V(Omega, 87)                                            \
    V(Omicron, 79)                                          \
    V(Phi, 70)                                              \
    V(Pi, 80)                                               \
    V(Psi, 89)                                              \
    V(Rfraktur, 194)                                        \
    V(Rho, 82)                                              \
    V(Sigma, 83)                                            \
    V(Tau, 84)                                              \
    V(Theta, 81)                                            \
    V(Upsilon, 85)                                          \
    V(Upsilon1, 161)                                        \
    V(Xi, 88)                                               \
    V(Zeta, 90)                                             \
    V(aleph, 192)                                           \
    V(alpha, 97)                                            \
    V(ampersand, 38)                                        \
    V(angle, 208)                                           \
    V(angleleft, 225)                                       \
    V(angleright, 241)                                      \
    V(approxequal, 187)                                     \
    V(arrowboth, 171)                                       \
    V(arrowdblboth, 219)                                    \
    V(arrowdbldown, 223)                                    \
    V(arrowdblleft, 220)                                    \
    V(arrowdblright, 222)                                   \
    V(arrowdblup, 221)                                      \
    V(arrowdown, 175)                                       \
    V(arrowhorizex, 190)                                    \
    V(arrowleft, 172)                                       \
    V(arrowright, 174)                                      \
    V(arrowup, 173)                                         \
    V(arrowvertex, 189)                                     \
    V(asteriskmath, 42)                                     \
    V(bar, 124)                                             \
    V(beta, 98)                                             \
    V(braceleft, 123)                                       \
    V(braceright, 125)                                      \
    V(bracelefttp, 236)                                     \
    V(braceleftmid, 237)                                    \
    V(braceleftbt, 238)                                     \
    V(bracerighttp, 252)                                    \
    V(bracerightmid, 253)                                   \
    V(bracerightbt, 254)                                    \
    V(braceex, 239)                                         \
    V(bracketleft, 91)                                      \
    V(bracketright, 93)                                     \
    V(bracketlefttp, 233)                                   \
    V(bracketleftex, 234)                                   \
    V(bracketleftbt, 235)                                   \
    V(bracketrighttp, 249)                                  \
    V(bracketrightex, 250)                                  \
    V(bracketrightbt, 251)                                  \
    V(bullet, 183)                                          \
    V(carriagereturn, 191)                                  \
    V(chi, 99)                                              \
    V(circlemultiply, 196)                                  \
    V(circleplus, 197)                                      \
    V(club, 167)                                            \
    V(colon, 58)                                            \
    V(comma, 44)                                            \
    V(congruent, 64)                                        \
    V(copyrightsans, 227)                                   \
    V(copyrightserif, 211)                                  \
    V(degree, 176)                                          \
    V(delta, 100)                                           \
    V(diamond, 168)                                         \
    V(divide, 184)                                          \
    V(dotmath, 215)                                         \
    V(eight, 56)                                            \
    V(element, 206)                                         \
    V(ellipsis, 188)                                        \
    V(emptyset, 198)                                        \
    V(epsilon, 101)                                         \
    V(equal, 61)                                            \
    V(equivalence, 186)                                     \
    V(eta, 104)                                             \
    V(exclam, 33)                                           \
    V(existential, 36)                                      \
    V(five, 53)                                             \
    V(florin, 166)                                          \
    V(four, 52)                                             \
    V(fraction, 164)                                        \
    V(gamma, 103)                                           \
    V(gradient, 209)                                        \
    V(greater, 62)                                          \
    V(greaterequal, 179)                                    \
    V(heart, 169)                                           \
    V(infinity, 165)                                        \
    V(integral, 242)                                        \
    V(integraltp, 243)                                      \
    V(integralex, 244)                                      \
    V(integralbt, 245)                                      \
    V(intersection, 199)                                    \
    V(iota, 105)                                            \
    V(kappa, 107)                                           \
    V(lambda, 108)                                          \
    V(less, 60)                                             \
    V(lessequal, 163)                                       \
    V(logicaland, 217)                                      \
    V(logicalnot, 216)                                      \
    V(logicalor, 218)                                       \
    V(lozenge, 224)                                         \
    V(minus, 45)                                            \
    V(minute, 162)                                          \
    V(mu, 109)                                              \
    V(multiply, 180)                                        \
    V(nine, 57)                                             \
    V(notelement, 207)                                      \
    V(notequal, 185)                                        \
    V(notsubset, 203)                                       \
    V(nu, 110)                                              \
    V(numbersign, 35)                                       \
    V(omega, 119)                                           \
    V(omega1, 118)                                          \
    V(omicron, 111)                                         \
    V(one, 49)                                              \
    V(parenleft, 40)                                        \
    V(parenright, 41)                                       \
    V(parenlefttp, 230)                                     \
    V(parenleftex, 231)                                     \
    V(parenleftbt, 232)                                     \
    V(parenrighttp, 246)                                    \
    V(parenrightex, 247)                                    \
    V(parenrightbt, 248)                                    \
    V(partialdiff, 182)                                     \
    V(percent, 37)                                          \
    V(period, 46)                                           \
    V(perpendicular, 94)                                    \
    V(phi, 102)                                             \
    V(phi1, 106)                                            \
    V(pi, 112)                                              \
    V(plus, 43)                                             \
    V(plusminus, 177)                                       \
    V(product, 213)                                         \
    V(propersubset, 204)                                    \
    V(propersuperset, 201)                                  \
    V(proportional, 181)                                    \
    V(psi, 121)                                             \
    V(question, 63)                                         \
    V(radical, 214)                                         \
    V(radicalex, 96) /* FIXME: What is this character, ? */ \
    V(reflexsubset, 205)                                    \
    V(reflexsuperset, 202)                                  \
    V(registersans, 226)                                    \
    V(registerserif, 210)                                   \
    V(rho, 114)                                             \
    V(second, 178)                                          \
    V(semicolon, 59)                                        \
    V(seven, 55)                                            \
    V(sigma, 115)                                           \
    V(sigma1, 86)                                           \
    V(similar, 126)                                         \
    V(six, 54)                                              \
    V(slash, 47)                                            \
    V(space, 32)                                            \
    V(spade, 170)                                           \
    V(suchthat, 39)                                         \
    V(summation, 229)                                       \
    V(tau, 116)                                             \
    V(therefore, 92)                                        \
    V(theta, 113)                                           \
    V(theta1, 74)                                           \
    V(three, 51)                                            \
    V(trademarksans, 228)                                   \
    V(trademarkserif, 212)                                  \
    V(two, 50)                                              \
    V(underscore, 95)                                       \
    V(union, 200)                                           \
    V(universal, 34)                                        \
    V(upsilon, 117)                                         \
    V(weierstrass, 195)                                     \
    V(xi, 120)                                              \
    V(zero, 48)                                             \
    V(zeta, 15)

#define ENUMERATE_ZAPF_DINGBATS_CHARACTER_SET(V) \
    V(space, 040)                                \
    V(a1, 041)                                   \
    V(a2, 042)                                   \
    V(a202, 043)                                 \
    V(a3, 044)                                   \
    V(a4, 045)                                   \
    V(a5, 046)                                   \
    V(a119, 047)                                 \
    V(a118, 050)                                 \
    V(a117, 051)                                 \
    V(a11, 052)                                  \
    V(a12, 053)                                  \
    V(a13, 054)                                  \
    V(a14, 055)                                  \
    V(a15, 056)                                  \
    V(a16, 057)                                  \
    V(a105, 060)                                 \
    V(a17, 061)                                  \
    V(a18, 062)                                  \
    V(a19, 063)                                  \
    V(a20, 064)                                  \
    V(a21, 065)                                  \
    V(a22, 066)                                  \
    V(a23, 067)                                  \
    V(a24, 070)                                  \
    V(a25, 071)                                  \
    V(a26, 072)                                  \
    V(a27, 073)                                  \
    V(a28, 074)                                  \
    V(a6, 075)                                   \
    V(a7, 076)                                   \
    V(a8, 077)                                   \
    V(a9, 100)                                   \
    V(a10, 101)                                  \
    V(a29, 102)                                  \
    V(a30, 103)                                  \
    V(a31, 104)                                  \
    V(a32, 105)                                  \
    V(a33, 106)                                  \
    V(a34, 107)                                  \
    V(a35, 110)                                  \
    V(a36, 111)                                  \
    V(a37, 112)                                  \
    V(a38, 113)                                  \
    V(a39, 114)                                  \
    V(a40, 115)                                  \
    V(a41, 116)                                  \
    V(a42, 117)                                  \
    V(a43, 120)                                  \
    V(a44, 121)                                  \
    V(a45, 122)                                  \
    V(a46, 123)                                  \
    V(a47, 124)                                  \
    V(a48, 125)                                  \
    V(a49, 126)                                  \
    V(a50, 127)                                  \
    V(a51, 130)                                  \
    V(a52, 131)                                  \
    V(a53, 132)                                  \
    V(a54, 133)                                  \
    V(a55, 134)                                  \
    V(a56, 135)                                  \
    V(a57, 136)                                  \
    V(a58, 137)                                  \
    V(a59, 140)                                  \
    V(a60, 141)                                  \
    V(a61, 142)                                  \
    V(a62, 143)                                  \
    V(a63, 144)                                  \
    V(a64, 145)                                  \
    V(a65, 146)                                  \
    V(a66, 147)                                  \
    V(a67, 150)                                  \
    V(a68, 151)                                  \
    V(a69, 152)                                  \
    V(a70, 153)                                  \
    V(a71, 154)                                  \
    V(a72, 155)                                  \
    V(a73, 156)                                  \
    V(a74, 157)                                  \
    V(a203, 160)                                 \
    V(a75, 161)                                  \
    V(a204, 162)                                 \
    V(a76, 163)                                  \
    V(a77, 164)                                  \
    V(a78, 165)                                  \
    V(a79, 166)                                  \
    V(a81, 167)                                  \
    V(a82, 170)                                  \
    V(a83, 171)                                  \
    V(a84, 172)                                  \
    V(a97, 173)                                  \
    V(a98, 174)                                  \
    V(a99, 175)                                  \
    V(a100, 176)                                 \
    V(a101, 241)                                 \
    V(a102, 242)                                 \
    V(a103, 243)                                 \
    V(a104, 244)                                 \
    V(a106, 245)                                 \
    V(a107, 246)                                 \
    V(a108, 247)                                 \
    V(a112, 250)                                 \
    V(a111, 251)                                 \
    V(a110, 252)                                 \
    V(a109, 253)                                 \
    V(a120, 254)                                 \
    V(a121, 255)                                 \
    V(a122, 256)                                 \
    V(a123, 257)                                 \
    V(a124, 260)                                 \
    V(a125, 261)                                 \
    V(a126, 262)                                 \
    V(a127, 263)                                 \
    V(a128, 264)                                 \
    V(a129, 265)                                 \
    V(a130, 266)                                 \
    V(a131, 267)                                 \
    V(a132, 270)                                 \
    V(a133, 271)                                 \
    V(a134, 272)                                 \
    V(a135, 273)                                 \
    V(a136, 274)                                 \
    V(a137, 275)                                 \
    V(a138, 276)                                 \
    V(a139, 277)                                 \
    V(a140, 300)                                 \
    V(a141, 301)                                 \
    V(a142, 302)                                 \
    V(a143, 303)                                 \
    V(a144, 304)                                 \
    V(a145, 305)                                 \
    V(a146, 306)                                 \
    V(a147, 307)                                 \
    V(a148, 310)                                 \
    V(a149, 311)                                 \
    V(a150, 312)                                 \
    V(a151, 313)                                 \
    V(a152, 314)                                 \
    V(a153, 315)                                 \
    V(a154, 316)                                 \
    V(a155, 317)                                 \
    V(a156, 320)                                 \
    V(a157, 321)                                 \
    V(a158, 322)                                 \
    V(a159, 323)                                 \
    V(a160, 324)                                 \
    V(a161, 325)                                 \
    V(a163, 326)                                 \
    V(a164, 327)                                 \
    V(a196, 330)                                 \
    V(a165, 331)                                 \
    V(a192, 332)                                 \
    V(a166, 333)                                 \
    V(a167, 334)                                 \
    V(a168, 335)                                 \
    V(a169, 336)                                 \
    V(a170, 337)                                 \
    V(a171, 340)                                 \
    V(a172, 341)                                 \
    V(a173, 342)                                 \
    V(a162, 343)                                 \
    V(a174, 344)                                 \
    V(a175, 345)                                 \
    V(a176, 346)                                 \
    V(a177, 347)                                 \
    V(a178, 350)                                 \
    V(a179, 351)                                 \
    V(a193, 352)                                 \
    V(a180, 353)                                 \
    V(a199, 354)                                 \
    V(a181, 355)                                 \
    V(a200, 356)                                 \
    V(a182, 357)                                 \
    V(a201, 361)                                 \
    V(a183, 362)                                 \
    V(a184, 363)                                 \
    V(a197, 364)                                 \
    V(a185, 365)                                 \
    V(a194, 366)                                 \
    V(a198, 367)                                 \
    V(a186, 370)                                 \
    V(a195, 371)                                 \
    V(a187, 372)                                 \
    V(a188, 373)                                 \
    V(a189, 374)                                 \
    V(a190, 375)                                 \
    V(a191, 376)

namespace PDF {

struct CharDescriptor {
    DeprecatedString name;
    u32 code_point;
};

class Encoding : public RefCounted<Encoding> {
public:
    static PDFErrorOr<NonnullRefPtr<Encoding>> create(HashMap<u16, CharDescriptor> descriptors);
    static PDFErrorOr<NonnullRefPtr<Encoding>> from_object(Document*, NonnullRefPtr<Object> const&);

    static NonnullRefPtr<Encoding> standard_encoding();
    static NonnullRefPtr<Encoding> mac_encoding();
    static NonnullRefPtr<Encoding> windows_encoding();
    static NonnullRefPtr<Encoding> pdf_doc_encoding();
    static NonnullRefPtr<Encoding> symbol_encoding();
    static NonnullRefPtr<Encoding> zapf_encoding();

    HashMap<u16, CharDescriptor> const& descriptors() const { return m_descriptors; }
    HashMap<DeprecatedString, u16> const& name_mapping() const { return m_name_mapping; }

    u16 get_char_code(DeprecatedString const&) const;
    CharDescriptor const& get_char_code_descriptor(u16 char_code) const;

    bool should_map_to_bullet(u16 char_code) const;

protected:
    HashMap<u16, CharDescriptor> m_descriptors;
    HashMap<DeprecatedString, u16> m_name_mapping;

    bool m_windows { false };
};

}
