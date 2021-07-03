/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/Console/GenericFramebufferConsole.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOGPU.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOGraphicsAdapter.h>

namespace Kernel::Graphics {

NonnullRefPtr<VirtIOGraphicsAdapter> VirtIOGraphicsAdapter::initialize(PCI::Address base_address)
{
    return adopt_ref(*new VirtIOGraphicsAdapter(base_address));
}

VirtIOGraphicsAdapter::VirtIOGraphicsAdapter(PCI::Address base_address)
    : PCI::DeviceController(base_address)
{
    m_gpu_device = adopt_ref(*new VirtIOGPU(base_address)).leak_ref();
}

void VirtIOGraphicsAdapter::initialize_framebuffer_devices()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Initializing framebuffer devices");
    VERIFY(!m_created_framebuffer_devices);
    m_gpu_device->create_framebuffer_devices();
    m_created_framebuffer_devices = true;

    // FIXME: This is a very wrong way to do this...
    GraphicsManagement::the().m_console = m_gpu_device->default_console();
}

void VirtIOGraphicsAdapter::enable_consoles()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Enabling consoles");
    m_gpu_device->for_each_framebuffer([&](auto& framebuffer, auto& console) {
        framebuffer.deactivate_writes();
        framebuffer.clear_to_black();
        console.enable();
        return IterationDecision::Continue;
    });
}

void VirtIOGraphicsAdapter::disable_consoles()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Disabling consoles");
    m_gpu_device->for_each_framebuffer([&](auto& framebuffer, auto& console) {
        console.disable();
        framebuffer.activate_writes();
        return IterationDecision::Continue;
    });
}

}
