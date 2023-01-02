/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/ScopeGuard.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <syscall.h>
#include <unistd.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/opendir.html
DIR* opendir(char const* name)
{
    int fd = open(name, O_RDONLY | O_DIRECTORY);
    if (fd == -1)
        return nullptr;
    return fdopendir(fd);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/fdopendir.html
DIR* fdopendir(int fd)
{
    if (fd == -1)
        return nullptr;
    DIR* dirp = (DIR*)malloc(sizeof(DIR));
    dirp->fd = fd;
    dirp->buffer = nullptr;
    dirp->buffer_size = 0;
    dirp->nextptr = nullptr;
    return dirp;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/closedir.html
int closedir(DIR* dirp)
{
    if (!dirp || dirp->fd == -1)
        return -EBADF;
    free(dirp->buffer);
    int rc = close(dirp->fd);
    if (rc == 0)
        dirp->fd = -1;
    free(dirp);
    return rc;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/rewinddir.html
void rewinddir(DIR* dirp)
{
    free(dirp->buffer);
    dirp->buffer = nullptr;
    dirp->buffer_size = 0;
    dirp->nextptr = nullptr;
    lseek(dirp->fd, 0, SEEK_SET);
}

struct [[gnu::packed]] sys_dirent {
    ino_t ino;
    u8 file_type;
    u32 namelen;
    char name[];
    size_t total_size()
    {
        return sizeof(ino_t) + sizeof(u8) + sizeof(u32) + sizeof(char) * namelen;
    }
};

static void create_struct_dirent(sys_dirent* sys_ent, struct dirent* str_ent)
{
    str_ent->d_ino = sys_ent->ino;
    str_ent->d_type = sys_ent->file_type;
    str_ent->d_off = 0;
    str_ent->d_reclen = sizeof(struct dirent);

    VERIFY(sizeof(str_ent->d_name) > sys_ent->namelen);

    // Note: We can't use any normal string function as sys_ent->name is
    // not null terminated. All string copy functions will attempt to read
    // the non-existent null terminator past the end of the source string.
    memcpy(str_ent->d_name, sys_ent->name, sys_ent->namelen);
    str_ent->d_name[sys_ent->namelen] = '\0';
}

static int allocate_dirp_buffer(DIR* dirp)
{
    if (dirp->buffer) {
        return 0;
    }

    struct stat st;
    // preserve errno since this could be a reentrant call
    int old_errno = errno;
    int rc = fstat(dirp->fd, &st);
    if (rc < 0) {
        int new_errno = errno;
        errno = old_errno;
        return new_errno;
    }
    size_t size_to_allocate = max(st.st_size, static_cast<off_t>(4096));
    dirp->buffer = (char*)malloc(size_to_allocate);
    if (!dirp->buffer)
        return ENOMEM;
    for (;;) {
        ssize_t nread = syscall(SC_get_dir_entries, dirp->fd, dirp->buffer, size_to_allocate);
        if (nread < 0) {
            if (nread == -EINVAL) {
                size_to_allocate *= 2;
                char* new_buffer = (char*)realloc(dirp->buffer, size_to_allocate);
                if (new_buffer) {
                    dirp->buffer = new_buffer;
                    continue;
                } else {
                    nread = -ENOMEM;
                }
            }
            // uh-oh, the syscall returned an error
            free(dirp->buffer);
            dirp->buffer = nullptr;
            return -nread;
        }
        dirp->buffer_size = nread;
        dirp->nextptr = dirp->buffer;
        break;
    }
    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/readdir.html
dirent* readdir(DIR* dirp)
{
    if (!dirp)
        return nullptr;
    if (dirp->fd == -1)
        return nullptr;

    if (int new_errno = allocate_dirp_buffer(dirp)) {
        // readdir is allowed to mutate errno
        errno = new_errno;
        return nullptr;
    }

    if (dirp->nextptr >= (dirp->buffer + dirp->buffer_size))
        return nullptr;

    auto* sys_ent = (sys_dirent*)dirp->nextptr;
    create_struct_dirent(sys_ent, &dirp->cur_ent);

    dirp->nextptr += sys_ent->total_size();
    return &dirp->cur_ent;
}

static bool compare_sys_struct_dirent(sys_dirent* sys_ent, struct dirent* str_ent)
{
    size_t namelen = min((size_t)256, sys_ent->namelen);
    // These fields are guaranteed by create_struct_dirent to be the same
    return sys_ent->ino == str_ent->d_ino
        && sys_ent->file_type == str_ent->d_type
        && sys_ent->total_size() == str_ent->d_reclen
        && strncmp(sys_ent->name, str_ent->d_name, namelen) == 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/readdir_r.html
int readdir_r(DIR* dirp, struct dirent* entry, struct dirent** result)
{
    if (!dirp || dirp->fd == -1) {
        *result = nullptr;
        return EBADF;
    }

    if (int new_errno = allocate_dirp_buffer(dirp)) {
        *result = nullptr;
        return new_errno;
    }

    // This doesn't care about dirp state; seek until we find the entry.
    // Unfortunately, we can't just compare struct dirent to sys_dirent, so
    // manually compare the fields. This seems a bit risky, but could work.
    auto* buffer = dirp->buffer;
    auto* sys_ent = (sys_dirent*)buffer;
    bool found = false;
    while (!(found || buffer >= dirp->buffer + dirp->buffer_size)) {
        found = compare_sys_struct_dirent(sys_ent, entry);

        // Make sure if we found one, it's the one after (end of buffer or not)
        buffer += sys_ent->total_size();
        sys_ent = (sys_dirent*)buffer;
    }

    // If we found one, but hit end of buffer, then EOD
    if (found && buffer >= dirp->buffer + dirp->buffer_size) {
        *result = nullptr;
        return 0;
    }
    // If we never found a match for entry in buffer, start from the beginning
    else if (!found) {
        buffer = dirp->buffer;
        sys_ent = (sys_dirent*)buffer;
    }

    *result = entry;
    create_struct_dirent(sys_ent, entry);

    return 0;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/dirfd.html
int dirfd(DIR* dirp)
{
    VERIFY(dirp);
    return dirp->fd;
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/alphasort.html
int alphasort(const struct dirent** d1, const struct dirent** d2)
{
    return strcoll((*d1)->d_name, (*d2)->d_name);
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/scandir.html
int scandir(char const* dir_name,
    struct dirent*** namelist,
    int (*select)(const struct dirent*),
    int (*compare)(const struct dirent**, const struct dirent**))
{
    auto dir = opendir(dir_name);
    if (dir == nullptr)
        return -1;
    ScopeGuard guard = [&] {
        closedir(dir);
    };

    Vector<struct dirent*> tmp_names;
    ScopeGuard names_guard = [&] {
        tmp_names.remove_all_matching([&](auto& entry) {
            free(entry);
            return true;
        });
    };

    while (true) {
        errno = 0;
        auto entry = readdir(dir);
        if (!entry)
            break;

        // Omit entries the caller chooses to ignore.
        if (select && !select(entry))
            continue;

        auto entry_copy = (struct dirent*)malloc(entry->d_reclen);
        if (!entry_copy)
            break;
        memcpy(entry_copy, entry, entry->d_reclen);
        tmp_names.append(entry_copy);
    }

    // Propagate any errors encountered while accumulating back to the user.
    if (errno) {
        return -1;
    }

    // Sort the entries if the user provided a comparator.
    if (compare) {
        qsort(tmp_names.data(), tmp_names.size(), sizeof(struct dirent*), (int (*)(void const*, void const*))compare);
    }

    int const size = tmp_names.size();
    auto** names = static_cast<struct dirent**>(kmalloc_array(size, sizeof(struct dirent*)));
    if (names == nullptr) {
        return -1;
    }
    for (auto i = 0; i < size; i++) {
        names[i] = tmp_names[i];
    }

    // Disable the scope guard which free's names on error.
    tmp_names.clear();

    *namelist = names;
    return size;
}
}
