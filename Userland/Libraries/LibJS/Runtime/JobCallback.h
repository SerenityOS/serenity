/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/VM.h>

namespace JS {

// 9.5.1 JobCallback Records, https://tc39.es/ecma262/#sec-jobcallback-records
struct JobCallback {
    struct CustomData {
        virtual ~CustomData() = default;
    };

    Handle<FunctionObject> callback;
    OwnPtr<CustomData> custom_data { nullptr };
};

// 9.5.2 HostMakeJobCallback ( callback ), https://tc39.es/ecma262/#sec-hostmakejobcallback
inline JobCallback make_job_callback(FunctionObject& callback)
{
    return { make_handle(&callback) };
}

// 9.5.3 HostCallJobCallback ( jobCallback, V, argumentsList ), https://tc39.es/ecma262/#sec-hostcalljobcallback
[[nodiscard]] inline Value call_job_callback(VM& vm, JobCallback& job_callback, Value this_value, MarkedValueList args)
{
    VERIFY(!job_callback.callback.is_null());
    auto& callback = *job_callback.callback.cell();
    return TRY_OR_DISCARD(vm.call(callback, this_value, move(args)));
}

}
