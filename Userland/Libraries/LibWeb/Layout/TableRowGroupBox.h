/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/Box.h>

namespace Web::Layout {

class TableRowGroupBox final : public Box {
    JS_CELL(TableRowGroupBox, Box);

public:
    TableRowGroupBox(DOM::Document&, DOM::Element*, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~TableRowGroupBox() override;
};

}
