/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Heap/Heap.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/WorkerGlobalScope.h>
#include <LibWeb/HTML/WorkerNavigator.h>

namespace Web::HTML {

JS::NonnullGCPtr<WorkerNavigator> WorkerNavigator::create(WorkerGlobalScope& global_scope)
{
    return *global_scope.heap().allocate<WorkerNavigator>(global_scope.realm(), global_scope);
}

WorkerNavigator::WorkerNavigator(WorkerGlobalScope& global_scope)
    : PlatformObject(global_scope.realm())
{
    set_prototype(&Bindings::cached_web_prototype(global_scope.realm(), "WorkerNavigator"));
}

WorkerNavigator::~WorkerNavigator() = default;

}
