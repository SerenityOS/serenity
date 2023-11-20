/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

namespace Gfx {

struct FourCC {
    constexpr FourCC(char const* name)
    {
        cc[0] = name[0];
        cc[1] = name[1];
        cc[2] = name[2];
        cc[3] = name[3];
    }

    bool operator==(FourCC const&) const = default;
    bool operator!=(FourCC const&) const = default;

    char cc[4];
};

}
