/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class RawFramebufferDevice : public FramebufferDevice {
    AK_MAKE_ETERNAL
    friend class GraphicsDevice;

public:
    static NonnullRefPtr<RawFramebufferDevice> create(const GraphicsDevice&, PhysicalAddress, size_t pitch, size_t width, size_t height);

    virtual ~RawFramebufferDevice() {};

private:
    RawFramebufferDevice(PhysicalAddress, size_t pitch, size_t width, size_t height);
    virtual const char* class_name() const override { return "RawFramebuffer"; }
};

}
