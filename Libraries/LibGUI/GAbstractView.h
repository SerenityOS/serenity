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

#include <AK/Function.h>
#include <LibGUI/GModel.h>
#include <LibGUI/GModelSelection.h>
#include <LibGUI/GScrollableWidget.h>

namespace GUI {

class ModelEditingDelegate;

class AbstractView : public ScrollableWidget {
    C_OBJECT_ABSTRACT(AbstractView)
    friend class Model;

public:
    void set_model(RefPtr<Model>&&);
    Model* model() { return m_model.ptr(); }
    const Model* model() const { return m_model.ptr(); }

    ModelSelection& selection() { return m_selection; }
    const ModelSelection& selection() const { return m_selection; }
    void select_all();

    bool is_editable() const { return m_editable; }
    void set_editable(bool editable) { m_editable = editable; }

    virtual bool accepts_focus() const override { return true; }
    virtual void did_update_model();
    virtual void did_update_selection();

    virtual Rect content_rect(const ModelIndex&) const { return {}; }
    virtual ModelIndex index_at_event_position(const Point&) const = 0;
    void begin_editing(const ModelIndex&);
    void stop_editing();

    void set_activates_on_selection(bool b) { m_activates_on_selection = b; }
    bool activates_on_selection() const { return m_activates_on_selection; }

    Function<void()> on_selection_change;
    Function<void(const ModelIndex&)> on_activation;
    Function<void(const ModelIndex&)> on_selection;
    Function<void(const ModelIndex&, const ContextMenuEvent&)> on_context_menu_request;

    Function<OwnPtr<ModelEditingDelegate>(const ModelIndex&)> aid_create_editing_delegate;

    void notify_selection_changed(Badge<ModelSelection>);

    NonnullRefPtr<Font> font_for_index(const ModelIndex&) const;

protected:
    explicit AbstractView(Widget* parent);
    virtual ~AbstractView() override;

    virtual void mousedown_event(MouseEvent&) override;
    virtual void mousemove_event(MouseEvent&) override;
    virtual void mouseup_event(MouseEvent&) override;
    virtual void doubleclick_event(MouseEvent&) override;
    virtual void context_menu_event(ContextMenuEvent&) override;

    virtual void did_scroll() override;
    void activate(const ModelIndex&);
    void activate_selected();
    void update_edit_widget_position();

    bool m_editable { false };
    ModelIndex m_edit_index;
    RefPtr<Widget> m_edit_widget;
    Rect m_edit_widget_content_rect;

    Point m_left_mousedown_position;
    bool m_might_drag { false };

private:
    RefPtr<Model> m_model;
    OwnPtr<ModelEditingDelegate> m_editing_delegate;
    ModelSelection m_selection;
    bool m_activates_on_selection { false };
};

}
