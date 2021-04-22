/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/BlockBox.h>

namespace Web::Layout {

class ListItemMarkerBox;

class ListItemBox final : public BlockBox {
public:
    ListItemBox(DOM::Document&, DOM::Element&, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~ListItemBox() override;

    void layout_marker();

private:
    RefPtr<ListItemMarkerBox> m_marker;
};

}
