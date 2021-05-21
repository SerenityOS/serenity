/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Singleton.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/BochsGraphicsAdapter.h>
#include <Kernel/Graphics/Console/FramebufferConsole.h>
#include <Kernel/Graphics/Console/TextModeConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/IntelNativeGraphicsAdapter.h>
#include <Kernel/Graphics/VGACompatibleAdapter.h>
#include <Kernel/IO.h>
#include <Kernel/Multiboot.h>
#include <Kernel/Panic.h>
#include <Kernel/VM/AnonymousVMObject.h>

namespace Kernel {

static AK::Singleton<GraphicsManagement> s_the;

GraphicsManagement& GraphicsManagement::the()
{
    return *s_the;
}

bool GraphicsManagement::is_initialized()
{
    return s_the.is_initialized();
}

UNMAP_AFTER_INIT GraphicsManagement::GraphicsManagement()
    : m_vga_font_region(MM.allocate_kernel_region(PAGE_SIZE, "VGA font", Region::Access::Read | Region::Access::Write, AllocationStrategy::AllocateNow).release_nonnull())
    , m_framebuffer_devices_allowed(!kernel_command_line().is_no_framebuffer_devices_mode())
{
}

void GraphicsManagement::deactivate_graphical_mode()
{
    for (auto& graphics_device : m_graphics_devices) {
        graphics_device.enable_consoles();
    }
}
void GraphicsManagement::activate_graphical_mode()
{
    for (auto& graphics_device : m_graphics_devices) {
        graphics_device.disable_consoles();
    }
}

UNMAP_AFTER_INIT RefPtr<GraphicsDevice> GraphicsManagement::determine_graphics_device(PCI::Address address, PCI::ID id) const
{
    if ((id.vendor_id == 0x1234 && id.device_id == 0x1111) || (id.vendor_id == 0x80ee && id.device_id == 0xbeef)) {
        return BochsGraphicsAdapter::initialize(address);
    }
    if (PCI::get_class(address) == 0x3 && PCI::get_subclass(address) == 0x0) {
        if (id.vendor_id == 0x8086) {
            auto adapter = IntelNativeGraphicsAdapter::initialize(address);
            if (!adapter.is_null())
                return adapter;
        }
        if (multiboot_info_ptr->framebuffer_type == MULTIBOOT_FRAMEBUFFER_TYPE_RGB) {
            dmesgln("Graphics: Using a preset resolution from the bootloader");
            return VGACompatibleAdapter::initialize_with_preset_resolution(address,
                PhysicalAddress((u32)(multiboot_info_ptr->framebuffer_addr)),
                multiboot_info_ptr->framebuffer_width,
                multiboot_info_ptr->framebuffer_height,
                multiboot_info_ptr->framebuffer_pitch);
        }
        return VGACompatibleAdapter::initialize(address);
    }
    return {};
}

UNMAP_AFTER_INIT bool GraphicsManagement::initialize()
{

    /* Explanation on the flow when not requesting to force not creating any 
     * framebuffer devices:
     * If the user wants to use a Console instead of the graphical environment,
     * they doesn't need to request text mode.
     * Graphical mode might not be accessible on bare-metal hardware because
     * the bootloader didn't set a framebuffer and we don't have a native driver
     * to set a framebuffer for it. We don't have VBE modesetting capabilities
     * in the kernel yet, so what will happen is one of the following situations:
     * 1. The bootloader didn't specify settings of a pre-set framebuffer. The
     * kernel has a native driver for a detected display adapter, therefore
     * the kernel can still set a framebuffer.
     * 2. The bootloader specified settings of a pre-set framebuffer, and the 
     * kernel has a native driver for a detected display adapter, therefore
     * the kernel can still set a framebuffer and change the settings of it.
     * In that situation, the kernel will simply ignore the Multiboot pre-set 
     * framebuffer.
     * 2. The bootloader specified settings of a pre-set framebuffer, and the 
     * kernel does not have a native driver for a detected display adapter, 
     * therefore the kernel will use the pre-set framebuffer. Modesetting is not
     * available in this situation.
     * 3. The bootloader didn't specify settings of a pre-set framebuffer, and 
     * the kernel does not have a native driver for a detected display adapter, 
     * therefore the kernel will try to initialize a VGA text mode console.
     * In that situation, the kernel will assume that VGA text mode was already
     * initialized, but will still try to modeset it. No switching to graphical 
     * environment is allowed in this case.
     * 
     * By default, the kernel assumes that no framebuffer was created until it
     * was proven that there's an existing framebuffer or we can modeset the 
     * screen resolution to create a framebuffer.
     * 
     * If the user requests to force no initialization of framebuffer devices
     * the same flow above will happen, except that no framebuffer device will
     * be created, so SystemServer will not try to initialize WindowServer.
     */

    if (kernel_command_line().is_no_framebuffer_devices_mode()) {
        dbgln("Forcing no initialization of framebuffer devices");
    }

    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        // Note: Each graphics controller will try to set its native screen resolution
        // upon creation. Later on, if we don't want to have framebuffer devices, a
        // framebuffer console will take the control instead.
        auto adapter = determine_graphics_device(address, id);
        if (!adapter)
            return;

        // If IO space is enabled, this VGA adapter is operating in VGA mode.
        if (adapter->type() == GraphicsDevice::Type::VGACompatible && PCI::is_io_space_enabled(address)) {
            VERIFY(m_vga_adapter.is_null());
            dbgln("Graphics adapter @ {} is operating in VGA mode", address);
            m_vga_adapter = adapter;
        }
        auto display_adapter = adapter.release_nonnull();
        m_graphics_devices.append(display_adapter);
        if (!m_framebuffer_devices_allowed) {
            display_adapter->enable_consoles();
            return;
        }
        display_adapter->initialize_framebuffer_devices();
    });
    if (m_graphics_devices.is_empty()) {
        dbgln("No graphics adapter was initialized.");
        return false;
    }
    return true;
}

bool GraphicsManagement::framebuffer_devices_exist() const
{
    for (auto& graphics_device : m_graphics_devices) {
        if (graphics_device.framebuffer_devices_initialized())
            return true;
    }
    return false;
}
}
