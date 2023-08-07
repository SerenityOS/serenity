/*
 * Copyright (c) 2020, the SerenityOS developers.
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/DOM/DOMImplementation.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ElementFactory.h>
#include <LibWeb/DOM/Text.h>
#include <LibWeb/HTML/Origin.h>
#include <LibWeb/Namespace.h>

namespace Web::DOM {

WebIDL::ExceptionOr<JS::NonnullGCPtr<DOMImplementation>> DOMImplementation::create(Document& document)
{
    auto& realm = document.realm();
    return MUST_OR_THROW_OOM(realm.heap().allocate<DOMImplementation>(realm, document));
}

DOMImplementation::DOMImplementation(Document& document)
    : PlatformObject(document.realm())
    , m_document(document)
{
}

DOMImplementation::~DOMImplementation() = default;

void DOMImplementation::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMImplementationPrototype>(realm, "DOMImplementation"));
}

void DOMImplementation::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_document);
}

// https://dom.spec.whatwg.org/#dom-domimplementation-createdocument
WebIDL::ExceptionOr<JS::NonnullGCPtr<Document>> DOMImplementation::create_document(DeprecatedString const& namespace_, DeprecatedString const& qualified_name, JS::GCPtr<DocumentType> doctype) const
{
    // FIXME: This should specifically be an XML document.
    auto xml_document = TRY(Document::create(realm()));

    xml_document->set_ready_for_post_load_tasks(true);

    JS::GCPtr<Element> element;

    if (!qualified_name.is_empty())
        element = TRY(xml_document->create_element_ns(namespace_, qualified_name, ElementCreationOptions {}));

    if (doctype)
        TRY(xml_document->append_child(*doctype));

    if (element)
        TRY(xml_document->append_child(*element));

    xml_document->set_origin(document().origin());

    if (namespace_ == Namespace::HTML)
        xml_document->set_content_type("application/xhtml+xml");
    else if (namespace_ == Namespace::SVG)
        xml_document->set_content_type("image/svg+xml");
    else
        xml_document->set_content_type("application/xml");

    return xml_document;
}

// https://dom.spec.whatwg.org/#dom-domimplementation-createhtmldocument
JS::NonnullGCPtr<Document> DOMImplementation::create_html_document(DeprecatedString const& title) const
{
    auto html_document = Document::create(realm()).release_value_but_fixme_should_propagate_errors();

    html_document->set_content_type("text/html");
    html_document->set_ready_for_post_load_tasks(true);

    auto doctype = heap().allocate<DocumentType>(realm(), html_document).release_allocated_value_but_fixme_should_propagate_errors();
    doctype->set_name("html");
    MUST(html_document->append_child(*doctype));

    auto html_element = create_element(html_document, HTML::TagNames::html, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_document->append_child(html_element));

    auto head_element = create_element(html_document, HTML::TagNames::head, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(head_element));

    if (!title.is_null()) {
        auto title_element = create_element(html_document, HTML::TagNames::title, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
        MUST(head_element->append_child(title_element));

        auto text_node = heap().allocate<Text>(realm(), html_document, title).release_allocated_value_but_fixme_should_propagate_errors();
        MUST(title_element->append_child(*text_node));
    }

    auto body_element = create_element(html_document, HTML::TagNames::body, Namespace::HTML).release_value_but_fixme_should_propagate_errors();
    MUST(html_element->append_child(body_element));

    html_document->set_origin(document().origin());

    return html_document;
}

// https://dom.spec.whatwg.org/#dom-domimplementation-createdocumenttype
WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentType>> DOMImplementation::create_document_type(DeprecatedString const& qualified_name, DeprecatedString const& public_id, DeprecatedString const& system_id)
{
    TRY(Document::validate_qualified_name(realm(), qualified_name));
    auto document_type = TRY(DocumentType::create(document()));
    document_type->set_name(qualified_name);
    document_type->set_public_id(public_id);
    document_type->set_system_id(system_id);
    return document_type;
}

}
