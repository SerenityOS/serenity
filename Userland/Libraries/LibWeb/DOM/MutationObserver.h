/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtrVector.h>
#include <AK/RefCounted.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/Bindings/CallbackType.h>
#include <LibWeb/Bindings/Wrappable.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/MutationRecord.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#dictdef-mutationobserverinit
struct MutationObserverInit {
    bool child_list { false };
    Optional<bool> attributes;
    Optional<bool> character_data;
    bool subtree { false };
    Optional<bool> attribute_old_value;
    Optional<bool> character_data_old_value;
    Optional<Vector<String>> attribute_filter;
};

// https://dom.spec.whatwg.org/#mutationobserver
class MutationObserver final
    : public RefCounted<MutationObserver>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::MutationObserverWrapper;

    static NonnullRefPtr<MutationObserver> create_with_global_object(HTML::Window& window_object, Bindings::CallbackType* callback)
    {
        return adopt_ref(*new MutationObserver(window_object, JS::make_handle(callback)));
    }

    virtual ~MutationObserver() override = default;

    ExceptionOr<void> observe(Node& target, MutationObserverInit options = {});
    void disconnect();
    NonnullRefPtrVector<MutationRecord> take_records();

    Vector<WeakPtr<Node>>& node_list() { return m_node_list; }
    Vector<WeakPtr<Node>> const& node_list() const { return m_node_list; }

    Bindings::CallbackType& callback() { return *m_callback; }

    void enqueue_record(Badge<Node>, NonnullRefPtr<MutationRecord> mutation_record)
    {
        m_record_queue.append(move(mutation_record));
    }

private:
    MutationObserver(HTML::Window& window_object, JS::Handle<Bindings::CallbackType> callback);

    // https://dom.spec.whatwg.org/#concept-mo-callback
    JS::Handle<Bindings::CallbackType> m_callback;

    // https://dom.spec.whatwg.org/#mutationobserver-node-list
    Vector<WeakPtr<Node>> m_node_list;

    // https://dom.spec.whatwg.org/#concept-mo-queue
    NonnullRefPtrVector<MutationRecord> m_record_queue;
};

// https://dom.spec.whatwg.org/#registered-observer
struct RegisteredObserver : public RefCounted<RegisteredObserver> {
    static NonnullRefPtr<RegisteredObserver> create(MutationObserver& observer, MutationObserverInit& options)
    {
        return adopt_ref(*new RegisteredObserver(observer, options));
    }

    RegisteredObserver(MutationObserver& observer, MutationObserverInit& options)
        : observer(observer)
        , options(options)
    {
    }

    virtual ~RegisteredObserver() = default;

    NonnullRefPtr<MutationObserver> observer;
    MutationObserverInit options;
};

// https://dom.spec.whatwg.org/#transient-registered-observer
struct TransientRegisteredObserver final : public RegisteredObserver {
    static NonnullRefPtr<TransientRegisteredObserver> create(MutationObserver& observer, MutationObserverInit& options, RegisteredObserver& source)
    {
        return adopt_ref(*new TransientRegisteredObserver(observer, options, source));
    }

    TransientRegisteredObserver(MutationObserver& observer, MutationObserverInit& options, RegisteredObserver& source)
        : RegisteredObserver(observer, options)
        , source(source)
    {
    }

    virtual ~TransientRegisteredObserver() override = default;

    NonnullRefPtr<RegisteredObserver> source;
};

}
