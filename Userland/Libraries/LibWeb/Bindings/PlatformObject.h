/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Weakable.h>
#include <LibJS/Heap/GCPtr.h>
#include <LibJS/Runtime/Object.h>
#include <LibWeb/Forward.h>

namespace Web::Bindings {

#define WEB_PLATFORM_OBJECT(class_, base_class) \
    JS_OBJECT(class_, base_class)               \
    auto& impl()                                \
    {                                           \
        return *this;                           \
    }                                           \
    auto const& impl() const                    \
    {                                           \
        return *this;                           \
    }

#define WRAPPER_HACK(class_, namespace_)        \
    namespace Web::Bindings {                   \
    using class_##Wrapper = namespace_::class_; \
    }

// https://webidl.spec.whatwg.org/#dfn-platform-object
class PlatformObject
    : public JS::Object
    , public Weakable<PlatformObject> {
    JS_OBJECT(PlatformObject, JS::Object);

public:
    virtual ~PlatformObject() override;

    JS::Realm& realm() const;

    // FIXME: This should return a type that works in both window and worker contexts.
    HTML::Window& global_object() const;

protected:
    PlatformObject(JS::Realm&);
    explicit PlatformObject(JS::Object& prototype);
};

}
