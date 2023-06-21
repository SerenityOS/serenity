/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Delay.h>
#include <Kernel/Devices/GPU/Intel/Transcoder/DisplayTranscoder.h>
#include <Kernel/Memory/PhysicalAddress.h>

namespace Kernel {

IntelDisplayTranscoder::IntelDisplayTranscoder(Memory::TypedMapping<TranscoderRegisters volatile> registers_mapping, Memory::TypedMapping<PipeRegisters volatile> pipe_registers_mapping)
    : m_transcoder_registers(move(registers_mapping))
    , m_pipe_registers(move(pipe_registers_mapping))
{
}

IntelDisplayTranscoder::ShadowRegisters IntelDisplayTranscoder::current_registers_state() const
{
    SpinlockLocker locker(m_access_lock);
    return m_shadow_registers;
}

ErrorOr<void> IntelDisplayTranscoder::set_mode_setting_timings(Badge<IntelDisplayConnectorGroup>, DisplayConnector::ModeSetting const& mode_setting)
{
    SpinlockLocker locker(m_access_lock);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "htotal - {}, {}", (mode_setting.horizontal_active - 1), (mode_setting.horizontal_total() - 1));
    m_shadow_registers.horizontal_total = ((mode_setting.horizontal_active - 1) | (mode_setting.horizontal_total() - 1) << 16);
    m_transcoder_registers->horizontal_total = ((mode_setting.horizontal_active - 1) | (mode_setting.horizontal_total() - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "hblank - {}, {}", (mode_setting.horizontal_blanking_start() - 1), (mode_setting.horizontal_blanking_start() + mode_setting.horizontal_blank_pixels - 1));
    m_shadow_registers.horizontal_blank = ((mode_setting.horizontal_blanking_start() - 1) | (mode_setting.horizontal_blanking_start() + mode_setting.horizontal_blank_pixels - 1) << 16);
    m_transcoder_registers->horizontal_blank = ((mode_setting.horizontal_blanking_start() - 1) | (mode_setting.horizontal_blanking_start() + mode_setting.horizontal_blank_pixels - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "hsync - {}, {}", (mode_setting.horizontal_sync_start() - 1), (mode_setting.horizontal_sync_end() - 1));
    m_shadow_registers.horizontal_sync = ((mode_setting.horizontal_sync_start() - 1) | (mode_setting.horizontal_sync_end() - 1) << 16);
    m_transcoder_registers->horizontal_sync = ((mode_setting.horizontal_sync_start() - 1) | (mode_setting.horizontal_sync_end() - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "vtotal - {}, {}", (mode_setting.vertical_active - 1), (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1));
    m_shadow_registers.vertical_total = ((mode_setting.vertical_active - 1) | (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1) << 16);
    m_transcoder_registers->vertical_total = ((mode_setting.vertical_active - 1) | (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "vblank - {}, {}", (mode_setting.vertical_blanking_start() - 1), (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1));
    m_shadow_registers.vertical_blank = ((mode_setting.vertical_blanking_start() - 1) | (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1) << 16);
    m_transcoder_registers->vertical_blank = ((mode_setting.vertical_blanking_start() - 1) | (mode_setting.vertical_blanking_start() + mode_setting.vertical_blank_lines - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "vsync - {}, {}", (mode_setting.vertical_sync_start() - 1), (mode_setting.vertical_sync_end() - 1));
    m_shadow_registers.vertical_sync = ((mode_setting.vertical_sync_start() - 1) | (mode_setting.vertical_sync_end() - 1) << 16);
    m_transcoder_registers->vertical_sync = ((mode_setting.vertical_sync_start() - 1) | (mode_setting.vertical_sync_end() - 1) << 16);

    dbgln_if(INTEL_GRAPHICS_DEBUG, "sourceSize - {}, {}", (mode_setting.vertical_active - 1), (mode_setting.horizontal_active - 1));
    m_shadow_registers.pipe_source = ((mode_setting.vertical_active - 1) | (mode_setting.horizontal_active - 1) << 16);
    m_transcoder_registers->pipe_source = ((mode_setting.vertical_active - 1) | (mode_setting.horizontal_active - 1) << 16);
    return {};
}

ErrorOr<void> IntelDisplayTranscoder::disable_pipe(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    m_pipe_registers->pipe_configuration = 0;
    m_shadow_registers.pipe_conf = 0;
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Disabling Pipe");
    size_t milliseconds_elapsed = 0;
    while (milliseconds_elapsed < 100) {
        u32 value = m_pipe_registers->pipe_configuration;
        if (!(value & (1 << 30)))
            return {};
        microseconds_delay(1000);
        milliseconds_elapsed++;
    }
    return Error::from_errno(EBUSY);
}

ErrorOr<void> IntelDisplayTranscoder::enable_pipe(Badge<IntelDisplayConnectorGroup>)
{
    SpinlockLocker locker(m_access_lock);
    u32 value = m_pipe_registers->pipe_configuration;
    // Note: Just verify these are not already enabled...
    if ((value & (1 << 30)) && (value & (1 << 31)))
        return {};

    // Note: Set the pipe configuration register with these bits:
    // 1. Bit 31 - to enable the Pipe
    // 2. Bit 24 - to enable Gamma Unit Mode to 10 bit Gamma mode.
    // 3. Bits 21-23 are set to zero to indicate Progressive mode (non Interlaced mode)
    // 4. Bits 18 and 19 are set to zero to indicate Normal operations of assigned
    //  Cursor and Display planes.
    m_pipe_registers->pipe_configuration = (1 << 31) | (1 << 24);
    m_shadow_registers.pipe_conf = (1 << 31) | (1 << 24);
    dbgln_if(INTEL_GRAPHICS_DEBUG, "Enabling Pipe");
    size_t milliseconds_elapsed = 0;
    while (milliseconds_elapsed < 100) {
        u32 value = m_pipe_registers->pipe_configuration;
        if ((value & (1 << 30)))
            return {};
        microseconds_delay(1000);
        milliseconds_elapsed++;
    }
    // FIXME: Seems like my video card is buggy and doesn't set the enabled bit (bit 30)!!
    return {};
}
bool IntelDisplayTranscoder::pipe_enabled(Badge<IntelDisplayConnectorGroup>) const
{
    SpinlockLocker locker(m_access_lock);
    u32 value = m_pipe_registers->pipe_configuration;
    return (value & (1 << 30));
}
}
