/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifndef KERNEL

#    include <AK/ByteString.h>
#    include <AK/StringView.h>
#    include <cxxabi.h>

namespace AK {

inline ByteString demangle(StringView name)
{
    int status = 0;
    auto* demangled_name = abi::__cxa_demangle(name.to_byte_string().characters(), nullptr, nullptr, &status);
    auto string = ByteString(status == 0 ? StringView { demangled_name, strlen(demangled_name) } : name);
    if (status == 0)
        free(demangled_name);
    return string;
}

}

#    if USING_AK_GLOBALLY
using AK::demangle;
#    endif

#endif
