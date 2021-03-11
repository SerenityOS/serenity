/*
 * Copyright (c) 2021, the SerenityOS developers
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

#include <AK/NonnullRefPtr.h>
#include <AK/Vector.h>
#include <LibCore/DirIterator.h>
#include <LibGUI/Model.h>

class MouseCursorModel final : public GUI::Model {
public:
    static NonnullRefPtr<MouseCursorModel> create() { return adopt(*new MouseCursorModel); }
    virtual ~MouseCursorModel() override { }

    enum Column {
        Bitmap,
        Name,
        __Count,
    };

    virtual int row_count(const GUI::ModelIndex&) const override { return m_cursors.size(); }
    virtual int column_count(const GUI::ModelIndex&) const override { return Column::__Count; }
    virtual String column_name(int column_index) const override
    {
        switch (column_index) {
        case Column::Bitmap:
            return {};
        case Column::Name:
            return "Name";
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
    virtual void update() override
    {
        m_cursors.clear();

        Core::DirIterator iterator("/res/cursors", Core::DirIterator::Flags::SkipDots);

        while (iterator.has_next()) {
            auto path = iterator.next_path();
            if (path.contains("2x"))
                continue;
            Cursor cursor;
            StringBuilder full_path;
            full_path.append("/res/cursors/");
            full_path.append(path);
            cursor.path = full_path.to_string();
            cursor.bitmap = Gfx::Bitmap::load_from_file(cursor.path);
            auto filename_split = path.split('.');
            cursor.name = filename_split[0];
            m_cursors.append(move(cursor));
        }

        did_update();
    }

private:
    MouseCursorModel() { }

    struct Cursor {
        RefPtr<Gfx::Bitmap> bitmap;
        String path;
        String name;
    };

    Vector<Cursor> m_cursors;
};

class FileIconsModel final : public GUI::Model {
public:
    static NonnullRefPtr<MouseCursorModel> create() { return adopt(*new FileIconsModel); }
    virtual ~FileIconsModel() override { }

    enum Column {
        BigIcon,
        LittleIcon,
        Name,
        __Count,
    };

    virtual int row_count(const GUI::ModelIndex&) const override { return m_icon_sets.size(); }
    virtual int column_count(const GUI::ModelIndex&) const override { return Column::__Count; }
    virtual String column_name(int column_index) const override
    {
        switch (column_index) {
        case Column::BigIcon:
            return {};
        case Column::LittleIcon:
            return {};
        case Column::Name:
            return "Name";
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
    virtual void update() override
    {
        m_icon_sets.clear();

        Core::DirIterator big_iterator("/res/icons/32x32", Core::DirIterator::Flags::SkipDots);

        while (big_iterator.has_next()) {
            auto path = big_iterator.next_path();
            if (!path.contains("filetype-") && !path.contains("app-"))
                continue;
            IconSet icon_set;
            StringBuilder full_path;
            full_path.append("/res/icons/32x32/");
            full_path.append(path);
            icon_set.big_icon = Gfx::Bitmap::load_from_file(full_path.to_string());
            auto filename_split = path.split('.');
            icon_set.name = filename_split[0];
            m_icon_sets.append(move(icon_set));
        }

        auto big_icons_found = m_icon_sets.size();

        Core::DirIterator little_iterator("/res/icons/16x16", Core::DirIterator::Flags::SkipDots);

        while (little_iterator.has_next()) {
            auto path = little_iterator.next_path();
            if (!path.contains("filetype-") && !path.contains("app-"))
                continue;
            IconSet icon_set;
            StringBuilder full_path;
            full_path.append("/res/icons/16x16/");
            full_path.append(path);
            icon_set.little_icon = Gfx::Bitmap::load_from_file(full_path.to_string());
            auto filename_split = path.split('.');
            icon_set.name = filename_split[0];
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

        did_update();
    }

private:
    FileIconsModel() { }

    struct IconSet {
        RefPtr<Gfx::Bitmap> big_icon;
        RefPtr<Gfx::Bitmap> little_icon;
        String name;
    };

    Vector<IconSet> m_icon_sets;
};
