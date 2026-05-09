/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/System.h>
#include <LibTest/TestCase.h>
#include <LibThreading/Thread.h>

#include <fcntl.h>
#include <serenity.h>
#include <sys/mman.h>
#include <sys/wait.h>

// A dead simple single-use barrier to make sure that page faults happen simultaneously.
// It spins instead of using blocking to ensure that both threads start causing page faults with a minimal time gap.
template<u32 NUMBER_OF_WAITERS>
class SpinningBarrier {
public:
    SpinningBarrier()
    {
        auto* shared_region = MUST(Core::System::mmap(nullptr, sizeof(State), PROT_READ | PROT_WRITE, MAP_SHARED | MAP_ANONYMOUS, -1, 0));
        m_state = new (shared_region) State;
    }

    void wait()
    {
        auto ticket = m_state->threads_waiting.fetch_add(1, AK::memory_order_acq_rel) + 1;

        if (ticket >= NUMBER_OF_WAITERS) {
            m_state->release.store(true, AK::memory_order_release);
        } else {
            while (!m_state->release.load(AK::memory_order_acquire)) { }
        }
    }

    ~SpinningBarrier()
    {
        m_state->~State();
        MUST(Core::System::munmap(m_state, sizeof(State)));
    }

private:
    struct State {
        Atomic<u32> threads_waiting { 0 };
        Atomic<bool> release { false };
    }* m_state;
};

// These tests check that two threads simultaneously causing page faults on the same page don't result in a crash or kernel panic.

static constexpr size_t AMOUNT_MEMORY = 32 * MiB;
static constexpr size_t PAGE_COUNT = AMOUNT_MEMORY / PAGE_SIZE;

TEST_CASE(anonymous_mmap_race)
{
    // This test case covers page faults on pages that have been committed but not yet allocated.

    auto* mem = reinterpret_cast<u8 volatile*>(mmap(nullptr, AMOUNT_MEMORY, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, -1, 0));
    VERIFY(mem != MAP_FAILED);

    ScopeGuard guard = [mem] { munmap(const_cast<u8*>(mem), AMOUNT_MEMORY); };

    SpinningBarrier<2> barrier;

    auto thread1 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            mem[i * PAGE_SIZE] = i;

        return 0;
    });

    auto thread2 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            mem[i * PAGE_SIZE] = i + 50;

        return 0;
    });

    thread1->start();
    thread2->start();

    MUST(thread1->join());
    MUST(thread2->join());

    for (size_t i = 0; i < PAGE_COUNT; i++) {
        // This assumes that bytewise memory accesses are atomic.
        EXPECT(mem[i * PAGE_SIZE] == (i & 0xff)
            || mem[i * PAGE_SIZE] == ((i + 50) & 0xff));
    }
}

static_assert(PAGE_SIZE > 16); // All of the following tests have the (hopefully always true) assumption that PAGE_SIZE > 16.

TEST_CASE(anonymous_mmap_race2)
{
    // This test case covers page faults on pages that have been committed but not yet allocated.

    auto* mem = reinterpret_cast<u8 volatile*>(mmap(nullptr, AMOUNT_MEMORY, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, -1, 0));
    VERIFY(mem != MAP_FAILED);

    ScopeGuard guard = [mem] { munmap(const_cast<u8*>(mem), AMOUNT_MEMORY); };

    SpinningBarrier<2> barrier;

    auto thread1 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            mem[i * PAGE_SIZE] = i;

        return 0;
    });

    auto thread2 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            mem[(i * PAGE_SIZE) + 16] = i + 50;

        return 0;
    });

    thread1->start();
    thread2->start();

    MUST(thread1->join());
    MUST(thread2->join());

    for (size_t i = 0; i < PAGE_COUNT; i++) {
        EXPECT_EQ(mem[i * PAGE_SIZE], (i & 0xff));
        EXPECT_EQ(mem[(i * PAGE_SIZE) + 16], ((i + 50) & 0xff));
    }
}

