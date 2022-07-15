/*
 * Copyright (c) 2021, Rodrigo Tobar <rtobarc@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>
#include <pthread.h>

TEST_CASE(rwlock_init)
{
    pthread_rwlock_t lock;
    auto result = pthread_rwlock_init(&lock, nullptr);
    EXPECT_EQ(0, result);
}

TEST_CASE(rwlock_rdlock)
{
    pthread_rwlock_t lock;
    auto result = pthread_rwlock_init(&lock, nullptr);
    EXPECT_EQ(0, result);

    result = pthread_rwlock_rdlock(&lock);
    EXPECT_EQ(0, result);
    result = pthread_rwlock_unlock(&lock);
    EXPECT_EQ(0, result);

    result = pthread_rwlock_rdlock(&lock);
    EXPECT_EQ(0, result);
    result = pthread_rwlock_rdlock(&lock);
    EXPECT_EQ(0, result);
    result = pthread_rwlock_unlock(&lock);
    EXPECT_EQ(0, result);
    result = pthread_rwlock_unlock(&lock);
    EXPECT_EQ(0, result);
}

TEST_CASE(rwlock_wrlock)
{
    pthread_rwlock_t lock;
    auto result = pthread_rwlock_init(&lock, nullptr);
    EXPECT_EQ(0, result);

    result = pthread_rwlock_wrlock(&lock);
    EXPECT_EQ(0, result);
    result = pthread_rwlock_unlock(&lock);
    EXPECT_EQ(0, result);
}

TEST_CASE(rwlock_rwr_sequence)
{
    pthread_rwlock_t lock;
    auto result = pthread_rwlock_init(&lock, nullptr);
    EXPECT_EQ(0, result);

    result = pthread_rwlock_rdlock(&lock);
    EXPECT_EQ(0, result);
    result = pthread_rwlock_unlock(&lock);
    EXPECT_EQ(0, result);

    result = pthread_rwlock_wrlock(&lock);
    EXPECT_EQ(0, result);
    result = pthread_rwlock_unlock(&lock);
    EXPECT_EQ(0, result);

    result = pthread_rwlock_rdlock(&lock);
    EXPECT_EQ(0, result);
    result = pthread_rwlock_unlock(&lock);
    EXPECT_EQ(0, result);
}

TEST_CASE(rwlock_wrlock_init_in_once)
{
    static pthread_rwlock_t lock;
    static pthread_once_t once1 = PTHREAD_ONCE_INIT;
    static pthread_once_t once2 = PTHREAD_ONCE_INIT;
    static pthread_once_t once3 = PTHREAD_ONCE_INIT;
    pthread_once(&once1, []() {
        pthread_once(&once2, []() {
            pthread_once(&once3, []() {
                auto result = pthread_rwlock_init(&lock, nullptr);
                EXPECT_EQ(0, result);
            });
        });
    });
    auto result = pthread_rwlock_wrlock(&lock);
    EXPECT_EQ(0, result);
    result = pthread_rwlock_unlock(&lock);
    EXPECT_EQ(0, result);
}
