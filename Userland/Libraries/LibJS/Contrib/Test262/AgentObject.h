/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/GlobalObject.h>
#include <LibJS/Runtime/Object.h>

namespace JS::Test262 {

class AgentObject final : public Object {
    JS_OBJECT(AgentObject, Object);

public:
    explicit AgentObject(Realm&);
    virtual void initialize(JS::Realm&) override;
    virtual ~AgentObject() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(monotonic_now);
    JS_DECLARE_NATIVE_FUNCTION(sleep);
};

}
