/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Error.h>
#include <AK/StringView.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/API/prctl_numbers.h>
#include <LibRuntime/System.h>
#include <LibSystem/syscall.h>

namespace Runtime {

namespace {
#ifdef NO_TLS
int s_cached_tid = 0;
#else
thread_local int s_cached_tid = 0;
#endif
Atomic<int> s_cached_pid = 0;

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

void dbgputstr(StringArgument const& string)
{
    auto [characters, length] = string.get();
    VERIFY(syscall(SC_dbgputstr, characters, length) == length);
}

ErrorOr<void> get_process_name(StringBuilder& result)
{
    TRY(result.try_append_unknown_length(32, [&](Bytes buffer) -> ErrorOr<size_t> {
        // FIXME: Why doesn't it return the length of the name?
        TRY(syscall_with_errno<void>(SC_prctl, PR_GET_PROCESS_NAME, buffer.data(), buffer.size(), nullptr));
        return strlen(reinterpret_cast<char*>(buffer.data()));
    }));
    return {};
}

StackBounds get_stack_bounds()
{
    StackBounds result;
    // get_stack_bounds will fail only if we provide invalid pointers. And if pointers to a stack
    // variable turn out to be invalid, something went horribly wrong, so we better off crashing.
    VERIFY(syscall(SC_get_stack_bounds, &result.user_stack_base, &result.user_stack_size) == 0);
    return result;
}

pid_t getpid()
{
    if (s_cached_pid == 0)
        s_cached_pid = cast_syscall_ret_to<pid_t>(syscall(SC_getpid));
    return s_cached_pid;
}

pid_t gettid()
{
    if (s_cached_tid == 0)
        s_cached_tid = cast_syscall_ret_to<pid_t>(syscall(SC_gettid));
    return s_cached_tid;
}

ErrorOr<void*> mmap(void* address, size_t size, RegionAccess access, MMap flags, StringView name, FileDescriptor fd, off_t offset, size_t alignment)
{
    Syscall::SC_mmap_params params {
        .addr = address,
        .size = size,
        .alignment = alignment,
        .prot = to_underlying(access),
        .flags = to_underlying(flags),
        .fd = fd.value(),
        .offset = offset,
        .name = { name.characters_without_null_termination(), name.length() }
    };
    return syscall_with_errno<void*>(SC_mmap, &params);
}

ErrorOr<void> mprotect(void* address, size_t size, RegionAccess access)
{
    return syscall_with_errno<void>(SC_mprotect, address, size, access);
}

ErrorOr<void> munmap(void* address, size_t size)
{
    return syscall_with_errno<void>(SC_munmap, address, size);
}

ErrorOr<void> set_mmap_name(void* address, size_t size, StringView name)
{
    Syscall::SC_set_mmap_name_params params {
        .addr = address,
        .size = size,
        .name = { name.characters_without_null_termination(), name.length() }
    };
    return syscall_with_errno<void>(SC_set_mmap_name, &params);
}

}
