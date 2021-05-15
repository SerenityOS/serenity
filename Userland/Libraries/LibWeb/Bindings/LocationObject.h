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

class LocationObject final : public JS::Object {
    JS_OBJECT(LocationObject, JS::Object);

public:
    explicit LocationObject(JS::GlobalObject&);
    virtual void initialize(JS::GlobalObject&) override;
    virtual ~LocationObject() override;

private:
    JS_DECLARE_NATIVE_FUNCTION(reload);

    JS_DECLARE_NATIVE_GETTER(href_getter);
    JS_DECLARE_NATIVE_SETTER(href_setter);

    JS_DECLARE_NATIVE_GETTER(host_getter);
    JS_DECLARE_NATIVE_GETTER(hostname_getter);
    JS_DECLARE_NATIVE_GETTER(pathname_getter);
    JS_DECLARE_NATIVE_GETTER(hash_getter);
    JS_DECLARE_NATIVE_GETTER(search_getter);
    JS_DECLARE_NATIVE_GETTER(protocol_getter);
};

}
}
