/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Format.h"
#include <AK/ByteString.h>
#include <AK/PrintfImplementation.h>
#include <AK/StringBuilder.h>

namespace Spreadsheet {

template<typename T, typename V>
struct SingleEntryListNext {
    ALWAYS_INLINE T operator()(V value) const
    {
        return (T)value;
    }
};

template<typename PutChFunc, typename ArgumentListRefT, template<typename T, typename U = ArgumentListRefT> typename NextArgument, typename CharType>
struct PrintfImpl : public PrintfImplementation::PrintfImpl<PutChFunc, ArgumentListRefT, NextArgument, CharType> {
    ALWAYS_INLINE PrintfImpl(PutChFunc& putch, char*& bufptr, int const& nwritten)
        : PrintfImplementation::PrintfImpl<PutChFunc, ArgumentListRefT, NextArgument>(putch, bufptr, nwritten)
    {
    }

    // Disallow pointer formats.
    ALWAYS_INLINE int format_n(PrintfImplementation::ModifierState const&, ArgumentListRefT&) const
    {
        return 0;
    }
    ALWAYS_INLINE int format_s(PrintfImplementation::ModifierState const&, ArgumentListRefT&) const
    {
        return 0;
    }
};

ByteString format_double(char const* format, double value)
{
    StringBuilder builder;
    auto putch = [&](auto, auto ch) { builder.append(ch); };
    printf_internal<decltype(putch), PrintfImpl, double, SingleEntryListNext>(putch, nullptr, format, value);

    return builder.to_byte_string();
}

}
