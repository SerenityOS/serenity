/*
 * Copyright (c) 2025, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Forward.h>
#include <AK/Types.h>

namespace AK {

class InternetChecksum {
public:
    InternetChecksum() = default;
    InternetChecksum(ReadonlyBytes);

    void update(ReadonlyBytes);
    NetworkOrdered<u16> digest();

private:
    u32 m_state { 0 };
    bool m_uneven_payload { false };
};

}

#ifdef USING_AK_GLOBALLY
using AK::InternetChecksum;
#endif
