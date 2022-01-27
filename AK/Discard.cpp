/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Discard.h>

#if defined(__serenity__) && defined(KERNEL)
#    include <Kernel/Process.h>
#    include <Kernel/kstdio.h>
#endif

namespace AK::Detail {

void __discarded_non_discardable()
{
#if defined(__serenity__) && defined(KERNEL)
    auto trace_or_error = Processor::current_thread()->backtrace();
    if (!trace_or_error.is_error()) {
        auto trace = trace_or_error.release_value();
        dbgln("Backtrace:");
        kernelputstr(trace->characters(), trace->length());
    } else {
        dbgln("Failed to create backtrace: {}", trace_or_error.error());
    }
#else
    abort();
#endif
}

}
