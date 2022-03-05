/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class ISAVGAAdapter final : public VGACompatibleAdapter {
    friend class GraphicsManagement;

public:
    static NonnullRefPtr<ISAVGAAdapter> initialize();

    // Note: We simply don't support old VGA framebuffer modes (like the 320x200 256-colors one)
    virtual bool framebuffer_devices_initialized() const override { return false; }

    virtual bool try_to_set_resolution(size_t output_port_index, size_t width, size_t height) override;
    virtual bool set_y_offset(size_t output_port_index, size_t y) override;

private:
    ISAVGAAdapter();

    // ^GenericGraphicsAdapter
    virtual void initialize_framebuffer_devices() override;

    virtual void enable_consoles() override;
    virtual void disable_consoles() override;

    RefPtr<Graphics::Console> m_framebuffer_console;
};
}
