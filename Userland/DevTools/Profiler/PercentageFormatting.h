/*
 * Copyright (c) 2023, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/IntegralMath.h>
#include <AK/Math.h>

namespace Profiler {

// Number of digits after the decimal point for sample percentages.
static constexpr int const number_of_percent_digits = 2;
static constexpr int const percent_digits_rounding = AK::pow(10, number_of_percent_digits);

ByteString format_percentage(auto value, auto total)
{
    auto percentage_full_precision = round_to<u64>(value * 100.f / total * percent_digits_rounding);
    return ByteString::formatted(
        "{}.{:02}",
        percentage_full_precision / percent_digits_rounding,
        percentage_full_precision % percent_digits_rounding);
}

}
