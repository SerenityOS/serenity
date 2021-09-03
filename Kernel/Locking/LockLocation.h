/*
 * Copyright (c) 2021, Brian Gianforcaro <bgianf@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#if LOCK_DEBUG
#    include <YAK/SourceLocation.h>
#endif

// Abstract SourceLocation away from the kernel's locking API to avoid a
// significant amount of #ifdefs in Mutex / MutexLocker / etc.
//
// To do this we declare LockLocation to be a zero sized struct which will
// get optimized out during normal compilation. When LOCK_DEBUG is enabled,
// we forward the implementation to YAK::SourceLocation and get rich debugging
// information for every caller.

namespace Kernel {

#if LOCK_DEBUG
using LockLocation = SourceLocation;
#else
struct LockLocation {
    static constexpr LockLocation current() { return {}; }
};
#endif

}
