/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
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
