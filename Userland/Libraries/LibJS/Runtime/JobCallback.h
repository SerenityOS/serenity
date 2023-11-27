/*
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/Completion.h>

namespace JS {

// 9.5.1 JobCallback Records, https://tc39.es/ecma262/#sec-jobcallback-records
struct JobCallback {
    struct CustomData {
        virtual ~CustomData() = default;
    };

    Handle<FunctionObject> callback;
    OwnPtr<CustomData> custom_data { nullptr };
};

JobCallback make_job_callback(FunctionObject& callback);
ThrowCompletionOr<Value> call_job_callback(VM&, JobCallback&, Value this_value, ReadonlySpan<Value> arguments_list);

}
