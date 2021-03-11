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

#pragma once

#include <AK/String.h>
#include <AK/Types.h>
#include <Kernel/Graphics/GraphicsDevice.h>
#include <Kernel/IO.h>
#include <Kernel/PCI/DeviceController.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class VMWareFramebufferDevice;
class GraphicsManagement;
class VMWareGraphicsAdapter final : public GraphicsDevice
    , public PCI::DeviceController {
    AK_MAKE_ETERNAL
    friend class VMWareFramebufferDevice;
    friend class GraphicsManagement;

public:
    static NonnullRefPtr<VMWareGraphicsAdapter> initialize(PCI::Address);
    virtual ~VMWareGraphicsAdapter() = default;

private:
    // ^GraphicsDevice
    virtual void enumerate_displays() override;
    virtual Type type() const override { return Type::Bochs; }

    explicit VMWareGraphicsAdapter(PCI::Address);

    void set_safe_resolution();

    bool negotiate_version();

    void write_svga_register(u16 index, u32 value);
    u32 read_svga_register(u16 index);

    bool validate_setup_resolution(size_t width, size_t height);
    u32 find_framebuffer_address();
    bool try_to_set_resolution(size_t width, size_t height);
    bool set_resolution(size_t width, size_t height);
    void set_resolution_registers(size_t width, size_t height);
    void set_y_offset(size_t);

    IOAddress m_svga_control;
    PhysicalAddress m_fifo_region;
    PhysicalAddress m_framebuffer_address;

    bool m_operable { false };
    RefPtr<VMWareFramebufferDevice> m_framebuffer;
};

}
