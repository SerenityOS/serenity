/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Platform.h>
#include <AK/ScopeGuard.h>
#include <fcntl.h>
#include <stddef.h>
#include <stdio.h>
#include <sys/cdefs.h>
#include <sys/ioctl.h>
#include <sys/stat.h>
#include <sys/sysmacros.h>
#include <unistd.h>

__BEGIN_DECLS

ALWAYS_INLINE int graphics_connector_get_properties(int fd, GraphicsConnectorProperties* info)
{
    return ioctl(fd, GRAPHICS_IOCTL_GET_PROPERTIES, info);
}

ALWAYS_INLINE int graphics_connector_get_head_edid(int fd, GraphicsHeadEDID* info)
{
    // FIXME: Optimize this function to get a minor number instead of a file descriptor.
    struct stat display_connector_stat;
    if (auto rc = fstat(fd, &display_connector_stat); rc < 0) {
        return rc;
    }
    auto minor_number = minor(display_connector_stat.st_rdev);

    auto edid_fd = open(ByteString::formatted("/sys/devices/graphics/connectors/{}/edid", minor_number).characters(), O_RDONLY);
    if (edid_fd < 0) {
        return edid_fd;
    }

    ScopeGuard close_on_return([&]() {
        close(edid_fd);
    });

    if (auto rc = read(edid_fd, info->bytes, info->bytes_size); rc < 0) {
        return rc;
    }

    return 0;
}

ALWAYS_INLINE int graphics_connector_set_responsible(int fd)
{
    return ioctl(fd, GRAPHICS_IOCTL_SET_RESPONSIBLE, nullptr);
}

ALWAYS_INLINE int graphics_connector_unset_responsible(int fd)
{
    return ioctl(fd, GRAPHICS_IOCTL_UNSET_RESPONSIBLE, nullptr);
}

ALWAYS_INLINE int fb_get_head_vertical_offset_buffer(int fd, GraphicsHeadVerticalOffset* vertical_offset)
{
    return ioctl(fd, GRAPHICS_IOCTL_GET_HEAD_VERTICAL_OFFSET_BUFFER, vertical_offset);
}

ALWAYS_INLINE int fb_set_head_vertical_offset_buffer(int fd, GraphicsHeadVerticalOffset* vertical_offset)
{
    return ioctl(fd, GRAPHICS_IOCTL_SET_HEAD_VERTICAL_OFFSET_BUFFER, vertical_offset);
}

ALWAYS_INLINE int graphics_connector_set_head_mode_setting(int fd, GraphicsHeadModeSetting* mode_setting)
{
    return ioctl(fd, GRAPHICS_IOCTL_SET_HEAD_MODE_SETTING, mode_setting);
}

ALWAYS_INLINE int graphics_connector_set_safe_head_mode_setting(int fd)
{
    return ioctl(fd, GRAPHICS_IOCTL_SET_SAFE_HEAD_MODE_SETTING, nullptr);
}

ALWAYS_INLINE int graphics_connector_get_head_mode_setting(int fd, GraphicsHeadModeSetting* mode_setting)
{
    GraphicsHeadModeSetting head_mode_setting;
    if (auto rc = ioctl(fd, GRAPHICS_IOCTL_GET_HEAD_MODE_SETTING, &head_mode_setting); rc < 0)
        return rc;
    mode_setting->horizontal_stride = head_mode_setting.horizontal_stride;
    mode_setting->pixel_clock_in_khz = head_mode_setting.pixel_clock_in_khz;
    mode_setting->horizontal_active = head_mode_setting.horizontal_active;
    mode_setting->horizontal_front_porch_pixels = head_mode_setting.horizontal_front_porch_pixels;
    mode_setting->horizontal_sync_time_pixels = head_mode_setting.horizontal_sync_time_pixels;
    mode_setting->horizontal_blank_pixels = head_mode_setting.horizontal_blank_pixels;
    mode_setting->vertical_active = head_mode_setting.vertical_active;
    mode_setting->vertical_front_porch_lines = head_mode_setting.vertical_front_porch_lines;
    mode_setting->vertical_sync_time_lines = head_mode_setting.vertical_sync_time_lines;
    mode_setting->vertical_blank_lines = head_mode_setting.vertical_blank_lines;
    mode_setting->horizontal_offset = head_mode_setting.horizontal_offset;
    mode_setting->vertical_offset = head_mode_setting.vertical_offset;
    return 0;
}

ALWAYS_INLINE int fb_flush_buffers(int fd, int index, FBRect const* rects, unsigned count)
{
    FBFlushRects fb_flush_rects;
    fb_flush_rects.buffer_index = index;
    fb_flush_rects.count = count;
    fb_flush_rects.rects = rects;
    return ioctl(fd, GRAPHICS_IOCTL_FLUSH_HEAD_BUFFERS, &fb_flush_rects);
}

ALWAYS_INLINE int fb_flush_head(int fd)
{
    return ioctl(fd, GRAPHICS_IOCTL_FLUSH_HEAD, nullptr);
}

__END_DECLS
