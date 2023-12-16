/*
 * Copyright (c) 2021-2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/LexicalPath.h>
#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Model.h>
#include <LibGfx/CursorParams.h>

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
    virtual ErrorOr<String> column_name(int column_index) const override
    {
        switch (column_index) {
        case Column::Bitmap:
            return String {};
        case Column::Name:
            return "Name"_string;
        }
        VERIFY_NOT_REACHED();
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        auto& cursor = m_cursors[index.row()];

        if (role == GUI::ModelRole::Display) {
            switch (index.column()) {
            case Column::Bitmap:
                if (!cursor.bitmap)
                    return {};
                return *cursor.bitmap;
            case Column::Name:
                return cursor.name;
            }
            VERIFY_NOT_REACHED();
        }
        return {};
    }

    virtual void invalidate() override
    {
        m_cursors.clear();

        Core::DirIterator iterator(ByteString::formatted("/res/cursor-themes/{}", GUI::ConnectionToWindowServer::the().get_cursor_theme()), Core::DirIterator::Flags::SkipDots);

        while (iterator.has_next()) {
            auto path = iterator.next_full_path();
            if (path.ends_with(".ini"sv))
                continue;
            if (path.contains("2x"sv))
                continue;
            Cursor cursor;
            cursor.path = move(path);
            cursor.name = LexicalPath::basename(cursor.path);

            // FIXME: Animated cursor bitmaps
            auto cursor_bitmap = Gfx::Bitmap::load_from_file(cursor.path).release_value_but_fixme_should_propagate_errors();
            auto cursor_bitmap_rect = cursor_bitmap->rect();

            cursor.params = Gfx::CursorParams::parse_from_filename(cursor.name, cursor_bitmap_rect.center()).constrained(*cursor_bitmap);
            cursor.bitmap = cursor_bitmap->cropped(Gfx::IntRect(Gfx::FloatRect(cursor_bitmap_rect).scaled(1.0 / cursor.params.frames(), 1.0))).release_value_but_fixme_should_propagate_errors();

            m_cursors.append(move(cursor));
        }

        Model::invalidate();
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
};

class FileIconsModel final : public GUI::Model {
public:
    static NonnullRefPtr<FileIconsModel> create() { return adopt_ref(*new FileIconsModel); }
    virtual ~FileIconsModel() override = default;

    enum Column {
        BigIcon,
        LittleIcon,
        Name,
        __Count,
    };

    virtual int row_count(const GUI::ModelIndex&) const override { return m_icon_sets.size(); }
    virtual int column_count(const GUI::ModelIndex&) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int column_index) const override
    {
        switch (column_index) {
        case Column::BigIcon:
            return String {};
        case Column::LittleIcon:
            return String {};
        case Column::Name:
            return "Name"_string;
        }
        VERIFY_NOT_REACHED();
    }

    virtual GUI::Variant data(const GUI::ModelIndex& index, GUI::ModelRole role) const override
    {
        auto& icon_set = m_icon_sets[index.row()];

        if (role == GUI::ModelRole::Display) {
            switch (index.column()) {
            case Column::BigIcon:
                if (!icon_set.big_icon)
                    return {};
                return *icon_set.big_icon;
            case Column::LittleIcon:
                if (!icon_set.little_icon)
                    return {};
                return *icon_set.little_icon;
            case Column::Name:
                return icon_set.name;
            }
            VERIFY_NOT_REACHED();
        }
        return {};
    }

    virtual void invalidate() override
    {
        m_icon_sets.clear();

        Core::DirIterator big_iterator("/res/icons/32x32", Core::DirIterator::Flags::SkipDots);

        while (big_iterator.has_next()) {
            auto path = big_iterator.next_full_path();
            if (!path.contains("filetype-"sv) && !path.contains("app-"sv))
                continue;
            IconSet icon_set;
            icon_set.big_icon = Gfx::Bitmap::load_from_file(path).release_value_but_fixme_should_propagate_errors();
            icon_set.name = LexicalPath::basename(path);
            m_icon_sets.append(move(icon_set));
        }

        auto big_icons_found = m_icon_sets.size();

        Core::DirIterator little_iterator("/res/icons/16x16", Core::DirIterator::Flags::SkipDots);

        while (little_iterator.has_next()) {
            auto path = little_iterator.next_full_path();
            if (!path.contains("filetype-"sv) && !path.contains("app-"sv))
                continue;
            IconSet icon_set;
            icon_set.little_icon = Gfx::Bitmap::load_from_file(path).release_value_but_fixme_should_propagate_errors();
            icon_set.name = LexicalPath::basename(path);
            for (size_t i = 0; i < big_icons_found; i++) {
                if (icon_set.name == m_icon_sets[i].name) {
                    m_icon_sets[i].little_icon = icon_set.little_icon;
                    goto next_iteration;
                }
            }
            m_icon_sets.append(move(icon_set));
        next_iteration:
            continue;
        }

        Model::invalidate();
    }

private:
    FileIconsModel() = default;

    struct IconSet {
        RefPtr<Gfx::Bitmap> big_icon;
        RefPtr<Gfx::Bitmap> little_icon;
        ByteString name;
    };

    Vector<IconSet> m_icon_sets;
};
