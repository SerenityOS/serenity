/*
 * Copyright (c) 2021, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Checked.h>
#include <AK/Try.h>
#include <Kernel/Debug.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Graphics/FramebufferDevice.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <Kernel/Memory/AnonymousVMObject.h>
#include <Kernel/Memory/MemoryManager.h>
#include <Kernel/Process.h>
#include <Kernel/Sections.h>
#include <Kernel/StdLib.h>
#include <LibC/errno_numbers.h>

#define MAX_RESOLUTION_WIDTH 4096
#define MAX_RESOLUTION_HEIGHT 2160

namespace Kernel {

KResult GenericFramebufferDevice::verify_head_index(int head_index) const
{
    if (head_index < 0)
        return KResult(EINVAL);
    if (!multihead_support() && head_index > 0)
        return KResult(ENOTSUP);
    return KSuccess;
}

KResult GenericFramebufferDevice::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    REQUIRE_PROMISE(video);
    switch (request) {
    case FB_IOCTL_GET_PROPERTIES: {
        auto user_properties = static_ptr_cast<FBProperties*>(arg);
        FBProperties properties;
        auto adapter = m_graphics_adapter.strong_ref();
        if (!adapter)
            return KResult(EIO);
        properties.multihead_support = multihead_support();
        properties.flushing_support = flushing_support();
        properties.doublebuffer_support = adapter->double_framebuffering_capable();
        properties.partial_flushing_support = partial_flushing_support();
        return copy_to_user(user_properties, &properties);
    }
    case FB_IOCTL_GET_HEAD_PROPERTIES: {
        auto user_head_properties = static_ptr_cast<FBHeadProperties*>(arg);
        FBHeadProperties head_properties;
        TRY(copy_from_user(&head_properties, user_head_properties));
        TRY(verify_head_index(head_properties.head_index));

        head_properties.pitch = TRY(pitch(head_properties.head_index));
        head_properties.width = TRY(width(head_properties.head_index));
        head_properties.height = TRY(height(head_properties.head_index));
        head_properties.buffer_length = TRY(buffer_length(head_properties.head_index));
        head_properties.offset = TRY(vertical_offset(head_properties.head_index));
        return copy_to_user(user_head_properties, &head_properties);
    }
    case FB_IOCTL_SET_HEAD_RESOLUTION: {
        auto user_head_resolution = static_ptr_cast<FBHeadResolution*>(arg);
        FBHeadResolution head_resolution;
        TRY(copy_from_user(&head_resolution, user_head_resolution));
        TRY(verify_head_index(head_resolution.head_index));

        if (head_resolution.pitch < 0)
            return KResult(EINVAL);
        if (head_resolution.width < 0)
            return KResult(EINVAL);
        if (head_resolution.height < 0)
            return KResult(EINVAL);
        TRY(set_head_resolution(head_resolution.head_index, head_resolution.width, head_resolution.height, head_resolution.pitch));
        return KSuccess;
    }
    case FB_IOCTL_SET_HEAD_VERTICAL_OFFSET_BUFFER: {
        auto user_head_vertical_buffer_offset = static_ptr_cast<FBHeadVerticalOffset*>(arg);
        FBHeadVerticalOffset head_vertical_buffer_offset;
        TRY(copy_from_user(&head_vertical_buffer_offset, user_head_vertical_buffer_offset));
        TRY(verify_head_index(head_vertical_buffer_offset.head_index));

        if (head_vertical_buffer_offset.offseted < 0 || head_vertical_buffer_offset.offseted > 1)
            return KResult(EINVAL);
        TRY(set_head_buffer(head_vertical_buffer_offset.head_index, head_vertical_buffer_offset.offseted));
        return KSuccess;
    }
    case FB_IOCTL_GET_HEAD_VERTICAL_OFFSET_BUFFER: {
        auto user_head_vertical_buffer_offset = static_ptr_cast<FBHeadVerticalOffset*>(arg);
        FBHeadVerticalOffset head_vertical_buffer_offset;
        TRY(copy_from_user(&head_vertical_buffer_offset, user_head_vertical_buffer_offset));
        TRY(verify_head_index(head_vertical_buffer_offset.head_index));

        head_vertical_buffer_offset.offseted = TRY(vertical_offseted(head_vertical_buffer_offset.head_index));
        return copy_to_user(user_head_vertical_buffer_offset, &head_vertical_buffer_offset);
    }
    case FB_IOCTL_FLUSH_HEAD_BUFFERS: {
        if (!partial_flushing_support())
            return KResult(ENOTSUP);
        auto user_flush_rects = static_ptr_cast<FBFlushRects*>(arg);
        FBFlushRects flush_rects;
        TRY(copy_from_user(&flush_rects, user_flush_rects));
        if (Checked<unsigned>::multiplication_would_overflow(flush_rects.count, sizeof(FBRect)))
            return KResult(EFAULT);
        MutexLocker locker(m_flushing_lock);
        if (flush_rects.count > 0) {
            for (unsigned i = 0; i < flush_rects.count; i++) {
                FBRect user_dirty_rect;
                TRY(copy_from_user(&user_dirty_rect, &flush_rects.rects[i]));
                TRY(flush_rectangle(flush_rects.buffer_index, user_dirty_rect));
            }
        }
        return KSuccess;
    };
    case FB_IOCTL_FLUSH_HEAD: {
        if (!flushing_support())
            return KResult(ENOTSUP);
        // Note: We accept a FBRect, but we only really care about the head_index value.
        auto user_rect = static_ptr_cast<FBRect*>(arg);
        FBRect rect;
        TRY(copy_from_user(&rect, user_rect));
        TRY(verify_head_index(rect.head_index));

        TRY(flush_head_buffer(rect.head_index));
        return KSuccess;
    }
    default:
        return EINVAL;
    };
}

GenericFramebufferDevice::GenericFramebufferDevice(const GenericGraphicsAdapter& adapter)
    : BlockDevice(29, GraphicsManagement::the().allocate_minor_device_number())
    , m_graphics_adapter(adapter)
{
}

}
