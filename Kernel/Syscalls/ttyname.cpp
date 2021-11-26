/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/FileSystem/OpenFileDescription.h>
#include <Kernel/Process.h>
#include <Kernel/TTY/MasterPTY.h>
#include <Kernel/TTY/TTY.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$ttyname(int fd, Userspace<char*> buffer, size_t size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(tty);
    auto description = TRY(fds().open_file_description(fd));
    if (!description->is_tty())
        return ENOTTY;
    auto& tty_name = description->tty()->tty_name();
    if (size < tty_name.length() + 1)
        return ERANGE;
    TRY(copy_to_user(buffer, tty_name.characters(), tty_name.length() + 1));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$ptsname(int fd, Userspace<char*> buffer, size_t size)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    REQUIRE_PROMISE(tty);
    auto description = TRY(fds().open_file_description(fd));
    auto* master_pty = description->master_pty();
    if (!master_pty)
        return ENOTTY;
    auto& pts_name = master_pty->pts_name();
    if (size < pts_name.length() + 1)
        return ERANGE;
    TRY(copy_to_user(buffer, pts_name.characters(), pts_name.length() + 1));
    return 0;
}

}
