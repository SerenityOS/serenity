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
#    ifndef NDEBUG
#        define VERIFY assert
#    else
#        define VERIFY(expr)              \
            (__builtin_expect(!(expr), 0) \
                    ? __builtin_trap()    \
                    : (void)0)
#    endif
#    define VERIFY_NOT_REACHED() VERIFY(false) /* NOLINT(cert-dcl03-c,misc-static-assert) No, this can't be static_assert, it's a runtime check */
static constexpr bool TODO = false;
#    define TODO() VERIFY(TODO)                /* NOLINT(cert-dcl03-c,misc-static-assert) No, this can't be static_assert, it's a runtime check */
#endif
