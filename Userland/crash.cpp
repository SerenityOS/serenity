#include <AK/Function.h>
#include <AK/String.h>
#include <Kernel/Syscall.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>
#include <sys/wait.h>

#pragma GCC optimize("O0")

static void print_usage_and_exit()
{
    printf("usage: crash -[AsdiamfMFTtSxyX]\n");
    exit(0);
}

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
    enum Mode {
        TestAllCrashTypes,
        SegmentationViolation,
        DivisionByZero,
        IllegalInstruction,
        Abort,
        WriteToUninitializedMallocMemory,
        WriteToFreedMemory,
        ReadFromUninitializedMallocMemory,
        ReadFromFreedMemory,
        WriteToReadonlyMemory,
        InvalidStackPointerOnSyscall,
        InvalidStackPointerOnPageFault,
        SyscallFromWritableMemory,
        WriteToFreedMemoryStillCachedByMalloc,
        ReadFromFreedMemoryStillCachedByMalloc,
        ExecuteNonExecutableMemory,
    };
    Mode mode = SegmentationViolation;

    if (argc != 2)
        print_usage_and_exit();

    if (String(argv[1]) == "-A")
        mode = TestAllCrashTypes;
    else if (String(argv[1]) == "-s")
        mode = SegmentationViolation;
    else if (String(argv[1]) == "-d")
        mode = DivisionByZero;
    else if (String(argv[1]) == "-i")
        mode = IllegalInstruction;
    else if (String(argv[1]) == "-a")
        mode = Abort;
    else if (String(argv[1]) == "-m")
        mode = ReadFromUninitializedMallocMemory;
    else if (String(argv[1]) == "-f")
        mode = ReadFromFreedMemory;
    else if (String(argv[1]) == "-M")
        mode = WriteToUninitializedMallocMemory;
    else if (String(argv[1]) == "-F")
        mode = WriteToFreedMemory;
    else if (String(argv[1]) == "-r")
        mode = WriteToReadonlyMemory;
    else if (String(argv[1]) == "-T")
        mode = InvalidStackPointerOnSyscall;
    else if (String(argv[1]) == "-t")
        mode = InvalidStackPointerOnPageFault;
    else if (String(argv[1]) == "-S")
        mode = SyscallFromWritableMemory;
    else if (String(argv[1]) == "-x")
        mode = ReadFromFreedMemoryStillCachedByMalloc;
    else if (String(argv[1]) == "-y")
        mode = WriteToFreedMemoryStillCachedByMalloc;
    else if (String(argv[1]) == "-X")
        mode = ExecuteNonExecutableMemory;
    else
        print_usage_and_exit();

    Crash::RunType run_type = mode == TestAllCrashTypes ? Crash::RunType::UsingChildProcess
                                                        : Crash::RunType::UsingCurrentProcess;

    if (mode == SegmentationViolation || mode == TestAllCrashTypes) {
        Crash("Segmentation violation", []() {
            volatile int* crashme = nullptr;
            *crashme = 0xbeef;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (mode == DivisionByZero || mode == TestAllCrashTypes) {
        Crash("Division by zero", []() {
            volatile int lala = 10;
            volatile int zero = 0;
            volatile int test = lala / zero;
            UNUSED_PARAM(test);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (mode == IllegalInstruction || mode == TestAllCrashTypes) {
        Crash("Illegal instruction", []() {
            asm volatile("ud2");
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (mode == Abort || mode == TestAllCrashTypes) {
        Crash("Abort", []() {
            abort();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (mode == ReadFromUninitializedMallocMemory || mode == TestAllCrashTypes) {
        Crash("Read from uninitialized malloc memory", []() {
            auto* uninitialized_memory = (volatile u32**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            volatile auto x = uninitialized_memory[0][0];
            UNUSED_PARAM(x);
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (mode == ReadFromFreedMemory || mode == TestAllCrashTypes) {
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

    if (mode == WriteToUninitializedMallocMemory || mode == TestAllCrashTypes) {
        Crash("Write to uninitialized malloc memory", []() {
            auto* uninitialized_memory = (volatile u32**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            uninitialized_memory[4][0] = 1;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (mode == WriteToFreedMemory || mode == TestAllCrashTypes) {
        Crash("Write to freed memory", []() {
            auto* uninitialized_memory = (volatile u32**)malloc(1024);
            if (!uninitialized_memory)
                return Crash::Failure::UnexpectedError;

            free(uninitialized_memory);
            uninitialized_memory[4][0] = 1;
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (mode == WriteToReadonlyMemory || mode == TestAllCrashTypes) {
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

    if (mode == InvalidStackPointerOnSyscall || mode == TestAllCrashTypes) {
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

    if (mode == InvalidStackPointerOnPageFault || mode == TestAllCrashTypes) {
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

    if (mode == SyscallFromWritableMemory || mode == TestAllCrashTypes) {
        Crash("Syscall from writable memory", []() {
            u8 buffer[] = { 0xb8, Syscall::SC_getuid, 0, 0, 0, 0xcd, 0x82 };
            ((void (*)())buffer)();
            return Crash::Failure::DidNotCrash;
        }).run(run_type);
    }

    if (mode == ReadFromFreedMemoryStillCachedByMalloc || mode == TestAllCrashTypes) {
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

    if (mode == WriteToFreedMemoryStillCachedByMalloc || mode == TestAllCrashTypes) {
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

    if (mode == ExecuteNonExecutableMemory || mode == TestAllCrashTypes) {
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

    return 0;
}

