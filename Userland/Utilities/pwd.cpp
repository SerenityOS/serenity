/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

int main(int, char**)
{
    char* cwd = getcwd(nullptr, 0);
    puts(cwd);
    free(cwd);
    return 0;
}
