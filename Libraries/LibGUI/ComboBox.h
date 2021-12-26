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

#include <LibGUI/Widget.h>

namespace GUI {

class ComboBoxEditor;
class ControlBoxButton;

class ComboBox : public Widget {
    C_OBJECT(ComboBox)
public:
    virtual ~ComboBox() override;

    String text() const;
    void set_text(const String&);

    void open();
    void close();
    void select_all();

    Model* model();
    const Model* model() const;
    void set_model(NonnullRefPtr<Model>);

    size_t selected_index() const;
    void set_selected_index(size_t index);

    bool only_allow_values_from_model() const { return m_only_allow_values_from_model; }
    void set_only_allow_values_from_model(bool);

    int model_column() const;
    void set_model_column(int);

    Function<void(const String&, const ModelIndex&)> on_change;
    Function<void()> on_return_pressed;

protected:
    ComboBox();
    virtual void resize_event(ResizeEvent&) override;

private:
    RefPtr<ComboBoxEditor> m_editor;
    RefPtr<ControlBoxButton> m_open_button;
    RefPtr<Window> m_list_window;
    RefPtr<ListView> m_list_view;
    bool m_only_allow_values_from_model { false };
};

}
