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
#include <Kernel/Graphics/BochsGraphicsAdapter.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class BochsFramebufferDevice final : public FramebufferDevice {
    AK_MAKE_ETERNAL
    friend class BochsGraphicsAdapter;

public:
    static NonnullRefPtr<BochsFramebufferDevice> create(const BochsGraphicsAdapter&, PhysicalAddress, size_t, size_t, size_t);

    virtual size_t framebuffer_size_in_bytes() const override { return m_framebuffer_pitch * m_framebuffer_height * 2; }

    virtual ~BochsFramebufferDevice() = default;

private:
    virtual int ioctl(FileDescription&, unsigned request, FlatPtr arg) override;

    BochsFramebufferDevice(const BochsGraphicsAdapter&, PhysicalAddress, size_t, size_t, size_t);
    virtual const char* class_name() const override { return "BXVGA"; }

    void set_y_offset(size_t);

    size_t m_y_offset { 0 };

    NonnullRefPtr<BochsGraphicsAdapter> m_bochs_adapter;
};

}
