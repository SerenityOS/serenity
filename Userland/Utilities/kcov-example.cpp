/*
 * Copyright (c) 2021, Patrick Meyer <git@the-space.agency>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/API/kcov.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <unistd.h>

// Note: This program requires serenity to be built with the CMake build option
// ENABLE_KERNEL_COVERAGE_COLLECTION
ErrorOr<int> serenity_main(Main::Arguments)
{
    constexpr size_t num_entries = 1024 * 100;

    int fd = TRY(Core::System::open("/dev/kcov"sv, O_RDWR));
    TRY(Core::System::ioctl(fd, KCOV_SETBUFSIZE, num_entries));
    kcov_pc_t* cover = (kcov_pc_t*)TRY(Core::System::mmap(NULL, num_entries * KCOV_ENTRY_SIZE, PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0));
    TRY(Core::System::ioctl(fd, KCOV_ENABLE));
    cover[0] = 0;

    // Example syscall so we actually cover some kernel code.
    getppid();

    TRY(Core::System::ioctl(fd, KCOV_DISABLE));

    u64 cov_idx = cover[0];
    for (size_t idx = 1; idx <= cov_idx; idx++)
        printf("%p\n", (void*)cover[idx]);

    TRY(Core::System::munmap(const_cast<u64*>(cover), num_entries * KCOV_ENTRY_SIZE));
    TRY(Core::System::close(fd));

    return 0;
}
