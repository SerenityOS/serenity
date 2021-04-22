/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <stdio.h>
#include <unistd.h>

int main(int, char**)
{
    if (reboot() < 0) {
        perror("reboot");
        return 1;
    }
    return 0;
}
