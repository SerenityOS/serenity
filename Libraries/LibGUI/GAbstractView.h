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

class GModelEditingDelegate;

class GAbstractView : public GScrollableWidget {
    C_OBJECT(GAbstractView)
    friend class GModel;

public:
    void set_model(RefPtr<GModel>&&);
    GModel* model() { return m_model.ptr(); }
    const GModel* model() const { return m_model.ptr(); }

    GModelSelection& selection() { return m_selection; }
    const GModelSelection& selection() const { return m_selection; }
    void select_all();

    bool is_editable() const { return m_editable; }
    void set_editable(bool editable) { m_editable = editable; }

    virtual bool accepts_focus() const override { return true; }
    virtual void did_update_model();
    virtual void did_update_selection();

    virtual Rect content_rect(const GModelIndex&) const { return {}; }
    virtual GModelIndex index_at_event_position(const Point&) const = 0;
    void begin_editing(const GModelIndex&);
    void stop_editing();

    void set_activates_on_selection(bool b) { m_activates_on_selection = b; }
    bool activates_on_selection() const { return m_activates_on_selection; }

    Function<void()> on_selection_change;
    Function<void(const GModelIndex&)> on_activation;
    Function<void(const GModelIndex&)> on_selection;
    Function<void(const GModelIndex&, const GContextMenuEvent&)> on_context_menu_request;

    Function<OwnPtr<GModelEditingDelegate>(const GModelIndex&)> aid_create_editing_delegate;

    void notify_selection_changed(Badge<GModelSelection>);

    NonnullRefPtr<Font> font_for_index(const GModelIndex&) const;

protected:
    explicit GAbstractView(GWidget* parent);
    virtual ~GAbstractView() override;

    virtual void did_scroll() override;
    void activate(const GModelIndex&);
    void activate_selected();
    void update_edit_widget_position();

    bool m_editable { false };
    GModelIndex m_edit_index;
    RefPtr<GWidget> m_edit_widget;
    Rect m_edit_widget_content_rect;

private:
    RefPtr<GModel> m_model;
    OwnPtr<GModelEditingDelegate> m_editing_delegate;
    GModelSelection m_selection;
    bool m_activates_on_selection { false };
};
