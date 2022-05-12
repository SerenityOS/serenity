/*
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 * Copyright (c) 2022, Linus Groh <linusg@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Filters/Filter.h"
#include "ImageEditor.h"
#include <AK/NonnullOwnPtr.h>
#include <LibGUI/TreeViewModel.h>

namespace PixelPaint {

class FilterNode final : public GUI::TreeViewModel::Node {
public:
    FilterNode(String text, Optional<GUI::Icon> icon, Node* parent_node, NonnullOwnPtr<Filter> filter)
        : Node(move(text), move(icon), parent_node)
        , m_filter(move(filter))
    {
    }

    Filter const& filter() const { return *m_filter; }
    Filter& filter() { return *m_filter; }

private:
    NonnullOwnPtr<Filter> m_filter;
};

ErrorOr<NonnullRefPtr<GUI::TreeViewModel>> create_filter_tree_model(ImageEditor*);

}
