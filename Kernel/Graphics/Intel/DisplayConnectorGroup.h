/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/Intel/Auxiliary/GMBusConnector.h>
#include <Kernel/Graphics/Intel/Definitions.h>
#include <Kernel/Graphics/Intel/NativeDisplayConnector.h>
#include <Kernel/Graphics/Intel/Plane/DisplayPlane.h>
#include <Kernel/Graphics/Intel/Transcoder/DisplayTranscoder.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/TypedMapping.h>
#include <LibEDID/EDID.h>

namespace Kernel {

class IntelNativeGraphicsAdapter;
class IntelDisplayConnectorGroup : public RefCounted<IntelDisplayConnectorGroup> {
    friend class IntelNativeGraphicsAdapter;

public:
    enum class Generation {
        Gen4,
    };
    struct MMIORegion {
        enum class BARAssigned {
            BAR0,
            BAR2,
        };
        BARAssigned pci_bar_assigned;
        PhysicalAddress pci_bar_paddr;
        size_t pci_bar_space_length;
    };

private:
    AK_TYPEDEF_DISTINCT_ORDERED_ID(size_t, RegisterOffset);

    enum class AnalogOutputRegisterOffset {
        AnalogDisplayPort = 0x61100,
        VGADisplayPlaneControl = 0x71400,
    };

public:
    static ErrorOr<NonnullLockRefPtr<IntelDisplayConnectorGroup>> try_create(Badge<IntelNativeGraphicsAdapter>, Generation, MMIORegion const&, MMIORegion const&);

    ErrorOr<void> set_safe_mode_setting(Badge<IntelNativeDisplayConnector>, IntelNativeDisplayConnector&);
    ErrorOr<void> set_mode_setting(Badge<IntelNativeDisplayConnector>, IntelNativeDisplayConnector&, DisplayConnector::ModeSetting const&);

private:
    IntelDisplayConnectorGroup(Generation generation, NonnullOwnPtr<GMBusConnector>, NonnullOwnPtr<Memory::Region> registers_region, MMIORegion const&, MMIORegion const&);

    ErrorOr<void> set_mode_setting(IntelNativeDisplayConnector&, DisplayConnector::ModeSetting const&);

    StringView convert_analog_output_register_to_string(AnalogOutputRegisterOffset index) const;
    void write_to_analog_output_register(AnalogOutputRegisterOffset, u32 value);
    u32 read_from_analog_output_register(AnalogOutputRegisterOffset) const;
    u32 read_from_global_generation_register(IntelGraphics::GlobalGenerationRegister index) const;
    void write_to_global_generation_register(IntelGraphics::GlobalGenerationRegister index, u32 value);
    void write_to_general_register(RegisterOffset offset, u32 value);
    u32 read_from_general_register(RegisterOffset offset) const;

    // DisplayConnector initialization related methods
    ErrorOr<void> initialize_connectors();
    ErrorOr<void> initialize_gen4_connectors();

    // General Modesetting methods
    ErrorOr<void> set_gen4_mode_setting(IntelNativeDisplayConnector&, DisplayConnector::ModeSetting const&);
    bool pipe_a_enabled() const;
    bool pipe_b_enabled() const;

    bool set_crt_resolution(DisplayConnector::ModeSetting const&);

    void disable_vga_emulation();
    void enable_vga_plane();

    void disable_dac_output();
    void enable_dac_output();

    void disable_pipe_a();
    void disable_pipe_b();

    void enable_pipe_a();

    bool wait_for_enabled_pipe_a(size_t milliseconds_timeout) const;
    bool wait_for_disabled_pipe_a(size_t milliseconds_timeout) const;
    bool wait_for_disabled_pipe_b(size_t milliseconds_timeout) const;

    Optional<IntelGraphics::PLLSettings> create_pll_settings(u64 target_frequency, u64 reference_clock, IntelGraphics::PLLMaxSettings const&);

    Spinlock<LockRank::None> m_control_lock;
    Spinlock<LockRank::None> m_modeset_lock;
    mutable Spinlock<LockRank::None> m_registers_lock;

    // Note: The linux driver specifies an enum of possible ports and there is only
    // 9 ports (PORT_{A-I}). PORT_TC{1-6} are mapped to PORT_{D-I}.
    Array<LockRefPtr<IntelNativeDisplayConnector>, 9> m_connectors;

    Array<OwnPtr<IntelDisplayTranscoder>, 5> m_transcoders;
    Array<OwnPtr<IntelDisplayPlane>, 3> m_planes;

    const MMIORegion m_mmio_first_region;
    const MMIORegion m_mmio_second_region;
    MMIORegion const& m_assigned_mmio_registers_region;

    const Generation m_generation;
    NonnullOwnPtr<Memory::Region> m_registers_region;
    NonnullOwnPtr<GMBusConnector> m_gmbus_connector;
};
}
