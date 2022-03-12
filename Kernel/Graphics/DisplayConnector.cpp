/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/DisplayConnector.h>
#include <Kernel/Graphics/GraphicsManagement.h>
#include <LibC/sys/ioctl_numbers.h>

namespace Kernel {

DisplayConnector::DisplayConnector()
    : CharacterDevice(28, GraphicsManagement::the().allocate_minor_device_number())
{
}

ErrorOr<Memory::Region*> DisplayConnector::mmap(Process&, OpenFileDescription&, Memory::VirtualRange const&, u64, int, bool)
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<size_t> DisplayConnector::read(OpenFileDescription&, u64, UserOrKernelBuffer&, size_t)
{
    return Error::from_errno(ENOTIMPL);
}
ErrorOr<size_t> DisplayConnector::write(OpenFileDescription&, u64 offset, UserOrKernelBuffer const& framebuffer_data, size_t length)
{
    SpinlockLocker locker(m_control_lock);
    // FIXME: We silently ignore the request if we are in console mode.
    // WindowServer is not ready yet to handle errors such as EBUSY currently.
    if (console_mode()) {
        return length;
    }
    return write_to_first_surface(offset, framebuffer_data, length);
}

DisplayConnector::Hardware3DAccelerationCommandSet DisplayConnector::hardware_3d_acceleration_commands_set() const
{
    return Hardware3DAccelerationCommandSet::None;
}

void DisplayConnector::will_be_destroyed()
{
    GraphicsManagement::the().detach_display_connector({}, *this);
    Device::will_be_destroyed();
}

void DisplayConnector::after_inserting()
{
    Device::after_inserting();
    GraphicsManagement::the().attach_new_display_connector({}, *this);
}

bool DisplayConnector::console_mode() const
{
    VERIFY(m_control_lock.is_locked());
    return m_console_mode;
}

void DisplayConnector::set_display_mode(Badge<GraphicsManagement>, DisplayMode mode)
{
    SpinlockLocker locker(m_control_lock);
    m_console_mode = mode == DisplayMode::Console ? true : false;
    if (m_console_mode)
        enable_console();
    else
        disable_console();
}

ErrorOr<void> DisplayConnector::flush_rectangle(size_t, FBRect const&)
{
    return Error::from_errno(ENOTSUP);
}

ErrorOr<void> DisplayConnector::ioctl(OpenFileDescription&, unsigned request, Userspace<void*> arg)
{
    SpinlockLocker locker(m_control_lock);
    if (request != FB_IOCTL_GET_HEAD_EDID) {
        // Allow anyone to query the EDID. Eventually we'll publish the current EDID on /sys
        // so it doesn't really make sense to require the video pledge to query it.
        TRY(Process::current().require_promise(Pledge::video));
    }

    // TODO: We really should have ioctls for destroying resources as well
    switch (request) {
    case FB_IOCTL_GET_PROPERTIES: {
        auto user_properties = static_ptr_cast<FBProperties*>(arg);
        FBProperties properties {};
        properties.flushing_support = flush_support();
        properties.doublebuffer_support = double_framebuffering_capable();
        properties.partial_flushing_support = partial_flush_support();
        properties.refresh_rate_support = refresh_rate_support();
        properties.hardware_3d_acceleration_command_set = to_underlying(hardware_3d_acceleration_commands_set());

        return copy_to_user(user_properties, &properties);
    }
    case FB_IOCTL_GET_HEAD_PROPERTIES: {
        auto user_head_properties = static_ptr_cast<FBHeadProperties*>(arg);
        FBHeadProperties head_properties {};
        TRY(copy_from_user(&head_properties, user_head_properties));

        Resolution current_resolution = TRY(get_resolution());
        head_properties.refresh_rate = 0;
        head_properties.pitch = current_resolution.pitch;
        head_properties.width = current_resolution.width;
        head_properties.height = current_resolution.height;
        head_properties.buffer_length = current_resolution.height * current_resolution.pitch;
        if (double_framebuffering_capable())
            head_properties.buffer_length = head_properties.buffer_length * 2;
        head_properties.offset = 0;
        return copy_to_user(user_head_properties, &head_properties);
    }
    case FB_IOCTL_GET_HEAD_EDID: {
        auto user_head_edid = static_ptr_cast<FBHeadEDID*>(arg);
        FBHeadEDID head_edid {};
        TRY(copy_from_user(&head_edid, user_head_edid));

        auto edid_bytes = TRY(get_edid());
        if (head_edid.bytes != nullptr) {
            // Only return the EDID if a buffer was provided. Either way,
            // we'll write back the bytes_size with the actual size
            if (head_edid.bytes_size < edid_bytes.size()) {
                head_edid.bytes_size = edid_bytes.size();
                TRY(copy_to_user(user_head_edid, &head_edid));
                return Error::from_errno(EOVERFLOW);
            }
            TRY(copy_to_user(head_edid.bytes, (void const*)edid_bytes.data(), edid_bytes.size()));
        }
        head_edid.bytes_size = edid_bytes.size();
        return copy_to_user(user_head_edid, &head_edid);
    }
    case FB_IOCTL_SET_HEAD_RESOLUTION: {
        auto user_resolution = static_ptr_cast<FBHeadResolution const*>(arg);
        auto head_resolution = TRY(copy_typed_from_user(user_resolution));

        if (head_resolution.refresh_rate < 0)
            return Error::from_errno(EINVAL);
        if (head_resolution.width < 0)
            return Error::from_errno(EINVAL);
        if (head_resolution.height < 0)
            return Error::from_errno(EINVAL);
        Resolution requested_resolution;
        requested_resolution.bpp = 32;
        if (refresh_rate_support())
            requested_resolution.refresh_rate = head_resolution.refresh_rate;
        requested_resolution.width = head_resolution.width;
        requested_resolution.height = head_resolution.height;

        TRY(set_resolution(requested_resolution));
        return {};
    }

    case FB_IOCTL_SET_HEAD_VERTICAL_OFFSET_BUFFER: {
        // FIXME: We silently ignore the request if we are in console mode.
        // WindowServer is not ready yet to handle errors such as EBUSY currently.
        if (console_mode()) {
            return {};
        }
        Resolution current_resolution = TRY(get_resolution());

        auto user_head_vertical_buffer_offset = static_ptr_cast<FBHeadVerticalOffset const*>(arg);
        auto head_vertical_buffer_offset = TRY(copy_typed_from_user(user_head_vertical_buffer_offset));

        if (head_vertical_buffer_offset.offsetted < 0 || head_vertical_buffer_offset.offsetted > 1)
            return Error::from_errno(EINVAL);
        TRY(set_y_offset(head_vertical_buffer_offset.offsetted == 0 ? 0 : current_resolution.height));
        if (head_vertical_buffer_offset.offsetted == 0)
            m_vertical_offsetted = false;
        else
            m_vertical_offsetted = true;
        return {};
    }
    case FB_IOCTL_GET_HEAD_VERTICAL_OFFSET_BUFFER: {
        auto user_head_vertical_buffer_offset = static_ptr_cast<FBHeadVerticalOffset*>(arg);
        FBHeadVerticalOffset head_vertical_buffer_offset {};
        TRY(copy_from_user(&head_vertical_buffer_offset, user_head_vertical_buffer_offset));

        head_vertical_buffer_offset.offsetted = m_vertical_offsetted;
        return copy_to_user(user_head_vertical_buffer_offset, &head_vertical_buffer_offset);
    }
    case FB_IOCTL_FLUSH_HEAD_BUFFERS: {
        if (!partial_flush_support())
            return Error::from_errno(ENOTSUP);
        auto user_flush_rects = static_ptr_cast<FBFlushRects const*>(arg);
        auto flush_rects = TRY(copy_typed_from_user(user_flush_rects));
        if (Checked<unsigned>::multiplication_would_overflow(flush_rects.count, sizeof(FBRect)))
            return Error::from_errno(EFAULT);
        MutexLocker locker(m_flushing_lock);
        if (flush_rects.count > 0) {
            for (unsigned i = 0; i < flush_rects.count; i++) {
                FBRect user_dirty_rect;
                TRY(copy_from_user(&user_dirty_rect, &flush_rects.rects[i]));
                TRY(flush_rectangle(flush_rects.buffer_index, user_dirty_rect));
            }
        }
        return {};
    };
    case FB_IOCTL_FLUSH_HEAD: {
        // FIXME: We silently ignore the request if we are in console mode.
        // WindowServer is not ready yet to handle errors such as EBUSY currently.
        if (console_mode())
            return {};
        if (!flush_support())
            return Error::from_errno(ENOTSUP);
        MutexLocker locker(m_flushing_lock);
        TRY(flush_first_surface());
        return {};
    }
    }
    return EINVAL;
}

}
