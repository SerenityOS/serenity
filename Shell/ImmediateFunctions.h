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

#pragma once

#include "Forward.h"
#include <AK/Array.h>
#include <AK/Function.h>
#include <AK/StringView.h>

namespace Shell {

using ImmediateFunctionType = Function<void(Shell&, NonnullRefPtrVector<AST::Node>, Function<IterationDecision(NonnullRefPtr<AST::Node>)>)>;

void ensure_immediate_functions();

#define ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS()           \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(count)         \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(length)        \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(nth)           \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(substring)     \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(slice)         \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(remove_suffix) \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(remove_prefix) \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(regex_replace) \
    __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(filter_glob)

enum ImmediateFunction : u32 {
#define __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(name) \
    name,

    ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS()

#undef __ENUMERATE_SHELL_IMMEDIATE_FUNCTION
        _Count,
    Invalid,
};

extern Array<ImmediateFunctionType, ImmediateFunction::_Count> s_immediate_functions;

inline auto immediate_functions() -> decltype(s_immediate_functions)&
{
    ensure_immediate_functions();
    return s_immediate_functions;
}

inline ImmediateFunctionType* immediate_function(ImmediateFunction fn)
{
    ensure_immediate_functions();
    if ((u32)fn >= ImmediateFunction::_Count)
        return nullptr;
    return &s_immediate_functions[(u32)fn];
}

inline ImmediateFunction immediate_function_by_name(const StringView& fn_name)
{
    ensure_immediate_functions();
#define __ENUMERATE_SHELL_IMMEDIATE_FUNCTION(name) \
    if (fn_name == #name)                          \
        return ImmediateFunction::name;

    ENUMERATE_SHELL_IMMEDIATE_FUNCTIONS()

#undef __ENUMERATE_SHELL_IMMEDIATE_FUNCTION

    return ImmediateFunction::Invalid;
}

}
