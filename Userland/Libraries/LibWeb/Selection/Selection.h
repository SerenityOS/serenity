/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::Selection {

class Selection final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(Selection, Bindings::PlatformObject);

public:
    static JS::NonnullGCPtr<Selection> create(JS::Realm&);

    virtual ~Selection() override;

    DOM::Node* anchor_node();
    unsigned anchor_offset();
    DOM::Node* focus_node();
    unsigned focus_offset() const;
    bool is_collapsed() const;
    unsigned range_count() const;
    String type() const;
    DOM::Range* get_range_at(unsigned index);
    void add_range(DOM::Range&);
    void remove_range(DOM::Range&);
    void remove_all_ranges();
    void empty();
    void collapse(DOM::Node*, unsigned offset);
    void set_position(DOM::Node*, unsigned offset);
    void collapse_to_start();
    void collapse_to_end();
    void extend(DOM::Node&, unsigned offset);
    void set_base_and_extent(DOM::Node& anchor_node, unsigned anchor_offset, DOM::Node& focus_node, unsigned focus_offset);
    void select_all_children(DOM::Node&);
    void delete_from_document();
    bool contains_node(DOM::Node&, bool allow_partial_containment) const;

    String to_string() const;

private:
    explicit Selection(JS::Realm&);
};

}
