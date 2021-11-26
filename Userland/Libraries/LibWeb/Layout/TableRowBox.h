/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/Box.h>

namespace Web::Layout {

class TableRowBox final : public Box {
public:
    TableRowBox(DOM::Document&, DOM::Element*, NonnullRefPtr<CSS::StyleProperties>);
    TableRowBox(DOM::Document&, DOM::Element*, CSS::ComputedValues);
    virtual ~TableRowBox() override;

    static CSS::Display static_display() { return CSS::Display { CSS::Display::Internal::TableRow }; }
};

}
