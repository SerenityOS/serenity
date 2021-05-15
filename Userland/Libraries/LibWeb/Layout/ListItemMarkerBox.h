/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobi@tobyase.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Layout/Box.h>

namespace Web::Layout {

class ListItemMarkerBox final : public Box {
public:
    explicit ListItemMarkerBox(DOM::Document&, CSS::ListStyleType, size_t index);
    virtual ~ListItemMarkerBox() override;

    virtual void paint(PaintContext&, PaintPhase) override;

private:
    CSS::ListStyleType m_list_style_type { CSS::ListStyleType::None };
    size_t m_index;

    String m_text {};
};

}
