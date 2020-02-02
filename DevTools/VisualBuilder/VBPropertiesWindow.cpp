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

#include "VBPropertiesWindow.h"
#include "VBWidgetPropertyModel.h"
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GComboBox.h>
#include <LibGUI/GModelEditingDelegate.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GWidget.h>

class BoolValuesModel final : public GModel {
public:
    virtual int row_count(const GModelIndex&) const override { return 2; }
    virtual int column_count(const GModelIndex&) const override { return 1; }
    virtual void update() override {}
    virtual GVariant data(const GModelIndex& index, Role role) const override
    {
        if (role != Role::Display)
            return {};
        switch (index.row()) {
        case 0:
            return "false";
        case 1:
            return "true";
        }
        ASSERT_NOT_REACHED();
    }
};

class BoolModelEditingDelegate : public GModelEditingDelegate {
public:
    BoolModelEditingDelegate() {}
    virtual ~BoolModelEditingDelegate() override {}

    virtual RefPtr<GWidget> create_widget() override
    {
        auto combo = GComboBox::construct(nullptr);
        combo->set_only_allow_values_from_model(true);
        combo->set_model(adopt(*new BoolValuesModel));
        combo->on_return_pressed = [this] { commit(); };
        combo->on_change = [this](auto&, auto&) { commit(); };
        return combo;
    }
    virtual GVariant value() const override { return static_cast<const GComboBox*>(widget())->text() == "true"; }
    virtual void set_value(const GVariant& value) override { static_cast<GComboBox*>(widget())->set_text(value.to_string()); }
    virtual void will_begin_editing() override
    {
        auto& combo = *static_cast<GComboBox*>(widget());
        combo.select_all();
        combo.open();
    }
};

VBPropertiesWindow::VBPropertiesWindow()
{
    set_title("Properties");
    set_rect(780, 200, 240, 280);

    auto widget = GWidget::construct();
    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GVBoxLayout>());
    widget->layout()->set_margins({ 2, 2, 2, 2 });
    set_main_widget(widget);

    m_table_view = GTableView::construct(widget);
    m_table_view->set_headers_visible(false);
    m_table_view->set_editable(true);

    m_table_view->aid_create_editing_delegate = [this](auto& index) -> OwnPtr<GModelEditingDelegate> {
        if (!m_table_view->model())
            return nullptr;
        auto type_index = m_table_view->model()->index(index.row(), VBWidgetPropertyModel::Column::Type);
        auto type = m_table_view->model()->data(type_index, GModel::Role::Custom).to_i32();
        switch ((GVariant::Type)type) {
        case GVariant::Type::Bool:
            return make<BoolModelEditingDelegate>();
        default:
            return make<GStringModelEditingDelegate>();
        }
    };
}

VBPropertiesWindow::~VBPropertiesWindow()
{
}
