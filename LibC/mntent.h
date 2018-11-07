#pragma once

#include <sys/cdefs.h>
#include <stdio.h>

__BEGIN_DECLS

#define MOUNTED "/etc/mtab"
#define MNTTAB "/etc/fstab"

struct mntent {
    char* mnt_fsname;
    char* mnt_dir;
    char* mnt_type;
    char* mnt_opts;
    int mnt_freq;
    int mnt_passno;
};

struct mntent* getmntent(FILE* stream);

__END_DECLS

