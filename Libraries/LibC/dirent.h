#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

struct dirent {
    ino_t d_ino;
    off_t d_off;
    unsigned short d_reclen;
    unsigned char d_type;
    char d_name[256];
};

struct __DIR {
    int fd;
    struct dirent cur_ent;
    char* buffer;
    size_t buffer_size;
    char* nextptr;
};
typedef struct __DIR DIR;

DIR* opendir(const char* name);
int closedir(DIR*);
struct dirent* readdir(DIR*);
int readdir_r(DIR*, struct dirent*, struct dirent**);
int dirfd(DIR*);

__END_DECLS
