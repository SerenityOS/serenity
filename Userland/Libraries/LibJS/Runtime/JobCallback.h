/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/FunctionObject.h>

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
    // 1. Return the JobCallback Record { [[Callback]]: callback, [[HostDefined]]: empty }.
    return { make_handle(&callback) };
}

// 9.5.3 HostCallJobCallback ( jobCallback, V, argumentsList ), https://tc39.es/ecma262/#sec-hostcalljobcallback
inline ThrowCompletionOr<Value> call_job_callback(GlobalObject& global_object, JobCallback& job_callback, Value this_value, MarkedVector<Value> arguments_list)
{
    // 1. Assert: IsCallable(jobCallback.[[Callback]]) is true.
    VERIFY(!job_callback.callback.is_null());

    // 2. Return ? Call(jobCallback.[[Callback]], V, argumentsList).
    return call(global_object, job_callback.callback.cell(), this_value, move(arguments_list));
}

}
