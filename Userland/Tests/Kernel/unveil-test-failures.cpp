/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <stdio.h>
#include <unistd.h>

int main()
{
    int res = unveil("/etc", "r");
    if (res < 0) {
        fprintf(stderr, "FAIL, unveil read only failed\n");
        return 1;
    }

    res = unveil("/etc", "w");
    if (res >= 0) {
        fprintf(stderr, "FAIL, unveil write permitted after unveil read only\n");
        return 1;
    }

    res = unveil("/etc", "x");
    if (res >= 0) {
        fprintf(stderr, "FAIL, unveil execute permitted after unveil read only\n");
        return 1;
    }

    res = unveil("/etc", "c");
    if (res >= 0) {
        fprintf(stderr, "FAIL, unveil create permitted after unveil read only\n");
        return 1;
    }

    res = unveil("/tmp/doesnotexist", "c");
    if (res < 0) {
        fprintf(stderr, "FAIL, unveil create on non-existent path failed\n");
        return 1;
    }

    res = unveil("/home", "b");
    if (res < 0) {
        fprintf(stderr, "FAIL, unveil browse failed\n");
        return 1;
    }

    res = unveil("/home", "w");
    if (res >= 0) {
        fprintf(stderr, "FAIL, unveil write permitted after unveil browse only\n");
        return 1;
    }

    res = unveil("/home", "x");
    if (res >= 0) {
        fprintf(stderr, "FAIL, unveil execute permitted after unveil browse only\n");
        return 1;
    }

    res = unveil("/home", "c");
    if (res >= 0) {
        fprintf(stderr, "FAIL, unveil create permitted after unveil browse only\n");
        return 1;
    }

    res = unveil(nullptr, nullptr);
    if (res < 0) {
        fprintf(stderr, "FAIL, unveil state lock failed\n");
        return 1;
    }

    res = unveil("/bin", "w");
    if (res >= 0) {
        fprintf(stderr, "FAIL, unveil permitted after unveil state locked\n");
        return 1;
    }

    printf("PASS\n");
    return 0;
}
