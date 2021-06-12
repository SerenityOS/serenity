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
    : GraphicsDevice(base_address)
{
    m_gpu_device = adopt_ref(*new VirtIOGPU(base_address)).leak_ref();
    m_framebuffer_console = Kernel::Graphics::VirtIOGPUConsole::initialize(m_gpu_device);
    // FIXME: This is a very wrong way to do this...
    GraphicsManagement::the().m_console = m_framebuffer_console;
}

void VirtIOGraphicsAdapter::initialize_framebuffer_devices()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Initializing framebuffer devices");
    VERIFY(m_framebuffer_device.is_null());
    m_framebuffer_device = adopt_ref(*new VirtIOFrameBufferDevice(m_gpu_device)).leak_ref();
}

void VirtIOGraphicsAdapter::enable_consoles()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Enabling consoles");
    VERIFY(m_framebuffer_console);
    if (m_framebuffer_device)
        m_framebuffer_device->deactivate_writes();
    m_gpu_device->clear_to_black();
    m_framebuffer_console->enable();
}

void VirtIOGraphicsAdapter::disable_consoles()
{
    dbgln_if(VIRTIO_DEBUG, "VirtIOGPU: Disabling consoles");
    VERIFY(m_framebuffer_device);
    VERIFY(m_framebuffer_console);
    m_framebuffer_console->disable();
    m_framebuffer_device->activate_writes();
}

}
