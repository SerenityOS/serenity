/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in input and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of input code must retain the above copyright notice, this
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

#include <AK/StdLibExtras.h>

namespace AK {

template<typename OutputType, typename InputType, bool is_base_type = IsBaseOf<OutputType, InputType>::value>
struct TypeTraits {
    static bool has_type(InputType&)
    {
        static_assert(IsVoid<OutputType>::value, "No TypeTraits for this type");
        return false;
    }
};

template<typename OutputType, typename InputType>
struct TypeTraits<OutputType, InputType, true> {
    static bool has_type(InputType&) { return true; }
};

template<typename OutputType, typename InputType>
inline bool is(InputType* input)
{
    return input && TypeTraits<const OutputType, const InputType>::has_type(*input);
}

template<typename OutputType, typename InputType>
inline bool is(InputType& input)
{
    return TypeTraits<const OutputType, const InputType>::has_type(input);
}

template<typename OutputType, typename InputType>
inline CopyConst<InputType, OutputType>* downcast(InputType* input)
{
    static_assert(IsBaseOf<InputType, OutputType>::value);
    ASSERT(!input || is<OutputType>(*input));
    return static_cast<CopyConst<InputType, OutputType>*>(input);
}

template<typename OutputType, typename InputType>
inline CopyConst<InputType, OutputType>& downcast(InputType& input)
{
    static_assert(IsBaseOf<InputType, OutputType>::value);
    ASSERT(is<OutputType>(input));
    return static_cast<CopyConst<InputType, OutputType>&>(input);
}

#define AK_BEGIN_TYPE_TRAITS(ClassName)                                   \
    namespace AK {                                                        \
    template<typename InputType>                                          \
    class TypeTraits<const ClassName, InputType, false> {                 \
    public:                                                               \
        static bool has_type(InputType& input) { return is_type(input); } \
                                                                          \
    private:

#define AK_END_TYPE_TRAITS() \
    }                        \
    ;                        \
    }

}

using AK::downcast;
using AK::is;
using AK::TypeTraits;
