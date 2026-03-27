/*
 * Copyright (c) 2021, Max Wipfli <max.wipfli@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>

#ifdef KERNEL
#    include <Kernel/Library/KString.h>
#    include <Kernel/Library/MiniStdLib.h>
#else
#    include <AK/ByteString.h>
#    include <string.h>
#endif

namespace Kernel {

inline bool is_absolute_path(StringView path);
#ifdef KERNEL
inline void canonicalize_absolute_path(KString& absolute_path);
#else
ByteString canonicalize_absolute_path(ByteString absolute_path);
#endif

inline bool is_absolute_path(StringView path)
{
    return !path.is_empty() && path[0] == '/';
}

#ifdef KERNEL
inline void canonicalize_absolute_path(KString& absolute_path)
#else
ByteString canonicalize_absolute_path(ByteString absolute_path)
#endif
{
    VERIFY(is_absolute_path(absolute_path.view()));
    char* path = const_cast<char*>(absolute_path.characters());
    char* src = path;
    char* dst = path;

    while (*src) {
        /* Collapse multiple slashes */
        if (*src == '/') {
            if (dst == path || *(dst - 1) != '/')
                *dst++ = '/';

            src++;
            while (*src == '/')
                src++;
            continue;
        }

        /* Identify segment */
        char* seg = src;
        while (*src && *src != '/')
            src++;

        int len = src - seg;

        /* Handle "." */
        if (len == 1 && seg[0] == '.') {
            continue;
        }

        /* Handle ".." */
        if (len == 2 && seg[0] == '.' && seg[1] == '.') {
            if (dst > path + 1) {
                dst--;
                while (dst > path && *(dst - 1) != '/')
                    dst--;
            } else {
                dst = path + 1;
            }
            continue;
        }

        /* Add slash if needed */
        if (dst > path && *(dst - 1) != '/')
            *dst++ = '/';

        /* Copy segment */
        memcpy(dst, seg, len);
        dst += len;
    }

    /* Remove trailing slash unless root */
    if (dst > path + 1 && *(dst - 1) == '/')
        dst--;

    /* Ensure at least "/" */
    if (dst == path)
        *dst++ = '/';

    *dst = '\0';

#ifdef KERNEL
    absolute_path.decrease_size((dst - path));
#else
    return absolute_path.substring(0, (dst - path));
#endif
}

}
