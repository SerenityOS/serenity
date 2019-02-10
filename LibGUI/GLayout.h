#pragma once

#include <AK/Badge.h>
#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>

class GWidget;

class GLayout {
public:
    GLayout();
    virtual ~GLayout();

    void add_widget(GWidget&);
    void add_layout(OwnPtr<GLayout>&&);

    virtual void run(GWidget&) = 0;

    void notify_adopted(Badge<GWidget>, GWidget&);
    void notify_disowned(Badge<GWidget>, GWidget&);

protected:
    struct Entry {
        WeakPtr<GWidget> widget;
        OwnPtr<GLayout> layout;
    };
    WeakPtr<GWidget> m_owner;
    Vector<Entry> m_entries;
};

