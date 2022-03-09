/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/DOMExceptionWrapper.h>
#include <LibWeb/Bindings/IDLAbstractOperations.h>
#include <LibWeb/Bindings/NodeWrapper.h>
#include <LibWeb/Bindings/NodeWrapperFactory.h>
#include <LibWeb/DOM/Node.h>
#include <LibWeb/DOM/NodeIterator.h>

namespace Web::DOM {

NodeIterator::NodeIterator(Node& root)
    : m_root(root)
    , m_reference(root)
{
    root.document().register_node_iterator({}, *this);
}

NodeIterator::~NodeIterator()
{
    m_root->document().unregister_node_iterator({}, *this);
}

// https://dom.spec.whatwg.org/#dom-document-createnodeiterator
NonnullRefPtr<NodeIterator> NodeIterator::create(Node& root, unsigned what_to_show, RefPtr<NodeFilter> filter)
{
    // 1. Let iterator be a new NodeIterator object.
    // 2. Set iterator’s root and iterator’s reference to root.
    auto iterator = adopt_ref(*new NodeIterator(root));

    // 3. Set iterator’s pointer before reference to true.
    iterator->m_pointer_before_reference = true;

    // 4. Set iterator’s whatToShow to whatToShow.
    iterator->m_what_to_show = what_to_show;

    // 5. Set iterator’s filter to filter.
    iterator->m_filter = move(filter);

    // 6. Return iterator.
    return iterator;
}

// https://dom.spec.whatwg.org/#dom-nodeiterator-detach
void NodeIterator::detach()
{
    // The detach() method steps are to do nothing.
    // Its functionality (disabling a NodeIterator object) was removed, but the method itself is preserved for compatibility.
}

// https://dom.spec.whatwg.org/#concept-nodeiterator-traverse
JS::ThrowCompletionOr<RefPtr<Node>> NodeIterator::traverse(Direction direction)
{
    // 1. Let node be iterator’s reference.
    auto node = m_reference;

    // 2. Let beforeNode be iterator’s pointer before reference.
    auto before_node = m_pointer_before_reference;

    // 3. While true:
    while (true) {
        // 4. Branch on direction:
        if (direction == Direction::Next) {
            // next
            // If beforeNode is false, then set node to the first node following node in iterator’s iterator collection.
            // If there is no such node, then return null.
            if (!before_node) {
                auto* next_node = node->next_in_pre_order(m_root.ptr());
                if (!next_node)
                    return RefPtr<Node> {};
                node = *next_node;
            } else {
                // If beforeNode is true, then set it to false.
                before_node = false;
            }
        } else {
            // previous
            // If beforeNode is true, then set node to the first node preceding node in iterator’s iterator collection.
            // If there is no such node, then return null.
            if (before_node) {
                if (node == m_root.ptr())
                    return nullptr;
                auto* previous_node = node->previous_in_pre_order();
                if (!previous_node)
                    return RefPtr<Node> {};
                node = *previous_node;
            } else {
                // If beforeNode is false, then set it to true.
                before_node = true;
            }
        }

        // 2. Let result be the result of filtering node within iterator.
        auto result = filter(*node);
        if (result.is_throw_completion())
            return result.release_error();

        // 3. If result is FILTER_ACCEPT, then break.
        if (result.value() == NodeFilter::FILTER_ACCEPT)
            break;
    }

    // 4. Set iterator’s reference to node.
    m_reference = node;

    // 5. Set iterator’s pointer before reference to beforeNode.
    m_pointer_before_reference = before_node;

    // 6. Return node.
    return RefPtr<Node> { node };
}

// https://dom.spec.whatwg.org/#concept-node-filter
JS::ThrowCompletionOr<NodeFilter::Result> NodeIterator::filter(Node& node)
{
    VERIFY(wrapper());
    auto& global_object = wrapper()->global_object();

    // 1. If traverser’s active flag is set, then throw an "InvalidStateError" DOMException.
    if (m_active)
        return JS::throw_completion(wrap(global_object, InvalidStateError::create("NodeIterator is already active")));

    // 2. Let n be node’s nodeType attribute value − 1.
    auto n = node.node_type() - 1;

    // 3. If the nth bit (where 0 is the least significant bit) of traverser’s whatToShow is not set, then return FILTER_SKIP.
    if (!(m_what_to_show & (1u << n)))
        return NodeFilter::FILTER_SKIP;

    // 4. If traverser’s filter is null, then return FILTER_ACCEPT.
    if (!m_filter)
        return NodeFilter::FILTER_ACCEPT;

    // 5. Set traverser’s active flag.
    m_active = true;

    // 6. Let result be the return value of call a user object’s operation with traverser’s filter, "acceptNode", and « node ».
    //    If this throws an exception, then unset traverser’s active flag and rethrow the exception.
    auto result = Bindings::IDL::call_user_object_operation(m_filter->callback(), "acceptNode", {}, wrap(global_object, node));
    if (result.is_abrupt()) {
        m_active = false;
        return result;
    }

    // 7. Unset traverser’s active flag.
    m_active = false;

    // 8. Return result.
    auto result_value = TRY(result.value()->to_i32(global_object));
    return static_cast<NodeFilter::Result>(result_value);
}

// https://dom.spec.whatwg.org/#dom-nodeiterator-nextnode
JS::ThrowCompletionOr<RefPtr<Node>> NodeIterator::next_node()
{
    return traverse(Direction::Next);
}

// https://dom.spec.whatwg.org/#dom-nodeiterator-previousnode
JS::ThrowCompletionOr<RefPtr<Node>> NodeIterator::previous_node()
{
    return traverse(Direction::Previous);
}

// https://dom.spec.whatwg.org/#nodeiterator-pre-removing-steps
void NodeIterator::run_pre_removing_steps(Node& to_be_removed_node)
{
    // NOTE: This function implements what the DOM specification tells us to do.
    //       However, it's known to not match other browsers: https://github.com/whatwg/dom/issues/907

    // 1. If toBeRemovedNode is not an inclusive ancestor of nodeIterator’s reference, or toBeRemovedNode is nodeIterator’s root, then return.
    if (!to_be_removed_node.is_inclusive_ancestor_of(m_reference) || &to_be_removed_node == m_root)
        return;

    // 2. If nodeIterator’s pointer before reference is true, then:
    if (m_pointer_before_reference) {
        // 1. Let next be toBeRemovedNode’s first following node that is an inclusive descendant of nodeIterator’s root and is not an inclusive descendant of toBeRemovedNode, and null if there is no such node.
        RefPtr<Node> next = to_be_removed_node.next_in_pre_order(m_root);
        while (next && (!next->is_inclusive_descendant_of(m_root) || next->is_inclusive_descendant_of(to_be_removed_node)))
            next = next->next_in_pre_order(m_root);

        // 2. If next is non-null, then set nodeIterator’s reference to next and return.
        if (next) {
            m_reference = *next;
            return;
        }

        // 3. Otherwise, set nodeIterator’s pointer before reference to false.
        m_pointer_before_reference = false;
    }

    // 3. Set nodeIterator’s reference to toBeRemovedNode’s parent, if toBeRemovedNode’s previous sibling is null,
    if (!to_be_removed_node.previous_sibling()) {
        VERIFY(to_be_removed_node.parent());
        m_reference = *to_be_removed_node.parent();
    } else {
        // ...and to the inclusive descendant of toBeRemovedNode’s previous sibling that appears last in tree order otherwise.
        auto* node = to_be_removed_node.previous_sibling();
        while (node->last_child())
            node = node->last_child();
        m_reference = *node;
    }
}

}
