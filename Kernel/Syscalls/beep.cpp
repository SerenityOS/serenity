/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/CommandLine.h>
#if ARCH(X86_64)
#    include <Kernel/Arch/x86_64/PCSpeaker.h>
#endif
#include <Kernel/Process.h>

namespace Kernel {

ErrorOr<FlatPtr> Process::sys$beep(int tone)
{
    VERIFY_NO_PROCESS_BIG_LOCK(this);
    if (!kernel_command_line().is_pc_speaker_enabled())
        return ENODEV;
    if (tone < 20 || tone > 20000)
        return EINVAL;
#if ARCH(X86_64)
    PCSpeaker::tone_on(tone);
    auto result = Thread::current()->sleep(Duration::from_nanoseconds(200'000'000));
    PCSpeaker::tone_off();
    if (result.was_interrupted())
        return EINTR;
    return 0;
#else
    return ENOTIMPL;
#endif
}

}
