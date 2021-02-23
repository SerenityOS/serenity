/*
 * Copyright (c) 2020, the SerenityOS developers.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Assertions.h>
#include <AK/StackInfo.h>
#include <stdio.h>
#include <string.h>

#ifdef __serenity__
#    include <serenity.h>
#elif defined(__linux__) or defined(__APPLE__)
#    include <pthread.h>
#endif

namespace AK {

StackInfo::StackInfo()
{
#ifdef __serenity__
    if (get_stack_bounds(&m_base, &m_size) < 0) {
        perror("get_stack_bounds");
        VERIFY_NOT_REACHED();
    }
#elif __linux__
    pthread_attr_t attr = {};
    if (int rc = pthread_getattr_np(pthread_self(), &attr) != 0) {
        fprintf(stderr, "pthread_getattr_np: %s\n", strerror(-rc));
        VERIFY_NOT_REACHED();
    }
    if (int rc = pthread_attr_getstack(&attr, (void**)&m_base, &m_size) != 0) {
        fprintf(stderr, "pthread_attr_getstack: %s\n", strerror(-rc));
        VERIFY_NOT_REACHED();
    }
    pthread_attr_destroy(&attr);
#elif __APPLE__
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
    VERIFY_NOT_REACHED();
#endif

    m_top = m_base + m_size;
}

}
