#include <AK/String.h>
#include <Kernel/Syscall.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/mman.h>

static void print_usage_and_exit()
{
    printf("usage: crash -[sdiamfMFTt]\n");
    exit(0);
}

#pragma GCC optimize("O0")
int main(int argc, char** argv)
{
    enum Mode {
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
    };
    Mode mode = SegmentationViolation;

    if (argc != 2)
        print_usage_and_exit();

    if (String(argv[1]) == "-s")
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
    else
        print_usage_and_exit();

    if (mode == SegmentationViolation) {
        volatile int* crashme = nullptr;
        *crashme = 0xbeef;
        ASSERT_NOT_REACHED();
    }

    if (mode == DivisionByZero) {
        volatile int lala = 10;
        volatile int zero = 0;
        volatile int test = lala / zero;
        (void)test;
        ASSERT_NOT_REACHED();
    }

    if (mode == IllegalInstruction) {
        asm volatile("ud2");
        ASSERT_NOT_REACHED();
    }

    if (mode == Abort) {
        abort();
        ASSERT_NOT_REACHED();
    }

    if (mode == ReadFromUninitializedMallocMemory) {
        auto* uninitialized_memory = (volatile u32**)malloc(1024);
        volatile auto x = uninitialized_memory[0][0];
        (void)x;
        ASSERT_NOT_REACHED();
    }

    if (mode == ReadFromFreedMemory) {
        auto* uninitialized_memory = (volatile u32**)malloc(1024);
        free(uninitialized_memory);
        volatile auto x = uninitialized_memory[4][0];
        (void)x;
        ASSERT_NOT_REACHED();
    }

    if (mode == WriteToUninitializedMallocMemory) {
        auto* uninitialized_memory = (volatile u32**)malloc(1024);
        uninitialized_memory[4][0] = 1;
        ASSERT_NOT_REACHED();
    }

    if (mode == WriteToFreedMemory) {
        auto* uninitialized_memory = (volatile u32**)malloc(1024);
        free(uninitialized_memory);
        uninitialized_memory[4][0] = 1;
        ASSERT_NOT_REACHED();
    }

    if (mode == WriteToReadonlyMemory) {
        auto* ptr = (u8*)mmap(nullptr, 4096, PROT_READ | PROT_WRITE, MAP_ANON, 0, 0);
        ASSERT(ptr != MAP_FAILED);
        *ptr = 'x'; // This should work fine.
        int rc = mprotect(ptr, 4096, PROT_READ);
        ASSERT(rc == 0);
        ASSERT(*ptr == 'x');
        *ptr = 'y'; // This should crash!
    }

    if (mode == InvalidStackPointerOnSyscall) {
        u8* makeshift_stack = (u8*)mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_STACK, 0, 0);
        if (!makeshift_stack) {
            perror("mmap");
            return 1;
        }
        u8* makeshift_esp = makeshift_stack + 2048;
        asm volatile("mov %%eax, %%esp" :: "a"(makeshift_esp));
        getuid();
        dbgprintf("Survived syscall with MAP_STACK stack\n");

        u8* bad_stack = (u8*)mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
        if (!bad_stack) {
            perror("mmap");
            return 1;
        }
        u8* bad_esp = bad_stack + 2048;
        asm volatile("mov %%eax, %%esp" :: "a"(bad_esp));
        getuid();

        ASSERT_NOT_REACHED();
    }

    if (mode == InvalidStackPointerOnPageFault) {
        u8* bad_stack = (u8*)mmap(nullptr, PAGE_SIZE, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, 0, 0);
        if (!bad_stack) {
            perror("mmap");
            return 1;
        }
        u8* bad_esp = bad_stack + 2048;
        asm volatile("mov %%eax, %%esp" :: "a"(bad_esp));
        asm volatile("pushl $0");
        ASSERT_NOT_REACHED();
    }

    if (mode == SyscallFromWritableMemory) {
        u8 buffer[] = { 0xb8, Syscall::SC_getuid, 0, 0, 0, 0xcd, 0x82 };
        ((void(*)())buffer)();
    }

    ASSERT_NOT_REACHED();
    return 0;
}
