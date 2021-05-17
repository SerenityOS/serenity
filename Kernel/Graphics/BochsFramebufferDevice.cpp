/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Singleton.h>
#include <Kernel/Debug.h>
#include <Kernel/Graphics/Bochs.h>
#include <Kernel/Graphics/BochsFramebufferDevice.h>
#include <Kernel/IO.h>
#include <Kernel/PCI/Access.h>
#include <Kernel/Process.h>
#include <LibC/errno_numbers.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

UNMAP_AFTER_INIT NonnullRefPtr<BochsFramebufferDevice> BochsFramebufferDevice::create(const BochsGraphicsAdapter& adapter, PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
{
    return adopt_ref(*new BochsFramebufferDevice(adapter, framebuffer_address, pitch, width, height));
}

UNMAP_AFTER_INIT BochsFramebufferDevice::BochsFramebufferDevice(const BochsGraphicsAdapter& adapter, PhysicalAddress framebuffer_address, size_t width, size_t height, size_t pitch)
    : FramebufferDevice(framebuffer_address, width, height, pitch)
    , m_bochs_adapter(adapter)
{
    m_bochs_adapter->set_safe_resolution();
    m_framebuffer_width = 1024;
    m_framebuffer_height = 768;
    m_framebuffer_pitch = m_framebuffer_width * sizeof(u32);
}

void BochsFramebufferDevice::set_y_offset(size_t y_offset)
{
    VERIFY(y_offset == 0 || y_offset == m_framebuffer_height);
    m_y_offset = y_offset;
    m_bochs_adapter->set_y_offset(y_offset);
}

int BochsFramebufferDevice::ioctl(FileDescription&, unsigned request, FlatPtr arg)
{
    REQUIRE_PROMISE(video);
    switch (request) {
    case FB_IOCTL_GET_SIZE_IN_BYTES: {
        auto* out = (size_t*)arg;
        size_t value = framebuffer_size_in_bytes();
        if (!copy_to_user(out, &value))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_GET_BUFFER: {
        auto* index = (int*)arg;
        int value = m_y_offset == 0 ? 0 : 1;
        if (!copy_to_user(index, &value))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_SET_BUFFER: {
        if (arg != 0 && arg != 1)
            return -EINVAL;
        set_y_offset(arg == 0 ? 0 : m_framebuffer_height);
        return 0;
    }
    case FB_IOCTL_GET_RESOLUTION: {
        auto* user_resolution = (FBResolution*)arg;
        FBResolution resolution;
        resolution.pitch = m_framebuffer_pitch;
        resolution.width = m_framebuffer_width;
        resolution.height = m_framebuffer_height;
        if (!copy_to_user(user_resolution, &resolution))
            return -EFAULT;
        return 0;
    }
    case FB_IOCTL_SET_RESOLUTION: {
        auto* user_resolution = (FBResolution*)arg;
        FBResolution resolution;
        if (!copy_from_user(&resolution, user_resolution))
            return -EFAULT;
        if (resolution.width > MAX_RESOLUTION_WIDTH || resolution.height > MAX_RESOLUTION_HEIGHT)
            return -EINVAL;

        if (!m_bochs_adapter->set_resolution(resolution.width, resolution.height)) {
            m_framebuffer_pitch = m_framebuffer_width * sizeof(u32);
            dbgln_if(BXVGA_DEBUG, "Reverting resolution: [{}x{}]", m_framebuffer_width, m_framebuffer_height);
            // Note: We try to revert everything back, and if it doesn't work, just assert.
            if (!m_bochs_adapter->set_resolution(m_framebuffer_width, m_framebuffer_height)) {
                VERIFY_NOT_REACHED();
            }
            resolution.pitch = m_framebuffer_pitch;
            resolution.width = m_framebuffer_width;
            resolution.height = m_framebuffer_height;
            if (!copy_to_user(user_resolution, &resolution))
                return -EFAULT;
            return -EINVAL;
        }
        m_framebuffer_width = resolution.width;
        m_framebuffer_height = resolution.height;
        m_framebuffer_pitch = m_framebuffer_width * sizeof(u32);

        dbgln_if(BXVGA_DEBUG, "New resolution: [{}x{}]", m_framebuffer_width, m_framebuffer_height);
        resolution.pitch = m_framebuffer_pitch;
        resolution.width = m_framebuffer_width;
        resolution.height = m_framebuffer_height;
        if (!copy_to_user(user_resolution, &resolution))
            return -EFAULT;
        return 0;
    }
    default:
        return -EINVAL;
    };
}
}
