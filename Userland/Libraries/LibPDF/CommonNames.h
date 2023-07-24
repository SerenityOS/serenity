/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>

#define ENUMERATE_COMMON_NAMES(A) \
    A(AIS)                        \
    A(Alternate)                  \
    A(ASCII85Decode)              \
    A(ASCIIHexDecode)             \
    A(Author)                     \
    A(BG)                         \
    A(BG2)                        \
    A(BM)                         \
    A(BaseEncoding)               \
    A(BaseFont)                   \
    A(BitsPerComponent)           \
    A(BlackPoint)                 \
    A(C)                          \
    A(CA)                         \
    A(CCITTFaxDecode)             \
    A(CF)                         \
    A(CFM)                        \
    A(CalRGB)                     \
    A(CIDFontType0)               \
    A(CIDFontType2)               \
    A(CIDSystemInfo)              \
    A(CIDToGIDMap)                \
    A(Colors)                     \
    A(ColorSpace)                 \
    A(Columns)                    \
    A(Contents)                   \
    A(Count)                      \
    A(CreationDate)               \
    A(Creator)                    \
    A(CropBox)                    \
    A(Crypt)                      \
    A(D)                          \
    A(DW)                         \
    A(DCTDecode)                  \
    A(DecodeParms)                \
    A(Decode)                     \
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
    A(FontFile)                   \
    A(FontFile2)                  \
    A(FontFile3)                  \
    A(Gamma)                      \
    A(H)                          \
    A(Height)                     \
    A(HT)                         \
    A(HTO)                        \
    A(ICCBased)                   \
    A(ID)                         \
    A(Image)                      \
    A(ImageMask)                  \
    A(Index)                      \
    A(Indexed)                    \
    A(Info)                       \
    A(JBIG2Decode)                \
    A(JPXDecode)                  \
    A(Keywords)                   \
    A(Kids)                       \
    A(L)                          \
    A(LC)                         \
    A(LJ)                         \
    A(LW)                         \
    A(LZWDecode)                  \
    A(Last)                       \
    A(LastChar)                   \
    A(Length)                     \
    A(Length1)                    \
    A(Length2)                    \
    A(Length3)                    \
    A(Limits)                     \
    A(Linearized)                 \
    A(ML)                         \
    A(Matrix)                     \
    A(MediaBox)                   \
    A(MissingWidth)               \
    A(ModDate)                    \
    A(N)                          \
    A(Names)                      \
    A(Next)                       \
    A(O)                          \
    A(OE)                         \
    A(OP)                         \
    A(OPM)                        \
    A(Ordering)                   \
    A(Outlines)                   \
    A(P)                          \
    A(Pages)                      \
    A(Parent)                     \
    A(Pattern)                    \
    A(Perms)                      \
    A(Predictor)                  \
    A(Prev)                       \
    A(Producer)                   \
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
    A(Separation)                 \
    A(StmF)                       \
    A(StrF)                       \
    A(Subject)                    \
    A(Subtype)                    \
    A(Supplement)                 \
    A(T)                          \
    A(TK)                         \
    A(TR)                         \
    A(TR2)                        \
    A(Title)                      \
    A(ToUnicode)                  \
    A(Type)                       \
    A(Type1C)                     \
    A(U)                          \
    A(UE)                         \
    A(UCR)                        \
    A(UseBlackPTComp)             \
    A(UserUnit)                   \
    A(V)                          \
    A(W)                          \
    A(WhitePoint)                 \
    A(Width)                      \
    A(Widths)                     \
    A(XObject)                    \
    A(XYZ)                        \
    A(ca)                         \
    A(op)

namespace PDF {

class CommonNames {
public:
#define ENUMERATE(name) static DeprecatedFlyString name;
    ENUMERATE_COMMON_NAMES(ENUMERATE)
#undef ENUMERATE

    static DeprecatedFlyString IdentityH;
};

}
