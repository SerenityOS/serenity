/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if defined(AK_OS_WINDOWS) && !defined(AK_COMPILER_GCC)
#    if defined(LibX86_EXPORTS)
#        define LibX86_API __declspec(dllexport)
#    else
#        define LibX86_API __declspec(dllimport)
#    endif
#else
#    define LibX86_API __attribute__((visibility("default")))
#endif
