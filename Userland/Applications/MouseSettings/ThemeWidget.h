/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/CursorParams.h>

#include "DoubleClickArrowWidget.h"
#include "LibGUI/FilePicker.h"

class MouseCursorModel final : public GUI::Model {
public:
    static NonnullRefPtr<MouseCursorModel> create() { return adopt_ref(*new MouseCursorModel); }
    virtual ~MouseCursorModel() override { }

    enum Column {
        Bitmap,
        Name,
        __Count,
    };

    virtual int row_count(const GUI::ModelIndex&) const override { return m_cursors.size(); }
    virtual int column_count(const GUI::ModelIndex&) const override { return Column::__Count; }

    virtual String column_name(int column_index) const override;
    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override;
    virtual void invalidate() override;

    void change_theme(String const& name)
    {
        m_theme_name = name;
        invalidate();
    }

private:
    MouseCursorModel() { }

    struct Cursor {
        RefPtr<Gfx::Bitmap> bitmap;
        String path;
        String name;
        Gfx::CursorParams params;
    };

    Vector<Cursor> m_cursors;
    String m_theme_name;
};

class ThemeModel final : public GUI::Model {
public:
    static NonnullRefPtr<ThemeModel> create() { return adopt_ref(*new ThemeModel); }
    virtual int row_count(const GUI::ModelIndex&) const override { return m_themes.size(); }
    virtual int column_count(const GUI::ModelIndex&) const override { return 1; }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override;
    virtual void invalidate() override;

private:
    Vector<String> m_themes;
};

class ThemeWidget final : public GUI::Widget {
    C_OBJECT(ThemeWidget)
public:
    virtual ~ThemeWidget() override;

    void update_window_server();
    void reset_default_values();

private:
    ThemeWidget();

    RefPtr<GUI::TableView> m_cursors_tableview;
    RefPtr<GUI::ComboBox> m_theme_name_box;
    String m_theme_name;
};
