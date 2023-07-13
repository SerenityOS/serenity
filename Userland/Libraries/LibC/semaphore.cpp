/*
 * Copyright (c) 2021, Gunnar Beutner <gbeutner@serenityos.org>
 * Copyright (c) 2021, Sergey Bugaev <bugaevc@serenityos.org>
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Atomic.h>
#include <AK/HashMap.h>
#include <AK/ScopeGuard.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <bits/pthread_cancel.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <semaphore.h>
#include <serenity.h>
#include <string.h>
#include <sys/file.h>
#include <sys/mman.h>
#include <sys/stat.h>

static constexpr u32 SEM_MAGIC = 0x78951230;

// Whether sem_wait() or sem_post() is responsible for waking any sleeping
// threads.
static constexpr u32 POST_WAKES = 1 << 31;

static constexpr auto sem_path_prefix = "/tmp/semaphore/"sv;
static constexpr auto SEM_NAME_MAX = PATH_MAX - sem_path_prefix.length();

// The returned string will always contain a null terminator, since it is used in C APIs.
static ErrorOr<String> sem_name_to_path(char const* name)
{
    if (name[0] != '/')
        return EINVAL;
    ++name;

    auto name_length = strnlen(name, SEM_NAME_MAX);
    if (name[name_length])
        return ENAMETOOLONG;

    auto name_view = StringView { name, name_length };
    if (name_view.contains('/'))
        return EINVAL;

    StringBuilder builder;
    TRY(builder.try_append(sem_path_prefix));
    TRY(builder.try_append(name_view));

    TRY(builder.try_append_code_point(0));
    return builder.to_string();
}

struct NamedSemaphore {
    size_t times_opened { 0 };
    dev_t dev { 0 };
    ino_t ino { 0 };
    sem_t* sem { nullptr };
};

static HashMap<String, NamedSemaphore> s_named_semaphores;
static pthread_mutex_t s_sem_mutex = PTHREAD_MUTEX_INITIALIZER;
static pthread_once_t s_sem_once = PTHREAD_ONCE_INIT;

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_open.html
sem_t* sem_open(char const* name, int flags, ...)
{
    auto path_or_error = sem_name_to_path(name);
    if (path_or_error.is_error()) {
        errno = path_or_error.error().code();
        return SEM_FAILED;
    }
    auto path = path_or_error.release_value();

    if (flags & ~(O_CREAT | O_EXCL)) {
        errno = EINVAL;
        return SEM_FAILED;
    }

    mode_t mode = 0;
    unsigned int value = 0;
    if (flags & O_CREAT) {
        va_list ap;
        va_start(ap, flags);
        mode = va_arg(ap, unsigned int);
        value = va_arg(ap, unsigned int);
        va_end(ap);
    }

    // Ensure we are not in the middle of modifying this structure while a child is being forked, which will cause the child to end up with a partially-modified entry
    pthread_once(&s_sem_once, []() {
        pthread_atfork([]() { pthread_mutex_lock(&s_sem_mutex); }, []() { pthread_mutex_unlock(&s_sem_mutex); }, []() { pthread_mutex_unlock(&s_sem_mutex); });
    });

    pthread_mutex_lock(&s_sem_mutex);
    ScopeGuard unlock_guard = [] { pthread_mutex_unlock(&s_sem_mutex); };

    // sem_name_to_path guarantees a null terminator.
    int fd = open(path.bytes_as_string_view().characters_without_null_termination(), O_RDWR | O_CLOEXEC | flags, mode);
    if (fd == -1)
        return SEM_FAILED;

    ScopeGuard close_guard = [&fd] {
        if (fd != -1)
            close(fd);
    };

    if (flock(fd, LOCK_EX) == -1)
        return SEM_FAILED;

    struct stat statbuf;
    if (fstat(fd, &statbuf) == -1)
        return SEM_FAILED;

    auto existing_semaphore = s_named_semaphores.get(path);
    if (existing_semaphore.has_value()) {
        // If the file did not exist (aka if O_CREAT && O_EXCL but no EEXIST), or if the inode was replaced, remove the entry and start from scratch
        if ((flags & (O_CREAT | O_EXCL)) == (O_CREAT | O_EXCL) || existing_semaphore->dev != statbuf.st_dev || existing_semaphore->ino != statbuf.st_ino) {
            s_named_semaphores.remove(path);
        } else { // otherwise, this is valid pre-existing named semaphore, so just increase the count and return it
            existing_semaphore->times_opened++;
            return existing_semaphore->sem;
        }
    }

    // If the file is smaller than the size, it's an uninitialized semaphore, so let's write an initial value
    if (statbuf.st_size < (off_t)sizeof(sem_t)) {
        sem_t init_sem;
        init_sem.magic = SEM_MAGIC;
        init_sem.value = value;
        init_sem.flags = SEM_FLAG_PROCESS_SHARED | SEM_FLAG_NAMED;
        if (write(fd, &init_sem, sizeof(sem_t)) != sizeof(sem_t))
            return SEM_FAILED;
    }

    if (flock(fd, LOCK_UN) == -1)
        return SEM_FAILED;

    auto* sem = (sem_t*)mmap(nullptr, sizeof(sem_t), PROT_READ | PROT_WRITE, MAP_SHARED, fd, 0);
    if (sem == MAP_FAILED)
        return SEM_FAILED;

    ArmedScopeGuard munmap_guard = [&sem] {
        munmap(sem, sizeof(sem_t));
    };

    if (sem->magic != SEM_MAGIC) {
        errno = EINVAL;
        return SEM_FAILED;
    }

    auto result = s_named_semaphores.try_set(move(path), { .times_opened = 1, .dev = statbuf.st_dev, .ino = statbuf.st_ino, .sem = sem });
    if (result.is_error()) {
        errno = result.error().code();
        return SEM_FAILED;
    }

    munmap_guard.disarm();
    return sem;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_close.html
int sem_close(sem_t* sem)
{
    if (sem->magic != SEM_MAGIC) {
        errno = EINVAL;
        return -1;
    }

    if ((sem->flags & SEM_FLAG_NAMED) == 0) {
        errno = EINVAL;
        return -1;
    }

    pthread_mutex_lock(&s_sem_mutex);
    ScopeGuard unlock_guard = [] { pthread_mutex_unlock(&s_sem_mutex); };

    auto it = s_named_semaphores.begin();
    for (; it != s_named_semaphores.end(); ++it) {
        if (it->value.sem != sem)
            continue;
        auto is_last = --it->value.times_opened == 0;
        if (is_last) {
            munmap(it->value.sem, sizeof(sem_t));
            s_named_semaphores.remove(it);
        }
        return 0;
    }

    errno = EINVAL;
    return -1;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_unlink.html
int sem_unlink(char const* name)
{
    auto path_or_error = sem_name_to_path(name);
    if (path_or_error.is_error()) {
        errno = path_or_error.error().code();
        return -1;
    }
    auto path = path_or_error.release_value();

    return unlink(path.bytes_as_string_view().characters_without_null_termination());
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_init.html
int sem_init(sem_t* sem, int process_shared, unsigned int value)
{
    if (value > SEM_VALUE_MAX) {
        errno = EINVAL;
        return -1;
    }

    sem->magic = SEM_MAGIC;
    sem->value = value;
    sem->flags = process_shared ? SEM_FLAG_PROCESS_SHARED : 0;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_destroy.html
int sem_destroy(sem_t* sem)
{
    if (sem->magic != SEM_MAGIC) {
        errno = EINVAL;
        return -1;
    }

    if (sem->flags & SEM_FLAG_NAMED) {
        errno = EINVAL;
        return -1;
    }

    sem->magic = 0;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_getvalue.html
int sem_getvalue(sem_t* sem, int* sval)
{
    if (sem->magic != SEM_MAGIC) {
        errno = EINVAL;
        return -1;
    }

    u32 value = AK::atomic_load(&sem->value, AK::memory_order_relaxed);
    *sval = value & ~POST_WAKES;
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_post.html
int sem_post(sem_t* sem)
{
    if (sem->magic != SEM_MAGIC) {
        errno = EINVAL;
        return -1;
    }

    u32 value = AK::atomic_fetch_add(&sem->value, 1u, AK::memory_order_release);
    // Fast path: no need to wake.
    if (!(value & POST_WAKES)) [[likely]]
        return 0;

    // Pass the responsibility for waking more threads if more slots become
    // available later to sem_wait() in the thread we're about to wake, as
    // opposed to further sem_post() calls that free up those slots.
    value = AK::atomic_fetch_and(&sem->value, ~POST_WAKES, AK::memory_order_relaxed);
    // Check if another sem_post() call has handled it already.
    if (!(value & POST_WAKES)) [[likely]]
        return 0;
    int rc = futex_wake(&sem->value, 1, sem->flags & SEM_FLAG_PROCESS_SHARED);
    VERIFY(rc >= 0);
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_trywait.html
int sem_trywait(sem_t* sem)
{
    if (sem->magic != SEM_MAGIC) {
        errno = EINVAL;
        return -1;
    }

    u32 value = AK::atomic_load(&sem->value, AK::memory_order_relaxed);
    u32 count = value & ~POST_WAKES;
    if (count == 0) {
        errno = EAGAIN;
        return -1;
    }
    // Decrement the count without touching the flag.
    u32 desired = (count - 1) | (value & POST_WAKES);
    bool exchanged = AK::atomic_compare_exchange_strong(&sem->value, value, desired, AK::memory_order_acquire);
    if (exchanged) [[likely]] {
        return 0;
    } else {
        errno = EAGAIN;
        return -1;
    }
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_wait.html
int sem_wait(sem_t* sem)
{
    if (sem->magic != SEM_MAGIC) {
        errno = EINVAL;
        return -1;
    }

    return sem_timedwait(sem, nullptr);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/sem_timedwait.html
int sem_timedwait(sem_t* sem, const struct timespec* abstime)
{
    __pthread_maybe_cancel();

    if (sem->magic != SEM_MAGIC) {
        errno = EINVAL;
        return -1;
    }

    u32 value = AK::atomic_load(&sem->value, AK::memory_order_relaxed);
    bool responsible_for_waking = false;
    bool process_shared = sem->flags & SEM_FLAG_PROCESS_SHARED;

    while (true) {
        u32 count = value & ~POST_WAKES;
        if (count > 0) [[likely]] {
            // It looks like there are some free slots.
            u32 whether_post_wakes = value & POST_WAKES;
            bool going_to_wake = false;
            if (responsible_for_waking && !whether_post_wakes) {
                // If we have ourselves been woken up previously, and the
                // POST_WAKES flag is not set, that means some more slots might
                // be available now, and it's us who has to wake up additional
                // threads.
                if (count > 1) [[unlikely]]
                    going_to_wake = true;
                // Pass the responsibility for waking up further threads back to
                // sem_post() calls. In particular, we don't want the threads
                // we're about to wake to try to wake anyone else.
                whether_post_wakes = POST_WAKES;
            }
            // Now, try to commit this.
            u32 desired = (count - 1) | whether_post_wakes;
            bool exchanged = AK::atomic_compare_exchange_strong(&sem->value, value, desired, AK::memory_order_acquire);
            if (!exchanged) [[unlikely]]
                // Re-evaluate.
                continue;
            if (going_to_wake) [[unlikely]] {
                int rc = futex_wake(&sem->value, count - 1, process_shared);
                VERIFY(rc >= 0);
            }
            return 0;
        }
        // We're probably going to sleep, so attempt to set the flag. We do not
        // commit to sleeping yet, though, as setting the flag may fail and
        // cause us to reevaluate what we're doing.
        if (value == 0) {
            bool exchanged = AK::atomic_compare_exchange_strong(&sem->value, value, POST_WAKES, AK::memory_order_relaxed);
            if (!exchanged) [[unlikely]]
                // Re-evaluate.
                continue;
            value = POST_WAKES;
        }
        // At this point, we're committed to sleeping.
        responsible_for_waking = true;
        futex_wait(&sem->value, value, abstime, CLOCK_REALTIME, process_shared);
        // This is the state we will probably see upon being waked:
        value = 1;
    }
}
