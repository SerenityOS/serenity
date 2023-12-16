/*
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Widget.h>

namespace GUI {

class PathBreadcrumbbar : public Widget {
    C_OBJECT_ABSTRACT(PathBreadcrumbbar)
public:
    static ErrorOr<NonnullRefPtr<PathBreadcrumbbar>> try_create();
    virtual ~PathBreadcrumbbar() override;

    void set_current_path(ByteString const&);

    void show_location_text_box();
    void hide_location_text_box();

    bool has_parent_segment() const;
    bool has_child_segment() const;

    void select_parent_segment();
    void select_child_segment();

    Function<void(StringView path)> on_path_change;
    Function<void(StringView path, GUI::DropEvent const&)> on_paths_drop;
    Function<void()> on_hide_location_box;

private:
    PathBreadcrumbbar(NonnullRefPtr<GUI::TextBox>, NonnullRefPtr<GUI::Breadcrumbbar>);

    NonnullRefPtr<GUI::TextBox> m_location_text_box;
    NonnullRefPtr<GUI::Breadcrumbbar> m_breadcrumbbar;

    ByteString m_current_path;
};

}
