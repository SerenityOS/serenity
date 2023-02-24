/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Tasks/PerformanceEventBuffer.h>
#include <Kernel/Tasks/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$perf_event(int type, FlatPtr arg1, FlatPtr arg2)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    auto* events_buffer = current_perf_events_buffer();
    if (!events_buffer)
        return 0;
    TRY(events_buffer->append(type, arg1, arg2, {}));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$perf_register_string(Userspace<char const*> user_string, size_t user_string_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    auto* events_buffer = current_perf_events_buffer();
    if (!events_buffer)
        return 0;

    auto string = TRY(try_copy_kstring_from_user(user_string, user_string_length));
    return events_buffer->register_string(move(string));
}

}
