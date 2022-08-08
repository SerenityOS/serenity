/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/AbstractRangePrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/DOM/AbstractRange.h>
#include <LibWeb/DOM/Document.h>

namespace Web::DOM {

AbstractRange::AbstractRange(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset)
    : Bindings::PlatformObject(start_container.document().preferred_window_object().ensure_web_prototype<Bindings::AbstractRangePrototype>("AbstractRange"))
    , m_start_container(start_container)
    , m_start_offset(start_offset)
    , m_end_container(end_container)
    , m_end_offset(end_offset)
{
}

AbstractRange::~AbstractRange() = default;

}
