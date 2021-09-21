/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Graphics/VirtIOGPU/Console.h>
#include <Kernel/Graphics/VirtIOGPU/FramebufferDevice.h>
#include <Kernel/Graphics/VirtIOGPU/GPU.h>

namespace Kernel::Graphics::VirtIOGPU {

class GraphicsAdapter final
    : public GraphicsDevice
    , public PCI::Device {
    AK_MAKE_ETERNAL

public:
    static NonnullRefPtr<GraphicsAdapter> initialize(PCI::DeviceIdentifier const&);

    virtual bool framebuffer_devices_initialized() const override { return m_created_framebuffer_devices; }

private:
    explicit GraphicsAdapter(PCI::DeviceIdentifier const&);

    virtual void initialize_framebuffer_devices() override;
    virtual Type type() const override { return Type::Raw; }

    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

    virtual bool modesetting_capable() const override { return false; }
    virtual bool double_framebuffering_capable() const override { return false; }

    virtual bool try_to_set_resolution(size_t, size_t, size_t) override { return false; }
    virtual bool set_y_offset(size_t, size_t) override { return false; }

    RefPtr<GPU> m_gpu_device;
    bool m_created_framebuffer_devices { false };
};
}
