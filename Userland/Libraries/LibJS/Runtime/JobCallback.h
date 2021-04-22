/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Function.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

// 9.4.1 JobCallback Records, https://tc39.es/ecma262/#sec-jobcallback-records
struct JobCallback {
    Function* callback;
};

// 9.4.2 HostMakeJobCallback, https://tc39.es/ecma262/#sec-hostmakejobcallback
inline JobCallback make_job_callback(Function& callback)
{
    return { &callback };
}

// 9.4.3 HostCallJobCallback, https://tc39.es/ecma262/#sec-hostcalljobcallback
template<typename... Args>
[[nodiscard]] inline Value call_job_callback(VM& vm, JobCallback& job_callback, Value this_value, Args... args)
{
    VERIFY(job_callback.callback);
    auto& callback = *job_callback.callback;
    return vm.call(callback, this_value, args...);
}

}
