/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/OwnPtr.h>
#include <YAK/Vector.h>
#include <YAK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Margins.h>
#include <LibGfx/Forward.h>

namespace GUI {

class Layout : public Core::Object {
    C_OBJECT_ABSTRACT(Layout);

public:
    virtual ~Layout();

    void add_widget(Widget&);
    void insert_widget_before(Widget& widget, Widget& before_widget);
    void add_layout(OwnPtr<Layout>&&);
    void add_spacer();

    void remove_widget(Widget&);

    virtual void run(Widget&) = 0;
    virtual Gfx::IntSize preferred_size() const = 0;

    void notify_adopted(Badge<Widget>, Widget&);
    void notify_disowned(Badge<Widget>, Widget&);

    const Margins& margins() const { return m_margins; }
    void set_margins(const Margins&);

    int spacing() const { return m_spacing; }
    void set_spacing(int);

protected:
    Layout();

    struct Entry {
        enum class Type {
            Invalid = 0,
            Widget,
            Layout,
            Spacer,
        };

        Type type { Type::Invalid };
        WeakPtr<Widget> widget;
        OwnPtr<Layout> layout;
    };
    void add_entry(Entry&&);

    WeakPtr<Widget> m_owner;
    Vector<Entry> m_entries;

    Margins m_margins;
    int m_spacing { 3 };
};

}
