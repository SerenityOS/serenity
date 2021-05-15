/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/UDPSocket.h>
#include <errno.h>
#include <sys/socket.h>

#ifndef SOCK_NONBLOCK
#    include <sys/ioctl.h>
#endif

namespace Core {

UDPSocket::UDPSocket(Object* parent)
    : Socket(Socket::Type::UDP, parent)
{
#ifdef SOCK_NONBLOCK
    int fd = socket(AF_INET, SOCK_DGRAM | SOCK_NONBLOCK, 0);
#else
    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    int option = 1;
    ioctl(fd, FIONBIO, &option);
#endif

    if (fd < 0) {
        set_error(errno);
    } else {
        set_fd(fd);
        set_mode(OpenMode::ReadWrite);
        set_error(0);
    }
}

UDPSocket::~UDPSocket()
{
}

}
