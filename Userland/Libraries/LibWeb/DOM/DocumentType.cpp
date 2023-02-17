/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>

namespace Web::DOM {

WebIDL::ExceptionOr<JS::NonnullGCPtr<DocumentType>> DocumentType::create(Document& document)
{
    return MUST_OR_THROW_OOM(document.heap().allocate<DocumentType>(document.realm(), document));
}

DocumentType::DocumentType(Document& document)
    : Node(document, NodeType::DOCUMENT_TYPE_NODE)
{
}

JS::ThrowCompletionOr<void> DocumentType::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DocumentTypePrototype>(realm, "DocumentType"));

    return {};
}

}
