/*
 * Copyright (c) 2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

__BEGIN_DECLS

// If opterr is set (the default), print error messages to stderr.
extern int opterr;
// On errors, optopt is set to the erroneous *character*.
extern int optopt;
// Index of the next argument to process upon a getopt*() call.
extern int optind;
// If set, reset the internal state kept by getopt*(). You may also want to set
// optind to 1 in that case. Alternatively, setting optind to 0 is treated like
// doing both of the above.
extern int optreset;
// After parsing an option that accept an argument, set to point to the argument
// value.
extern char* optarg;

#define no_argument 0
#define required_argument 1
#define optional_argument 2

struct option {
    const char* name;
    int has_arg;
    int* flag;
    int val;
};

int getopt(int argc, char** argv, const char* short_options);
int getopt_long(int argc, char** argv, const char* short_options, const struct option* long_options, int* out_long_option_index);

__END_DECLS
