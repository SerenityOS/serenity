/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <stdio.h>
#include <unistd.h>

int main(int, char**)
{
    char* tty = ttyname(0);
    if (!tty) {
        perror("Error");
        return 1;
    }
    outln("{}", tty);
    return 0;
}
