/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <errno.h>
#include <pthread.h>
#include <unistd.h>

TEST_CASE(spin_init_process_scope)
{
    {
        pthread_spinlock_t lock {};
        auto result = pthread_spin_init(&lock, PTHREAD_SCOPE_PROCESS);
        EXPECT_EQ(0, result);
        result = pthread_spin_destroy(&lock);
        EXPECT_EQ(0, result);
    }

    {
        pthread_spinlock_t garbage_lock { 0x1337 };
        auto result = pthread_spin_init(&garbage_lock, PTHREAD_SCOPE_PROCESS);
        EXPECT_EQ(0, result);
        result = pthread_spin_destroy(&garbage_lock);
        EXPECT_EQ(0, result);
    }
}

TEST_CASE(spin_init_system_scope)
{
    pthread_spinlock_t lock {};
    auto result = pthread_spin_init(&lock, PTHREAD_SCOPE_SYSTEM);
    EXPECT_EQ(0, result);
    pthread_spinlock_t garbage_lock { 0x99999 };
    result = pthread_spin_init(&garbage_lock, PTHREAD_SCOPE_PROCESS);
    EXPECT_EQ(0, result);
}

TEST_CASE(spin_lock)
{
    pthread_spinlock_t lock {};
    auto result = pthread_spin_lock(&lock);
    EXPECT_EQ(0, result);

    // We should detect that this thread already holds this lock.
    result = pthread_spin_lock(&lock);
    EXPECT_EQ(EDEADLK, result);
}

TEST_CASE(spin_try_lock)
{
    {
        pthread_spinlock_t lock {};
        auto result = pthread_spin_trylock(&lock);
        EXPECT_EQ(0, result);

        result = pthread_spin_unlock(&lock);
        EXPECT_EQ(0, result);
    }

    {
        pthread_spinlock_t lock {};
        auto result = pthread_spin_trylock(&lock);
        EXPECT_EQ(0, result);

        // We should detect that this thread already holds the lock.
        result = pthread_spin_trylock(&lock);
        EXPECT_EQ(EBUSY, result);
    }
}

static void lock_from_different_thread(pthread_spinlock_t* lock)
{
    pthread_t thread_id {};
    auto result = pthread_create(
        &thread_id, nullptr, [](void* param) -> void* {
            auto lock = (pthread_spinlock_t*)param;
            pthread_spin_lock(lock);
            return nullptr;
        },
        lock);
    EXPECT_EQ(0, result);

    result = pthread_join(thread_id, nullptr);
    EXPECT_EQ(0, result);
}

TEST_CASE(spin_unlock)
{
    {
        pthread_spinlock_t lock {};
        auto result = pthread_spin_lock(&lock);
        EXPECT_EQ(0, result);

        result = pthread_spin_unlock(&lock);
        EXPECT_EQ(0, result);
    }

    {
        pthread_spinlock_t lock {};
        lock_from_different_thread(&lock);

        auto result = pthread_spin_unlock(&lock);
        EXPECT_EQ(EPERM, result);
    }
}

TEST_CASE(spin_destroy)
{
    {
        pthread_spinlock_t lock {};
        auto result = pthread_spin_lock(&lock);
        EXPECT_EQ(0, result);

        result = pthread_spin_destroy(&lock);
        EXPECT_EQ(EBUSY, result);

        result = pthread_spin_unlock(&lock);
        EXPECT_EQ(0, result);
    }

    {
        pthread_spinlock_t lock {};
        lock_from_different_thread(&lock);

        auto result = pthread_spin_destroy(&lock);
        EXPECT_EQ(EBUSY, result);
    }
}
