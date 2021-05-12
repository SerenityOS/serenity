/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/TCPSocket.h>
#include <errno.h>
#include <sys/socket.h>

#ifndef SOCK_NONBLOCK
#    include <sys/ioctl.h>
#endif

namespace Core {

TCPSocket::TCPSocket(int fd, Object* parent)
    : Socket(Socket::Type::TCP, parent)
{
    // NOTE: This constructor is used by TCPServer::accept(), so the socket is already connected.
    m_connected = true;
    set_fd(fd);
    set_mode(OpenMode::ReadWrite);
    set_error(0);
}

TCPSocket::TCPSocket(Object* parent)
    : Socket(Socket::Type::TCP, parent)
{
#ifdef SOCK_NONBLOCK
    int fd = socket(AF_INET, SOCK_STREAM | SOCK_NONBLOCK, 0);
#else
    int fd = socket(AF_INET, SOCK_STREAM, 0);
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

TCPSocket::~TCPSocket()
{
}

}
