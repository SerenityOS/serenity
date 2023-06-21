/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 * Copyright (c) 2023, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.globl _invoke_entry
.hidden _invoke_entry
.type _invoke_entry,@function
_invoke_entry: # (argc, argv, envp, entry)
    br x3
