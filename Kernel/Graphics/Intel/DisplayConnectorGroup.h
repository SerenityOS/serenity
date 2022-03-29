/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefPtr.h>
#include <AK/Try.h>
#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/Intel/Definitions.h>
#include <Kernel/Graphics/Intel/GMBusConnector.h>
#include <Kernel/Graphics/Intel/NativeDisplayConnector.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Memory/TypedMapping.h>
#include <LibEDID/EDID.h>

namespace Kernel {

class IntelDisplayConnectorGroup : public RefCounted<IntelDisplayConnectorGroup> {
    friend class IntelNativeGraphicsAdapter;

public:
    struct MMIORegion {
        enum class BARAssigned {
            BAR0,
            BAR2,
        };
        BARAssigned pci_bar_assigned;
        PhysicalAddress pci_bar_paddr;
        size_t pci_bar_space_length;
    };

public:
    static ErrorOr<NonnullLockRefPtr<IntelDisplayConnectorGroup>> try_create(MMIORegion const&, MMIORegion const&);

    ErrorOr<void> set_safe_mode_setting(Badge<IntelNativeDisplayConnector>, IntelNativeDisplayConnector&);
    ErrorOr<void> set_mode_setting(Badge<IntelNativeDisplayConnector>, IntelNativeDisplayConnector&, DisplayConnector::ModeSetting const&);

private:
    IntelDisplayConnectorGroup(NonnullOwnPtr<GMBusConnector>, NonnullOwnPtr<Memory::Region> registers_region, MMIORegion const&, MMIORegion const&);
    ErrorOr<void> initialize_connectors();

    ErrorOr<void> set_mode_setting(IntelNativeDisplayConnector&, DisplayConnector::ModeSetting const&);

    void write_to_register(IntelGraphics::RegisterIndex, u32 value) const;
    u32 read_from_register(IntelGraphics::RegisterIndex) const;

    bool pipe_a_enabled() const;
    bool pipe_b_enabled() const;

    bool is_resolution_valid(size_t width, size_t height);

    bool set_crt_resolution(DisplayConnector::ModeSetting const&);

    void disable_output();
    void enable_output(size_t width);

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
    void set_display_timings(DisplayConnector::ModeSetting const&);
    void enable_pipe_a();
    void enable_primary_plane(size_t stride);

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

    const MMIORegion m_mmio_first_region;
    const MMIORegion m_mmio_second_region;
    MMIORegion const& m_assigned_mmio_registers_region;

    NonnullOwnPtr<Memory::Region> m_registers_region;
    NonnullOwnPtr<GMBusConnector> m_gmbus_connector;
};
}
