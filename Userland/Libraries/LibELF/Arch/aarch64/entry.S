/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.globl _invoke_entry
.hidden _invoke_entry
.type _invoke_entry,@function
_invoke_entry: # (argc, argv, envp, entry)
    # FIXME: Possibly incomplete.
    ret
