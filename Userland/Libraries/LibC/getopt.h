/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <bits/getopt.h>
#include <sys/cdefs.h>

__BEGIN_DECLS

#define no_argument 0
#define required_argument 1
#define optional_argument 2

struct option {
    char const* name;
    int has_arg;
    int* flag;
    int val;
};

int getopt_long(int argc, char* const* argv, char const* short_options, const struct option* long_options, int* out_long_option_index);

__END_DECLS
