/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/StringView.h>
#include <Kernel/API/Syscall.h>
#include <LibRuntime/System.h>
#include <LibSystem/syscall.h>

namespace Runtime {

namespace {
#ifdef NO_TLS
int s_cached_tid = 0;
#else
thread_local int s_cached_tid = 0;
#endif

constexpr StringView syscall_names[] = {
#define __ENUMERATE_SYSCALL(sys_call, needs_lock) #sys_call##sv,
    ENUMERATE_SYSCALLS(__ENUMERATE_SYSCALL)
#undef __ENUMERATE_SYSCALL
};

template<typename T>
T cast_syscall_ret_to(uintptr_t value)
{
    if constexpr (IsSame<T, void>) {
        VERIFY(value == 0);
    } else {
        static_assert(sizeof(T) <= sizeof(uintptr_t));
        if constexpr (IsPointer<T>) {
            return reinterpret_cast<T>(value);
        } else if constexpr (IsSigned<T>) {
            auto signed_value = static_cast<MakeSigned<uintptr_t>>(value);
            VERIFY(AK::is_within_range<T>(signed_value));
            return static_cast<T>(signed_value);
        } else {
            VERIFY(AK::is_within_range<T>(value));
            return static_cast<T>(value);
        }
    }
}

template<typename T>
ErrorOr<T> syscall_with_errno(Syscall::Function function, auto... args)
{
    static_assert(((sizeof(args) <= sizeof(uintptr_t)) && ...));
    uintptr_t rc = syscall(function, args...);
    if (static_cast<i64>(rc) < 0 && static_cast<i64>(rc) > -EMAXERRNO)
        return Error::from_syscall(syscall_names[function], static_cast<int>(rc));
    if constexpr (IsSame<T, void>) {
        VERIFY(rc == 0);
        return {};
    } else {
        return cast_syscall_ret_to<T>(rc);
    }
}
}

pid_t gettid()
{
    if (s_cached_tid == 0)
        s_cached_tid = cast_syscall_ret_to<pid_t>(syscall(SC_gettid));
    return s_cached_tid;
}

}
