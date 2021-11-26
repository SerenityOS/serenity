/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <sys/xattr.h>

ssize_t getxattr(const char*, const char*, void*, size_t)
{
    dbgln("FIXME: Implement getxattr()");
    return 0;
}

ssize_t lgetxattr(const char*, const char*, void*, size_t)
{
    dbgln("FIXME: Implement lgetxattr()");
    return 0;
}

ssize_t fgetxattr(int, const char*, void*, size_t)
{
    dbgln("FIXME: Implement fgetxattr()");
    return 0;
}

int setxattr(const char*, const char*, const void*, size_t, int)
{
    dbgln("FIXME: Implement setxattr()");
    return 0;
}

int lsetxattr(const char*, const char*, const void*, size_t, int)
{
    dbgln("FIXME: Implement lsetxattr()");
    return 0;
}

int fsetxattr(int, const char*, const void*, size_t, int)
{
    dbgln("FIXME: Implement fsetxattr()");
    return 0;
}

ssize_t listxattr(const char*, char*, size_t)
{
    dbgln("FIXME: Implement listxattr()");
    return 0;
}

ssize_t llistxattr(const char*, char*, size_t)
{
    dbgln("FIXME: Implement llistxattr()");
    return 0;
}

ssize_t flistxattr(int, char*, size_t)
{
    dbgln("FIXME: Implement flistxattr()");
    return 0;
}
