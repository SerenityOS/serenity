/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/HashMap.h>
#include <RequestServer/Protocol.h>
#include <errno.h>
#include <fcntl.h>
#include <string.h>
#include <unistd.h>

namespace RequestServer {

static HashMap<String, Protocol*>& all_protocols()
{
    static HashMap<String, Protocol*> map;
    return map;
}

Protocol* Protocol::find_by_name(const String& name)
{
    return all_protocols().get(name).value_or(nullptr);
}

Protocol::Protocol(const String& name)
{
    all_protocols().set(name, this);
}

Protocol::~Protocol()
{
    // FIXME: Do proper de-registration.
    VERIFY_NOT_REACHED();
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

}
