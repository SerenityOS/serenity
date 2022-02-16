/*
 * Copyright (c) 2022, Sviatoslav Peleshko <speles@mail.ua>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <ftw.h>

extern "C" {

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/ftw.html
int ftw(const char* dirpath, int (*fn)(const char* fpath, const struct stat* sb, int typeflag), int nopenfd)
{
    (void)dirpath;
    (void)fn;
    (void)nopenfd;

    dbgln("FIXME: Implement ftw()");
    TODO();
}

// https://pubs.opengroup.org/onlinepubs/9699919799/functions/nftw.html
int nftw(const char* dirpath, int (*fn)(const char* fpath, const struct stat* sb, int typeflag, struct FTW* ftwbuf), int nopenfd, int flags)
{
    (void)dirpath;
    (void)fn;
    (void)nopenfd;
    (void)flags;

    dbgln("FIXME: Implement nftw()");
    TODO();
}
}
