/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/AsyncFromSyncIterator.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS {

// 27.1.4.2 The %AsyncFromSyncIteratorPrototype% Object, https://tc39.es/ecma262/#sec-%asyncfromsynciteratorprototype%-object
class AsyncFromSyncIteratorPrototype final : public PrototypeObject<AsyncFromSyncIteratorPrototype, AsyncFromSyncIterator> {
    JS_PROTOTYPE_OBJECT(AsyncFromSyncIteratorPrototype, AsyncFromSyncIterator, AsyncFromSyncIterator);

public:
    explicit AsyncFromSyncIteratorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~AsyncFromSyncIteratorPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
    JS_DECLARE_NATIVE_FUNCTION(return_);
    JS_DECLARE_NATIVE_FUNCTION(throw_);
};

ThrowCompletionOr<Object*> create_async_from_sync_iterator(GlobalObject&, Object& sync_iterator);

}
