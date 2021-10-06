/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/BlockContainer.h>

namespace Web::Layout {

class TableBox final : public Layout::BlockContainer {
public:
    TableBox(DOM::Document&, DOM::Element*, NonnullRefPtr<CSS::StyleProperties>);
    TableBox(DOM::Document&, DOM::Element*, CSS::ComputedValues);
    virtual ~TableBox() override;

    static CSS::Display static_display() { return CSS::Display::from_short(CSS::Display::Short::Table); }
};

}
