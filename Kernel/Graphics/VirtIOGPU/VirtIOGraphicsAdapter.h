/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Graphics/VirtIOGPU/VirtIOFrameBufferDevice.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOGPU.h>
#include <Kernel/Graphics/VirtIOGPU/VirtIOGPUConsole.h>

namespace Kernel::Graphics {

class VirtIOGraphicsAdapter final : public GraphicsDevice {
    AK_MAKE_ETERNAL

public:
    static NonnullRefPtr<VirtIOGraphicsAdapter> initialize(PCI::Address);

    virtual bool framebuffer_devices_initialized() const override { return m_created_framebuffer_devices; }

private:
    explicit VirtIOGraphicsAdapter(PCI::Address base_address);

    virtual void initialize_framebuffer_devices() override;
    virtual Type type() const override { return Type::Raw; }

    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

    virtual bool modesetting_capable() const override { return false; }
    virtual bool double_framebuffering_capable() const override { return false; }

    virtual bool try_to_set_resolution(size_t, size_t, size_t) override { return false; }
    virtual bool set_y_offset(size_t, size_t) override { return false; }

    RefPtr<VirtIOGPU> m_gpu_device;
    bool m_created_framebuffer_devices { false };
};

}
