/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Margins.h>
#include <LibGUI/Object.h>
#include <LibGUI/UIDimensions.h>
#include <LibGfx/Forward.h>

namespace GUI::Registration {
extern GUI::ObjectClassRegistration registration_Layout;
}

#define REGISTER_LAYOUT(namespace_, class_name)                                                                                                      \
    namespace GUI::Registration {                                                                                                                    \
    ::GUI::ObjectClassRegistration registration_##class_name(                                                                                        \
        #namespace_ "::" #class_name##sv, []() { return static_ptr_cast<GUI::Object>(namespace_::class_name::construct()); }, &registration_Layout); \
    }

namespace GUI {

class Layout : public GUI::Object {
    C_OBJECT_ABSTRACT(Layout);

public:
    virtual ~Layout();

    void add_widget(Widget&);
    void insert_widget_before(Widget& widget, Widget& before_widget);
    void add_layout(OwnPtr<Layout>&&);
    void add_spacer();

    void remove_widget(Widget&);

    virtual void run(Widget&) = 0;
    virtual UISize preferred_size() const = 0;
    virtual UISize min_size() const = 0;

    void notify_adopted(Badge<Widget>, Widget&);
    void notify_disowned(Badge<Widget>, Widget&);

    Margins const& margins() const { return m_margins; }
    void set_margins(Margins const&);

    static constexpr int default_spacing = 3;
    int spacing() const { return m_spacing; }
    void set_spacing(int);

protected:
    Layout(Margins, int spacing);

    struct Entry {
        enum class Type {
            Invalid = 0,
            Widget,
            Layout,
            Spacer,
        };

        Type type { Type::Invalid };
        WeakPtr<Widget> widget {};
        OwnPtr<Layout> layout {};
    };
    void add_entry(Entry&&);

    WeakPtr<Widget> m_owner;
    Vector<Entry> m_entries;

    Margins m_margins;
    int m_spacing { default_spacing };
};

}
