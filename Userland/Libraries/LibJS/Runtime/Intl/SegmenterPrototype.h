/*
 * Copyright (c) 2022, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibJS/Runtime/Intl/Segmenter.h>
#include <LibJS/Runtime/PrototypeObject.h>

namespace JS::Intl {

class SegmenterPrototype final : public PrototypeObject<SegmenterPrototype, Segmenter> {
    JS_PROTOTYPE_OBJECT(SegmenterPrototype, Segmenter, Segmenter);

public:
    explicit SegmenterPrototype(GlobalObject&);
    virtual void initialize(GlobalObject&) override;
    virtual ~SegmenterPrototype() override = default;

private:
    JS_DECLARE_NATIVE_FUNCTION(segment);
    JS_DECLARE_NATIVE_FUNCTION(resolved_options);
};

}
