/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Intel/Transcoder/DisplayTranscoder.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

IntelDisplayTranscoder::IntelDisplayTranscoder(Memory::TypedMapping<TranscoderRegisters volatile> registers_mapping)
    : m_transcoder_registers(move(registers_mapping))
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

}
