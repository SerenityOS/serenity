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

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <Kernel/Process.h>

namespace Kernel {

int Process::sys$pledge(Userspace<const Syscall::SC_pledge_params*> user_params)
{
    Syscall::SC_pledge_params params;
    if (!copy_from_user(&params, user_params))
        return -EFAULT;

    if (params.promises.length > 1024 || params.execpromises.length > 1024)
        return -E2BIG;

    String promises;
    if (params.promises.characters) {
        promises = copy_string_from_user(params.promises);
        if (promises.is_null())
            return -EFAULT;
    }

    String execpromises;
    if (params.execpromises.characters) {
        execpromises = copy_string_from_user(params.execpromises);
        if (execpromises.is_null())
            return -EFAULT;
    }

    auto parse_pledge = [&](auto& pledge_spec, u32& mask, u32 current_mask) {
        // We keep track of both separately because we want "-stdio stdio" to drop stdio.
        u32 drop_mask = 0, keep_mask = 0;

        auto parts = pledge_spec.split_view(' ');
        for (auto& part : parts) {
#define __ENUMERATE_PLEDGE_PROMISE(x)        \
    if (part == #x) {                        \
        keep_mask |= (1u << (u32)Pledge::x); \
        continue;                            \
    }                                        \
    if (part == "-" #x) {                    \
        drop_mask |= (1u << (u32)Pledge::x); \
        continue;                            \
    }
            ENUMERATE_PLEDGE_PROMISES
#undef __ENUMERATE_PLEDGE_PROMISE
            if (part == "all") {
                // We don't assign here because "stdio all" should fail if stdio was already dropped.
                keep_mask |= current_mask;
                continue;
            }
            if (part == "-all") {
                drop_mask = (u32)-1;
                continue;
            }
            if (part == "dns") {
                // "dns" is an alias for "unix" since DNS queries go via LookupServer
                keep_mask |= (1u << (u32)Pledge::unix);
                continue;
            }
            if (part == "-dns") {
                drop_mask |= (1u << (u32)Pledge::unix);
                continue;
            }

            mask = keep_mask & ~drop_mask;
            return false;
        }

        mask = keep_mask & ~drop_mask;
        return true;
    };

    Optional<u32> new_promises;
    Optional<u32> new_execpromises;

    if (!promises.is_null()) {
        new_promises = 0;
        if (!parse_pledge(promises, new_promises.value(), m_promises.value_or((u32)-1)))
            return -EINVAL;
        if (new_promises.value() & ~m_promises.value_or((u32)-1))
            return -EPERM;
    } else {
        new_promises = m_promises;
    }

    if (!execpromises.is_null()) {
        new_execpromises = 0;
        if (!parse_pledge(execpromises, new_execpromises.value(), m_execpromises.value_or((u32)-1)))
            return -EINVAL;
        if (new_execpromises.value() & ~m_execpromises.value_or((u32)-1))
            return -EPERM;
    } else {
        new_execpromises = m_execpromises;
    }

    m_promises = new_promises;
    m_execpromises = new_execpromises;

    return 0;
}

}
