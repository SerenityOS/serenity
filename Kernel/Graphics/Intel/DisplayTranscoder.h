/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Graphics/Intel/Definitions.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

class IntelDisplayConnectorGroup;
class IntelDisplayTranscoder {
public:
    // Note: This is used to "cache" all the registers we wrote to, because
    // we might not be able to read them directly from hardware later.
    struct ShadowRegisters {
        u32 horizontal_total;
        u32 horizontal_blank;
        u32 horizontal_sync;
        u32 vertical_total;
        u32 vertical_blank;
        u32 vertical_sync;
        u32 exit_line;
        u32 pipe_source;
        u32 pipe_border_color_pattern;
        u32 reserved;
        u32 vsync_shift;
        u32 pipe_mult;
    };

    static ErrorOr<NonnullOwnPtr<IntelDisplayTranscoder>> create_with_physical_address(PhysicalAddress transcoder_registers_start_address);

    ErrorOr<void> set_mode_setting_timings(Badge<IntelDisplayConnectorGroup>, DisplayConnector::ModeSetting const&);

    ShadowRegisters current_registers_state() const;

private:
    struct [[gnu::packed]] TranscoderRegisters {
        u32 horizontal_total;
        u32 horizontal_blank;
        u32 horizontal_sync;
        u32 vertical_total;
        u32 vertical_blank;
        u32 vertical_sync;
        u32 exit_line;
        u32 pipe_source;
        u32 pipe_border_color_pattern;
        u32 reserved;
        u32 vsync_shift;
        u32 pipe_mult;
    };

    explicit IntelDisplayTranscoder(Memory::TypedMapping<TranscoderRegisters volatile>);
    mutable Spinlock<LockRank::None> m_access_lock;
    ShadowRegisters m_shadow_registers {};
    Memory::TypedMapping<TranscoderRegisters volatile> m_transcoder_registers;
};
}
