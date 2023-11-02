/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#ifndef KERNEL

#    include <AK/DeprecatedString.h>
#    include <AK/StringView.h>
#    if !defined(AK_OS_WINDOWS)
#        include <cxxabi.h>
#    else
#        include <windows.h>
// NOTE: Windows must go first
#        include <dbghelp.h>
#    endif

namespace AK {

inline DeprecatedString demangle(StringView name)
{
#    if !defined(AK_OS_WINDOWS)
    int status = 0;
    auto* demangled_name = abi::__cxa_demangle(name.to_deprecated_string().characters(), nullptr, nullptr, &status);
    auto string = DeprecatedString(status == 0 ? StringView { demangled_name, strlen(demangled_name) } : name);
    if (status == 0)
        free(demangled_name);
    return string;
#    else
    char* realname = (char*)malloc(1024 * sizeof(char));
    realname ? realname[0] = 0, ::UnDecorateSymbolName(name.to_deprecated_string().characters(), realname, 1024, 0) : 0;
    DeprecatedString string = DeprecatedString(realname);
    free(realname);
    return string;
#    endif
}

}

#    if USING_AK_GLOBALLY
using AK::demangle;
#    endif

#endif
