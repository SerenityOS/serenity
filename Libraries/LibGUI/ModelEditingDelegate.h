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

#include <LibGUI/Model.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

namespace GUI {

class ModelEditingDelegate {
public:
    virtual ~ModelEditingDelegate() {}

    void bind(Model& model, const ModelIndex& index)
    {
        if (m_model.ptr() == &model && m_index == index)
            return;
        m_model = model;
        m_index = index;
        m_widget = create_widget();
    }

    Widget* widget() { return m_widget; }
    const Widget* widget() const { return m_widget; }

    Function<void()> on_commit;

    virtual Variant value() const = 0;
    virtual void set_value(const Variant&) = 0;

    virtual void will_begin_editing() {}

protected:
    ModelEditingDelegate() {}

    virtual RefPtr<Widget> create_widget() = 0;
    void commit()
    {
        if (on_commit)
            on_commit();
    }

private:
    RefPtr<Model> m_model;
    ModelIndex m_index;
    RefPtr<Widget> m_widget;
};

class StringModelEditingDelegate : public ModelEditingDelegate {
public:
    StringModelEditingDelegate() {}
    virtual ~StringModelEditingDelegate() override {}

    virtual RefPtr<Widget> create_widget() override
    {
        auto textbox = TextBox::construct();
        textbox->on_return_pressed = [this] {
            commit();
        };
        return textbox;
    }
    virtual Variant value() const override { return static_cast<const TextBox*>(widget())->text(); }
    virtual void set_value(const Variant& value) override { static_cast<TextBox*>(widget())->set_text(value.to_string()); }
};

}
