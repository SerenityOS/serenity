/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/SegmentIterator.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS::Intl {

class SegmentIteratorPrototype final : public PrototypeObject<SegmentIteratorPrototype, SegmentIterator> {
    JS_PROTOTYPE_OBJECT(SegmentIteratorPrototype, SegmentIterator, SegmentIterator);

public:
    explicit SegmentIteratorPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~SegmentIteratorPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(next);
};

}
