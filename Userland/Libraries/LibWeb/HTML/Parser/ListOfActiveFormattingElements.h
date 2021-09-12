/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Forward.h>

namespace Web::HTML {

class ListOfActiveFormattingElements {
public:
    ListOfActiveFormattingElements() { }
    ~ListOfActiveFormattingElements();

    struct Entry {
        bool is_marker() const { return !element; }

        RefPtr<DOM::Element> element;
    };

    bool is_empty() const { return m_entries.is_empty(); }
    bool contains(const DOM::Element&) const;

    void add(DOM::Element& element);
    void add_marker();

    void remove(DOM::Element&);

    const Vector<Entry>& entries() const { return m_entries; }
    Vector<Entry>& entries() { return m_entries; }

    DOM::Element* last_element_with_tag_name_before_marker(const FlyString& tag_name);

    void clear_up_to_the_last_marker();

private:
    Vector<Entry> m_entries;
};

}
