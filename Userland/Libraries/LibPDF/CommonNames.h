/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

#define ENUMERATE_COMMON_NAMES(V) \
    V(AIS)                        \
    V(ASCII85Decode)              \
    V(ASCIIHexDecode)             \
    V(BG)                         \
    V(BG2)                        \
    V(BM)                         \
    V(BaseFont)                   \
    V(BlackPoint)                 \
    V(C)                          \
    V(CA)                         \
    V(CCITTFaxDecode)             \
    V(CalRGB)                     \
    V(ColorSpace)                 \
    V(Contents)                   \
    V(Count)                      \
    V(CropBox)                    \
    V(Crypt)                      \
    V(D)                          \
    V(DCTDecode)                  \
    V(Dest)                       \
    V(DeviceCMYK)                 \
    V(DeviceGray)                 \
    V(DeviceRGB)                  \
    V(E)                          \
    V(ExtGState)                  \
    V(F)                          \
    V(FL)                         \
    V(Filter)                     \
    V(First)                      \
    V(Fit)                        \
    V(FitB)                       \
    V(FitBH)                      \
    V(FitBV)                      \
    V(FitH)                       \
    V(FitR)                       \
    V(FitV)                       \
    V(FlateDecode)                \
    V(Font)                       \
    V(Gamma)                      \
    V(H)                          \
    V(HT)                         \
    V(HTO)                        \
    V(JBIG2Decode)                \
    V(JPXDecode)                  \
    V(Kids)                       \
    V(L)                          \
    V(LC)                         \
    V(LJ)                         \
    V(LW)                         \
    V(LZWDecode)                  \
    V(Last)                       \
    V(Length)                     \
    V(Linearized)                 \
    V(ML)                         \
    V(Matrix)                     \
    V(MediaBox)                   \
    V(N)                          \
    V(Next)                       \
    V(O)                          \
    V(OP)                         \
    V(OPM)                        \
    V(Outlines)                   \
    V(P)                          \
    V(Pages)                      \
    V(Parent)                     \
    V(Pattern)                    \
    V(Prev)                       \
    V(RI)                         \
    V(Resources)                  \
    V(Root)                       \
    V(Rotate)                     \
    V(RunLengthDecode)            \
    V(SA)                         \
    V(SM)                         \
    V(SMask)                      \
    V(T)                          \
    V(TK)                         \
    V(TR)                         \
    V(TR2)                        \
    V(Title)                      \
    V(Type)                       \
    V(UCR)                        \
    V(UseBlackPTComp)             \
    V(UserUnit)                   \
    V(WhitePoint)                 \
    V(XYZ)                        \
    V(ca)                         \
    V(op)

namespace PDF {

class CommonNames {
public:
#define ENUMERATE(name) static FlyString name;
    ENUMERATE_COMMON_NAMES(ENUMERATE)
#undef ENUMERATE
};

}
