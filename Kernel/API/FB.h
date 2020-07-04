/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <sys/cdefs.h>
#include <sys/ioctl.h>

__BEGIN_DECLS

int fb_get_size_in_bytes(int fd, size_t* out)
{
    return ioctl(fd, FB_IOCTL_GET_SIZE_IN_BYTES, out);
}

int fb_get_resolution(int fd, FBResolution* info)
{
    return ioctl(fd, FB_IOCTL_GET_RESOLUTION, info);
}

int fb_set_resolution(int fd, FBResolution* info)
{
    return ioctl(fd, FB_IOCTL_SET_RESOLUTION, info);
}

int fb_get_buffer(int fd, int* index)
{
    return ioctl(fd, FB_IOCTL_GET_BUFFER, index);
}

int fb_set_buffer(int fd, int index)
{
    return ioctl(fd, FB_IOCTL_SET_BUFFER, index);
}

__END_DECLS
