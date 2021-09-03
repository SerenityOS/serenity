/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/String.h>
#include <YAK/StringView.h>

#ifndef BUILDING_SERENITY_TOOLCHAIN
#    include <cxxabi.h>
#endif

namespace YAK {

inline String demangle(const StringView& name)
{
#ifdef BUILDING_SERENITY_TOOLCHAIN
    return name;
#else
    int status = 0;
    auto* demangled_name = abi::__cxa_demangle(name.to_string().characters(), nullptr, nullptr, &status);
    auto string = String(status == 0 ? demangled_name : name);
    if (status == 0)
        kfree(demangled_name);
    return string;
#endif
}

}

using YAK::demangle;
