/*
 * Copyright (c) 2021, David Tuin <davidot@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedFlyString.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibJS/Heap/Cell.h>

namespace JS {

struct PrivateName {
    PrivateName() = default;
    PrivateName(u64 unique_id, DeprecatedFlyString description)
        : unique_id(unique_id)
        , description(move(description))
    {
    }

    u64 unique_id { 0 };
    DeprecatedFlyString description;

    bool operator==(PrivateName const& rhs) const;
};

class PrivateEnvironment : public Cell {
    JS_CELL(PrivateEnvironment, Cell);

public:
    PrivateName resolve_private_identifier(DeprecatedFlyString const& identifier) const;

    void add_private_name(Badge<ClassExpression>, DeprecatedFlyString description);

private:
    explicit PrivateEnvironment(PrivateEnvironment* parent);

    virtual void visit_edges(Visitor&) override;

    auto find_private_name(DeprecatedFlyString const& description) const
    {
        return m_private_names.find_if([&](PrivateName const& private_name) {
            return private_name.description == description;
        });
    }

    static u64 s_next_id;

    GCPtr<PrivateEnvironment> m_outer_environment; // [[OuterEnv]]
    Vector<PrivateName> m_private_names;           // [[Names]]
    u64 m_unique_id;
};

}
