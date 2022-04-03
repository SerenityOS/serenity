/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Time.h>
#include <Kernel/Process.h>
#include <Kernel/Time/TimeManagement.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$map_time_page()
{
    VERIFY_PROCESS_BIG_LOCK_ACQUIRED(this);
    TRY(require_promise(Pledge::stdio));

    auto& vmobject = TimeManagement::the().time_page_vmobject();

    auto* region = TRY(address_space().allocate_region_with_vmobject(Memory::RandomizeVirtualAddress::Yes, {}, PAGE_SIZE, PAGE_SIZE, vmobject, 0, "Kernel time page"sv, PROT_READ, true));
    return region->vaddr().get();
}

ErrorOr<FlatPtr> Process::sys$clock_gettime(clockid_t clock_id, Userspace<timespec*> user_ts)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));

    TRY(TimeManagement::validate_clock_id(clock_id));

    auto ts = TimeManagement::the().current_time(clock_id).to_timespec();
    TRY(copy_to_user(user_ts, &ts));
    return 0;
}

ErrorOr<FlatPtr> Process::sys$clock_settime(clockid_t clock_id, Userspace<timespec const*> user_ts)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::settime));

    if (!is_superuser())
        return EPERM;

    auto time = TRY(copy_time_from_user(user_ts));

    switch (clock_id) {
    case CLOCK_REALTIME:
        TimeManagement::the().set_epoch_time(time);
        break;
    default:
        return EINVAL;
    }
    return 0;
}

ErrorOr<FlatPtr> Process::sys$clock_nanosleep(Userspace<Syscall::SC_clock_nanosleep_params const*> user_params)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    TRY(require_promise(Pledge::stdio));
    auto params = TRY(copy_typed_from_user(user_params));

    auto requested_sleep = TRY(copy_time_from_user(params.requested_sleep));

    bool is_absolute;
    switch (params.flags) {
    case 0:
        is_absolute = false;
        break;
    case TIMER_ABSTIME:
        is_absolute = true;
        break;
    default:
        return EINVAL;
    }

    TRY(TimeManagement::validate_clock_id(params.clock_id));

    bool was_interrupted;
    if (is_absolute) {
        was_interrupted = Thread::current()->sleep_until(params.clock_id, requested_sleep).was_interrupted();
    } else {
        Time remaining_sleep;
        was_interrupted = Thread::current()->sleep(params.clock_id, requested_sleep, &remaining_sleep).was_interrupted();
        timespec remaining_sleep_ts = remaining_sleep.to_timespec();
        if (was_interrupted && params.remaining_sleep) {
            TRY(copy_to_user(params.remaining_sleep, &remaining_sleep_ts));
        }
    }
    if (was_interrupted)
        return EINTR;
    return 0;
}

ErrorOr<FlatPtr> Process::sys$adjtime(Userspace<timeval const*> user_delta, Userspace<timeval*> user_old_delta)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    if (user_old_delta) {
        timespec old_delta_ts = TimeManagement::the().remaining_epoch_time_adjustment();
        timeval old_delta;
        timespec_to_timeval(old_delta_ts, old_delta);
        TRY(copy_to_user(user_old_delta, &old_delta));
    }

    if (user_delta) {
        TRY(require_promise(Pledge::settime));
        if (!is_superuser())
            return EPERM;
        auto delta = TRY(copy_time_from_user(user_delta));

        // FIXME: Should use AK::Time internally
        TimeManagement::the().set_remaining_epoch_time_adjustment(delta.to_timespec());
    }

    return 0;
}

}
