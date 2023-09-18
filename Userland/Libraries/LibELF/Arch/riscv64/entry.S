/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.globl _invoke_entry
.hidden _invoke_entry
.type _invoke_entry,@function
_invoke_entry: # (argc, argv, envp, entry)
    jr a3
