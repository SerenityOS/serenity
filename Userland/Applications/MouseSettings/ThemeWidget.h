/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "DoubleClickArrowWidget.h"
#include <LibGUI/Model.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGfx/CursorParams.h>

namespace MouseSettings {
class MouseCursorModel final : public GUI::Model {
public:
    static NonnullRefPtr<MouseCursorModel> create() { return adopt_ref(*new MouseCursorModel); }
    virtual ~MouseCursorModel() override = default;

    enum Column {
        Bitmap,
        Name,
        __Count,
    };

    virtual int row_count(const GUI::ModelIndex&) const override { return m_cursors.size(); }
    virtual int column_count(const GUI::ModelIndex&) const override { return Column::__Count; }

    virtual ErrorOr<String> column_name(int column_index) const override;
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override;
    virtual void invalidate() override;

    void change_theme(ByteString const& name)
    {
        m_theme_name = name;
        invalidate();
    }

private:
    MouseCursorModel() = default;

    struct Cursor {
        RefPtr<Gfx::Bitmap> bitmap;
        ByteString path;
        ByteString name;
        Gfx::CursorParams params;
    };

    Vector<Cursor> m_cursors;
    ByteString m_theme_name;
};

class ThemeModel final : public GUI::Model {
public:
    static NonnullRefPtr<ThemeModel> create() { return adopt_ref(*new ThemeModel); }
    virtual int row_count(const GUI::ModelIndex&) const override { return m_themes.size(); }
    virtual int column_count(const GUI::ModelIndex&) const override { return 1; }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override;
    virtual Vector<GUI::ModelIndex> matches(StringView, unsigned = GUI::Model::MatchesFlag::AllMatching, GUI::ModelIndex const& = GUI::ModelIndex()) override;
    virtual void invalidate() override;

private:
    Vector<ByteString> m_themes;
};

class ThemeWidget final : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(ThemeWidget)
public:
    static ErrorOr<NonnullRefPtr<ThemeWidget>> try_create();
    ErrorOr<void> initialize();

    virtual ~ThemeWidget() override = default;

    virtual void apply_settings() override;
    virtual void reset_default_values() override;

private:
    ThemeWidget() = default;

    RefPtr<GUI::TableView> m_cursors_tableview;
    RefPtr<GUI::ComboBox> m_theme_name_box;
    RefPtr<MouseCursorModel> m_mouse_cursor_model;
};
}
