/*
 * Copyright (c) 2022, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "LibGUI/CheckBox.h"
#include <LibGUI/Button.h>
#include <LibGUI/ListView.h>
#include <LibGUI/SettingsWindow.h>

class DomainListModel : public GUI::Model {
public:
    ErrorOr<void> load();
    ErrorOr<void> save();
    void reset_default_values();

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return m_domain_list.size(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 1; }
    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole = GUI::ModelRole::Display) const override { return m_domain_list[index.row()]; }

    void add_domain(String name);
    void delete_domain(size_t index);

private:
    bool m_was_modified { false };
    Vector<String> m_domain_list;
};

class ContentFilterSettingsWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT(ContentFilterSettingsWidget)
public:
    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    ContentFilterSettingsWidget();

    RefPtr<GUI::Menu> m_entry_context_menu;
    RefPtr<GUI::CheckBox> m_enable_content_filtering_checkbox;
    RefPtr<GUI::Button> m_add_new_domain_button;
    RefPtr<GUI::ListView> m_domain_list_view;
    RefPtr<DomainListModel> m_domain_list_model;
};
