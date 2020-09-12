/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Format.h"
#include <AK/PrintfImplementation.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

namespace Spreadsheet {

template<typename T, typename V>
struct SingleEntryListNext {
    ALWAYS_INLINE T operator()(V value) const
    {
        return (T)value;
    }
};

template<typename PutChFunc, typename ArgumentListRefT, template<typename T, typename U = ArgumentListRefT> typename NextArgument>
struct PrintfImpl : public PrintfImplementation::PrintfImpl<PutChFunc, ArgumentListRefT, NextArgument> {
    ALWAYS_INLINE PrintfImpl(PutChFunc& putch, char*& bufptr, const int& nwritten)
        : PrintfImplementation::PrintfImpl<PutChFunc, ArgumentListRefT, NextArgument>(putch, bufptr, nwritten)
    {
    }

    // Disallow pointer formats.
    ALWAYS_INLINE int format_n(const PrintfImplementation::ModifierState&, ArgumentListRefT&) const
    {
        return 0;
    }
    ALWAYS_INLINE int format_s(const PrintfImplementation::ModifierState&, ArgumentListRefT&) const
    {
        return 0;
    }
};

String format_double(const char* format, double value)
{
    StringBuilder builder;
    auto putch = [&](auto, auto ch) { builder.append(ch); };
    printf_internal<decltype(putch), PrintfImpl, double, SingleEntryListNext>(putch, nullptr, format, value);

    return builder.build();
}

}
