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

#ifdef __serenity__
#    include <serenity.h>
#elif __linux__ or __APPLE__
#    include <pthread.h>
#endif

namespace AK {

StackInfo::StackInfo()
{
#ifdef __serenity__
    if (get_stack_bounds(&m_base, &m_size) < 0) {
        perror("get_stack_bounds");
        ASSERT_NOT_REACHED();
    }
#elif __linux__
    pthread_attr_t attr = {};
    if (int rc = pthread_getattr_np(pthread_self(), &attr) != 0) {
        fprintf(stderr, "pthread_getattr_np: %s\n", strerror(-rc));
        ASSERT_NOT_REACHED();
    }
    if (int rc = pthread_attr_getstack(&attr, (void**)&m_base, &m_size) != 0) {
        fprintf(stderr, "pthread_attr_getstack: %s\n", strerror(-rc));
        ASSERT_NOT_REACHED();
    }
    pthread_attr_destroy(&attr);
#elif __APPLE__
    m_base = (FlatPtr)pthread_get_stackaddr_np(pthread_self());
    pthread_attr_t attr = {};
    if (int rc = pthread_attr_getstacksize(&attr, &m_size) != 0) {
        fprintf(stderr, "pthread_attr_getstacksize: %s\n", strerror(-rc));
        ASSERT_NOT_REACHED();
    }
    pthread_attr_destroy(&attr);
#else
    ASSERT_NOT_REACHED();
#endif

    m_top = m_base + m_size;
}

}
