/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/FileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

KResultOr<int> Process::sys$ttyname(int fd, Userspace<char*> buffer, size_t size)
{
    REQUIRE_PROMISE(tty);
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    if (!description->is_tty())
        return ENOTTY;
    auto tty_name = description->tty()->tty_name();
    if (size < tty_name.length() + 1)
        return ERANGE;
    if (!copy_to_user(buffer, tty_name.characters(), tty_name.length() + 1))
        return EFAULT;
    return 0;
}

KResultOr<int> Process::sys$ptsname(int fd, Userspace<char*> buffer, size_t size)
{
    REQUIRE_PROMISE(tty);
    auto description = file_description(fd);
    if (!description)
        return EBADF;
    auto* master_pty = description->master_pty();
    if (!master_pty)
        return ENOTTY;
    auto pts_name = master_pty->pts_name();
    if (size < pts_name.length() + 1)
        return ERANGE;
    if (!copy_to_user(buffer, pts_name.characters(), pts_name.length() + 1))
        return EFAULT;
    return 0;
}

}
