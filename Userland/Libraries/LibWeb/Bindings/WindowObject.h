/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021-2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/TypeCasts.h>
#include <AK/Variant.h>
#include <AK/Weakable.h>
#include <LibJS/Runtime/Completion.h>
#include <LibJS/Runtime/GlobalObject.h>
#include <LibWeb/Bindings/CallbackType.h>
#include <LibWeb/Bindings/CrossOriginAbstractOperations.h>
#include <LibWeb/Forward.h>
#include <LibWeb/HTML/GlobalEventHandlers.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/HTML/WindowEventHandlers.h>

namespace Web {
namespace Bindings {

class WindowObject
    : public JS::GlobalObject
    , public Weakable<WindowObject> {
    JS_OBJECT(WindowObject, JS::GlobalObject);

public:
    explicit WindowObject(JS::Realm&, HTML::Window&);
    virtual void initialize(JS::Realm&) override;
    virtual ~WindowObject() override = default;
};

}
}
