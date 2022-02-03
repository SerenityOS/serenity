/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Graphics/Console/Console.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GenericGraphicsAdapter.h>
#include <Kernel/Graphics/VGA/GenericDisplayConnector.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {

class VGAGenericAdapter : public GenericGraphicsAdapter {
public:
    virtual bool framebuffer_devices_initialized() const override { return !m_framebuffer_device.is_null(); }

    virtual bool modesetting_capable() const override { return false; }
    virtual bool double_framebuffering_capable() const override { return false; }

    virtual bool vga_compatible() const override final { return true; }

    virtual ErrorOr<void> set_resolution(size_t, size_t, size_t) override { return Error::from_errno(ENOTSUP); }
    virtual ErrorOr<void> set_y_offset(size_t, size_t) override { return Error::from_errno(ENOTSUP); }

    ErrorOr<ByteBuffer> get_edid(size_t) const override { return Error::from_errno(ENOTSUP); }

protected:
    VGAGenericAdapter() { }

    RefPtr<FramebufferDevice> m_framebuffer_device;
    OwnPtr<VGAGenericDisplayConnector> m_display_connector;
};
}
