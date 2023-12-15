/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <AK/Error.h>
#include <AK/StringView.h>
#include <Kernel/API/POSIX/sys/auxv.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/API/prctl_numbers.h>
#include <LibRuntime/Serenity/PosixThreadSupport.h>
#include <LibRuntime/System.h>
#include <LibSystem/syscall.h>
#include <sys/internals.h>
#include <unistd.h>

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

ErrorOr<void> close(FileDescriptor fd)
{
    __pthread_maybe_cancel();
    return syscall_with_errno<void>(SC_close, fd.value());
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

Optional<long> getauxval(long type)
{
    auxv_t* auxvp = (auxv_t*)__auxiliary_vector;
    for (; auxvp->a_type != AT_NULL; ++auxvp) {
        if (auxvp->a_type == type)
            return auxvp->a_un.a_val;
    }
    return {};
}

Optional<StringView> getenv(StringView name)
{
    for (size_t i = 0; environ[i]; ++i) {
        char const* decl = environ[i];
        char* eq = strchr(decl, '=');
        if (!eq)
            continue;
        if (name == StringView { decl, static_cast<size_t>(eq - decl) })
            return { { eq + 1, strlen(eq + 1) } };
    }
    return {};
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

ErrorOr<bool> isatty(FileDescriptor fd)
{
    __pthread_maybe_cancel();
    return syscall_with_errno<bool>(SC_fcntl, fd.value(), F_ISTTY);
}

ErrorOr<off_t> lseek(FileDescriptor fd, off_t offset, SeekWhence whence)
{
    VERIFY(TRY(syscall_with_errno<FlatPtr>(SC_lseek, fd.value(), &offset, whence)) == 0);
    return offset;
}

ErrorOr<bool> madvise_set_volatile(void* address, size_t size, bool is_volatile)
{
    return syscall_with_errno<bool>(SC_madvise, address, size, is_volatile ? MADV_SET_VOLATILE : MADV_SET_NONVOLATILE);
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

ErrorOr<void> perf_event(int type, uintptr_t arg1, FlatPtr arg2)
{
    return syscall_with_errno<void>(SC_perf_event, type, arg1, arg2);
}

ErrorOr<size_t> read(FileDescriptor fd, void* buffer, size_t count)
{
    __pthread_maybe_cancel();
    return syscall_with_errno<size_t>(SC_read, fd.value(), buffer, count);
}

Optional<StringView> secure_getenv(StringView name)
{
    if (getauxval(AT_SECURE).value_or(0))
        return {};
    return getenv(name);
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

ErrorOr<size_t> write(FileDescriptor fd, void const* buffer, size_t count)
{
    __pthread_maybe_cancel();
    return syscall_with_errno<size_t>(SC_write, fd.value(), buffer, count);
}

}