TEST_CASE(anonymous_noreserve_mmap_race)
{
    // This test case covers page faults on uncommitted pages.

    auto* mem = reinterpret_cast<u8 volatile*>(mmap(nullptr, AMOUNT_MEMORY, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE | MAP_NORESERVE, -1, 0));
    VERIFY(mem != MAP_FAILED);

    ScopeGuard guard = [mem] { munmap(const_cast<u8*>(mem), AMOUNT_MEMORY); };

    SpinningBarrier<2> barrier;

    auto thread1 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            mem[i * PAGE_SIZE] = i;

        return 0;
    });

    auto thread2 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            mem[(i * PAGE_SIZE) + 16] = i + 50;

        return 0;
    });

    thread1->start();
    thread2->start();

    MUST(thread1->join());
    MUST(thread2->join());

    for (size_t i = 0; i < PAGE_COUNT; i++) {
        EXPECT_EQ(mem[i * PAGE_SIZE], (i & 0xff));
        EXPECT_EQ(mem[(i * PAGE_SIZE) + 16], ((i + 50) & 0xff));
    }
}

TEST_CASE(anonymous_cow_mmap_thread_race)
{
    // This test case covers simultaneous copy-on-write page faults in two child threads.

    auto* mem = reinterpret_cast<u8 volatile*>(mmap(nullptr, AMOUNT_MEMORY, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, -1, 0));
    VERIFY(mem != MAP_FAILED);

    ScopeGuard guard = [mem] { munmap(const_cast<u8*>(mem), AMOUNT_MEMORY); };

    // Cause page faults in every allocated page to ensure that all memory is allocated.
    // This is needed to make all of these pages copy-on-write after the fork().
    for (size_t i = 0; i < PAGE_COUNT; i++)
        mem[i * PAGE_SIZE] = i;

    pid_t pid = fork();
    VERIFY(pid != -1);

    // All pages in both the parent and child should be COW now.

    if (pid == 0) {
        SpinningBarrier<2> barrier;

        auto thread1 = Threading::Thread::construct([mem, &barrier] {
            barrier.wait();
            for (size_t i = 0; i < PAGE_COUNT; i++)
                mem[i * PAGE_SIZE] = i;

            return 0;
        });

        auto thread2 = Threading::Thread::construct([mem, &barrier] {
            barrier.wait();
            for (size_t i = 0; i < PAGE_COUNT; i++)
                mem[(i * PAGE_SIZE) + 16] = i + 50;

            return 0;
        });

        thread1->start();
        thread2->start();

        MUST(thread1->join());
        MUST(thread2->join());

        for (size_t i = 0; i < PAGE_COUNT; i++) {
            EXPECT_EQ(mem[i * PAGE_SIZE], (i & 0xff));
            EXPECT_EQ(mem[(i * PAGE_SIZE) + 16], ((i + 50) & 0xff));
        }

        munmap(const_cast<u8*>(mem), AMOUNT_MEMORY);
        _exit(0);
    }

    int status = 0;
    waitpid(pid, &status, 0);
}

TEST_CASE(anonymous_cow_mmap_child_parent_race)
{
    // This test case covers simultaneous copy-on-write page faults in the parent and child process.

    auto* mem = reinterpret_cast<u8 volatile*>(mmap(nullptr, AMOUNT_MEMORY, PROT_READ | PROT_WRITE, MAP_ANONYMOUS | MAP_PRIVATE, -1, 0));
    VERIFY(mem != MAP_FAILED);

    ScopeGuard guard = [mem] { munmap(const_cast<u8*>(mem), AMOUNT_MEMORY); };

    // Cause page faults in every allocated page to ensure that all memory is allocated.
    // This is needed to make all of these pages copy-on-write after the fork().
    for (size_t i = 0; i < PAGE_COUNT; i++)
        mem[i * PAGE_SIZE] = i;

    SpinningBarrier<2> barrier;

    pid_t pid = fork();
    VERIFY(pid != -1);

    // All pages in both the parent and child should be COW now.

    if (pid == 0) {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            mem[i * PAGE_SIZE] = i;

        munmap(const_cast<u8*>(mem), AMOUNT_MEMORY);
        _exit(0);
    }

    barrier.wait();
    for (size_t i = 0; i < PAGE_COUNT; i++)
        mem[(i * PAGE_SIZE) + 16] = i + 50;

    int status = 0;
    waitpid(pid, &status, 0);

    for (size_t i = 0; i < PAGE_COUNT; i++) {
        EXPECT_EQ(mem[i * PAGE_SIZE], (i & 0xff));
        EXPECT_EQ(mem[(i * PAGE_SIZE) + 16], ((i + 50) & 0xff));
    }
}

