/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#if defined(KERNEL)
#    include <Kernel/Library/Assertions.h>
#else
#    include <assert.h>
extern "C" __attribute__((noreturn)) void ak_verification_failed(char const*);
#    if !defined(NDEBUG) && !defined(WIN32)
#        define VERIFY assert
#    else
#        define __stringify_helper(x) #x
#        define __stringify(x) __stringify_helper(x)
#        define VERIFY(expr)                                                                \
            (__builtin_expect(!(expr), 0)                                                   \
                    ? ak_verification_failed(#expr "\n" __FILE__ ":" __stringify(__LINE__)) \
                    : (void)0)
#    endif
#    define VERIFY_NOT_REACHED() VERIFY(false) /* NOLINT(cert-dcl03-c,misc-static-assert) No, this can't be static_assert, it's a runtime check */
static constexpr bool TODO = false;
#    define TODO() VERIFY(TODO)                /* NOLINT(cert-dcl03-c,misc-static-assert) No, this can't be static_assert, it's a runtime check */
#    define TODO_AARCH64() VERIFY(TODO)        /* NOLINT(cert-dcl03-c,misc-static-assert) No, this can't be static_assert, it's a runtime check */
#    define TODO_RISCV64() VERIFY(TODO)        /* NOLINT(cert-dcl03-c,misc-static-assert) No, this can't be static_assert, it's a runtime check */
#endif
