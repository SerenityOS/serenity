/*
 * Copyright (c) 2021-2026, Leon Albrecht <leon2002.la@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Concepts.h>
#include <AK/FloatingPoint.h>

namespace AK {

template<FloatingPoint FloatT>
FloatT copysign(FloatT x, FloatT y)
{
    using Extractor = FloatExtractor<FloatT>;
    auto ex = Extractor::from_float(x);
    auto ey = Extractor::from_float(y);
    ex.sign = ey.sign;
    return ex.to_float();
}

}
