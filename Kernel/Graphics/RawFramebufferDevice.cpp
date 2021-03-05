/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/RawFramebufferDevice.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<RawFramebufferDevice> RawFramebufferDevice::create(const GraphicsDevice&, PhysicalAddress framebuffer_address, size_t pitch, size_t width, size_t height)
{
    return adopt_ref(*new RawFramebufferDevice(framebuffer_address, pitch, width, height));
}
UNMAP_AFTER_INIT RawFramebufferDevice::RawFramebufferDevice(PhysicalAddress framebuffer_address, size_t pitch, size_t width, size_t height)
    : FramebufferDevice(framebuffer_address, pitch, width, height)
{
}

}
