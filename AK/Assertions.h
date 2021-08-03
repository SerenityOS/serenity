/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#if defined(KERNEL)
#    include <Kernel/Assertions.h>
#else
#    include <assert.h>
#    define VERIFY assert
#    define VERIFY_NOT_REACHED() assert(false)
static constexpr bool TODO = false;
#    define TODO() VERIFY(TODO)
#endif
