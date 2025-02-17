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
    X(ASCII85Decode)              \
    X(ASCIIHexDecode)             \
    X(Alternate)                  \
    X(AntiAlias)                  \
    X(Author)                     \
    X(BBox)                       \
    X(BG)                         \
    X(BG2)                        \
    X(BM)                         \
    X(Background)                 \
    X(BaseEncoding)               \
    X(BaseFont)                   \
    X(BitsPerComponent)           \
    X(BitsPerSample)              \
    X(BlackIs1)                   \
    X(BlackPoint)                 \
    X(Bounds)                     \
    X(C)                          \
    X(C0)                         \
    X(C1)                         \
    X(CA)                         \
    X(CCITTFaxDecode)             \
    X(CF)                         \
    X(CFM)                        \
    X(CIDFontType0)               \
    X(CIDFontType0C)              \
    X(CIDFontType2)               \
    X(CIDSystemInfo)              \
    X(CIDToGIDMap)                \
    X(CalGray)                    \
    X(CalRGB)                     \
    X(CharProcs)                  \
    X(ColorSpace)                 \
    X(Colors)                     \
    X(Columns)                    \
    X(Contents)                   \
    X(Coords)                     \
    X(Count)                      \
    X(CreationDate)               \
    X(Creator)                    \
    X(CropBox)                    \
    X(Crypt)                      \
    X(D)                          \
    X(DCTDecode)                  \
    X(DW)                         \
    X(DW2)                        \
    X(DamagedRowsBeforeError)     \
    X(Decode)                     \
    X(DecodeParms)                \
    X(DescendantFonts)            \
    X(Dest)                       \
    X(Dests)                      \
    X(DeviceCMYK)                 \
    X(DeviceGray)                 \
    X(DeviceN)                    \
    X(DeviceRGB)                  \
    X(Differences)                \
    X(Domain)                     \
    X(E)                          \
    X(EarlyChange)                \
    X(Encode)                     \
    X(EncodedByteAlign)           \
    X(Encoding)                   \
    X(Encrypt)                    \
    X(EncryptMetadata)            \
    X(EndOfBlock)                 \
    X(EndOfLine)                  \
    X(ExtGState)                  \
    X(Extend)                     \
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
    X(Flags)                      \
    X(FlateDecode)                \
    X(Font)                       \
    X(FontDescriptor)             \
    X(FontFamily)                 \
    X(FontFile)                   \
    X(FontFile2)                  \
    X(FontFile3)                  \
    X(FontMatrix)                 \
    X(FunctionType)               \
    X(Function)                   \
    X(Functions)                  \
    X(Gamma)                      \
    X(H)                          \
    X(HT)                         \
    X(HTO)                        \
    X(Height)                     \
    X(ICCBased)                   \
    X(ID)                         \
    X(Image)                      \
    X(ImageMask)                  \
    X(Index)                      \
    X(Indexed)                    \
    X(Info)                       \
    X(Intent)                     \
    X(JBIG2Decode)                \
    X(JBIG2Globals)               \
    X(JPXDecode)                  \
    X(K)                          \
    X(Keywords)                   \
    X(Kids)                       \
    X(L)                          \
    X(LC)                         \
    X(LJ)                         \
    X(LW)                         \
    X(LZWDecode)                  \
    X(Lab)                        \
    X(Last)                       \
    X(LastChar)                   \
    X(Length)                     \
    X(Length1)                    \
    X(Length2)                    \
    X(Length3)                    \
    X(Limits)                     \
    X(Linearized)                 \
    X(ML)                         \
    X(Mask)                       \
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
    X(Order)                      \
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
    X(Range)                      \
    X(Registry)                   \
    X(Resources)                  \
    X(Root)                       \
    X(Rows)                       \
    X(Rotate)                     \
    X(RunLengthDecode)            \
    X(S)                          \
    X(SA)                         \
    X(SM)                         \
    X(SMask)                      \
    X(SMaskInData)                \
    X(Separation)                 \
    X(Shading)                    \
    X(ShadingType)                \
    X(Size)                       \
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
    X(UCR)                        \
    X(UE)                         \
    X(UseBlackPTComp)             \
    X(UserUnit)                   \
    X(V)                          \
    X(W)                          \
    X(W2)                         \
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

    // Special cases where the string value isn't identical to the name.
    static DeprecatedFlyString IdentityH;
    static DeprecatedFlyString IdentityV;
};

}
