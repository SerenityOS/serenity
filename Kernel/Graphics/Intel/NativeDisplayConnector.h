/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Try.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Graphics/Intel/Auxiliary/GMBusConnector.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {

namespace IntelGraphics {

enum class RegisterIndex {
    PipeAConf = 0x70008,
    PipeBConf = 0x71008,
    GMBusData = 0x510C,
    GMBusStatus = 0x5108,
    GMBusCommand = 0x5104,
    GMBusClock = 0x5100,
    DisplayPlaneAControl = 0x70180,
    DisplayPlaneBControl = 0x71180,
    DisplayPlaneALinearOffset = 0x70184,
    DisplayPlaneAStride = 0x70188,
    DisplayPlaneASurface = 0x7019C,
    DPLLDivisorA0 = 0x6040,
    DPLLDivisorA1 = 0x6044,
    DPLLControlA = 0x6014,
    DPLLControlB = 0x6018,
    DPLLMultiplierA = 0x601C,
    HTotalA = 0x60000,
    HBlankA = 0x60004,
    HSyncA = 0x60008,
    VTotalA = 0x6000C,
    VBlankA = 0x60010,
    VSyncA = 0x60014,
    PipeASource = 0x6001C,
    AnalogDisplayPort = 0x61100,
    VGADisplayPlaneControl = 0x71400,
};

struct PLLSettings;

struct PLLParameterLimit {
    size_t min, max;
};

struct PLLMaxSettings {
    PLLParameterLimit dot_clock, vco, n, m, m1, m2, p, p1, p2;
};
}

class IntelNativeDisplayConnector final
    : public DisplayConnector {
    friend class IntelNativeGraphicsAdapter;
    friend class DeviceManagement;

public:
    static ErrorOr<NonnullLockRefPtr<IntelNativeDisplayConnector>> try_create(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, PhysicalAddress registers_region_address, size_t registers_region_length);

private:
    // ^DisplayConnector
    // FIXME: Implement modesetting capabilities in runtime from userland...
    virtual bool mutable_mode_setting_capable() const override { return false; }
    // FIXME: Implement double buffering capabilities in runtime from userland...
    virtual bool double_framebuffering_capable() const override { return false; }
    virtual ErrorOr<void> set_mode_setting(ModeSetting const&) override;
    virtual ErrorOr<void> set_safe_mode_setting() override;
    virtual ErrorOr<void> set_y_offset(size_t y) override;
    virtual ErrorOr<void> unblank() override;
    virtual ErrorOr<void> flush_first_surface() override final;
    virtual void enable_console() override;
    virtual void disable_console() override;
    virtual bool partial_flush_support() const override { return false; }
    virtual bool flush_support() const override { return false; }
    // Note: Paravirtualized hardware doesn't require a defined refresh rate for modesetting.
    virtual bool refresh_rate_support() const override { return true; }

    IntelNativeDisplayConnector(PhysicalAddress framebuffer_address, size_t framebuffer_resource_size, NonnullOwnPtr<GMBusConnector>, NonnullOwnPtr<Memory::Region> registers_region);

    ErrorOr<void> create_attached_framebuffer_console();
    ErrorOr<void> initialize_gmbus_settings_and_read_edid();

    void write_to_register(IntelGraphics::RegisterIndex, u32 value) const;
    u32 read_from_register(IntelGraphics::RegisterIndex) const;

    bool pipe_a_enabled() const;
    bool pipe_b_enabled() const;

    bool is_resolution_valid(size_t width, size_t height);

    bool set_safe_crt_resolution();

    void disable_output();
    void enable_output(PhysicalAddress fb_address, size_t width);

    void disable_vga_emulation();
    void enable_vga_plane();

    void disable_dac_output();
    void enable_dac_output();

    void disable_all_planes();
    void disable_pipe_a();
    void disable_pipe_b();
    void disable_dpll();

    void set_dpll_registers(IntelGraphics::PLLSettings const&);

    void enable_dpll_without_vga(IntelGraphics::PLLSettings const&, size_t dac_multiplier);
    void set_display_timings(Graphics::Modesetting const&);
    void enable_pipe_a();
    void enable_primary_plane(PhysicalAddress fb_address, size_t stride);

    bool wait_for_enabled_pipe_a(size_t milliseconds_timeout) const;
    bool wait_for_disabled_pipe_a(size_t milliseconds_timeout) const;
    bool wait_for_disabled_pipe_b(size_t milliseconds_timeout) const;

    void gmbus_read_edid();

    Optional<IntelGraphics::PLLSettings> create_pll_settings(u64 target_frequency, u64 reference_clock, IntelGraphics::PLLMaxSettings const&);

    mutable Spinlock<LockRank::None> m_registers_lock {};
    LockRefPtr<Graphics::GenericFramebufferConsole> m_framebuffer_console;

    const PhysicalAddress m_registers;
    NonnullOwnPtr<Memory::Region> m_registers_region;
    NonnullOwnPtr<GMBusConnector> m_gmbus_connector;
};
}
