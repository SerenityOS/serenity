/*
 * Copyright (c) 2022, Łukasz Maciejewski <lukasz.m.maciejewski@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Optional.h>
#include <AK/RefCounted.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibWeb/Bindings/CallbackType.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/MutationRecord.h>
#include <LibWeb/DOM/Node.h>

namespace Web::DOM {

struct MutationObserverInit {
    Optional<bool> child_list = false;
    Optional<bool> attributes;
    Optional<bool> character_data;
    Optional<bool> subtree = false;
    Optional<bool> attribute_old_value;
    Optional<bool> character_data_old_value;
    Vector<String> attribute_filter;
};

class MutationObserver
    : public RefCounted<MutationObserver>
    , public Bindings::Wrappable {

    Bindings::WindowObject& m_js_global_object;
    Bindings::CallbackType m_callback;
    NonnullRefPtrVector<Node> m_observed_nodes;

public:
    using WrapperType = Bindings::MutationObserverWrapper;

    static NonnullRefPtr<MutationObserver> create_with_global_object(Bindings::WindowObject& js_global_object, Bindings::CallbackType const& callback);

    MutationObserver(Bindings::WindowObject& js_global_object, Bindings::CallbackType callback);

    void observe(Node&, MutationObserverInit const& options = {});
    void disconnect();
    Vector<MutationRecord> take_records();

    void notify(NonnullRefPtr<MutationRecord> data);
};

}
