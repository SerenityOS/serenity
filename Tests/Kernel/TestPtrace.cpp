/*
 * Copyright (c) 2026, the SerenityOS developers.
 * Copyright (c) 2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Atomic.h>
#include <LibCore/System.h>
#include <LibTest/TestCase.h>
#include <errno.h>
#include <pthread.h>
#include <signal.h>
#include <sys/arch/regs.h>
#include <sys/ptrace.h>
#include <unistd.h>

static Atomic<bool> s_exit_thread = false;

static void* thread_entry(void*)
{
    while (!s_exit_thread)
        continue;

    return nullptr;
}

TEST_CASE(ptrace_self_attach_fail)
{
    pthread_t thread = 0;
    VERIFY(pthread_create(&thread, nullptr, thread_entry, nullptr) == 0);
    VERIFY(thread > 0);

    int ptrace_return = ptrace(PT_ATTACH, thread, 0, 0);
    int error = errno;
    EXPECT_EQ(ptrace_return, -1);
    EXPECT_EQ(error, EPERM);

    s_exit_thread = true;
    VERIFY(pthread_join(thread, nullptr) == 0);
}

static void wait_for_child_stop(pid_t child_pid, int expected_signal)
{
    auto wait_result = TRY_OR_FAIL(Core::System::waitpid(child_pid, WSTOPPED));
    EXPECT(WIFSTOPPED(wait_result.status));
    EXPECT_EQ(WSTOPSIG(wait_result.status), expected_signal);
}

static void wait_for_child_exit(pid_t child_pid)
{
    auto wait_result = TRY_OR_FAIL(Core::System::waitpid(child_pid, 0));
    EXPECT(WIFSIGNALED(wait_result.status));
    EXPECT_EQ(WTERMSIG(wait_result.status), SIGTERM);
}

TEST_CASE(ptrace_trace_me)
{
    auto child_pid = TRY_OR_FAIL(Core::System::fork());

    if (child_pid == 0) {
        TRY_OR_FAIL(Core::System::ptrace(PT_TRACE_ME, 0, 0, 0));
        raise(SIGSTOP);

        for (;;)
            pause();

        VERIFY_NOT_REACHED();
    }

    wait_for_child_stop(child_pid, SIGSTOP);

    TRY_OR_FAIL(Core::System::ptrace(PT_ATTACH, child_pid, 0, 0));
    wait_for_child_stop(child_pid, SIGSTOP);

    TRY_OR_FAIL(Core::System::ptrace(PT_DETACH, child_pid, 0, 0));

    TRY_OR_FAIL(Core::System::kill(child_pid, SIGTERM));
    wait_for_child_exit(child_pid);
}

TEST_CASE(ptrace_attach_and_detach)
{
    auto child_pid = TRY_OR_FAIL(Core::System::fork());

    if (child_pid == 0) {
        for (;;)
            pause();

        VERIFY_NOT_REACHED();
    }

    TRY_OR_FAIL(Core::System::ptrace(PT_ATTACH, child_pid, 0, 0));
    wait_for_child_stop(child_pid, SIGSTOP);

    TRY_OR_FAIL(Core::System::ptrace(PT_DETACH, child_pid, 0, 0));

    TRY_OR_FAIL(Core::System::kill(child_pid, SIGTERM));
    wait_for_child_exit(child_pid);
}

template<CallableAs<void> ChildFn, CallableAs<void, pid_t> ParentFn>
static void run_generic_test(ChildFn child_callback, ParentFn parent_callback)
{
    auto child_pid = TRY_OR_FAIL(Core::System::fork());

    if (child_pid == 0) {
        TRY_OR_FAIL(Core::System::ptrace(PT_TRACE_ME, 0, 0, 0));
        raise(SIGSTOP);

        child_callback();

        for (;;)
            pause();

        VERIFY_NOT_REACHED();
    }

    wait_for_child_stop(child_pid, SIGSTOP);

    TRY_OR_FAIL(Core::System::ptrace(PT_ATTACH, child_pid, 0, 0));
    wait_for_child_stop(child_pid, SIGSTOP);

    TRY_OR_FAIL(Core::System::ptrace(PT_CONTINUE, child_pid, 0, 0));

    parent_callback(child_pid);

    TRY_OR_FAIL(Core::System::ptrace(PT_DETACH, child_pid, 0, 0));

    TRY_OR_FAIL(Core::System::kill(child_pid, SIGTERM));
    wait_for_child_exit(child_pid);
}

TEST_CASE(ptrace_breakpoint_instruction)
{
    run_generic_test(
        [] {
#if ARCH(X86_64)
            asm volatile(R"(
                int3
            )" :);
#elif ARCH(AARCH64)
            asm volatile(R"(
                brk #0
            )" :);
#elif ARCH(RISCV64)
            asm volatile(R"(
                ebreak
            )" :);
#else
#    error Unknown architecture
#endif
        },
        [](pid_t child_pid) {
            wait_for_child_stop(child_pid, SIGTRAP);
        });
}

TEST_CASE(ptrace_ebusy)
{
    run_generic_test(
        [] {
            // Do nothing.
        },
        [](pid_t child_pid) {
            PtraceRegisters regs = {};
            auto res = Core::System::ptrace(PT_GETREGS, child_pid, &regs, 0);
            EXPECT(res.is_error());
            EXPECT_EQ(res.error().code(), EBUSY);

            TRY_OR_FAIL(Core::System::kill(child_pid, SIGSTOP));
            wait_for_child_stop(child_pid, SIGSTOP);

            res = Core::System::ptrace(PT_GETREGS, child_pid, &regs, 0);
            EXPECT(!res.is_error());
        });
}

TEST_CASE(ptrace_read_register)
{
    run_generic_test(
        [] {
#if ARCH(X86_64)
            asm volatile(R"(
                mov $42, %%eax
                int3
            )" ::: "rax");
#elif ARCH(AARCH64)
            asm volatile(R"(
                mov x0, #42
                brk #0
            )" ::: "x0");
#elif ARCH(RISCV64)
            asm volatile(R"(
                li x5, 42
                ebreak
            )" ::: "x5");
#else
#    error Unknown architecture
#endif
        },
        [](pid_t child_pid) {
            wait_for_child_stop(child_pid, SIGTRAP);

            PtraceRegisters regs = {};
            TRY_OR_FAIL(Core::System::ptrace(PT_GETREGS, child_pid, &regs, 0));
#if ARCH(X86_64)
            EXPECT_EQ(regs.rax, 42u);
#elif ARCH(AARCH64)
            EXPECT_EQ(regs.x[0], 42u);
#elif ARCH(RISCV64)
            EXPECT_EQ(regs.x[4], 42u);
#else
#    error Unknown architecture
#endif
        });
}

TEST_CASE(ptrace_write_register)
{
    run_generic_test(
        [] {
#if ARCH(X86_64)
            asm volatile(R"(
                mov $0, %%eax
                int3
                mov %%eax, %%ebx
                int3
            )" ::: "rax", "rbx");
#elif ARCH(AARCH64)
            asm volatile(R"(
                mov x0, #0
                brk #0
                mov x1, x0
                brk #0
            )" ::: "x0", "x1");
#elif ARCH(RISCV64)
            asm volatile(R"(
                li x5, 0
                ebreak
                mv x6, x5
                ebreak
            )" ::: "x5", "x6");
#else
#    error Unknown architecture
#endif
        },
        [](pid_t child_pid) {
            wait_for_child_stop(child_pid, SIGTRAP);

            PtraceRegisters regs = {};
            TRY_OR_FAIL(Core::System::ptrace(PT_GETREGS, child_pid, &regs, 0));

#if ARCH(X86_64)
            regs.rax = 123;
#elif ARCH(AARCH64)
            regs.x[0] = 123;
#elif ARCH(RISCV64)
            regs.x[4] = 123;
#else
#    error Unknown architecture
#endif
            TRY_OR_FAIL(Core::System::ptrace(PT_SETREGS, child_pid, &regs, 0));

            TRY_OR_FAIL(Core::System::ptrace(PT_CONTINUE, child_pid, 0, 0));

            wait_for_child_stop(child_pid, SIGTRAP);

            TRY_OR_FAIL(Core::System::ptrace(PT_GETREGS, child_pid, &regs, 0));
#if ARCH(X86_64)
            EXPECT_EQ(regs.rbx, 123u);
#elif ARCH(AARCH64)
            EXPECT_EQ(regs.x[1], 123u);
#elif ARCH(RISCV64)
            EXPECT_EQ(regs.x[5], 123u);
#else
#    error Unknown architecture
#endif
        });
}

TEST_CASE(ptrace_single_step)
{
#if ARCH(RISCV64)
    dbgln("FIXME: Support PT_SINGLESTEP on RISC-V.");
    return;
#endif

    run_generic_test(
        [] {
#if ARCH(X86_64)
            asm volatile(R"(
                mov $0, %%eax
                int3
                mov $42, %%eax
                mov $123, %%eax
            )" ::: "rax");
#elif ARCH(AARCH64)
            asm volatile(R"(
                mov x0, #0
                brk #0
                mov x0, #42
                mov x0, #123
            )" ::: "x0");
#elif ARCH(RISCV64)
            asm volatile(R"(
                li x5, 0
                ebreak
                li x5, 42
                li x5, 123
            )" ::: "x5");
#else
#    error Unknown architecture
#endif
        },
        [](pid_t child_pid) {
            wait_for_child_stop(child_pid, SIGTRAP);

            PtraceRegisters regs = {};
            TRY_OR_FAIL(Core::System::ptrace(PT_GETREGS, child_pid, &regs, 0));
#if ARCH(X86_64)
            EXPECT_EQ(regs.rax, 0u);
#elif ARCH(AARCH64)
            EXPECT_EQ(regs.x[0], 0u);
#elif ARCH(RISCV64)
            EXPECT_EQ(regs.x[4], 0u);
#else
#    error Unknown architecture
#endif

            TRY_OR_FAIL(Core::System::ptrace(PT_SINGLESTEP, child_pid, 0, 0));
            TRY_OR_FAIL(Core::System::ptrace(PT_CONTINUE, child_pid, 0, 0));

            wait_for_child_stop(child_pid, SIGTRAP);

            TRY_OR_FAIL(Core::System::ptrace(PT_GETREGS, child_pid, &regs, 0));
#if ARCH(X86_64)
            EXPECT_EQ(regs.rax, 42u);
#elif ARCH(AARCH64)
            EXPECT_EQ(regs.x[0], 42u);
#elif ARCH(RISCV64)
            EXPECT_EQ(regs.x[4], 42u);
#else
#    error Unknown architecture
#endif

            TRY_OR_FAIL(Core::System::ptrace(PT_SINGLESTEP, child_pid, 0, 0));
            TRY_OR_FAIL(Core::System::ptrace(PT_CONTINUE, child_pid, 0, 0));

            wait_for_child_stop(child_pid, SIGTRAP);

            TRY_OR_FAIL(Core::System::ptrace(PT_GETREGS, child_pid, &regs, 0));
#if ARCH(X86_64)
            EXPECT_EQ(regs.rax, 123u);
#elif ARCH(AARCH64)
            EXPECT_EQ(regs.x[0], 123u);
#elif ARCH(RISCV64)
            EXPECT_EQ(regs.x[4], 123u);
#else
#    error Unknown architecture
#endif

#if ARCH(X86_64)
            // FIXME: This is very awkward.
            //        The kernel should probably clear the trap flag for us, like we do on AArch64.
            regs.rflags &= ~0x100;
            TRY_OR_FAIL(Core::System::ptrace(PT_SETREGS, child_pid, &regs, 0));
#endif
        });
}

// FIXME: Add tests for:
//        - PT_SYSCALL
//        - PT_PEEK
//        - PT_POKE
//        - PT_PEEKDEBUG
//        - PT_POKEDEBUG
//        - ptrace_peekbuf()
