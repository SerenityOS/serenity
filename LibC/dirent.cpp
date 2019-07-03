#include <AK/Assertions.h>
#include <AK/StdLibExtras.h>
#include <Kernel/Syscall.h>
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

dirent* readdir(DIR* dirp)
{
    if (!dirp)
        return nullptr;
    if (dirp->fd == -1)
        return nullptr;

    if (!dirp->buffer) {
        struct stat st;
        int rc = fstat(dirp->fd, &st);
        if (rc < 0)
            return nullptr;
        size_t size_to_allocate = max(st.st_size, 4096);
        dirp->buffer = (char*)malloc(size_to_allocate);
        ssize_t nread = syscall(SC_get_dir_entries, dirp->fd, dirp->buffer, size_to_allocate);
        dirp->buffer_size = nread;
        dirp->nextptr = dirp->buffer;
    }

    if (dirp->nextptr >= (dirp->buffer + dirp->buffer_size))
        return nullptr;

    auto* sys_ent = (sys_dirent*)dirp->nextptr;
    dirp->cur_ent.d_ino = sys_ent->ino;
    dirp->cur_ent.d_type = sys_ent->file_type;
    dirp->cur_ent.d_off = 0;
    dirp->cur_ent.d_reclen = sys_ent->total_size();
    for (size_t i = 0; i < sys_ent->namelen; ++i)
        dirp->cur_ent.d_name[i] = sys_ent->name[i];
    // FIXME: I think this null termination behavior is not supposed to be here.
    dirp->cur_ent.d_name[sys_ent->namelen] = '\0';

    dirp->nextptr += sys_ent->total_size();
    return &dirp->cur_ent;
}

int dirfd(DIR* dirp)
{
    ASSERT(dirp);
    return dirp->fd;
}
}
