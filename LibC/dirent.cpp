#include <dirent.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <errno.h>
#include <Kernel/Syscall.h>

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
    int rc = close(dirp->fd);
    if (rc == 0)
        dirp->fd = -1;
    return rc;
}

struct sys_dirent {
    ino_t ino;
    byte file_type;
    size_t namelen;
    char name[];
    size_t total_size()
    {
        return sizeof(ino_t) + sizeof(byte) + sizeof(size_t) + sizeof(char) * namelen;
    }
} __attribute__ ((packed));

dirent* readdir(DIR* dirp)
{
    if (!dirp)
        return nullptr;
    if (dirp->fd == -1)
        return nullptr;

    if (!dirp->buffer) {
        // FIXME: Figure out how much to actually allocate.
        dirp->buffer = (char*)malloc(4096);
        ssize_t nread = Syscall::invoke(Syscall::GetDirEntries, (dword)dirp->fd, (dword)dirp->buffer, 4096);
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

}

