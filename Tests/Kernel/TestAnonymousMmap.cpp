/*
 * Copyright (c) 2024, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <sys/wait.h>
#include <unistd.h>

static void check_if_page_zeroed(char* ptr, size_t page)
{
    for (size_t j = 0; j < PAGE_SIZE; ++j)
        EXPECT(ptr[page * PAGE_SIZE + j] == 0);
}

TEST_CASE(shared_anonymous_mmap)
{
    size_t pages = 100;
    size_t len = pages * PAGE_SIZE;
    char* shared_ptr = (char*)mmap(nullptr, len, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_SHARED, -1, 0);
    EXPECT(shared_ptr != MAP_FAILED);

    size_t forks = 20;
    for (size_t i = 0; i < forks; ++i) {
        pid_t pid = fork();
        VERIFY(pid != -1);
        if (pid == 0) {
            // sleep so that multiple child processes can be created before performing the writes
            sleep(1);
            char c = '$' + (char)i;
            shared_ptr[i * PAGE_SIZE] = c;
            exit(EXIT_SUCCESS);
        }
    }

    // wait for all child processes to exit
    for (size_t i = 0; i < forks; ++i)
        wait(nullptr);

    // check that writes to the shared anonymous mmap in the multiple child processes are visible
    for (size_t i = 0; i < forks; ++i) {
        char c = '$' + (char)i;
        EXPECT(shared_ptr[i * PAGE_SIZE] == c);
    }

    // check that the pages that haven't been written to are zeroed
    for (size_t i = forks; i < pages; ++i)
        check_if_page_zeroed(shared_ptr, i);
}

TEST_CASE(private_anonymous_mmap)
{
    size_t pages = 100;
    size_t len = pages * PAGE_SIZE;
    char* private_ptr = (char*)mmap(nullptr, len, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, -1, 0);
    EXPECT(private_ptr != MAP_FAILED);

    pid_t pid = fork();
    VERIFY(pid != -1);
    if (pid == 0) {
        // write to all pages of the mmap region
        for (size_t i = 0; i < pages; ++i)
            private_ptr[i * PAGE_SIZE] = '$';
        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);
        // check that the writes that happened in the child process are not visible, all pages should be zeroed
        for (size_t i = 0; i < pages; ++i)
            check_if_page_zeroed(private_ptr, i);
    }
}

TEST_CASE(test_that_partial_munmap_does_not_break_cow)
{
    size_t pages = 3;
    size_t len = pages * PAGE_SIZE;
    char* map = (char*)mmap(nullptr, len, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, -1, 0);
    EXPECT(map != MAP_FAILED);

    // make writes before forking so the pages are marked as cow
    map[0] = 'A';
    map[PAGE_SIZE] = 'B';
    map[2 * PAGE_SIZE] = 'C';

    pid_t pid = fork();
    VERIFY(pid != -1);
    if (pid == 0) {
        EXPECT(map[0] == 'A');
        EXPECT(map[PAGE_SIZE] == 'B');
        EXPECT(map[2 * PAGE_SIZE] == 'C');

        // unmap part of the range, this should not interfere with the cow status of map's pages
        int rc = munmap(map + PAGE_SIZE, PAGE_SIZE);
        VERIFY(rc != -1);

        EXPECT(map[0] == 'A');
        EXPECT(map[2 * PAGE_SIZE] == 'C');

        // write to map, these writes should be local to this child process
        map[0] = '!';
        map[2 * PAGE_SIZE] = '!';

        exit(EXIT_SUCCESS);
    } else {
        wait(NULL);
        // test that the writes made in the child process are not visible in this parent process
        EXPECT(map[0] == 'A');
        EXPECT(map[PAGE_SIZE] == 'B');
        EXPECT(map[2 * PAGE_SIZE] == 'C');
    }
}
