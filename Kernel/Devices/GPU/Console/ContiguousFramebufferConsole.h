/*
 * Copyright (c) 2021, Sahan Fernando <sahan.h.fernando@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/GPU/Console/GenericFramebufferConsole.h>

namespace Kernel::Graphics {

class ContiguousFramebufferConsole final : public GenericFramebufferConsole {
public:
    static NonnullLockRefPtr<ContiguousFramebufferConsole> initialize(PhysicalAddress, size_t width, size_t height, size_t pitch);

    virtual void set_resolution(size_t width, size_t height, size_t pitch) override;
    virtual void flush(size_t, size_t, size_t, size_t) override { }

private:
    virtual u8* framebuffer_data() override
    {
        return m_framebuffer_region->vaddr().as_ptr();
    }
    OwnPtr<Memory::Region> m_framebuffer_region;
    ContiguousFramebufferConsole(PhysicalAddress, size_t width, size_t height, size_t pitch);
    PhysicalAddress m_framebuffer_address;
};

}
