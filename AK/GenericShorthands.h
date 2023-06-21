/*
 * Copyright (c) 2022, Frhun <serenitystuff@frhun.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

namespace AK {

template<typename T, typename... Ts>
[[nodiscard]] constexpr bool first_is_one_of(T const to_compare, Ts const... valid_values)
{
    return (... || (to_compare == valid_values));
}

template<typename T, typename... Ts>
[[nodiscard]] constexpr bool first_is_smaller_than_one_of(T const to_compare, Ts const... valid_values)
{
    return (... || (to_compare < valid_values));
}

template<typename T, typename... Ts>
[[nodiscard]] constexpr bool first_is_smaller_or_equal_than_one_of(T const to_compare, Ts const... valid_values)
{
    return (... || (to_compare <= valid_values));
}

template<typename T, typename... Ts>
[[nodiscard]] constexpr bool first_is_larger_than_one_of(T const to_compare, Ts const... valid_values)
{
    return (... || (to_compare > valid_values));
}

template<typename T, typename... Ts>
[[nodiscard]] constexpr bool first_is_larger_or_equal_than_one_of(T const to_compare, Ts const... valid_values)
{
    return (... || (to_compare >= valid_values));
}

template<typename T, typename... Ts>
[[nodiscard]] constexpr bool first_is_smaller_than_all_of(T const to_compare, Ts const... valid_values)
{
    return (... && (to_compare < valid_values));
}

template<typename T, typename... Ts>
[[nodiscard]] constexpr bool first_is_smaller_or_equal_than_all_of(T const to_compare, Ts const... valid_values)
{
    return (... && (to_compare <= valid_values));
}

template<typename T, typename... Ts>
[[nodiscard]] constexpr bool first_is_larger_than_all_of(T const to_compare, Ts const... valid_values)
{
    return (... && (to_compare > valid_values));
}

template<typename T, typename... Ts>
[[nodiscard]] constexpr bool first_is_larger_or_equal_than_all_of(T const to_compare, Ts const... valid_values)
{
    return (... && (to_compare >= valid_values));
}
}

#if USING_AK_GLOBALLY
using AK::first_is_larger_or_equal_than_all_of;
using AK::first_is_larger_or_equal_than_one_of;
using AK::first_is_larger_than_all_of;
using AK::first_is_larger_than_one_of;
using AK::first_is_one_of;
using AK::first_is_smaller_or_equal_than_all_of;
using AK::first_is_smaller_or_equal_than_one_of;
using AK::first_is_smaller_than_all_of;
using AK::first_is_smaller_than_one_of;
#endif
