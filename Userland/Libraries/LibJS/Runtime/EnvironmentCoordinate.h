/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <LibJS/Forward.h>

namespace JS {

struct EnvironmentCoordinate {
    u32 hops { invalid_marker };
    u32 index { invalid_marker };

    static FlatPtr hops_offset() { return OFFSET_OF(EnvironmentCoordinate, hops); }
    static FlatPtr index_offset() { return OFFSET_OF(EnvironmentCoordinate, index); }

    bool is_valid() const { return hops != invalid_marker && index != invalid_marker; }

    static constexpr u32 invalid_marker = 0xfffffffe;
};

}