TEST_CASE(inode_mmap_write_race)
{
    // This test case covers write page faults to inode mmaps.

    static char const* FILE_NAME = "/tmp/inode-read-fault-race-test";

    int fd = open(FILE_NAME, O_RDWR | O_CREAT | O_TRUNC, S_IRUSR | S_IWUSR);
    VERIFY(fd != -1);

    // Note: Using ftruncate should cause this to be a sparse file, so it shouldn't take up any disk (or rather RAMFS) space.
    int rc = ftruncate(fd, AMOUNT_MEMORY);
    VERIFY(rc == 0);

    auto* mem = reinterpret_cast<u8 volatile*>(mmap(nullptr, AMOUNT_MEMORY, PROT_READ | PROT_WRITE, MAP_PRIVATE, fd, 0));
    VERIFY(mem != MAP_FAILED);

    ScopeGuard guard = [mem, fd] {
        munmap(const_cast<u8*>(mem), AMOUNT_MEMORY);
        close(fd);
        unlink(FILE_NAME);
    };

    SpinningBarrier<2> barrier;

    auto thread1 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            mem[i * PAGE_SIZE] = i;

        return 0;
    });

    auto thread2 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            mem[(i * PAGE_SIZE) + 16] = i + 50;

        return 0;
    });

    thread1->start();
    thread2->start();

    MUST(thread1->join());
    MUST(thread2->join());

    for (size_t i = 0; i < PAGE_COUNT; i++) {
        EXPECT_EQ(mem[i * PAGE_SIZE], (i & 0xff));
        EXPECT_EQ(mem[(i * PAGE_SIZE) + 16], ((i + 50) & 0xff));
    }
}

TEST_CASE(inode_mmap_read_race)
{
    // This test case covers read page faults to inode mmaps.

    static char const* FILE_NAME = "/tmp/inode-write-fault-race-test";

    int fd = open(FILE_NAME, O_RDWR | O_CREAT | O_TRUNC, S_IRUSR | S_IWUSR);
    VERIFY(fd != -1);

    // Note: Using ftruncate should cause this to be a sparse file, so it shouldn't take up any disk (or rather RAMFS) space.
    int rc = ftruncate(fd, AMOUNT_MEMORY);
    VERIFY(rc == 0);

    auto* mem = reinterpret_cast<u8 volatile*>(mmap(nullptr, AMOUNT_MEMORY, PROT_READ | PROT_WRITE, MAP_PRIVATE, fd, 0));
    VERIFY(mem != MAP_FAILED);

    ScopeGuard guard = [mem, fd] {
        munmap(const_cast<u8*>(mem), AMOUNT_MEMORY);
        close(fd);
        unlink(FILE_NAME);
    };

    SpinningBarrier<2> barrier;

    auto thread1 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            AK::taint_for_optimizer(mem[i * PAGE_SIZE]);

        return 0;
    });

    auto thread2 = Threading::Thread::construct([mem, &barrier] {
        barrier.wait();
        for (size_t i = 0; i < PAGE_COUNT; i++)
            AK::taint_for_optimizer(mem[i * PAGE_SIZE]);

        return 0;
    });

    thread1->start();
    thread2->start();

    MUST(thread1->join());
    MUST(thread2->join());
}
