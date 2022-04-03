/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <sys/xattr.h>

ssize_t getxattr(char const*, char const*, void*, size_t)
{
    dbgln("FIXME: Implement getxattr()");
    return 0;
}

ssize_t lgetxattr(char const*, char const*, void*, size_t)
{
    dbgln("FIXME: Implement lgetxattr()");
    return 0;
}

ssize_t fgetxattr(int, char const*, void*, size_t)
{
    dbgln("FIXME: Implement fgetxattr()");
    return 0;
}

int setxattr(char const*, char const*, void const*, size_t, int)
{
    dbgln("FIXME: Implement setxattr()");
    return 0;
}

int lsetxattr(char const*, char const*, void const*, size_t, int)
{
    dbgln("FIXME: Implement lsetxattr()");
    return 0;
}

int fsetxattr(int, char const*, void const*, size_t, int)
{
    dbgln("FIXME: Implement fsetxattr()");
    return 0;
}

ssize_t listxattr(char const*, char*, size_t)
{
    dbgln("FIXME: Implement listxattr()");
    return 0;
}

ssize_t llistxattr(char const*, char*, size_t)
{
    dbgln("FIXME: Implement llistxattr()");
    return 0;
}

ssize_t flistxattr(int, char*, size_t)
{
    dbgln("FIXME: Implement flistxattr()");
    return 0;
}
