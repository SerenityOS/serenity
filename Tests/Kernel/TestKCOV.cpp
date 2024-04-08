/*
 * Copyright (c) 2024, Space Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/kcov.h>
#include <Kernel/Sections.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <LibTest/Macros.h>
#include <LibTest/TestCase.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <unistd.h>

// LibTests runs multithreaded, but KCOV is designed so that only one thread per process can
// open() the KCOV device at any given time. As a workaround we fork() before every test.
// In the child we then run the actual test. In the parent we wait for the child to exit
// and then exit the parent.
static void fork_and_kill_parent()
{
    int pid = fork();
    EXPECT(pid >= 0);
    if (pid > 0) { // parent
        int status;
        waitpid(pid, &status, 0);
        exit(EXIT_SUCCESS);
    }
}

TEST_CASE(kcov_basic)
{
    fork_and_kill_parent();
    constexpr size_t num_entries = 1024 * 100;

    int fd = TRY_OR_FAIL(Core::System::open("/dev/kcov"sv, O_RDWR));
    TRY_OR_FAIL(Core::System::ioctl(fd, KCOV_SETBUFSIZE, num_entries));
    kcov_pc_t* cover = (kcov_pc_t*)TRY_OR_FAIL(Core::System::mmap(NULL, num_entries * KCOV_ENTRY_SIZE, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0));
    TRY_OR_FAIL(Core::System::ioctl(fd, KCOV_ENABLE));
    cover[0] = 0;

    // Example syscall so we actually cover some kernel code.
    getppid();

    TRY_OR_FAIL(Core::System::ioctl(fd, KCOV_DISABLE));

    u64 cov_idx = cover[0];
    for (size_t idx = 1; idx <= cov_idx; idx++)
        // If we enforced disable_kaslr, we could check if we actually covered addresses contained
        // by getppid(). However that would make it harder to run this test. It's also not really
        // required, as recording bogus PCs is not a common failure mode for KCOV in my experience.
        ASSUME(cover[idx] > KERNEL_MAPPING_BASE);

    // cover[0] contains the recorded PC count, followed by the recorded PCs. Let's make a
    // conservative guess. We should record way more PCs, even for a simple getppid().
    ASSUME(cover[0] > 10);

    TRY_OR_FAIL(Core::System::munmap(const_cast<u64*>(cover), num_entries * KCOV_ENTRY_SIZE));
    TRY_OR_FAIL(Core::System::close(fd));
}

BENCHMARK_CASE(kcov_loop)
{
    fork_and_kill_parent();
    constexpr int iterations = 100000;
    constexpr size_t num_entries = 1024 * 100;

    int fd = TRY_OR_FAIL(Core::System::open("/dev/kcov"sv, O_RDWR));
    TRY_OR_FAIL(Core::System::ioctl(fd, KCOV_SETBUFSIZE, num_entries));
    kcov_pc_t* cover = (kcov_pc_t*)TRY_OR_FAIL(Core::System::mmap(NULL, num_entries * KCOV_ENTRY_SIZE, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0));
    TRY_OR_FAIL(Core::System::ioctl(fd, KCOV_ENABLE));
    cover[0] = 0;

    for (size_t i = 0; i < iterations; ++i)
        getppid();

    TRY_OR_FAIL(Core::System::ioctl(fd, KCOV_DISABLE));

    TRY_OR_FAIL(Core::System::munmap(const_cast<u64*>(cover), num_entries * KCOV_ENTRY_SIZE));
    TRY_OR_FAIL(Core::System::close(fd));
}
