/*
 * Copyright (c) 2021, Leon Albrecht <leon2002.la@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Platform.h>

#if ARCH(I386)
#    include "i386/regs.h"
#elif ARCH(X86_64)
#    include "x86_64/regs.h"
#endif
