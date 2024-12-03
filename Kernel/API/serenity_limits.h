/*
 * Copyright (c) 2022, sin-ack <sin-ack@protonmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#define PTHREAD_STACK_MAX (32 * 1024 * 1024) // 32MiB

#ifdef __serenity__
#    define SERENITY_PAGE_SIZE PAGE_SIZE
#else
#    define SERENITY_PAGE_SIZE 4096
#endif
