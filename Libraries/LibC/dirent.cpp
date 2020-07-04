/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <Kernel/API/Syscall.h>
#include <dirent.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

extern "C" {

DIR* opendir(const char* name)
{
    int fd = open(name, O_RDONLY | O_DIRECTORY);
    if (fd == -1)
        return nullptr;
    DIR* dirp = (DIR*)malloc(sizeof(DIR));
    dirp->fd = fd;
    dirp->buffer = nullptr;
    dirp->buffer_size = 0;
    dirp->nextptr = nullptr;
    return dirp;
}

int closedir(DIR* dirp)
{
    if (!dirp || dirp->fd == -1)
        return -EBADF;
    if (dirp->buffer)
        free(dirp->buffer);
    int rc = close(dirp->fd);
    if (rc == 0)
        dirp->fd = -1;
    free(dirp);
    return rc;
}

struct [[gnu::packed]] sys_dirent
{
    ino_t ino;
    u8 file_type;
    size_t namelen;
    char name[];
    size_t total_size()
    {
        return sizeof(ino_t) + sizeof(u8) + sizeof(size_t) + sizeof(char) * namelen;
    }
};

static void create_struct_dirent(sys_dirent* sys_ent, struct dirent* str_ent)
{
    str_ent->d_ino = sys_ent->ino;
    str_ent->d_type = sys_ent->file_type;
    str_ent->d_off = 0;
    str_ent->d_reclen = sys_ent->total_size();
    for (size_t i = 0; i < sys_ent->namelen; ++i)
        str_ent->d_name[i] = sys_ent->name[i];
    // FIXME: I think this null termination behavior is not supposed to be here.
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
    ssize_t nread = syscall(SC_get_dir_entries, dirp->fd, dirp->buffer, size_to_allocate);
    if (nread < 0) {
        // uh-oh, the syscall returned an error
        free(dirp->buffer);
        dirp->buffer = nullptr;
        return -nread;
    }
    dirp->buffer_size = nread;
    dirp->nextptr = dirp->buffer;
    return 0;
}

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

int dirfd(DIR* dirp)
{
    ASSERT(dirp);
    return dirp->fd;
}
}
