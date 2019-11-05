#pragma once

#include <AK/Badge.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibGUI/GMargins.h>

class GWidget;

class GLayout {
public:
    GLayout();
    virtual ~GLayout();

    void add_widget(GWidget&);
    void insert_widget_before(GWidget& widget, GWidget& before_widget);
    void add_layout(OwnPtr<GLayout>&&);
    void add_spacer();

    void remove_widget(GWidget&);

    virtual void run(GWidget&) = 0;

    void notify_adopted(Badge<GWidget>, GWidget&);
    void notify_disowned(Badge<GWidget>, GWidget&);

    GMargins margins() const { return m_margins; }
    void set_margins(const GMargins&);

    int spacing() const { return m_spacing; }
    void set_spacing(int);

protected:
    struct Entry {
        enum class Type {
            Invalid = 0,
            Widget,
            Layout,
            Spacer,
        };

        Type type { Type::Invalid };
        WeakPtr<GWidget> widget;
        OwnPtr<GLayout> layout;
    };
    void add_entry(Entry&&);

    WeakPtr<GWidget> m_owner;
    Vector<Entry> m_entries;

    GMargins m_margins;
    int m_spacing { 3 };
};
