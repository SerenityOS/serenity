/*
 * Copyright (c) 2026, miridav
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NeverDestroyed.h>
#include <AK/Optional.h>
#include <AK/Time.h>
#include <Kernel/Locking/LockRank.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class GoldfishRTC final {

    public:
    struct Registers {
        u32 time_low;
        u32 time_high;
    };

    // Public only for NeverDestroyed; do not construct directly.
    explicit GoldfishRTC(Memory::TypedMapping<Registers volatile>);

    static ErrorOr<void> initialize(PhysicalAddress);
    static GoldfishRTC& the();
    static bool is_initialized();

    UnixDateTime now() const;
    UnixDateTime boot_time() const;

private:
    Memory::TypedMapping<Registers volatile> m_regs;
    UnixDateTime m_boot_time;
    mutable Spinlock<LockRank::None> m_lock;

};

}