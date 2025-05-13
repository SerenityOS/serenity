/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>

#include <AK/Platform.h>
VALIDATE_IS_RISCV64()

namespace Kernel {

class ProcessorInfo {
public:
    ProcessorInfo();

    u32 mvendorid() const { return m_mvendorid; }
    FlatPtr marchid() const { return m_marchid; }
    FlatPtr mimpid() const { return m_mimpid; }

private:
    // mvendorid, marchid, and mimpid can all be zero if they aren't implemented.
    u32 m_mvendorid { 0 };
    FlatPtr m_marchid { 0 };
    FlatPtr m_mimpid { 0 };
};

}
