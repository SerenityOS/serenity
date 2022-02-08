/*
 * Copyright (c) 2021, Linus Groh <linusg@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Weakable.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

// 9.3 Realms, https://tc39.es/ecma262/#realm-record
class Realm final
    : public Cell
    , public Weakable<Realm> {
public:
    struct HostDefined {
        virtual ~HostDefined() = default;
    };

    Realm() = default;

    static Realm* create(VM&);

    void set_global_object(GlobalObject&, Object* this_value = nullptr);

    [[nodiscard]] GlobalObject& global_object() const { return *m_global_object; }
    [[nodiscard]] GlobalEnvironment& global_environment() const { return *m_global_environment; }

    HostDefined* host_defined() { return m_host_defined; }
    void set_host_defined(OwnPtr<HostDefined> host_defined) { m_host_defined = move(host_defined); }

private:
    virtual char const* class_name() const override { return "Realm"; }
    virtual void visit_edges(Visitor&) override;

    GlobalObject* m_global_object { nullptr };           // [[GlobalObject]]
    GlobalEnvironment* m_global_environment { nullptr }; // [[GlobalEnv]]
    OwnPtr<HostDefined> m_host_defined;                  // [[HostDefined]]
};

}
