/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/Assertions.h>
#include <YAK/Platform.h>
#include <YAK/StdLibExtras.h>

namespace YAK {

template<typename OutputType, typename InputType>
ALWAYS_INLINE bool is(InputType& input)
{
    if constexpr (requires { input.template fast_is<OutputType>(); }) {
        return input.template fast_is<OutputType>();
    }
    return dynamic_cast<CopyConst<InputType, OutputType>*>(&input);
}

template<typename OutputType, typename InputType>
ALWAYS_INLINE bool is(InputType* input)
{
    return input && is<OutputType>(*input);
}

template<typename OutputType, typename InputType>
ALWAYS_INLINE CopyConst<InputType, OutputType>* verify_cast(InputType* input)
{
    static_assert(IsBaseOf<InputType, OutputType>);
    VERIFY(!input || is<OutputType>(*input));
    return static_cast<CopyConst<InputType, OutputType>*>(input);
}

template<typename OutputType, typename InputType>
ALWAYS_INLINE CopyConst<InputType, OutputType>& verify_cast(InputType& input)
{
    static_assert(IsBaseOf<InputType, OutputType>);
    VERIFY(is<OutputType>(input));
    return static_cast<CopyConst<InputType, OutputType>&>(input);
}

}

using YAK::is;
using YAK::verify_cast;
