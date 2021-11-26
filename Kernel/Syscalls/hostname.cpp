/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$gethostname(Userspace<char*> buffer, size_t size)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    REQUIRE_PROMISE(stdio);
    if (size > NumericLimits<ssize_t>::max())
        return EINVAL;
    return hostname().with_shared([&](const auto& name) -> ErrorOr<FlatPtr> {
        if (size < (name.length() + 1))
            return ENAMETOOLONG;
        TRY(copy_to_user(buffer, name.characters(), name.length() + 1));
        return 0;
    });
}

ErrorOr<FlatPtr> Process::sys$sethostname(Userspace<const char*> buffer, size_t length)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this)
    REQUIRE_NO_PROMISES;
    if (!is_superuser())
        return EPERM;
    if (length > 64)
        return ENAMETOOLONG;
    auto new_name = TRY(try_copy_kstring_from_user(buffer, length));
    return hostname().with_exclusive([&](auto& name) -> ErrorOr<FlatPtr> {
        // FIXME: Use KString instead of String here.
        name = new_name->view();
        return 0;
    });
}

}
