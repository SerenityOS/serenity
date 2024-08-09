/*
 * Copyright (c) 2023, Edwin Rijkee <edwin@virtualparadise.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/GPU/3dfx/Definitions.h>
#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>
#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::VoodooGraphics {

class VoodooDisplayConnector final
    : public DisplayConnector {
    friend class VoodooGraphicsAdapter;
    friend class Kernel::Device;

public:
    static ErrorOr<NonnullRefPtr<VoodooDisplayConnector>> create(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, Memory::TypedMapping<RegisterMap volatile>, NonnullOwnPtr<IOWindow> io_window);

private:
    ErrorOr<void> fetch_and_initialize_edid();
    ErrorOr<void> create_attached_framebuffer_console();
    VoodooDisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, Memory::TypedMapping<RegisterMap volatile>, NonnullOwnPtr<IOWindow> io_window);

    virtual bool mutable_mode_setting_capable() const override final { return false; }
    virtual bool double_framebuffering_capable() const override { return false; }
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) override;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> set_safe_mode_setting() override final;
    virtual ErrorOr<void> unblank() override;
    virtual bool partial_flush_support() const override final { return false; }
    virtual bool flush_support() const override final { return false; }
    virtual bool refresh_rate_support() const override final { return false; }
    virtual ErrorOr<void> flush_first_surface() override final;
    virtual void enable_console() override final;
    virtual void disable_console() override final;

    ErrorOr<IterationDecision> for_each_dmt_timing_in_edid(Function<IterationDecision(EDID::DMT::MonitorTiming const&)>) const;
    ErrorOr<ModeSetting> find_suitable_mode(ModeSetting const& requested_mode) const;
    u8 read_vga(VGAPort port);
    u8 read_vga_indexed(VGAPort index_port, VGAPort data_port, u8 index);
    void write_vga(VGAPort port, u8 value);
    void write_vga_indexed(VGAPort index_port, VGAPort data_port, u8 index, u8 value);
    ErrorOr<void> wait_for_fifo_space(u32 minimum_entries);
    static PLLSettings calculate_pll(i32 desired_frequency_in_khz);
    ErrorOr<ModeRegisters> prepare_mode_switch(ModeSetting const& mode_setting);
    ErrorOr<void> perform_mode_switch(ModeRegisters const& regs);

    LockRefPtr<Graphics::GenericFramebufferConsole> m_framebuffer_console;
    Memory::TypedMapping<RegisterMap volatile> m_registers;
    NonnullOwnPtr<IOWindow> m_io_window;
};
}
