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
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VGACompatibleAdapter.h>
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

    bool framebuffer_devices_console_only() const;
    bool framebuffer_devices_use_bootloader_framebuffer() const;
    bool framebuffer_devices_exist() const;

    void set_vga_text_mode_cursor(size_t console_width, size_t x, size_t y) const;
    void disable_vga_text_mode_console_cursor() const;
    void disable_vga_emulation_access_permanently();

    RefPtr<Graphics::Console> console() const { return m_console; }
    void set_console(Graphics::Console&);

    void deactivate_graphical_mode();
    void activate_graphical_mode();

private:
    void enable_vga_text_mode_console_cursor() const;

    bool determine_and_initialize_graphics_device(PCI::DeviceIdentifier const&);
    bool determine_and_initialize_isa_graphics_device();
    NonnullRefPtrVector<GenericGraphicsAdapter> m_graphics_devices;
    RefPtr<Graphics::Console> m_console;

    // Note: there could be multiple VGA adapters, but only one can operate in VGA mode
    RefPtr<VGACompatibleAdapter> m_vga_adapter;
    unsigned m_current_minor_number { 0 };

    mutable RecursiveSpinlock m_main_vga_lock;
    bool m_vga_access_is_disabled { false };
};

}
