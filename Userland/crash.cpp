/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2019-2020, Shannon Booth <shannon.ml.booth@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Function.h>
#include <AK/String.h>
#include <Kernel/Syscall.h>
#include <LibBareMetal/IO.h>
#include <LibCore/ArgsParser.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/wait.h>

#pragma GCC optimize("O0")

class Crash {
public:
    enum class RunType {
        UsingChildProcess,
        UsingCurrentProcess,
    };

    enum class Failure {
        DidNotCrash,
        UnexpectedError,
    };

    Crash(String test_type, Function<Crash::Failure()> crash_function)
        : m_type(test_type)
        , m_crash_function(move(crash_function))
    {
    }

    void run(RunType run_type)
    {
        printf("\x1B[33mTesting\x1B[0m: \"%s\"\n", m_type.characters());

        auto run_crash_and_print_if_error = [this]() {
            auto failure = m_crash_function();

            // If we got here something went wrong
            printf("\x1B[31mFAIL\x1B[0m: ");
            switch (failure) {
            case Failure::DidNotCrash:
                printf("Did not crash!\n");
                break;
            case Failure::UnexpectedError:
                printf("Unexpected error!\n");
                break;
            default:
                ASSERT_NOT_REACHED();
            }
        };

        if (run_type == RunType::UsingCurrentProcess) {
            run_crash_and_print_if_error();
        } else {

            // Run the test in a child process so that we do not crash the crash program :^)
            pid_t pid = fork();
            if (pid < 0) {
                perror("fork");
                ASSERT_NOT_REACHED();
            } else if (pid == 0) {
                run_crash_and_print_if_error();
                exit(0);
            }

            int status;
            waitpid(pid, &status, 0);
            if (WIFSIGNALED(status))
                printf("\x1B[32mPASS\x1B[0m: Terminated with signal %d\n", WTERMSIG(status));
        }
    }

private:
    String m_type;
    Function<Crash::Failure()> m_crash_function;
};

