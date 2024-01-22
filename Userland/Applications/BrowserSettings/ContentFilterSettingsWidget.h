/*
 * Copyright (c) 2022, Maciej Zygmanowski <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/CheckBox.h>
#include <LibGUI/ListView.h>
#include <LibGUI/Menu.h>
#include <LibGUI/SettingsWindow.h>

class DomainListModel : public GUI::Model {
public:
    virtual ErrorOr<String> filter_list_file_path() const;
    ErrorOr<void> load();
    ErrorOr<void> save();
    virtual void reset_default_values();

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return m_domain_list.size(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return 1; }
    virtual GUI::Variant data(GUI::ModelIndex const& index, GUI::ModelRole = GUI::ModelRole::Display) const override { return m_domain_list[index.row()]; }

    void add_domain(String name);
    void delete_domain(size_t index);

protected:
    bool m_was_modified { false };
    Vector<String> m_domain_list;
};

namespace BrowserSettings {

class ContentFilterSettingsWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(ContentFilterSettingsWidget)

public:
    static ErrorOr<NonnullRefPtr<ContentFilterSettingsWidget>> try_create();
    ErrorOr<void> initialize();

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    ContentFilterSettingsWidget() = default;

    void set_domain_list_model(NonnullRefPtr<DomainListModel>);

    RefPtr<GUI::Menu> m_entry_context_menu;
    RefPtr<GUI::CheckBox> m_enable_content_filtering_checkbox;
    RefPtr<GUI::Button> m_add_new_domain_button;
    RefPtr<GUI::ListView> m_domain_list_view;
    RefPtr<DomainListModel> m_domain_list_model;
};

}
