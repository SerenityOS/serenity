/*
 * Copyright (c) 2022, Łukasz Maciejewski <lukasz.m.maciejewski@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <AK/Forward.h>
#include <LibJS/Runtime/Value.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/MutationRecordWrapper.h>
#include <LibWeb/DOM/MutationObserver.h>
#include <LibWeb/DOM/MutationRecord.h>
#include <LibWeb/DOM/NodeList.h>

namespace Web::DOM {

NonnullRefPtr<MutationObserver> MutationObserver::create_with_global_object(
    Bindings::WindowObject& js_global_object,
    Bindings::CallbackType const& callback)
{
    return make_ref_counted<MutationObserver>(js_global_object, callback);
}

MutationObserver::MutationObserver(Bindings::WindowObject& js_global_object, Bindings::CallbackType callback)
    : m_js_global_object(js_global_object)
    , m_callback { std::move(callback) }
{
}

namespace {

Node::MutationObserverParameters init_to_parameters(MutationObserverInit const& options)
{
    // TODO: this is not yet exactly up to spec
    // https://dom.spec.whatwg.org/#interface-mutationobserver - the observe method
    return {
        options.child_list.value_or(false),
        options.attributes.value_or(false) or options.attribute_old_value.value_or(false),
        options.character_data.value_or(false) or options.character_data_old_value.value_or(false),
        options.subtree.value_or(false),
        options.attribute_filter,
    };
}

}

void MutationObserver::observe(Node& node, MutationObserverInit const& options)
{
    m_observed_nodes.append(node);
    node.register_mutation_observer({ adopt_ref(*this), init_to_parameters(options) });
}

void MutationObserver::disconnect()
{
    for (auto& node : m_observed_nodes) {
        node.remove_mutation_observer(adopt_ref(*this));
    }

    m_observed_nodes.clear();
}

void MutationObserver::notify(NonnullRefPtr<MutationRecord> data)
{
    // TODO: this needs some work - object is not read correctly on the js side
    auto* callback_object = m_callback.callback.cell();
    JS::Completion cmpl = Web::Bindings::IDL::invoke_callback(
        m_callback,
        wrap(callback_object->global_object(), data));
}

Vector<MutationRecord> MutationObserver::take_records()
{
    dbgln("MutationObserver::take_records");
    return {};
}

}
