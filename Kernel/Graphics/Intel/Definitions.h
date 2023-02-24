/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

namespace Kernel::IntelGraphics {

enum class Generation {
    Gen4,
    Gen9,
};

struct PLLSettings;

struct PLLParameterLimit {
    size_t min, max;
};

struct PLLMaxSettings {
    PLLParameterLimit dot_clock, vco, n, m, m1, m2, p, p1, p2;
};

struct PLLSettings {
    bool is_valid() const { return (n != 0 && m1 != 0 && m2 != 0 && p1 != 0 && p2 != 0); }
    u64 compute_dot_clock(u64 refclock) const
    {
        return (refclock * (5 * m1 + m2) / n) / (p1 * p2);
    }

    u64 compute_vco(u64 refclock) const
    {
        return refclock * (5 * m1 + m2) / n;
    }

    u64 compute_m() const
    {
        return 5 * m1 + m2;
    }

    u64 compute_p() const
    {
        return p1 * p2;
    }
    u64 n { 0 };
    u64 m1 { 0 };
    u64 m2 { 0 };
    u64 p1 { 0 };
    u64 p2 { 0 };
};
}
