/*
 * Copyright (c) 2023, the SerenityOS contributors.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>

__BEGIN_DECLS

// If opterr is set (the default), print error messages to stderr.
extern int opterr;
// On errors, optopt is set to the erroneous *character*.
extern int optopt;
// Index of the next argument to process upon a getopt*() call.
extern int optind;
// If set, reset the internal state kept by getopt*(). You may also want to set
// optind to 1 in that case.
extern int optreset;
// After parsing an option that accept an argument, set to point to the argument
// value.
extern char* optarg;

int getopt(int argc, char* const* argv, char const* short_options);
int getsubopt(char** optionp, char* const* tokens, char** valuep);

__END_DECLS
