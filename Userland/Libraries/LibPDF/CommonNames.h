/*
 * Copyright (c) 2021-2022, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>

#define ENUMERATE_COMMON_NAMES(X) \
    X(A)                          \
    X(AIS)                        \
    X(Alternate)                  \
    X(ASCII85Decode)              \
    X(ASCIIHexDecode)             \
    X(Author)                     \
    X(BG)                         \
    X(BG2)                        \
    X(BM)                         \
    X(BaseEncoding)               \
    X(BaseFont)                   \
    X(BitsPerComponent)           \
    X(BlackPoint)                 \
    X(C)                          \
    X(CA)                         \
    X(CCITTFaxDecode)             \
    X(CF)                         \
    X(CFM)                        \
    X(CalRGB)                     \
    X(CIDFontType0)               \
    X(CIDFontType2)               \
    X(CIDSystemInfo)              \
    X(CIDToGIDMap)                \
    X(Colors)                     \
    X(ColorSpace)                 \
    X(Columns)                    \
    X(Contents)                   \
    X(Count)                      \
    X(CreationDate)               \
    X(Creator)                    \
    X(CropBox)                    \
    X(Crypt)                      \
    X(D)                          \
    X(DW)                         \
    X(DCTDecode)                  \
    X(DecodeParms)                \
    X(Decode)                     \
    X(DescendantFonts)            \
    X(Dest)                       \
    X(Dests)                      \
    X(DeviceCMYK)                 \
    X(DeviceGray)                 \
    X(DeviceRGB)                  \
    X(Differences)                \
    X(E)                          \
    X(Encoding)                   \
    X(Encrypt)                    \
    X(EncryptMetadata)            \
    X(ExtGState)                  \
    X(F)                          \
    X(FL)                         \
    X(Filter)                     \
    X(First)                      \
    X(FirstChar)                  \
    X(Fit)                        \
    X(FitB)                       \
    X(FitBH)                      \
    X(FitBV)                      \
    X(FitH)                       \
    X(FitR)                       \
    X(FitV)                       \
    X(FlateDecode)                \
    X(Font)                       \
    X(FontDescriptor)             \
    X(FontFamily)                 \
    X(FontFile)                   \
    X(FontFile2)                  \
    X(FontFile3)                  \
    X(Gamma)                      \
    X(H)                          \
    X(Height)                     \
    X(HT)                         \
    X(HTO)                        \
    X(ICCBased)                   \
    X(ID)                         \
    X(Image)                      \
    X(ImageMask)                  \
    X(Index)                      \
    X(Indexed)                    \
    X(Info)                       \
    X(JBIG2Decode)                \
    X(JPXDecode)                  \
    X(Keywords)                   \
    X(Kids)                       \
    X(L)                          \
    X(LC)                         \
    X(LJ)                         \
    X(LW)                         \
    X(LZWDecode)                  \
    X(Last)                       \
    X(LastChar)                   \
    X(Length)                     \
    X(Length1)                    \
    X(Length2)                    \
    X(Length3)                    \
    X(Limits)                     \
    X(Linearized)                 \
    X(ML)                         \
    X(Matrix)                     \
    X(MediaBox)                   \
    X(MissingWidth)               \
    X(ModDate)                    \
    X(N)                          \
    X(Names)                      \
    X(Next)                       \
    X(O)                          \
    X(OE)                         \
    X(OP)                         \
    X(OPM)                        \
    X(Ordering)                   \
    X(Outlines)                   \
    X(P)                          \
    X(Pages)                      \
    X(Parent)                     \
    X(Pattern)                    \
    X(Perms)                      \
    X(Predictor)                  \
    X(Prev)                       \
    X(Producer)                   \
    X(R)                          \
    X(RI)                         \
    X(Registry)                   \
    X(Resources)                  \
    X(Root)                       \
    X(Rotate)                     \
    X(RunLengthDecode)            \
    X(S)                          \
    X(SA)                         \
    X(SM)                         \
    X(SMask)                      \
    X(Separation)                 \
    X(StmF)                       \
    X(StrF)                       \
    X(Subject)                    \
    X(Subtype)                    \
    X(Supplement)                 \
    X(T)                          \
    X(TK)                         \
    X(TR)                         \
    X(TR2)                        \
    X(Title)                      \
    X(ToUnicode)                  \
    X(Type)                       \
    X(Type1C)                     \
    X(U)                          \
    X(UE)                         \
    X(UCR)                        \
    X(UseBlackPTComp)             \
    X(UserUnit)                   \
    X(V)                          \
    X(W)                          \
    X(WhitePoint)                 \
    X(Width)                      \
    X(Widths)                     \
    X(XObject)                    \
    X(XYZ)                        \
    X(ca)                         \
    X(op)

namespace PDF {

class CommonNames {
public:
#define ENUMERATE(name) static DeprecatedFlyString name;
    ENUMERATE_COMMON_NAMES(ENUMERATE)
#undef ENUMERATE

    static DeprecatedFlyString IdentityH;
};

}
