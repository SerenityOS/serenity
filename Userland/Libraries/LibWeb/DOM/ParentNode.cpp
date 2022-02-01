/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/NodeOperations.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/DOM/StaticNodeList.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Namespace.h>

namespace Web::DOM {

ExceptionOr<RefPtr<Element>> ParentNode::query_selector(StringView selector_text)
{
    auto maybe_selectors = parse_selector(CSS::ParsingContext(*this), selector_text);
    if (!maybe_selectors.has_value())
        return DOM::SyntaxError::create("Failed to parse selector");

    auto selectors = maybe_selectors.value();

    RefPtr<Element> result;
    // FIXME: This should be shadow-including. https://drafts.csswg.org/selectors-4/#match-a-selector-against-a-tree
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        for (auto& selector : selectors) {
            if (SelectorEngine::matches(selector, element)) {
                result = element;
                return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    });

    return result;
}

ExceptionOr<NonnullRefPtr<NodeList>> ParentNode::query_selector_all(StringView selector_text)
{
    auto maybe_selectors = parse_selector(CSS::ParsingContext(*this), selector_text);
    if (!maybe_selectors.has_value())
        return DOM::SyntaxError::create("Failed to parse selector");

    auto selectors = maybe_selectors.value();

    NonnullRefPtrVector<Node> elements;
    // FIXME: This should be shadow-including. https://drafts.csswg.org/selectors-4/#match-a-selector-against-a-tree
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        for (auto& selector : selectors) {
            if (SelectorEngine::matches(selector, element)) {
                elements.append(element);
            }
        }
        return IterationDecision::Continue;
    });

    return StaticNodeList::create(move(elements));
}

RefPtr<Element> ParentNode::first_element_child()
{
    return first_child_of_type<Element>();
}

RefPtr<Element> ParentNode::last_element_child()
{
    return last_child_of_type<Element>();
}

// https://dom.spec.whatwg.org/#dom-parentnode-childelementcount
u32 ParentNode::child_element_count() const
{
    u32 count = 0;
    for (auto* child = first_child(); child; child = child->next_sibling()) {
        if (is<Element>(child))
            ++count;
    }
    return count;
}

// https://dom.spec.whatwg.org/#dom-parentnode-children
NonnullRefPtr<HTMLCollection> ParentNode::children()
{
    // The children getter steps are to return an HTMLCollection collection rooted at this matching only element children.
    // FIXME: This should return the same HTMLCollection object every time,
    //        but that would cause a reference cycle since HTMLCollection refs the root.
    return HTMLCollection::create(*this, [this](Element const& element) {
        return is_parent_of(element);
    });
}

// https://dom.spec.whatwg.org/#concept-getelementsbytagname
// NOTE: This method is only exposed on Document and Element, but is in ParentNode to prevent code duplication.
NonnullRefPtr<HTMLCollection> ParentNode::get_elements_by_tag_name(FlyString const& qualified_name)
{
    // 1. If qualifiedName is "*" (U+002A), return a HTMLCollection rooted at root, whose filter matches only descendant elements.
    if (qualified_name == "*") {
        return HTMLCollection::create(*this, [this](Element const& element) {
            return element.is_descendant_of(*this);
        });
    }

    // FIXME: 2. Otherwise, if root’s node document is an HTML document, return a HTMLCollection rooted at root, whose filter matches the following descendant elements:
    //           (It is currently always a HTML document)
    return HTMLCollection::create(*this, [this, qualified_name](Element const& element) {
        if (!element.is_descendant_of(*this))
            return false;

        // - Whose namespace is the HTML namespace and whose qualified name is qualifiedName, in ASCII lowercase.
        if (element.namespace_() == Namespace::HTML)
            return element.qualified_name().to_lowercase() == qualified_name.to_lowercase();

        // - Whose namespace is not the HTML namespace and whose qualified name is qualifiedName.
        return element.qualified_name() == qualified_name;
    });

    // FIXME: 3. Otherwise, return a HTMLCollection rooted at root, whose filter matches descendant elements whose qualified name is qualifiedName.
}

