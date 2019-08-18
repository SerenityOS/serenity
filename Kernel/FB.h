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
