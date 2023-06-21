/*
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Fetch/Infrastructure/ConnectionTimingInfo.h>

namespace Web::Fetch::Infrastructure {

ConnectionTimingInfo::ConnectionTimingInfo() = default;

JS::NonnullGCPtr<ConnectionTimingInfo> ConnectionTimingInfo::create(JS::VM& vm)
{
    return vm.heap().allocate_without_realm<ConnectionTimingInfo>();
}

}
