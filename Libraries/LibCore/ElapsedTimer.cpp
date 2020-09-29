/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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
#include <AK/Time.h>
#include <LibCore/ElapsedTimer.h>
#include <sys/time.h>
#include <time.h>

namespace Core {

void ElapsedTimer::start()
{
    m_valid = true;
    timespec now_spec;
    clock_gettime(CLOCK_MONOTONIC, &now_spec);
    m_origin_time.tv_sec = now_spec.tv_sec;
    m_origin_time.tv_usec = now_spec.tv_nsec / 1000;
}

int ElapsedTimer::elapsed() const
{
    ASSERT(is_valid());
    struct timeval now;
    timespec now_spec;
    clock_gettime(CLOCK_MONOTONIC, &now_spec);
    now.tv_sec = now_spec.tv_sec;
    now.tv_usec = now_spec.tv_nsec / 1000;
    struct timeval diff;
    timeval_sub(now, m_origin_time, diff);
    return diff.tv_sec * 1000 + diff.tv_usec / 1000;
}

}
