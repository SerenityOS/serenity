/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Types.h>
#include <AK/Weakable.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Devices/BlockDevice.h>
#include <Kernel/PhysicalAddress.h>

namespace Kernel {
class GenericGraphicsAdapter
    : public RefCounted<GenericGraphicsAdapter>
    , public Weakable<GenericGraphicsAdapter> {
public:
    virtual ~GenericGraphicsAdapter() = default;
    virtual void initialize_framebuffer_devices() = 0;
    virtual void enable_consoles() = 0;
    virtual void disable_consoles() = 0;
    bool consoles_enabled() const { return m_consoles_enabled; }
    virtual bool framebuffer_devices_initialized() const = 0;

    virtual bool modesetting_capable() const = 0;
    virtual bool double_framebuffering_capable() const = 0;

    virtual bool vga_compatible() const = 0;

    virtual bool try_to_set_resolution(size_t output_port_index, size_t width, size_t height) = 0;
    virtual bool set_y_offset(size_t output_port_index, size_t y) = 0;

protected:
    GenericGraphicsAdapter() = default;

    bool m_consoles_enabled { false };
};

}