// https://dom.spec.whatwg.org/#concept-getelementsbytagnamens
// NOTE: This method is only exposed on Document and Element, but is in ParentNode to prevent code duplication.
NonnullRefPtr<HTMLCollection> ParentNode::get_elements_by_tag_name_ns(FlyString const& nullable_namespace, FlyString const& local_name)
{
    // 1. If namespace is the empty string, set it to null.
    String namespace_ = nullable_namespace;
    if (namespace_.is_empty())
        namespace_ = {};

    // 2. If both namespace and localName are "*" (U+002A), return a HTMLCollection rooted at root, whose filter matches descendant elements.
    if (namespace_ == "*" && local_name == "*") {
        return HTMLCollection::create(*this, [this](Element const& element) {
            return element.is_descendant_of(*this);
        });
    }

    // 3. Otherwise, if namespace is "*" (U+002A), return a HTMLCollection rooted at root, whose filter matches descendant elements whose local name is localName.
    if (namespace_ == "*") {
        return HTMLCollection::create(*this, [this, local_name](Element const& element) {
            return element.is_descendant_of(*this) && element.local_name() == local_name;
        });
    }

    // 4. Otherwise, if localName is "*" (U+002A), return a HTMLCollection rooted at root, whose filter matches descendant elements whose namespace is namespace.
    if (local_name == "*") {
        return HTMLCollection::create(*this, [this, namespace_](Element const& element) {
            return element.is_descendant_of(*this) && element.namespace_() == namespace_;
        });
    }

    // 5. Otherwise, return a HTMLCollection rooted at root, whose filter matches descendant elements whose namespace is namespace and local name is localName.
    return HTMLCollection::create(*this, [this, namespace_, local_name](Element const& element) {
        return element.is_descendant_of(*this) && element.namespace_() == namespace_ && element.local_name() == local_name;
    });
}

// https://dom.spec.whatwg.org/#dom-parentnode-prepend
ExceptionOr<void> ParentNode::prepend(Vector<Variant<NonnullRefPtr<Node>, String>> const& nodes)
{
    // 1. Let node be the result of converting nodes into a node given nodes and this’s node document.
    auto node_or_exception = convert_nodes_to_single_node(nodes, document());
    if (node_or_exception.is_exception())
        return node_or_exception.exception();

    auto node = node_or_exception.release_value();

    // 2. Pre-insert node into this before this’s first child.
    auto result = pre_insert(node, first_child());
    if (result.is_exception())
        return result.exception();

    return {};
}

ExceptionOr<void> ParentNode::append(Vector<Variant<NonnullRefPtr<Node>, String>> const& nodes)
{
    // 1. Let node be the result of converting nodes into a node given nodes and this’s node document.
    auto node_or_exception = convert_nodes_to_single_node(nodes, document());
    if (node_or_exception.is_exception())
        return node_or_exception.exception();

    auto node = node_or_exception.release_value();

    // 2. Append node to this.
    auto result = append_child(node);
    if (result.is_exception())
        return result.exception();

    return {};
}

ExceptionOr<void> ParentNode::replace_children(Vector<Variant<NonnullRefPtr<Node>, String>> const& nodes)
{
    // 1. Let node be the result of converting nodes into a node given nodes and this’s node document.
    auto node_or_exception = convert_nodes_to_single_node(nodes, document());
    if (node_or_exception.is_exception())
        return node_or_exception.exception();

    auto node = node_or_exception.release_value();

    // 2. Ensure pre-insertion validity of node into this before null.
    auto validity_exception = ensure_pre_insertion_validity(node, nullptr);
    if (validity_exception.is_exception())
        return validity_exception.exception();

    // 3. Replace all with node within this.
    replace_all(node);
    return {};
}

}
