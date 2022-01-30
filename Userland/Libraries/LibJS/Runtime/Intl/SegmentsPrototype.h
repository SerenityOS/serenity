/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/Segments.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS::Intl {

class SegmentsPrototype final : public PrototypeObject<SegmentsPrototype, Segments> {
    JS_PROTOTYPE_OBJECT(SegmentsPrototype, Segments, Segments);

public:
    explicit SegmentsPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~SegmentsPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(containing);
    JS_DECLARE_NATIVE_FUNCTION(symbol_iterator);
};

}
