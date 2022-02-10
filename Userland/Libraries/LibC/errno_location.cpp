/*
 * Copyright (c) 2018-2020, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <errno.h>

extern "C" {

int* __errno_location();

int* __errno_location()
{
    return &errno;
}
}
