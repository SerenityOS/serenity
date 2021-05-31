/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <errno.h>
#include <semaphore.h>

int sem_close(sem_t*)
{
    errno = ENOSYS;
    return -1;
}

int sem_destroy(sem_t* sem)
{
    auto rc = pthread_mutex_destroy(&sem->mtx);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    rc = pthread_cond_destroy(&sem->cv);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    return 0;
}

int sem_getvalue(sem_t* sem, int* sval)
{
    auto rc = pthread_mutex_trylock(&sem->mtx);

    if (rc == EBUSY) {
        *sval = 0;
        return 0;
    }

    if (rc != 0) {
        errno = rc;
        return -1;
    }

    *sval = sem->value;

    pthread_mutex_unlock(&sem->mtx);

    return 0;
}

int sem_init(sem_t* sem, int shared, unsigned int value)
{
    if (shared) {
        errno = ENOSYS;
        return -1;
    }

    if (value > SEM_VALUE_MAX) {
        errno = EINVAL;
        return -1;
    }

    auto rc = pthread_mutex_init(&sem->mtx, nullptr);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    rc = pthread_cond_init(&sem->cv, nullptr);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    sem->value = value;

    return 0;
}

sem_t* sem_open(const char*, int, ...)
{
    errno = ENOSYS;
    return nullptr;
}

int sem_post(sem_t* sem)
{
    auto rc = pthread_mutex_lock(&sem->mtx);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    if (sem->value == SEM_VALUE_MAX) {
        pthread_mutex_unlock(&sem->mtx);
        errno = EOVERFLOW;
        return -1;
    }

    sem->value++;

    rc = pthread_cond_signal(&sem->cv);
    if (rc != 0) {
        pthread_mutex_unlock(&sem->mtx);
        errno = rc;
        return -1;
    }

    rc = pthread_mutex_unlock(&sem->mtx);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    return 0;
}

int sem_trywait(sem_t* sem)
{
    auto rc = pthread_mutex_lock(&sem->mtx);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    if (sem->value == 0) {
        pthread_mutex_unlock(&sem->mtx);
        errno = EAGAIN;
        return -1;
    }

    sem->value--;

    rc = pthread_mutex_unlock(&sem->mtx);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    return 0;
}

int sem_unlink(const char*)
{
    errno = ENOSYS;
    return -1;
}

int sem_wait(sem_t* sem)
{
    auto rc = pthread_mutex_lock(&sem->mtx);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    while (sem->value == 0) {
        rc = pthread_cond_wait(&sem->cv, &sem->mtx);
        if (rc != 0) {
            pthread_mutex_unlock(&sem->mtx);
            errno = rc;
            return -1;
        }
    }

    sem->value--;

    rc = pthread_mutex_unlock(&sem->mtx);
    if (rc != 0) {
        errno = rc;
        return -1;
    }

    return 0;
}
