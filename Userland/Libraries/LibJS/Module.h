/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Heap/Handle.h>
#include <LibJS/Runtime/Realm.h>

namespace JS {

// 16.2.1.4 Abstract Module Records, https://tc39.es/ecma262/#sec-abstract-module-records
class Module : public RefCounted<Module> {
public:
    virtual ~Module();

    Realm& realm() { return *m_realm.cell(); }
    Realm const& realm() const { return *m_realm.cell(); }

    Environment* environment() { return m_environment.cell(); }
    Object* namespace_() { return m_namespace.cell(); }

protected:
    explicit Module(Realm&);

private:
    // Handles are not safe unless we keep the VM alive.
    NonnullRefPtr<VM> m_vm;

    Handle<Realm> m_realm;             // [[Realm]]
    Handle<Environment> m_environment; // [[Environment]]
    Handle<Object> m_namespace;        // [[Namespace]]
};

}
