/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/PageDirectory.h>
#elif ARCH(AARCH64)
#    include <Kernel/Arch/aarch64/PageDirectory.h>
#elif ARCH(RISCV64)
#    include <Kernel/Arch/riscv64/PageDirectory.h>
#else
#    error "Unknown architecture"
#endif
