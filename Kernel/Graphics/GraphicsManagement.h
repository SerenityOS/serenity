/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Graphics/Generic/DisplayConnector.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VirtIOGPU/GraphicsAdapter.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

class GraphicsManagement {

public:
    static GraphicsManagement& the();
    static bool is_initialized();
    bool initialize();

    unsigned allocate_minor_device_number() { return m_current_minor_number++; };
    GraphicsManagement();

    void attach_new_display_connector(Badge<DisplayConnector>, DisplayConnector&);
    void detach_display_connector(Badge<DisplayConnector>, DisplayConnector&);

    void set_vga_text_mode_cursor(size_t console_width, size_t x, size_t y);
    void disable_vga_text_mode_console_cursor();
    void disable_vga_emulation_access_permanently();

    RefPtr<Graphics::Console> console() const { return m_console; }
    void set_console(Graphics::Console&);

    void deactivate_graphical_mode();
    void activate_graphical_mode();

private:
    void enable_vga_text_mode_console_cursor();

    bool determine_and_initialize_graphics_device(PCI::DeviceIdentifier const&);

    void initialize_preset_resolution_generic_display_connector();

    NonnullRefPtrVector<GenericGraphicsAdapter> m_graphics_devices;
    RefPtr<Graphics::Console> m_console;

    // Note: This is only used when booting with kernel commandline that includes "graphics_subsystem_mode=limited"
    RefPtr<GenericDisplayConnector> m_preset_resolution_generic_display_connector;

    unsigned m_current_minor_number { 0 };

    SpinlockProtected<IntrusiveList<&DisplayConnector::m_list_node>> m_display_connector_nodes { LockRank::None };

    RecursiveSpinlock m_main_vga_lock { LockRank::None };
    bool m_vga_access_is_disabled { false };
};

}
