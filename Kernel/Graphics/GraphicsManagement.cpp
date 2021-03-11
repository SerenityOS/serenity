/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Checked.h>
#include <AK/Singleton.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/BochsGraphicsAdapter.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VGACompatibleAdapter.h>
#include <Kernel/Graphics/VMWareGraphicsAdapter.h>
#include <Kernel/Multiboot.h>

namespace Kernel {

static AK::Singleton<GraphicsManagement> s_the;

GraphicsManagement& GraphicsManagement::the()
{
    return *s_the;
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
        if (id.vendor_id == 0x15ad && id.device_id == 0x0405) {
            return VMWareGraphicsAdapter::initialize(address);
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
        adapter->enumerate_displays();
        m_graphics_devices.append(adapter.release_nonnull());
    });
    return true;
}

}
