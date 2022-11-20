/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Assertions.h>
#include <AK/Platform.h>
#include <AK/StackInfo.h>
#include <stdio.h>
#include <string.h>

#ifdef AK_OS_SERENITY
#    include <serenity.h>
#elif defined(AK_OS_LINUX) or defined(AK_OS_MACOS)
#    include <pthread.h>
#elif defined(AK_OS_FREEBSD)
#    include <pthread.h>
#    include <pthread_np.h>
#endif

namespace AK {

StackInfo::StackInfo()
{
#ifdef AK_OS_SERENITY
    if (get_stack_bounds(&m_base, &m_size) < 0) {
        perror("get_stack_bounds");
        VERIFY_NOT_REACHED();
    }
#elif defined(AK_OS_LINUX) or defined(AK_OS_FREEBSD)
    int rc;
    pthread_attr_t attr;
    pthread_attr_init(&attr);

#    ifdef AK_OS_LINUX
    if ((rc = pthread_getattr_np(pthread_self(), &attr)) != 0) {
        fprintf(stderr, "pthread_getattr_np: %s\n", strerror(rc));
        VERIFY_NOT_REACHED();
    }
#    else
    if ((rc = pthread_attr_get_np(pthread_self(), &attr)) != 0) {
        fprintf(stderr, "pthread_attr_get_np: %s\n", strerror(rc));
        VERIFY_NOT_REACHED();
    }
#    endif

    if ((rc = pthread_attr_getstack(&attr, (void**)&m_base, &m_size)) != 0) {
        fprintf(stderr, "pthread_attr_getstack: %s\n", strerror(rc));
        VERIFY_NOT_REACHED();
    }
    pthread_attr_destroy(&attr);
#elif defined(AK_OS_MACOS)
    // NOTE: !! On MacOS, pthread_get_stackaddr_np gives the TOP of the stack, not the bottom!
    FlatPtr top_of_stack = (FlatPtr)pthread_get_stackaddr_np(pthread_self());
    m_size = (size_t)pthread_get_stacksize_np(pthread_self());
    // https://github.com/rust-lang/rust/issues/43347#issuecomment-316783599
    // https://developer.apple.com/library/archive/qa/qa1419/_index.html
    //
    // MacOS seems inconsistent on what stack size is given for the main thread.
    // According to the Apple docs, default for main thread is 8MB, and default for
    // other threads is 512KB
    constexpr size_t eight_megabytes = 0x800000;
    if (pthread_main_np() == 1 && m_size < eight_megabytes) {
        // Assume no one messed with stack size linker options for the main thread,
        // and just set it to 8MB.
        m_size = eight_megabytes;
    }
    m_base = top_of_stack - m_size;
#else
#    pragma message "StackInfo not supported on this platform! Recursion checks and stack scans may not work properly"
    m_size = (size_t)~0;
    m_base = 0;
#endif

    m_top = m_base + m_size;
}

}
