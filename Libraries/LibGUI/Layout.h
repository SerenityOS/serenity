/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Margins.h>

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

    void notify_adopted(Badge<Widget>, Widget&);
    void notify_disowned(Badge<Widget>, Widget&);

    Margins margins() const { return m_margins; }
    void set_margins(const Margins&);

    int spacing() const { return m_spacing; }
    void set_spacing(int);

    virtual void save_to(JsonObject&) override;

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
