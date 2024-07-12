/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/Platform.h>
#include <AK/Types.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Devices/GPU/Console/Console.h>
#include <Kernel/Devices/GPU/DisplayConnector.h>
#include <Kernel/Devices/GPU/GPUDevice.h>
#include <Kernel/Devices/GPU/Generic/DisplayConnector.h>
#include <Kernel/Devices/GPU/VirtIO/GraphicsAdapter.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Memory/Region.h>

namespace Kernel {

class GraphicsManagement {

public:
    static GraphicsManagement& the();
    static bool is_initialized();
    bool initialize();

    unsigned allocate_minor_device_number() { return m_current_minor_number++; }
    GraphicsManagement();

    void attach_new_display_connector(Badge<DisplayConnector>, DisplayConnector&);
    void detach_display_connector(Badge<DisplayConnector>, DisplayConnector&);

    LockRefPtr<Graphics::Console> console() const { return m_console; }
    void set_console(Graphics::Console&);

    void deactivate_graphical_mode();
    void activate_graphical_mode();

private:
    void enable_vga_text_mode_console_cursor();

    ErrorOr<void> determine_and_initialize_graphics_device(PCI::DeviceIdentifier const&);

    void initialize_preset_resolution_generic_display_connector();

    Vector<NonnullLockRefPtr<GPUDevice>> m_graphics_devices;
    LockRefPtr<Graphics::Console> m_console;

    // Note: This is only used when booting with kernel commandline that includes "graphics_subsystem_mode=limited"
    RefPtr<GenericDisplayConnector> m_preset_resolution_generic_display_connector;

    RefPtr<DisplayConnector> m_platform_board_specific_display_connector;

    unsigned m_current_minor_number { 0 };

    SpinlockProtected<IntrusiveList<&DisplayConnector::m_list_node>, LockRank::None> m_display_connector_nodes {};
};

}
