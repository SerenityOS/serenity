/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <AK/NonnullOwnPtr.h>
#include <RequestServer/Protocol.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>

namespace RequestServer {

static HashMap<ByteString, NonnullOwnPtr<Protocol>>& all_protocols()
{
    static HashMap<ByteString, NonnullOwnPtr<Protocol>> map;
    return map;
}

Protocol* Protocol::find_by_name(ByteString const& name)
{
    return all_protocols().get(name).map([](auto& p) -> Protocol* { return p; }).value_or(nullptr);
}

Protocol::Protocol(ByteString const& name)
    : m_name(name)
{
}

ErrorOr<Protocol::Pipe> Protocol::get_pipe_for_request()
{
    int fd_pair[2] { 0 };
    if (pipe(fd_pair) != 0) {
        auto saved_errno = errno;
        dbgln("Protocol: pipe() failed: {}", strerror(saved_errno));
        return Error::from_errno(saved_errno);
    }
    fcntl(fd_pair[1], F_SETFL, fcntl(fd_pair[1], F_GETFL) | O_NONBLOCK);
    return Pipe { fd_pair[0], fd_pair[1] };
}

void Protocol::install(NonnullOwnPtr<Protocol> protocol)
{
    auto name = protocol->name();
    all_protocols().set(move(name), move(protocol));
}

}
