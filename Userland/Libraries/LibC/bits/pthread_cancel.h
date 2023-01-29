/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <sys/cdefs.h>
#include <sys/types.h>

__BEGIN_DECLS

// This is our hook for cancellation points.
#ifdef _DYNAMIC_LOADER
inline void __pthread_maybe_cancel(void)
{
}
#else
void __pthread_maybe_cancel(void);
#endif

__END_DECLS
