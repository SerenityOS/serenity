/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibWeb/DOM/Attr.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/StaticRange.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::DOM {

StaticRange::StaticRange(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset)
    : AbstractRange(start_container, start_offset, end_container, end_offset)
{
    set_prototype(&start_container.document().window().cached_web_prototype("StaticRange"));
}

StaticRange::~StaticRange() = default;

// https://dom.spec.whatwg.org/#dom-staticrange-staticrange
WebIDL::ExceptionOr<StaticRange*> StaticRange::create_with_global_object(HTML::Window& window, StaticRangeInit& init)
{
    // 1. If init["startContainer"] or init["endContainer"] is a DocumentType or Attr node, then throw an "InvalidNodeTypeError" DOMException.
    if (is<DocumentType>(*init.start_container) || is<Attr>(*init.start_container))
        return DOM::InvalidNodeTypeError::create(window, "startContainer cannot be a DocumentType or Attribute node.");

    if (is<DocumentType>(*init.end_container) || is<Attr>(*init.end_container))
        return DOM::InvalidNodeTypeError::create(window, "endContainer cannot be a DocumentType or Attribute node.");

    // 2. Set this’s start to (init["startContainer"], init["startOffset"]) and end to (init["endContainer"], init["endOffset"]).
    return window.heap().allocate<StaticRange>(window.realm(), *init.start_container, init.start_offset, *init.end_container, init.end_offset);
}

}
