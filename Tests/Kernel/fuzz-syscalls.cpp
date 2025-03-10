/*
 * Copyright (c) 2021, Ben Wiederhake <BenWiederhake.GitHub@gmx.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteString.h>
#include <AK/Format.h>
#include <AK/Random.h>
#include <AK/StringBuilder.h>
#include <AK/Vector.h>
#include <Kernel/API/SyscallString.h>
#include <errno.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <syscall.h>

static bool is_deadly_syscall(int fn)
{
    return fn == SC_exit || fn == SC_fork || fn == SC_sigreturn || fn == SC_exit_thread;
}

static bool is_unfuzzable_syscall(int fn)
{
    return fn == SC_dump_backtrace || fn == SC_munmap || fn == SC_kill || fn == SC_killpg;
}

static bool is_nosys_syscall(int fn)
{
    return fn == SC_futex;
}

static bool is_bad_idea(int fn, size_t const* direct_sc_args, size_t const* fake_sc_params, char const* some_string)
{
    switch (fn) {
    case SC_mprotect:
        // This would mess with future tests or crash the fuzzer.
        return direct_sc_args[0] == (size_t)fake_sc_params || direct_sc_args[0] == (size_t)some_string;
    case SC_read:
    case SC_readv:
        // FIXME: Known bug: https://github.com/SerenityOS/serenity/issues/5328
        return direct_sc_args[0] == 1;
    case SC_write:
    case SC_pwritev:
        // FIXME: Known bug: https://github.com/SerenityOS/serenity/issues/5328
        return direct_sc_args[0] == 0;
    case SC_pledge:
        // Equivalent to pledge(nullptr, _), which would kill the fuzzer.
        return direct_sc_args[0] == (size_t)fake_sc_params && fake_sc_params[1] == 0;
    default:
        return false;
    }
}

static void do_systematic_tests()
{
    int rc;

    for (int i = 0; i < Syscall::Function::__Count; ++i) {
        dbgln("Testing syscall #{} ({})", i, Syscall::to_string((Syscall::Function)i));
        if (is_deadly_syscall(i)) {
            dbgln("(skipping deadly syscall)");
            continue;
        }
        // This is pure torture
        rc = syscall(Syscall::Function(i), 0xc0000001, 0xc0000002, 0xc0000003);
        VERIFY(rc != -ENOSYS || is_nosys_syscall(i));
    }

    // Finally, test invalid syscalls:
    dbgln("Testing syscall #{} (n+1)", (int)Syscall::Function::__Count);
    rc = syscall(Syscall::Function::__Count, 0xc0000001, 0xc0000002, 0xc0000003);
    VERIFY(rc == -ENOSYS);
    dbgln("Testing syscall #-1");
    rc = syscall(Syscall::Function(-1), 0xc0000001, 0xc0000002, 0xc0000003);
    VERIFY(rc == -ENOSYS);
}

static void randomize_from(size_t* buffer, size_t len, Vector<size_t> const& values)
{
    for (size_t i = 0; i < len; ++i) {
        buffer[i] = values[get_random_uniform(values.size())];
    }
}

// The largest SC_*_params struct is SC_mmap_params with 9 size_ts (36 bytes on x86, 72 on x86_64).
static constexpr size_t fake_params_count = sizeof(Syscall::SC_mmap_params) / sizeof(size_t);

static void do_weird_call(size_t attempt, int syscall_fn, size_t arg1, size_t arg2, size_t arg3, size_t* fake_params)
{
    // Report to dbg what we're about to do, in case it's interesting:
    StringBuilder builder;
    builder.appendff("#{}: Calling {}({:p}, {:p}, {:p}) with {:p} containing [",
        attempt, Syscall::to_string((Syscall::Function)syscall_fn), arg1, arg2, arg3, fake_params);
    for (size_t i = 0; i < fake_params_count; ++i) {
        if (i != 0)
            builder.append(", "sv);
        builder.appendff("{:p}", fake_params[i]);
    }
    builder.append(']');
    dbgln("{}", builder.to_byte_string());

    // Actually do the syscall ('fake_params' is passed indirectly, if any of arg1, arg2, or arg3 point to it.
    int rc = syscall(Syscall::Function(syscall_fn), arg1, arg2, arg3);
    VERIFY(rc != -ENOSYS || is_nosys_syscall(syscall_fn));
}

static void do_random_tests()
{
    // Make it less likely to kill ourselves due to sys$alarm(1):
    {
        struct sigaction act_ignore = { { SIG_IGN }, 0, 0 };
        int rc = sigaction(SIGALRM, &act_ignore, nullptr);
        VERIFY(rc == 0);
    }

    // Note that we will also make lots of syscalls for randomness and debugging.
    size_t const fuzz_syscall_count = 10000;

    size_t direct_sc_args[3] = { 0 };
    // Isolate to a separate region to make corruption less likely, because we will write to it:
    auto* fake_sc_params = reinterpret_cast<size_t*>(mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANON | MAP_RANDOMIZED, 0, 0));
    char const* some_string = "Hello, world!";
    Vector<size_t> interesting_values = {
        0,
        1,
        reinterpret_cast<size_t>(some_string),
        strlen(some_string),
        reinterpret_cast<size_t>(fake_sc_params),
        0xc0000000,
        0xc0000000 - PAGE_SIZE,
        0xffffffff,
    };
    dbgln("Doing a few random syscalls with:");
    for (auto const& interesting_value : interesting_values) {
        dbgln("  {0} ({0:p})", interesting_value);
    }
    for (size_t i = 0; i < fuzz_syscall_count; ++i) {
        // Construct a nice syscall:
        int syscall_fn = get_random_uniform(Syscall::Function::__Count);
        randomize_from(direct_sc_args, array_size(direct_sc_args), interesting_values);
        randomize_from(fake_sc_params, fake_params_count, interesting_values);

        if (is_deadly_syscall(syscall_fn)
            || is_unfuzzable_syscall(syscall_fn)
            || is_bad_idea(syscall_fn, direct_sc_args, fake_sc_params, some_string)) {
            // Retry, and don't count towards syscall limit.
            --i;
            continue;
        }

        do_weird_call(i, syscall_fn, direct_sc_args[0], direct_sc_args[1], direct_sc_args[2], fake_sc_params);
    }
}

int main()
{
    do_systematic_tests();

    do_random_tests();

    // If the Kernel survived, pass.
    printf("PASS\n");
    return 0;
}
