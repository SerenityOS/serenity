/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibWeb/Forward.h>

namespace Web {
namespace Bindings {

class NavigatorObject final : public JS::Object {
    JS_OBJECT(NavigatorObject, JS::Object);

public:
    NavigatorObject(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~NavigatorObject() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(user_agent_getter);
    JS_DECLARE_NATIVE_FUNCTION(cookie_enabled_getter);
};

}
}
