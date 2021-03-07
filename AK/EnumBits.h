/*
 * Copyright (c) 2021, Brian Gianforcaro <b.gianfo@gmail.com>
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

#include "AK/StdLibExtras.h"

// Enables bitwise operators for the specified Enum type.
//
#define AK_ENUM_BITWISE_OPERATORS(Enum) \
    _AK_ENUM_BITWISE_OPERATORS_INTERNAL(Enum, )

// Enables bitwise operators for the specified Enum type, this
// version is meant for use on enums which are private to the
// containing type.
//
#define AK_ENUM_BITWISE_FRIEND_OPERATORS(Enum) \
    _AK_ENUM_BITWISE_OPERATORS_INTERNAL(Enum, friend)

#define _AK_ENUM_BITWISE_OPERATORS_INTERNAL(Enum, Prefix)                    \
                                                                             \
    [[nodiscard]] Prefix constexpr inline Enum operator|(Enum lhs, Enum rhs) \
    {                                                                        \
        using Type = UnderlyingType<Enum>::Type;                             \
        return static_cast<Enum>(                                            \
            static_cast<Type>(lhs) | static_cast<Type>(rhs));                \
    }                                                                        \
                                                                             \
    [[nodiscard]] Prefix constexpr inline Enum operator&(Enum lhs, Enum rhs) \
    {                                                                        \
        using Type = UnderlyingType<Enum>::Type;                             \
        return static_cast<Enum>(                                            \
            static_cast<Type>(lhs) & static_cast<Type>(rhs));                \
    }                                                                        \
                                                                             \
    [[nodiscard]] Prefix constexpr inline Enum operator^(Enum lhs, Enum rhs) \
    {                                                                        \
        using Type = UnderlyingType<Enum>::Type;                             \
        return static_cast<Enum>(                                            \
            static_cast<Type>(lhs) ^ static_cast<Type>(rhs));                \
    }                                                                        \
                                                                             \
    [[nodiscard]] Prefix constexpr inline Enum operator~(Enum rhs)           \
    {                                                                        \
        using Type = UnderlyingType<Enum>::Type;                             \
        return static_cast<Enum>(                                            \
            ~static_cast<Type>(rhs));                                        \
    }                                                                        \
                                                                             \
    Prefix constexpr inline Enum& operator|=(Enum& lhs, Enum rhs)            \
    {                                                                        \
        using Type = UnderlyingType<Enum>::Type;                             \
        lhs = static_cast<Enum>(                                             \
            static_cast<Type>(lhs) | static_cast<Type>(rhs));                \
        return lhs;                                                          \
    }                                                                        \
                                                                             \
    Prefix constexpr inline Enum& operator&=(Enum& lhs, Enum rhs)            \
    {                                                                        \
        using Type = UnderlyingType<Enum>::Type;                             \
        lhs = static_cast<Enum>(                                             \
            static_cast<Type>(lhs) & static_cast<Type>(rhs));                \
        return lhs;                                                          \
    }                                                                        \
                                                                             \
    Prefix constexpr inline Enum& operator^=(Enum& lhs, Enum rhs)            \
    {                                                                        \
        using Type = UnderlyingType<Enum>::Type;                             \
        lhs = static_cast<Enum>(                                             \
            static_cast<Type>(lhs) ^ static_cast<Type>(rhs));                \
        return lhs;                                                          \
    }                                                                        \
                                                                             \
    Prefix constexpr inline bool has_flag(Enum value, Enum mask)             \
    {                                                                        \
        using Type = UnderlyingType<Enum>::Type;                             \
        return static_cast<Type>(value & mask) != 0;                         \
    }
