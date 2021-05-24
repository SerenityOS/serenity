/*
 * Copyright (c) 2021, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>

#define ENUMERATE_COMMON_NAMES(V) \
    V(ASCII85Decode)              \
    V(ASCIIHexDecode)             \
    V(BaseFont)                   \
    V(C)                          \
    V(CCITTFaxDecode)             \
    V(Contents)                   \
    V(Count)                      \
    V(CropBox)                    \
    V(Crypt)                      \
    V(DCTDecode)                  \
    V(Dest)                       \
    V(F)                          \
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
    V(JBIG2Decode)                \
    V(JPXDecode)                  \
    V(Kids)                       \
    V(LZWDecode)                  \
    V(Last)                       \
    V(Length)                     \
    V(MediaBox)                   \
    V(Next)                       \
    V(Outlines)                   \
    V(Pages)                      \
    V(Parent)                     \
    V(Resources)                  \
    V(Root)                       \
    V(Rotate)                     \
    V(RunLengthDecode)            \
    V(Title)                      \
    V(Type)                       \
    V(UserUnit)                   \
    V(XYZ)

namespace PDF {

class CommonNames {
public:
#define ENUMERATE(name) static FlyString name;
    ENUMERATE_COMMON_NAMES(ENUMERATE)
#undef ENUMERATE
};

}
