/*
 * Copyright (c) 2020, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/SelectorEngine.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/HTMLCollection.h>
#include <LibWeb/DOM/NodeOperations.h>
#include <LibWeb/DOM/ParentNode.h>
#include <LibWeb/DOM/StaticNodeList.h>
#include <LibWeb/Dump.h>
#include <LibWeb/Infra/Strings.h>
#include <LibWeb/Namespace.h>

namespace Web::DOM {

// https://dom.spec.whatwg.org/#dom-parentnode-queryselector
WebIDL::ExceptionOr<JS::GCPtr<Element>> ParentNode::query_selector(StringView selector_text)
{
    // The querySelector(selectors) method steps are to return the first result of running scope-match a selectors string selectors against this,
    // if the result is not an empty list; otherwise null.

    // https://dom.spec.whatwg.org/#scope-match-a-selectors-string
    // To scope-match a selectors string selectors against a node, run these steps:
    // 1. Let s be the result of parse a selector selectors.
    auto maybe_selectors = parse_selector(CSS::Parser::ParsingContext(*this), selector_text);

    // 2. If s is failure, then throw a "SyntaxError" DOMException.
    if (!maybe_selectors.has_value())
        return WebIDL::SyntaxError::create(realm(), "Failed to parse selector"_fly_string);

    auto selectors = maybe_selectors.value();

    // 3. Return the result of match a selector against a tree with s and node’s root using scoping root node.
    JS::GCPtr<Element> result;
    // FIXME: This should be shadow-including. https://drafts.csswg.org/selectors-4/#match-a-selector-against-a-tree
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        for (auto& selector : selectors) {
            if (SelectorEngine::matches(selector, {}, element, {}, this)) {
                result = &element;
                return IterationDecision::Break;
            }
        }
        return IterationDecision::Continue;
    });

    return result;
}

// https://dom.spec.whatwg.org/#dom-parentnode-queryselectorall
WebIDL::ExceptionOr<JS::NonnullGCPtr<NodeList>> ParentNode::query_selector_all(StringView selector_text)
{
    // The querySelectorAll(selectors) method steps are to return the static result of running scope-match a selectors string selectors against this.

    // https://dom.spec.whatwg.org/#scope-match-a-selectors-string
    // To scope-match a selectors string selectors against a node, run these steps:
    // 1. Let s be the result of parse a selector selectors.
    auto maybe_selectors = parse_selector(CSS::Parser::ParsingContext(*this), selector_text);

    // 2. If s is failure, then throw a "SyntaxError" DOMException.
    if (!maybe_selectors.has_value())
        return WebIDL::SyntaxError::create(realm(), "Failed to parse selector"_fly_string);

    auto selectors = maybe_selectors.value();

    // 3. Return the result of match a selector against a tree with s and node’s root using scoping root node.
    Vector<JS::Handle<Node>> elements;
    // FIXME: This should be shadow-including. https://drafts.csswg.org/selectors-4/#match-a-selector-against-a-tree
    for_each_in_subtree_of_type<Element>([&](auto& element) {
        for (auto& selector : selectors) {
            if (SelectorEngine::matches(selector, {}, element, {}, this)) {
                elements.append(&element);
            }
        }
        return IterationDecision::Continue;
    });

    return StaticNodeList::create(realm(), move(elements));
}

JS::GCPtr<Element> ParentNode::first_element_child()
{
    return first_child_of_type<Element>();
}

JS::GCPtr<Element> ParentNode::last_element_child()
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

void ParentNode::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_children);
}

// https://dom.spec.whatwg.org/#dom-parentnode-children
JS::NonnullGCPtr<HTMLCollection> ParentNode::children()
{
    // The children getter steps are to return an HTMLCollection collection rooted at this matching only element children.
    if (!m_children) {
        m_children = HTMLCollection::create(*this, HTMLCollection::Scope::Children, [](Element const&) {
            return true;
        });
    }
    return *m_children;
}

// https://dom.spec.whatwg.org/#concept-getelementsbytagname
// NOTE: This method is only exposed on Document and Element, but is in ParentNode to prevent code duplication.
JS::NonnullGCPtr<HTMLCollection> ParentNode::get_elements_by_tag_name(FlyString const& qualified_name)
{
    // 1. If qualifiedName is "*" (U+002A), return a HTMLCollection rooted at root, whose filter matches only descendant elements.
    if (qualified_name == "*") {
        return HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](Element const&) {
            return true;
        });
    }

    // 2. Otherwise, if root’s node document is an HTML document, return a HTMLCollection rooted at root, whose filter matches the following descendant elements:
    if (root().document().document_type() == Document::Type::HTML) {
        FlyString qualified_name_in_ascii_lowercase = MUST(Infra::to_ascii_lowercase(qualified_name));
        return HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [qualified_name, qualified_name_in_ascii_lowercase](Element const& element) {
            // - Whose namespace is the HTML namespace and whose qualified name is qualifiedName, in ASCII lowercase.
            if (element.namespace_uri() == Namespace::HTML)
                return element.qualified_name() == qualified_name_in_ascii_lowercase;

            // - Whose namespace is not the HTML namespace and whose qualified name is qualifiedName.
            return element.qualified_name() == qualified_name;
        });
    }

    // 3. Otherwise, return a HTMLCollection rooted at root, whose filter matches descendant elements whose qualified name is qualifiedName.
    return HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [qualified_name](Element const& element) {
        return element.qualified_name() == qualified_name;
    });
}

// https://dom.spec.whatwg.org/#concept-getelementsbytagnamens
// NOTE: This method is only exposed on Document and Element, but is in ParentNode to prevent code duplication.
JS::NonnullGCPtr<HTMLCollection> ParentNode::get_elements_by_tag_name_ns(Optional<String> const& nullable_namespace, FlyString const& local_name)
{
    // 1. If namespace is the empty string, set it to null.
    Optional<FlyString> namespace_;
    if (nullable_namespace.has_value() && !nullable_namespace->is_empty())
        namespace_ = nullable_namespace.value();

    // 2. If both namespace and localName are "*" (U+002A), return a HTMLCollection rooted at root, whose filter matches descendant elements.
    if (namespace_ == "*" && local_name == "*") {
        return HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [](Element const&) {
            return true;
        });
    }

    // 3. Otherwise, if namespace is "*" (U+002A), return a HTMLCollection rooted at root, whose filter matches descendant elements whose local name is localName.
    if (namespace_ == "*") {
        return HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [local_name](Element const& element) {
            return element.local_name() == local_name;
        });
    }

    // 4. Otherwise, if localName is "*" (U+002A), return a HTMLCollection rooted at root, whose filter matches descendant elements whose namespace is namespace.
    if (local_name == "*") {
        return HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [namespace_](Element const& element) {
            return element.namespace_uri() == namespace_;
        });
    }

    // 5. Otherwise, return a HTMLCollection rooted at root, whose filter matches descendant elements whose namespace is namespace and local name is localName.
    return HTMLCollection::create(*this, HTMLCollection::Scope::Descendants, [namespace_, local_name](Element const& element) {
        return element.namespace_uri() == namespace_ && element.local_name() == local_name;
    });
}

// https://dom.spec.whatwg.org/#dom-parentnode-prepend
WebIDL::ExceptionOr<void> ParentNode::prepend(Vector<Variant<JS::Handle<Node>, String>> const& nodes)
{
    // 1. Let node be the result of converting nodes into a node given nodes and this’s node document.
    auto node = TRY(convert_nodes_to_single_node(nodes, document()));

    // 2. Pre-insert node into this before this’s first child.
    (void)TRY(pre_insert(node, first_child()));

    return {};
}

WebIDL::ExceptionOr<void> ParentNode::append(Vector<Variant<JS::Handle<Node>, String>> const& nodes)
{
    // 1. Let node be the result of converting nodes into a node given nodes and this’s node document.
    auto node = TRY(convert_nodes_to_single_node(nodes, document()));

    // 2. Append node to this.
    (void)TRY(append_child(node));

    return {};
}

WebIDL::ExceptionOr<void> ParentNode::replace_children(Vector<Variant<JS::Handle<Node>, String>> const& nodes)
{
    // 1. Let node be the result of converting nodes into a node given nodes and this’s node document.
    auto node = TRY(convert_nodes_to_single_node(nodes, document()));

    // 2. Ensure pre-insertion validity of node into this before null.
    TRY(ensure_pre_insertion_validity(node, nullptr));

    // 3. Replace all with node within this.
    replace_all(*node);
    return {};
}

}
