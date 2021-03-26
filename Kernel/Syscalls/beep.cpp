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

#include <Kernel/Devices/PCSpeaker.h>
#include <Kernel/Process.h>
#include <Kernel/Thread.h>

namespace Kernel {

static int stack_overflow(int a)
{
    return stack_overflow(a + 1) + stack_overflow(a + 2);
}

int Process::sys$beep()
{
    dbgln("BEEEEEEP");
    //auto* thread = Thread::current();
    //dbgln("thread kernel stack base=0x{:x} top={:x}", thread->kernel_stack_base(), thread->kernel_stack_top());

    //*(int*)(thread->kernel_stack_base() - 50) = 25;
    //return 0;
    return stack_overflow(0);
    /*
    PCSpeaker::tone_on(440);
    auto result = Thread::current()->sleep({ 0, 200 });
    PCSpeaker::tone_off();
    if (result.was_interrupted())
        return -EINTR;
    return 0;
    */
}

}
