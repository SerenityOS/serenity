/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

// Includes essentially mandated by POSIX:
// https://pubs.opengroup.org/onlinepubs/9699919799/basedefs/semaphore.h.html
#include <fcntl.h>
#include <time.h>

#include <limits.h>
#include <pthread.h>
#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

#define SEM_FLAG_PROCESS_SHARED (1 << 0)
#define SEM_FLAG_NAMED (1 << 1)
typedef struct {
    uint32_t magic;
    uint32_t value;
    uint8_t flags;
} sem_t;

int sem_close(sem_t*);
int sem_destroy(sem_t*);
int sem_getvalue(sem_t*, int*);
int sem_init(sem_t*, int, unsigned int);
sem_t* sem_open(char const*, int, ...);
int sem_post(sem_t*);
int sem_trywait(sem_t*);
int sem_unlink(char const*);
int sem_wait(sem_t*);
int sem_timedwait(sem_t*, const struct timespec* abstime);

#define SEM_FAILED ((sem_t*)0)
#define SEM_VALUE_MAX INT_MAX

__END_DECLS
