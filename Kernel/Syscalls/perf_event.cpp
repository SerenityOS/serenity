/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/PerformanceEventBuffer.h>
#include <Kernel/Process.h>

namespace Kernel {

KResultOr<FlatPtr> Process::sys$perf_event(int type, FlatPtr arg1, FlatPtr arg2)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto events_buffer = current_perf_events_buffer();
    if (!events_buffer) {
        if (!create_perf_events_buffer_if_needed())
            return ENOMEM;
        events_buffer = perf_events();
    }
    return events_buffer->append(type, arg1, arg2, nullptr);
}

KResultOr<FlatPtr> Process::sys$perf_register_string(FlatPtr string_id, Userspace<char const*> user_string, size_t user_string_length)
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this)
    auto* events_buffer = current_perf_events_buffer();
    if (!events_buffer)
        return KSuccess;

    auto string_or_error = try_copy_kstring_from_user(user_string, user_string_length);
    if (string_or_error.is_error())
        return string_or_error.error();

    return events_buffer->register_string(string_id, string_or_error.release_value());
}

}
