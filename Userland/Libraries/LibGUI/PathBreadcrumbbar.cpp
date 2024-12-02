/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Mustafa Quraish <mustafa@cs.toronto.edu>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PathBreadcrumbbar.h"
#include <AK/LexicalPath.h>
#include <LibCore/MimeData.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Breadcrumbbar.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/TextBox.h>

REGISTER_WIDGET(GUI, PathBreadcrumbbar)

namespace GUI {

ErrorOr<NonnullRefPtr<PathBreadcrumbbar>> PathBreadcrumbbar::try_create()
{
    auto location_text_box = TextBox::construct();
    auto breadcrumbbar = Breadcrumbbar::construct();

    auto path_breadcrumbbar = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PathBreadcrumbbar(*location_text_box, *breadcrumbbar)));
    path_breadcrumbbar->set_layout<GUI::VerticalBoxLayout>();
    TRY(path_breadcrumbbar->try_add_child(location_text_box));
    TRY(path_breadcrumbbar->try_add_child(breadcrumbbar));

    return path_breadcrumbbar;
}

PathBreadcrumbbar::PathBreadcrumbbar(NonnullRefPtr<GUI::TextBox> location_text_box, NonnullRefPtr<GUI::Breadcrumbbar> breadcrumbbar)
    : Widget()
    , m_location_text_box(move(location_text_box))
    , m_breadcrumbbar(move(breadcrumbbar))
{
    m_location_text_box->set_visible(false);

    m_location_text_box->on_escape_pressed = [&] {
        hide_location_text_box();
    };
    m_location_text_box->on_focusout = [&] {
        hide_location_text_box();
    };

    m_location_text_box->on_return_pressed = [&] {
        if (FileSystem::is_directory(m_location_text_box->text())) {
            set_current_path(m_location_text_box->text());
            hide_location_text_box();
        }
    };

    m_breadcrumbbar->set_visible(true);

    m_breadcrumbbar->on_segment_change = [&](Optional<size_t> segment_index) {
        if (!segment_index.has_value())
            return;

        if (!on_path_change)
            return;

        auto segment_path = m_breadcrumbbar->segment_data(*segment_index);
        on_path_change(segment_path);
    };

    m_breadcrumbbar->on_segment_drag_enter = [&](size_t, GUI::DragEvent& event) {
        if (event.mime_data().has_urls())
            event.accept();
    };

    m_breadcrumbbar->on_segment_drop = [&](size_t segment_index, GUI::DropEvent const& event) {
        if (!event.mime_data().has_urls())
            return;
        if (on_paths_drop)
            on_paths_drop(m_breadcrumbbar->segment_data(segment_index), event);
    };

    m_breadcrumbbar->on_doubleclick = [&](auto) {
        show_location_text_box();
    };
}

PathBreadcrumbbar::~PathBreadcrumbbar() = default;

void PathBreadcrumbbar::set_current_path(ByteString const& new_path)
{
    if (new_path == m_current_path)
        return;

    LexicalPath lexical_path(new_path);
    m_current_path = new_path;

    auto segment_index_of_new_path_in_breadcrumbbar = m_breadcrumbbar->find_segment_with_data(new_path);

    if (segment_index_of_new_path_in_breadcrumbbar.has_value()) {
        auto new_segment_index = segment_index_of_new_path_in_breadcrumbbar.value();
        m_breadcrumbbar->set_selected_segment(new_segment_index);

        // If the path change was because the directory we were in was deleted,
        // remove the breadcrumbs for it.
        if ((new_segment_index + 1 < m_breadcrumbbar->segment_count())
            && !FileSystem::is_directory(m_breadcrumbbar->segment_data(new_segment_index + 1))) {
            m_breadcrumbbar->remove_end_segments(new_segment_index + 1);
        }
    } else {
        m_breadcrumbbar->clear_segments();

        m_breadcrumbbar->append_segment("/", GUI::FileIconProvider::icon_for_path("/"sv).bitmap_for_size(16), "/", "/"_string);
        StringBuilder builder;

        for (auto& part : lexical_path.parts()) {
            // NOTE: We rebuild the path as we go, so we have something to pass to GUI::FileIconProvider.
            builder.append('/');
            builder.append(part);

            m_breadcrumbbar->append_segment(part, GUI::FileIconProvider::icon_for_path(builder.string_view()).bitmap_for_size(16), builder.string_view(), MUST(builder.to_string()));
        }

        m_breadcrumbbar->set_selected_segment(m_breadcrumbbar->segment_count() - 1);
    }
}

bool PathBreadcrumbbar::has_parent_segment() const
{
    return m_breadcrumbbar->has_parent_segment();
}

bool PathBreadcrumbbar::has_child_segment() const
{
    return m_breadcrumbbar->has_child_segment();
}

void PathBreadcrumbbar::select_parent_segment()
{
    if (!has_parent_segment())
        return;
    m_breadcrumbbar->set_selected_segment(m_breadcrumbbar->selected_segment().value() - 1);
}

void PathBreadcrumbbar::select_child_segment()
{
    if (!has_child_segment())
        return;
    m_breadcrumbbar->set_selected_segment(m_breadcrumbbar->selected_segment().value() + 1);
}

void PathBreadcrumbbar::show_location_text_box()
{
    if (m_location_text_box->is_visible())
        return;
    m_location_text_box->set_visible(true);
    m_breadcrumbbar->set_visible(false);

    m_location_text_box->set_icon(GUI::FileIconProvider::icon_for_path(m_current_path).bitmap_for_size(16));
    m_location_text_box->set_text(m_current_path);
    m_location_text_box->select_all();
    m_location_text_box->set_focus(true);
}

void PathBreadcrumbbar::hide_location_text_box()
{
    if (!m_location_text_box->is_visible())
        return;
    m_location_text_box->set_visible(false);
    m_breadcrumbbar->set_visible(true);

    m_location_text_box->set_focus(false);

    if (on_hide_location_box)
        on_hide_location_box();
}

}
