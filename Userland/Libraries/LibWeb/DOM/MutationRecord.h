/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#mutationrecord
// NOTE: This is implemented as a pure virtual interface with the actual implementation in the CPP file to prevent this circular dependency: Node.h -> MutationRecord.h -> MutationObserver.h -> Node.h
//       This is also why this uses raw pointers and references, since using (NN)RP requires us to include the templated type, even in a header specifying a function return type.
class MutationRecord
    : public RefCounted<MutationRecord>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::MutationRecordWrapper;

    static NonnullRefPtr<MutationRecord> create(FlyString const& type, Node& target, NodeList& added_nodes, NodeList& removed_nodes, Node* previous_sibling, Node* next_sibling, String const& attribute_name, String const& attribute_namespace, String const& old_value);

    virtual ~MutationRecord() override = default;

    virtual FlyString const& type() const = 0;
    virtual Node const* target() const = 0;
    virtual NodeList const* added_nodes() const = 0;
    virtual NodeList const* removed_nodes() const = 0;
    virtual Node const* previous_sibling() const = 0;
    virtual Node const* next_sibling() const = 0;
    virtual String const& attribute_name() const = 0;
    virtual String const& attribute_namespace() const = 0;
    virtual String const& old_value() const = 0;
};

}