int main(int argc, char** argv)
{
    bool do_all_crash_types = false;
    bool do_segmentation_violation = false;
    bool do_division_by_zero = false;
    bool do_illegal_instruction = false;
    bool do_abort = false;
    bool do_write_to_uninitialized_malloc_memory = false;
    bool do_write_to_freed_memory = false;
    bool do_write_to_read_only_memory = false;
    bool do_read_from_uninitialized_malloc_memory = false;
    bool do_read_from_freed_memory = false;
    bool do_invalid_stack_pointer_on_syscall = false;
    bool do_invalid_stack_pointer_on_page_fault  = false;
    bool do_syscall_from_writeable_memory = false;
    bool do_write_to_freed_memory_still_cached_by_malloc = false;
    bool do_read_from_freed_memory_still_cached_by_malloc = false;
    bool do_execute_non_executable_memory = false;
    bool do_trigger_user_mode_instruction_prevention = false;
    bool do_use_io_instruction = false;
    bool do_read_cpu_counter = false;

    auto args_parser = Core::ArgsParser();
    args_parser.add_option(do_all_crash_types, "Test that all of the following crash types crash as expected", nullptr, 'A');
    args_parser.add_option(do_segmentation_violation, "Perform a segmentation violation by dereferencing an invalid pointer", nullptr, 's');
    args_parser.add_option(do_division_by_zero, "Perform a division by zero", nullptr, 'd');
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
    args_parser.add_option(do_write_to_freed_memory_still_cached_by_malloc, "Read from recently freed memory (tests an opportunistic malloc guard)", nullptr, 'x');
    args_parser.add_option(do_read_from_freed_memory_still_cached_by_malloc, "Write to recently free memory (tests an opportunistic malloc guard)", nullptr, 'y');
    args_parser.add_option(do_execute_non_executable_memory, "Attempt to execute non-executable memory (not mapped with PROT_EXEC)", nullptr, 'X');
    args_parser.add_option(do_trigger_user_mode_instruction_prevention, "Attempt to trigger an x86 User Mode Instruction Prevention fault", nullptr, 'U');
    args_parser.add_option(do_use_io_instruction, "Use an x86 I/O instruction in userspace", nullptr, 'I');
    args_parser.add_option(do_read_cpu_counter, "Read the x86 TSC (Time Stamp Counter) directly", nullptr, 'c');

    if (argc != 2) {
        args_parser.print_usage(stderr, argv[0]);
        exit(1);
    }

    args_parser.parse(argc, argv);

    Crash::RunType run_type = do_all_crash_types ? Crash::RunType::UsingChildProcess
                                                 : Crash::RunType::UsingCurrentProcess;

    if (do_segmentation_violation || do_all_crash_types) {
        Crash("Segmentation violation", []() {
            volatile int* crashme = nullptr;
            *crashme = 0xbeef;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_division_by_zero || do_all_crash_types) {
        Crash("Division by zero", []() {
            volatile int lala = 10;
            volatile int zero = 0;
            volatile int test = lala / zero;
            UNUSED_PARAM(test);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_illegal_instruction|| do_all_crash_types) {
        Crash("Illegal instruction", []() {
            asm volatile("ud2");
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_abort || do_all_crash_types) {
        Crash("Abort", []() {
            abort();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_read_from_uninitialized_malloc_memory || do_all_crash_types) {
        Crash("Read from uninitialized malloc memory", []() {
            auto* uninitialized_memory = (volatile u32**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            volatile auto x = uninitialized_memory[0][0];
            UNUSED_PARAM(x);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_read_from_uninitialized_malloc_memory || do_all_crash_types) {
        Crash("Read from freed memory", []() {
            auto* uninitialized_memory = (volatile u32**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            free(uninitialized_memory);
            volatile auto x = uninitialized_memory[4][0];
            UNUSED_PARAM(x);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_write_to_uninitialized_malloc_memory || do_all_crash_types) {
        Crash("Write to uninitialized malloc memory", []() {
            auto* uninitialized_memory = (volatile u32**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            uninitialized_memory[4][0] = 1;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_write_to_freed_memory || do_all_crash_types) {
        Crash("Write to freed memory", []() {
            auto* uninitialized_memory = (volatile u32**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            free(uninitialized_memory);
            uninitialized_memory[4][0] = 1;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_write_to_read_only_memory || do_all_crash_types) {
        Crash("Write to read only memory", []() {
            auto* ptr = (u8*)mmap(nullptr, 4096, PROT_READ | PROT_WRITE, MAP_ANON, 0, 0);
            if (ptr != MAP_FAILED)
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
        Crash("Invalid stack pointer on syscall", []() {
            u8* makeshift_stack = (u8*)mmap(nullptr, 0, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_STACK, 0, 0);
            if (!makeshift_stack)
                return Crash::Failure::UnexpectedError;

            u8* makeshift_esp = makeshift_stack + 2048;
            asm volatile("mov %%eax, %%esp" ::"a"(makeshift_esp));
            getuid();
            dbgprintf("Survived syscall with MAP_STACK stack\n");

            u8* bad_stack = (u8*)mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
            if (!bad_stack)
                return Crash::Failure::UnexpectedError;

            u8* bad_esp = bad_stack + 2048;
            asm volatile("mov %%eax, %%esp" ::"a"(bad_esp));
            getuid();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_invalid_stack_pointer_on_page_fault || do_all_crash_types) {
        Crash("Invalid stack pointer on page fault", []() {
            u8* bad_stack = (u8*)mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
            if (!bad_stack)
                return Crash::Failure::UnexpectedError;

            u8* bad_esp = bad_stack + 2048;
            asm volatile("mov %%eax, %%esp" ::"a"(bad_esp));
            asm volatile("pushl $0");
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_syscall_from_writeable_memory || do_all_crash_types) {
        Crash("Syscall from writable memory", []() {
            u8 buffer[] = { 0xb8, Syscall::SC_getuid, 0, 0, 0, 0xcd, 0x82 };
            ((void (*)())buffer)();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_read_from_freed_memory_still_cached_by_malloc || do_all_crash_types) {
        Crash("Read from memory still cached by malloc", []() {
            auto* ptr = (u8*)malloc(1024);
            if (!ptr)
                return Crash::Failure::UnexpectedError;

            free(ptr);
            dbgprintf("ptr = %p\n", ptr);
            volatile auto foo = *ptr;
            UNUSED_PARAM(foo);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_write_to_freed_memory_still_cached_by_malloc || do_all_crash_types) {
        Crash("Write to freed memory still cached by malloc", []() {
            auto* ptr = (u8*)malloc(1024);
            if (!ptr)
                return Crash::Failure::UnexpectedError;
            free(ptr);
            dbgprintf("ptr = %p\n", ptr);
            *ptr = 'x';
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_execute_non_executable_memory || do_all_crash_types) {
        Crash("Execute non executable memory", []() {
            auto* ptr = (u8*)mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
            if (ptr == MAP_FAILED)
                return Crash::Failure::UnexpectedError;

            ptr[0] = 0xc3; // ret
            typedef void* (*CrashyFunctionPtr)();
            ((CrashyFunctionPtr)ptr)();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_trigger_user_mode_instruction_prevention || do_all_crash_types) {
        Crash("Trigger x86 User Mode Instruction Prevention", []() {
            asm volatile("str %eax");
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_use_io_instruction || do_all_crash_types) {
        Crash("Attempt to use an I/O instruction", [] {
            u8 keyboard_status = IO::in8(0x64);
            printf("Keyboard status: %#02x\n", keyboard_status);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (do_read_cpu_counter || do_all_crash_types) {
        Crash("Read the CPU timestamp counter", [] {
            asm volatile("rdtsc");
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    return 0;
}
