/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ByteString.h>
#include <AK/Function.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/IO.h>
#endif
#include <LibCore/ArgsParser.h>
#include <LibCore/EventReceiver.h>
#include <LibTest/CrashTest.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/wait.h>
#include <syscall.h>
#include <unistd.h>

using Test::Crash;

#if defined(AK_COMPILER_CLANG)
#    pragma clang optimize off
#else
#    pragma GCC optimize("O0")
#endif

int main(int argc, char** argv)
{
    Vector<StringView> arguments;
    arguments.ensure_capacity(argc);
    for (auto i = 0; i < argc; ++i)
        arguments.append({ argv[i], strlen(argv[i]) });

    bool do_all_crash_types = false;
    bool do_segmentation_violation = false;
    // RISC-V does not trap divisions by zero, see M extension version 2.0, subsection 2 and table 1.
#if !ARCH(RISCV64)
    bool do_division_by_zero = false;
#endif
    bool do_illegal_instruction = false;
    bool do_abort = false;
    bool do_write_to_uninitialized_malloc_memory = false;
    bool do_write_to_freed_memory = false;
    bool do_write_to_read_only_memory = false;
    bool do_read_from_uninitialized_malloc_memory = false;
    bool do_read_from_freed_memory = false;
    bool do_invalid_stack_pointer_on_syscall = false;
    bool do_invalid_stack_pointer_on_page_fault = false;
    bool do_syscall_from_writeable_memory = false;
    bool do_legitimate_syscall = false;
    bool do_execute_non_executable_memory = false;
    bool do_use_priviledged_instruction = false;
#if ARCH(X86_64)
    bool do_use_io_instruction = false;
#endif
    bool do_pledge_violation = false;
    bool do_failing_assertion = false;
    bool do_deref_null_refptr = false;

    auto args_parser = Core::ArgsParser();
    args_parser.set_general_help(
        "Exercise error-handling paths of the execution environment "
        "(i.e., Kernel or UE) by crashing in many different ways.");
    args_parser.add_option(do_all_crash_types, "Test that all (except -U) of the following crash types crash as expected (default behavior)", nullptr, 'A');
    args_parser.add_option(do_segmentation_violation, "Perform a segmentation violation by dereferencing an invalid pointer", nullptr, 's');
#if !ARCH(RISCV64)
    args_parser.add_option(do_division_by_zero, "Perform a division by zero", nullptr, 'd');
#endif
    args_parser.add_option(do_illegal_instruction, "Execute an illegal CPU instruction", nullptr, 'i');
    args_parser.add_option(do_abort, "Call `abort()`", nullptr, 'a');
    args_parser.add_option(do_read_from_uninitialized_malloc_memory, "Read a pointer from uninitialized malloc memory, then read from it", nullptr, 'm');
    args_parser.add_option(do_read_from_freed_memory, "Read a pointer from memory freed using `free()`, then read from it", nullptr, 'f');
    args_parser.add_option(do_write_to_uninitialized_malloc_memory, "Read a pointer from uninitialized malloc memory, then write to it", nullptr, 'M');
    args_parser.add_option(do_write_to_freed_memory, "Read a pointer from memory freed using `free()`, then write to it", nullptr, 'F');
    args_parser.add_option(do_write_to_read_only_memory, "Write to read-only memory", nullptr, 'r');
    args_parser.add_option(do_invalid_stack_pointer_on_syscall, "Make a syscall while using an invalid stack pointer", nullptr, 'T');
    args_parser.add_option(do_invalid_stack_pointer_on_page_fault, "Trigger a page fault while using an invalid stack pointer", nullptr, 't');
    args_parser.add_option(do_syscall_from_writeable_memory, "Make a syscall from writeable memory", nullptr, 'S');
    args_parser.add_option(do_legitimate_syscall, "Make a syscall from legitimate memory (but outside syscall-code mapped region)", nullptr, 'y');
    args_parser.add_option(do_execute_non_executable_memory, "Attempt to execute non-executable memory (not mapped with PROT_EXEC)", nullptr, 'X');
    args_parser.add_option(do_use_priviledged_instruction, "Attempt to use a priviledged instruction in user mode. WARNING: This test runs only when invoked manually, see #10042.", nullptr, 'U');
#if ARCH(X86_64)
    args_parser.add_option(do_use_io_instruction, "Use an x86 I/O instruction in userspace", nullptr, 'I');
#endif
    args_parser.add_option(do_pledge_violation, "Violate pledge()'d promises", nullptr, 'p');
    args_parser.add_option(do_failing_assertion, "Perform a failing assertion", nullptr, 'n');
    args_parser.add_option(do_deref_null_refptr, "Dereference a null RefPtr", nullptr, 'R');

    if (argc == 1) {
        do_all_crash_types = true;
    } else if (argc != 2) {
        args_parser.print_usage(stderr, arguments[0]);
        exit(1);
    }

    args_parser.parse(arguments);

    Crash::RunType run_type = do_all_crash_types ? Crash::RunType::UsingChildProcess
                                                 : Crash::RunType::UsingCurrentProcess;
    bool any_failures = false;

    if (do_segmentation_violation || do_all_crash_types) {
        any_failures |= !Crash("Segmentation violation", []() {
            int volatile* crashme = nullptr;
            *crashme = 0xbeef;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

#if !ARCH(RISCV64)
    if (do_division_by_zero || do_all_crash_types) {
        any_failures |= !Crash("Division by zero", []() {
            int volatile lala = 10;
            int volatile zero = 0;
            [[maybe_unused]] int volatile test = lala / zero;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }
#endif

    if (do_illegal_instruction || do_all_crash_types) {
        any_failures |= !Crash("Illegal instruction", []() {
#if ARCH(AARCH64)
            asm volatile("udf #0" :);
#elif ARCH(X86_64)
            asm volatile("ud2" :);
#elif ARCH(RISCV64)
            // Invalid instructions are not required to trap on RISC-V.
            // However, writing to a read-only CSR, which the non-compressed unimp pseudoinstruction
            // gets expanded to, is required to cause an illegal-instruction exception.
            asm volatile(R"(
                .option push
                .option arch, -c
                    unimp
                .option pop
            )" :);
#else
#    error Unknown architecture
#endif
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_abort || do_all_crash_types) {
        any_failures |= !Crash("Abort", []() {
            abort();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_read_from_uninitialized_malloc_memory || do_all_crash_types) {
        any_failures |= !Crash("Read from uninitialized malloc memory", []() {
            auto* uninitialized_memory = (u32 volatile**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            [[maybe_unused]] auto volatile x = uninitialized_memory[0][0];
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_read_from_freed_memory || do_all_crash_types) {
        any_failures |= !Crash("Read from freed memory", []() {
            auto* uninitialized_memory = (u32 volatile**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            free(uninitialized_memory);
#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wuse-after-free"
            [[maybe_unused]] auto volatile x = uninitialized_memory[4][0];
#pragma GCC diagnostic pop
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_write_to_uninitialized_malloc_memory || do_all_crash_types) {
        any_failures |= !Crash("Write to uninitialized malloc memory", []() {
            auto* uninitialized_memory = (u32 volatile**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            uninitialized_memory[4][0] = 1;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_write_to_freed_memory || do_all_crash_types) {
        any_failures |= !Crash("Write to freed memory", []() {
            auto* uninitialized_memory = (u32 volatile**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

#pragma GCC diagnostic push
#pragma GCC diagnostic ignored "-Wuse-after-free"
            free(uninitialized_memory);
            uninitialized_memory[4][0] = 1;
#pragma GCC diagnostic pop
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_write_to_read_only_memory || do_all_crash_types) {
        any_failures |= !Crash("Write to read only memory", []() {
            auto* ptr = (u8*)mmap(nullptr, 4096, PROT_READ | PROT_WRITE, MAP_ANON | MAP_PRIVATE, 0, 0);
            if (ptr == MAP_FAILED)
                return Crash::Failure::UnexpectedError;

            *ptr = 'x'; // This should work fine.
            int rc = mprotect(ptr, 4096, PROT_READ);
            if (rc != 0 || *ptr != 'x')
                return Crash::Failure::UnexpectedError;

            *ptr = 'y'; // This should crash!
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_invalid_stack_pointer_on_syscall || do_all_crash_types) {
        any_failures |= !Crash("Invalid stack pointer on syscall", []() {
            u8* makeshift_stack = (u8*)mmap(nullptr, 0, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_STACK, 0, 0);
            if (!makeshift_stack)
                return Crash::Failure::UnexpectedError;

            u8* makeshift_stack_pointer = makeshift_stack + 2048;
#if ARCH(X86_64)
            asm volatile("mov %%eax, %%esp" ::"a"(makeshift_stack_pointer));
#elif ARCH(AARCH64)
            (void)makeshift_stack_pointer;
            TODO_AARCH64();
#elif ARCH(RISCV64)
            asm volatile("mv sp, %0" :: "r"(makeshift_stack_pointer));
#else
#    error Unknown architecture
#endif
            getuid();
            dbgln("Survived syscall with MAP_STACK stack");

            u8* bad_stack = (u8*)mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
            if (!bad_stack)
                return Crash::Failure::UnexpectedError;

            u8* bad_stack_pointer = bad_stack + 2048;
#if ARCH(X86_64)
            asm volatile("mov %%eax, %%esp" ::"a"(bad_stack_pointer));
#elif ARCH(AARCH64)
            (void)bad_stack_pointer;
            TODO_AARCH64();
#elif ARCH(RISCV64)
            asm volatile("mv sp, %0" :: "r"(bad_stack_pointer));
#else
#    error Unknown architecture
#endif
            getuid();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_invalid_stack_pointer_on_page_fault || do_all_crash_types) {
        any_failures |= !Crash("Invalid stack pointer on page fault", []() {
            u8* bad_stack = (u8*)mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
            if (!bad_stack)
                return Crash::Failure::UnexpectedError;

            u8* bad_stack_pointer = bad_stack + 2048;
#if ARCH(X86_64)
            asm volatile("movq %%rax, %%rsp" ::"a"(bad_stack_pointer));
            asm volatile("pushq $0");
#elif ARCH(AARCH64)
            (void)bad_stack_pointer;
            TODO_AARCH64();
#elif ARCH(RISCV64)
            asm volatile("mv sp, %0" :: "r"(bad_stack_pointer));
            asm volatile("sd zero, (sp)");
#else
#    error Unknown architecture
#endif

            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_syscall_from_writeable_memory || do_all_crash_types) {
        any_failures |= !Crash("Syscall from writable memory", []() {
            u8 buffer[] = { 0xb8, Syscall::SC_getuid, 0, 0, 0, 0xcd, 0x82 };
            ((void (*)())buffer)();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_legitimate_syscall || do_all_crash_types) {
        any_failures |= !Crash("Regular syscall from outside syscall-code mapped region", []() {
            // Since 'crash' is dynamically linked, and DynamicLoader only allows LibSystem to make syscalls, this should kill us:
            Syscall::invoke(Syscall::SC_getuid);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_execute_non_executable_memory || do_all_crash_types) {
        any_failures |= !Crash("Execute non executable memory", []() {
            auto* ptr = (u8*)mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
            if (ptr == MAP_FAILED)
                return Crash::Failure::UnexpectedError;

#if ARCH(X86_64)
            ptr[0] = 0xc3; // ret
#elif ARCH(AARCH64)
            (void)ptr;
            TODO_AARCH64();
#elif ARCH(RISCV64)
            // ret / jalr x0, 0(x1)
            ptr[0] = 0x67;
            ptr[1] = 0x80;
            ptr[2] = 0x00;
            ptr[3] = 0x00;
#else
#    error Unknown architecture
#endif
            typedef void* (*CrashyFunctionPtr)();
            ((CrashyFunctionPtr)ptr)();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_use_priviledged_instruction) {
        any_failures |= !Crash("Use a priviledged instruction in user mode", []() {
#if ARCH(X86_64)
            asm volatile("str %eax");
#elif ARCH(AARCH64)
            TODO_AARCH64();
#elif ARCH(RISCV64)
            asm volatile("sret");
#else
#    error Unknown architecture
#endif
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

#if ARCH(X86_64)
    if (do_use_io_instruction || do_all_crash_types) {
        any_failures |= !Crash("Attempt to use an I/O instruction", [] {
            u8 keyboard_status = IO::in8(0x64);
            outln("Keyboard status: {:#02x}", keyboard_status);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }
#endif

    if (do_pledge_violation || do_all_crash_types) {
        any_failures |= !Crash("Violate pledge()'d promises", [] {
            if (pledge("", nullptr) < 0) {
                perror("pledge");
                return Crash::Failure::DidNotCrash;
            }
            outln("Didn't pledge 'stdio', this should fail!");
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_failing_assertion || do_all_crash_types) {
        any_failures |= !Crash("Perform a failing assertion", [] {
            VERIFY(1 == 2);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_deref_null_refptr || do_all_crash_types) {
        any_failures |= !Crash("Dereference a null RefPtr", [] {
            RefPtr<Core::EventReceiver> p;
            *p;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    return any_failures;
}
