/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <AK/Types.h>
#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/Devices/GPU/Intel/Definitions.h>
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
        u32 dpll_reserved_dac_multiplier;
        u32 dpll_raw_dac_multiplier;
        u32 dpll_divisor_a0;
        u32 dpll_divisor_a1;
        u32 dpll_p1;
        u32 dpll_control;
        u32 m1_value;
        u32 n1_value;
        u32 m2_value;
        u32 n2_value;
        u32 m1_link;
        u32 n1_link;
        u32 m2_link;
        u32 n2_link;
        u32 pipe_conf;
    };

    ErrorOr<void> set_mode_setting_timings(Badge<IntelDisplayConnectorGroup>, DisplayConnector::ModeSetting const&);
    virtual ErrorOr<void> set_dpll_settings(Badge<IntelDisplayConnectorGroup>, IntelGraphics::PLLSettings const& settings, size_t dac_multiplier) = 0;
    virtual ErrorOr<void> enable_dpll_without_vga(Badge<IntelDisplayConnectorGroup>) = 0;
    virtual ErrorOr<void> disable_dpll(Badge<IntelDisplayConnectorGroup>) = 0;

    ErrorOr<void> disable_pipe(Badge<IntelDisplayConnectorGroup>);
    ErrorOr<void> enable_pipe(Badge<IntelDisplayConnectorGroup>);
    bool pipe_enabled(Badge<IntelDisplayConnectorGroup>) const;

    ShadowRegisters current_registers_state() const;

    virtual ~IntelDisplayTranscoder() = default;

protected:
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
        u32 m1_value;
        u32 n1_value;
        u32 m2_value;
        u32 n2_value;
        u32 m1_link;
        u32 n1_link;
        u32 m2_link;
        u32 n2_link;
    };

    struct [[gnu::packed]] PipeRegisters {
        u32 pipe_display_scan_line;
        u32 pipe_display_scan_line_count_range_compare;
        u32 pipe_configuration;
        u32 reserved;
        u32 pipe_gamma_correction_max_red;
        u32 pipe_gamma_correction_max_green;
        u32 pipe_gamma_correction_max_blue;
        u32 reserved2[2];
        u32 pipe_display_status;
        u32 reserved3[2];
        u32 display_arbitration_control;
        u32 display_fifo_watermark_control1;
        u32 display_fifo_watermark_control2;
        u32 display_fifo_watermark_control3;
        u32 pipe_frame_count_high;
        // Note: The specification calls this "Pipe Frame Count Low and Pixel Count"
        u32 pipe_frame_count_low;
    };

    IntelDisplayTranscoder(Memory::TypedMapping<TranscoderRegisters volatile>, Memory::TypedMapping<PipeRegisters volatile>);
    mutable Spinlock<LockRank::None> m_access_lock;

    ShadowRegisters m_shadow_registers {};
    Memory::TypedMapping<TranscoderRegisters volatile> m_transcoder_registers;
    Memory::TypedMapping<PipeRegisters volatile> m_pipe_registers;
};
}
