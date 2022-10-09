/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/HashMap.h>
#include <LibJS/Forward.h>
#include <LibJS/Heap/Cell.h>
#include <LibJS/Heap/Heap.h>
#include <LibJS/Runtime/VM.h>
#include <LibWeb/Bindings/HostDefined.h>

namespace Web::Bindings {

class Intrinsics final : public JS::Cell {
    JS_CELL(Intrinsics, JS::Cell);

public:
    Intrinsics(JS::Realm& realm)
        : m_realm(realm)
    {
    }

    JS::Object& cached_web_prototype(String const& class_name);

    template<typename T>
    JS::Object& ensure_web_prototype(String const& class_name)
    {
        auto it = m_prototypes.find(class_name);
        if (it != m_prototypes.end())
            return *it->value;
        auto& realm = *m_realm;
        auto* prototype = heap().allocate<T>(realm, realm);
        m_prototypes.set(class_name, prototype);
        return *prototype;
    }

    template<typename T>
    JS::NativeFunction& ensure_web_constructor(String const& class_name)
    {
        auto it = m_constructors.find(class_name);
        if (it != m_constructors.end())
            return *it->value;
        auto& realm = *m_realm;
        auto* constructor = heap().allocate<T>(realm, realm);
        m_constructors.set(class_name, constructor);
        return *constructor;
    }

private:
    virtual void visit_edges(JS::Cell::Visitor&) override;

    HashMap<String, JS::Object*> m_prototypes;
    HashMap<String, JS::NativeFunction*> m_constructors;

    JS::NonnullGCPtr<JS::Realm> m_realm;
};

[[nodiscard]] inline Intrinsics& host_defined_intrinsics(JS::Realm& realm)
{
    return *verify_cast<HostDefined>(realm.host_defined())->intrinsics;
}

template<typename T>
[[nodiscard]] JS::Object& ensure_web_prototype(JS::Realm& realm, String const& class_name)
{
    return host_defined_intrinsics(realm).ensure_web_prototype<T>(class_name);
}

template<typename T>
[[nodiscard]] JS::NativeFunction& ensure_web_constructor(JS::Realm& realm, String const& class_name)
{
    return host_defined_intrinsics(realm).ensure_web_constructor<T>(class_name);
}

[[nodiscard]] inline JS::Object& cached_web_prototype(JS::Realm& realm, String const& class_name)
{
    return host_defined_intrinsics(realm).cached_web_prototype(class_name);
}

}
