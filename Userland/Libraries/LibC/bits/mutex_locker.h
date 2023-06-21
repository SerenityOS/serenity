/*
 * Copyright (c) 2021, Daniel Bertalan <dani@danielbertalan.dev>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <pthread.h>

// We don't want to bring LibThreading headers into LibC, so we use plain
// pthread mutexes and this RAII guard.
namespace LibC {

class [[nodiscard]] MutexLocker {
public:
    explicit MutexLocker(pthread_mutex_t& mutex)
        : m_mutex(mutex)
    {
        lock();
    }

    ~MutexLocker()
    {
        unlock();
    }

    void lock() { pthread_mutex_lock(&m_mutex); }
    void unlock() { pthread_mutex_unlock(&m_mutex); }

private:
    pthread_mutex_t& m_mutex;
};

}
