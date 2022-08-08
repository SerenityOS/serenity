/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/AbstractRange.h>

namespace Web::DOM {

// NOTE: We must use RP instead of NNRP here, otherwise the generated code cannot default initialize this struct.
//       They will never be null, as they are marked as required and non-null in the dictionary.
struct StaticRangeInit {
    RefPtr<Node> start_container;
    u32 start_offset { 0 };
    RefPtr<Node> end_container;
    u32 end_offset { 0 };
};

class StaticRange final : public AbstractRange {
    JS_OBJECT(StaticRange, JS::Object);

public:
    static ExceptionOr<StaticRange*> create_with_global_object(Bindings::WindowObject&, StaticRangeInit& init);

    StaticRange(Node& start_container, u32 start_offset, Node& end_container, u32 end_offset);
    virtual ~StaticRange() override;

    StaticRange& impl() { return *this; }
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::DOM::StaticRange& object) { return &object; }
using StaticRangeWrapper = Web::DOM::StaticRange;
}
