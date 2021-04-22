/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

.global _init
.section .init
_init:
    push %ebp

.global _fini
.section .fini
_fini:
    push %ebp
