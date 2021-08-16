/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

#include <stdarg.h>

#ifdef _WIN64
#define EXPORT __declspec(dllexport)
#else
#define EXPORT
#endif

typedef struct {
    unsigned char* writeback;
    int* argids;
} call_info;

#define WRITEBACK_BYTES_PER_ARG 8
#define write_back_ptr(type) ((type*)(info->writeback + (i * WRITEBACK_BYTES_PER_ARG)))

// need to pass `num` separately as last argument preceding varargs according to spec (and for MSVC)
EXPORT void varargs(call_info* info, int num, ...) {
    va_list a_list;
    va_start(a_list, num);

    for (int i = 0; i < num; i++) {
        int id = info->argids[i];
        switch (id) {
            case 0: // int
                *write_back_ptr(int) = va_arg(a_list, int);
                break;
            case 1: // double
                *write_back_ptr(double) = va_arg(a_list, double);
                break;
        }
    }

    va_end(a_list);
}
