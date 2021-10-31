/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Object.h>
#include <LibWeb/CSS/Serialize.h>

namespace Web::Bindings {

// The `CSS` namespace object in IDL. https://www.w3.org/TR/cssom-1/#namespacedef-css
class CSSNamespace final : public JS::Object {
    JS_OBJECT(CSSNamespace, JS::Object)

public:
    explicit CSSNamespace(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~CSSNamespace() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(escape);
    JS_DECLARE_NATIVE_FUNCTION(supports);
};

}
