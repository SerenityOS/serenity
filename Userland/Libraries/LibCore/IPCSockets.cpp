/*
 * Copyright (c) 2021, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/IPCSockets.h>

#include <AK/String.h>
#include <LibCore/StandardPaths.h>
#include <unistd.h>

#ifdef __serenity__
namespace Core::IPCSockets {

String system_socket_directory()
{
    return "/tmp/portal/system";
}

String user_socket_directory()
{
    return "/tmp/portal/user";
}

String system_socket(StringView const& basename)
{
    return String::formatted("{}/{}", system_socket_directory(), basename);
}

String user_socket(StringView const& basename)
{
    return String::formatted("{}/{}", user_socket_directory(), basename);
}

ErrorOr<void> unveil_system_socket(StringView name)
{
    // NOTE: We probably don't need sockets with other access than rw.
    if (unveil(system_socket(name).characters(), "rw") > 0) {
        perror("unveil_system_socket");
        return Error::from_errno(errno);
    }
    return {};
}

ErrorOr<void> unveil_user_socket(StringView name)
{
    // NOTE: We probably don't need sockets with other access than rw.
    if (unveil(user_socket(name).characters(), "rw") > 0) {
        perror("unveil_user_socket");
        return Error::from_errno(errno);
    }
    return {};
}

}
#endif
