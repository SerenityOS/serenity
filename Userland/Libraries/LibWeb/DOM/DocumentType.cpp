/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/DocumentTypePrototype.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>

namespace Web::DOM {

JS::NonnullGCPtr<DocumentType> DocumentType::create(Document& document)
{
    return *document.heap().allocate<DocumentType>(document.realm(), document);
}

DocumentType::DocumentType(Document& document)
    : Node(document, NodeType::DOCUMENT_TYPE_NODE)
{
    set_prototype(&window().ensure_web_prototype<Bindings::DocumentTypePrototype>("DocumentType"));
}

}
