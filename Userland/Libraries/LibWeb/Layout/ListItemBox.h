/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/BlockContainer.h>

namespace Web::Layout {

class ListItemBox final : public BlockContainer {
public:
    ListItemBox(DOM::Document&, DOM::Element*, NonnullRefPtr<CSS::StyleProperties>);
    virtual ~ListItemBox() override;

    DOM::Element& dom_node() { return static_cast<DOM::Element&>(*BlockContainer::dom_node()); }
    DOM::Element const& dom_node() const { return static_cast<DOM::Element const&>(*BlockContainer::dom_node()); }

    ListItemMarkerBox const* marker() const { return m_marker; }
    void set_marker(RefPtr<ListItemMarkerBox>);

private:
    RefPtr<ListItemMarkerBox> m_marker;
};

}
