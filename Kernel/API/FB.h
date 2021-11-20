/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>
#include <stddef.h>
#include <sys/cdefs.h>
#include <sys/ioctl.h>

__BEGIN_DECLS

ALWAYS_INLINE int fb_get_properties(int fd, FBProperties* info)
{
    return ioctl(fd, FB_IOCTL_GET_PROPERTIES, info);
}

ALWAYS_INLINE int fb_get_head_properties(int fd, FBHeadProperties* info)
{
    return ioctl(fd, FB_IOCTL_GET_HEAD_PROPERTIES, info);
}

ALWAYS_INLINE int fb_get_resolution(int fd, FBHeadResolution* info)
{
    FBHeadProperties head_properties;
    head_properties.head_index = info->head_index;
    if (auto rc = ioctl(fd, FB_IOCTL_GET_HEAD_PROPERTIES, &head_properties); rc < 0)
        return rc;
    info->head_index = head_properties.head_index;
    info->pitch = head_properties.pitch;
    info->width = head_properties.width;
    info->height = head_properties.height;
    return 0;
}

ALWAYS_INLINE int fb_set_resolution(int fd, FBHeadResolution* info)
{
    return ioctl(fd, FB_IOCTL_SET_HEAD_RESOLUTION, info);
}

ALWAYS_INLINE int fb_get_head_vertical_offset_buffer(int fd, FBHeadVerticalOffset* vertical_offset)
{
    return ioctl(fd, FB_IOCTL_GET_HEAD_VERTICAL_OFFSET_BUFFER, vertical_offset);
}

ALWAYS_INLINE int fb_set_head_vertical_offset_buffer(int fd, FBHeadVerticalOffset* vertical_offset)
{
    return ioctl(fd, FB_IOCTL_SET_HEAD_VERTICAL_OFFSET_BUFFER, vertical_offset);
}

ALWAYS_INLINE int fb_flush_buffers(int fd, int index, FBRect const* rects, unsigned count)
{
    FBFlushRects fb_flush_rects;
    fb_flush_rects.buffer_index = index;
    fb_flush_rects.count = count;
    fb_flush_rects.rects = rects;
    return ioctl(fd, FB_IOCTL_FLUSH_HEAD_BUFFERS, &fb_flush_rects);
}

__END_DECLS
