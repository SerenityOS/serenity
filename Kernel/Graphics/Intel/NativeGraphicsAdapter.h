/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Definitions.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/VGACompatibleAdapter.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

namespace IntelGraphics {

enum RegisterIndex {
    PipeAConf = 0x70008,
    PipeBConf = 0x71008,
    GMBusData = 0x510C,
    GMBusStatus = 0x5108,
    GMBusCommand = 0x5104,
    GMBusClock = 0x5100,
    DisplayPlaneAControl = 0x70180,
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
}

class IntelNativeGraphicsAdapter final
    : public VGACompatibleAdapter {
    AK_MAKE_ETERNAL
public:
    struct PLLSettings {
        bool is_valid() const { return (n != 0 && m1 != 0 && m2 != 0 && p1 != 0 && p2 != 0); }
        u64 compute_dot_clock(u64 refclock) const
        {
            return (refclock * (5 * (m1) + (m2)) / (n)) / (p1 * p2);
        }

        u64 compute_vco(u64 refclock) const
        {
            return refclock * (5 * (m1) + (m2)) / n;
        }

        u64 compute_m() const
        {
            return 5 * (m1) + (m2);
        }

        u64 compute_p() const
        {
            return p1 * p2;
        }
        u64 n { 0 };
        u64 m1 { 0 };
        u64 m2 { 0 };
        u64 p1 { 0 };
        u64 p2 { 0 };
    };
    struct PLLParameterLimit {
        size_t min, max;
    };
    struct PLLMaxSettings {
        PLLParameterLimit dot_clock, vco, n, m, m1, m2, p, p1, p2;
    };

private:
    enum GMBusPinPair : u8 {
        None = 0,
        DedicatedControl = 1,
        DedicatedAnalog = 0b10,
        IntegratedDigital = 0b11,
        sDVO = 0b101,
        Dconnector = 0b111,
    };

    enum class GMBusStatus {
        TransactionCompletion,
        HardwareReady
    };

    enum GMBusCycle {
        Wait = 1,
        Stop = 4,
    };

public:
    static RefPtr<IntelNativeGraphicsAdapter> initialize(PCI::DeviceIdentifier const&);

private:
    explicit IntelNativeGraphicsAdapter(PCI::Address);

    void write_to_register(IntelGraphics::RegisterIndex, u32 value) const;
    u32 read_from_register(IntelGraphics::RegisterIndex) const;

    // ^GenericGraphicsAdapter
    virtual void initialize_framebuffer_devices() override;

    bool pipe_a_enabled() const;
    bool pipe_b_enabled() const;

    bool is_resolution_valid(size_t width, size_t height);

    bool set_crt_resolution(size_t width, size_t height);

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

    void set_dpll_registers(const PLLSettings&);

    void enable_dpll_without_vga(const PLLSettings&, size_t dac_multiplier);
    void set_display_timings(const Graphics::Modesetting&);
    void enable_pipe_a();
    void set_framebuffer_parameters(size_t, size_t);
    void enable_primary_plane(PhysicalAddress fb_address, size_t stride);

    bool wait_for_enabled_pipe_a(size_t milliseconds_timeout) const;
    bool wait_for_disabled_pipe_a(size_t milliseconds_timeout) const;
    bool wait_for_disabled_pipe_b(size_t milliseconds_timeout) const;

    void set_gmbus_default_rate();
    void set_gmbus_pin_pair(GMBusPinPair pin_pair);

    // FIXME: It would be better if we generalize the I2C access later on
    void gmbus_read_edid();
    void gmbus_write(unsigned address, u32 byte);
    void gmbus_read(unsigned address, u8* buf, size_t length);
    bool gmbus_wait_for(GMBusStatus desired_status, Optional<size_t> milliseconds_timeout);

    Optional<PLLSettings> create_pll_settings(u64 target_frequency, u64 reference_clock, const PLLMaxSettings&);

    Spinlock m_control_lock;
    Spinlock m_modeset_lock;
    mutable Spinlock m_registers_lock;

    Graphics::VideoInfoBlock m_crt_edid;
    const PhysicalAddress m_registers;
    const PhysicalAddress m_framebuffer_addr;
    OwnPtr<Memory::Region> m_registers_region;
};

}
