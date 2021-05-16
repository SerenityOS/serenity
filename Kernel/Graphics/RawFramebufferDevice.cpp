/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/RawFramebufferDevice.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<RawFramebufferDevice> RawFramebufferDevice::create(const GraphicsDevice&, PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
{
    return adopt_ref(*new RawFramebufferDevice(framebuffer_address, width, height, pitch));
}
UNMAP_AFTER_INIT RawFramebufferDevice::RawFramebufferDevice(PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
    : FramebufferDevice(framebuffer_address, width, height, pitch)
{
}

}
