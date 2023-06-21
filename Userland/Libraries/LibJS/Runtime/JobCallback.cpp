/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/AbstractOperations.h>
#include <LibJS/Runtime/FunctionObject.h>
#include <LibJS/Runtime/JobCallback.h>

namespace JS {

// 9.5.2 HostMakeJobCallback ( callback ), https://tc39.es/ecma262/#sec-hostmakejobcallback
JobCallback make_job_callback(FunctionObject& callback)
{
    // 1. Return the JobCallback Record { [[Callback]]: callback, [[HostDefined]]: empty }.
    return { make_handle(&callback) };
}

// 9.5.3 HostCallJobCallback ( jobCallback, V, argumentsList ), https://tc39.es/ecma262/#sec-hostcalljobcallback
ThrowCompletionOr<Value> call_job_callback(VM& vm, JobCallback& job_callback, Value this_value, MarkedVector<Value> arguments_list)
{
    // 1. Assert: IsCallable(jobCallback.[[Callback]]) is true.
    VERIFY(!job_callback.callback.is_null());

    // 2. Return ? Call(jobCallback.[[Callback]], V, argumentsList).
    return call(vm, job_callback.callback.cell(), this_value, move(arguments_list));
}

}
