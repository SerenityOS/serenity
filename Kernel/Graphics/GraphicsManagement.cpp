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
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/IntelNativeGraphicsAdapter.h>
#include <Kernel/Graphics/VGACompatibleAdapter.h>
#include <Kernel/Multiboot.h>

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
    : m_textmode_enabled(kernel_command_line().is_text_mode())
{
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
        VERIFY(multiboot_info_ptr->framebuffer_type == MULTIBOOT_FRAMEBUFFER_TYPE_RGB || multiboot_info_ptr->framebuffer_type == MULTIBOOT_FRAMEBUFFER_TYPE_EGA_TEXT);
        return VGACompatibleAdapter::initialize_with_preset_resolution(address,
            PhysicalAddress((u32)(multiboot_info_ptr->framebuffer_addr)),
            multiboot_info_ptr->framebuffer_pitch,
            multiboot_info_ptr->framebuffer_width,
            multiboot_info_ptr->framebuffer_height);
    }
    return {};
}

UNMAP_AFTER_INIT bool GraphicsManagement::initialize()
{
    if (kernel_command_line().is_text_mode()) {
        dbgln("Text mode enabled");
        return false;
    }

    PCI::enumerate([&](const PCI::Address& address, PCI::ID id) {
        auto adapter = determine_graphics_device(address, id);
        if (!adapter)
            return;
        adapter->initialize_framebuffer_devices();
        m_graphics_devices.append(adapter.release_nonnull());
    });
    return true;
}

}
