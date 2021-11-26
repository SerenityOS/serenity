/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLTemplateElement.h>

namespace Web::HTML {

HTMLTemplateElement::HTMLTemplateElement(DOM::Document& document, QualifiedName qualified_name)
    : HTMLElement(document, move(qualified_name))
{
    m_content = adopt_ref(*new DOM::DocumentFragment(appropriate_template_contents_owner_document(document)));
    m_content->set_host(*this);
}

HTMLTemplateElement::~HTMLTemplateElement()
{
}

DOM::Document& HTMLTemplateElement::appropriate_template_contents_owner_document(DOM::Document& document)
{
    if (!document.created_for_appropriate_template_contents()) {
        if (!document.associated_inert_template_document()) {
            auto new_document = DOM::Document::create();
            new_document->set_created_for_appropriate_template_contents(true);

            // FIXME: If doc is an HTML document, mark new doc as an HTML document also.

            document.set_associated_inert_template_document(new_document);
        }

        return *document.associated_inert_template_document();
    }

    return document;
}

// https://html.spec.whatwg.org/multipage/scripting.html#the-template-element:concept-node-adopt-ext
void HTMLTemplateElement::adopted_from(DOM::Document&)
{
    // NOTE: It seems the spec has been changed since appropriate_template_contents_owner_document was written above.
    //       That function is now part of document, which ends up returning associated_inert_template_document in the new version anyway.
    appropriate_template_contents_owner_document(document()).adopt_node(content());
}

// https://html.spec.whatwg.org/multipage/scripting.html#the-template-element:concept-node-clone-ext
void HTMLTemplateElement::cloned(Node& copy, bool clone_children)
{
    if (!clone_children)
        return;

    auto& template_clone = verify_cast<HTMLTemplateElement>(copy);

    content()->for_each_child([&](auto& child) {
        auto cloned_child = child.clone_node(&template_clone.content()->document(), true);

        // FIXME: Should this use TreeNode::append_child instead?
        template_clone.content()->append_child(cloned_child);
    });
}

}
