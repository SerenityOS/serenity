/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibWeb/DOM/Attribute.h>
#include <LibWeb/DOM/DocumentType.h>
#include <LibWeb/DOM/ExceptionOr.h>
#include <LibWeb/DOM/StaticRange.h>

namespace Web::DOM {

StaticRange::StaticRange(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset)
    : AbstractRange(start_container, start_offset, end_container, end_offset)
{
}

StaticRange::~StaticRange()
{
}

// https://dom.spec.whatwg.org/#dom-staticrange-staticrange
ExceptionOr<NonnullRefPtr<StaticRange>> StaticRange::create_with_global_object(JS::GlobalObject&, StaticRangeInit& init)
{
    // 1. If init["startContainer"] or init["endContainer"] is a DocumentType or Attr node, then throw an "InvalidNodeTypeError" DOMException.
    if (is<DocumentType>(*init.start_container) || is<Attribute>(*init.start_container))
        return DOM::InvalidNodeTypeError::create("startContainer cannot be a DocumentType or Attribute node.");

    if (is<DocumentType>(*init.end_container) || is<Attribute>(*init.end_container))
        return DOM::InvalidNodeTypeError::create("endContainer cannot be a DocumentType or Attribute node.");

    // 2. Set this’s start to (init["startContainer"], init["startOffset"]) and end to (init["endContainer"], init["endOffset"]).
    return adopt_ref(*new StaticRange(*init.start_container, init.start_offset, *init.end_container, init.end_offset));
}

}
