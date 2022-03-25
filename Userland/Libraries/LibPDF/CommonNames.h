/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

#define ENUMERATE_COMMON_NAMES(A) \
    A(AIS)                        \
    A(Alternate)                  \
    A(ASCII85Decode)              \
    A(ASCIIHexDecode)             \
    A(BG)                         \
    A(BG2)                        \
    A(BM)                         \
    A(BaseEncoding)               \
    A(BaseFont)                   \
    A(BlackPoint)                 \
    A(C)                          \
    A(CA)                         \
    A(CCITTFaxDecode)             \
    A(CalRGB)                     \
    A(CIDSystemInfo)              \
    A(CIDToGIDMap)                \
    A(ColorSpace)                 \
    A(Contents)                   \
    A(Count)                      \
    A(CropBox)                    \
    A(Crypt)                      \
    A(D)                          \
    A(DW)                         \
    A(DCTDecode)                  \
    A(DescendantFonts)            \
    A(Dest)                       \
    A(Dests)                      \
    A(DeviceCMYK)                 \
    A(DeviceGray)                 \
    A(DeviceRGB)                  \
    A(Differences)                \
    A(E)                          \
    A(Encoding)                   \
    A(Encrypt)                    \
    A(EncryptMetadata)            \
    A(ExtGState)                  \
    A(F)                          \
    A(FL)                         \
    A(Filter)                     \
    A(First)                      \
    A(FirstChar)                  \
    A(Fit)                        \
    A(FitB)                       \
    A(FitBH)                      \
    A(FitBV)                      \
    A(FitH)                       \
    A(FitR)                       \
    A(FitV)                       \
    A(FlateDecode)                \
    A(Font)                       \
    A(FontDescriptor)             \
    A(FontFamily)                 \
    A(FontFile1)                  \
    A(FontFile2)                  \
    A(FontFile3)                  \
    A(Gamma)                      \
    A(H)                          \
    A(HT)                         \
    A(HTO)                        \
    A(ICCBased)                   \
    A(ID)                         \
    A(JBIG2Decode)                \
    A(JPXDecode)                  \
    A(Kids)                       \
    A(L)                          \
    A(LC)                         \
    A(LJ)                         \
    A(LW)                         \
    A(LZWDecode)                  \
    A(Last)                       \
    A(LastChar)                   \
    A(Length)                     \
    A(Linearized)                 \
    A(ML)                         \
    A(Matrix)                     \
    A(MediaBox)                   \
    A(MissingWidth)               \
    A(N)                          \
    A(Next)                       \
    A(O)                          \
    A(OP)                         \
    A(OPM)                        \
    A(Ordering)                   \
    A(Outlines)                   \
    A(P)                          \
    A(Pages)                      \
    A(Parent)                     \
    A(Pattern)                    \
    A(Prev)                       \
    A(R)                          \
    A(RI)                         \
    A(Registry)                   \
    A(Resources)                  \
    A(Root)                       \
    A(Rotate)                     \
    A(RunLengthDecode)            \
    A(SA)                         \
    A(SM)                         \
    A(SMask)                      \
    A(Subtype)                    \
    A(Supplement)                 \
    A(T)                          \
    A(TK)                         \
    A(TR)                         \
    A(TR2)                        \
    A(Title)                      \
    A(ToUnicode)                  \
    A(Type)                       \
    A(U)                          \
    A(UCR)                        \
    A(UseBlackPTComp)             \
    A(UserUnit)                   \
    A(W)                          \
    A(WhitePoint)                 \
    A(Widths)                     \
    A(XYZ)                        \
    A(ca)                         \
    A(op)

namespace PDF {

class CommonNames {
public:
#define ENUMERATE(name) static FlyString name;
    ENUMERATE_COMMON_NAMES(ENUMERATE)
#undef ENUMERATE

    static FlyString IdentityH;
};

}
