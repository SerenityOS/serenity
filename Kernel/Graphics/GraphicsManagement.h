/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Types.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/Graphics/VGACompatibleAdapter.h>
#include <Kernel/PCI/Definitions.h>
#include <Kernel/VM/Region.h>

namespace Kernel {

class BochsGraphicsAdapter;
class IntelNativeGraphicsAdapter;
class VGACompatibleAdapter;
class GraphicsManagement {
    friend class BochsGraphicsAdapter;
    friend class IntelNativeGraphicsAdapter;
    friend class VGACompatibleAdapter;
    AK_MAKE_ETERNAL

public:
    static GraphicsManagement& the();
    static bool is_initialized();
    bool initialize();

    unsigned allocate_minor_device_number() { return m_current_minor_number++; };
    GraphicsManagement();

    bool framebuffer_devices_allowed() const { return m_framebuffer_devices_allowed; }
    bool framebuffer_devices_exist() const;

    SpinLock<u8>& main_vga_lock() { return m_main_vga_lock; }
    RefPtr<Graphics::Console> console() const { return m_console; }

    void deactivate_graphical_mode();
    void activate_graphical_mode();

private:
    RefPtr<GraphicsDevice> determine_graphics_device(PCI::Address address, PCI::ID id) const;

    NonnullRefPtrVector<GraphicsDevice> m_graphics_devices;
    NonnullOwnPtr<Region> m_vga_font_region;
    RefPtr<Graphics::Console> m_console;

    // Note: there could be multiple VGA adapters, but only one can operate in VGA mode
    RefPtr<VGACompatibleAdapter> m_vga_adapter;
    unsigned m_current_minor_number { 0 };
    const bool m_framebuffer_devices_allowed;

    SpinLock<u8> m_main_vga_lock;
};

}
