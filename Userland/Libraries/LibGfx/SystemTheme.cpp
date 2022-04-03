/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/LexicalPath.h>
#include <AK/QuickSort.h>
#include <LibCore/ConfigFile.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/SystemTheme.h>
#include <string.h>

namespace Gfx {

static SystemTheme dummy_theme;
static SystemTheme const* theme_page = &dummy_theme;
static Core::AnonymousBuffer theme_buffer;

Core::AnonymousBuffer& current_system_theme_buffer()
{
    VERIFY(theme_buffer.is_valid());
    return theme_buffer;
}

void set_system_theme(Core::AnonymousBuffer buffer)
{
    theme_buffer = move(buffer);
    theme_page = theme_buffer.data<SystemTheme>();
}

Core::AnonymousBuffer load_system_theme(Core::ConfigFile const& file)
{
    auto buffer = Core::AnonymousBuffer::create_with_size(sizeof(SystemTheme)).release_value();

    auto* data = buffer.data<SystemTheme>();

    auto get_color = [&](auto& name) {
        auto color_string = file.read_entry("Colors", name);
        auto color = Color::from_string(color_string);
        if (!color.has_value())
            return Color(Color::Black);
        return color.value();
    };

    auto get_flag = [&](auto& name) {
        return file.read_bool_entry("Flags", name, false);
    };

    auto get_alignment = [&](auto& name, auto role) {
        auto alignment = file.read_entry("Alignments", name).to_lowercase();
        if (alignment.is_empty()) {
            switch (role) {
            case (int)AlignmentRole::TitleAlignment:
                return Gfx::TextAlignment::CenterLeft;
            default:
                dbgln("Alignment {} has no fallback value!", name);
                return Gfx::TextAlignment::CenterLeft;
            }
        }

        if (alignment == "left" || alignment == "centerleft")
            return Gfx::TextAlignment::CenterLeft;
        else if (alignment == "right" || alignment == "centerright")
            return Gfx::TextAlignment::CenterRight;
        else if (alignment == "center")
            return Gfx::TextAlignment::Center;

        dbgln("Alignment {} has an invalid value!", name);
        return Gfx::TextAlignment::CenterLeft;
    };

    auto get_metric = [&](auto& name, auto role) {
        int metric = file.read_num_entry("Metrics", name, -1);
        if (metric == -1) {
            switch (role) {
            case (int)MetricRole::BorderThickness:
                return 4;
            case (int)MetricRole::BorderRadius:
                return 0;
            case (int)MetricRole::TitleHeight:
                return 19;
            case (int)MetricRole::TitleButtonHeight:
                return 15;
            case (int)MetricRole::TitleButtonWidth:
                return 15;
            default:
                dbgln("Metric {} has no fallback value!", name);
                return 16;
            }
        }
        return metric;
    };

    auto get_path = [&](auto& name, auto role, bool allow_empty) {
        auto path = file.read_entry("Paths", name);
        if (path.is_empty()) {
            switch (role) {
            case (int)PathRole::TitleButtonIcons:
                return "/res/icons/16x16/";
            default:
                return allow_empty ? "" : "/res/";
            }
        }
        return &path[0];
    };

#undef __ENUMERATE_COLOR_ROLE
#define __ENUMERATE_COLOR_ROLE(role) \
    data->color[(int)ColorRole::role] = get_color(#role).value();
    ENUMERATE_COLOR_ROLES(__ENUMERATE_COLOR_ROLE)
#undef __ENUMERATE_COLOR_ROLE

#undef __ENUMERATE_ALIGNMENT_ROLE
#define __ENUMERATE_ALIGNMENT_ROLE(role) \
    data->alignment[(int)AlignmentRole::role] = get_alignment(#role, (int)AlignmentRole::role);
    ENUMERATE_ALIGNMENT_ROLES(__ENUMERATE_ALIGNMENT_ROLE)
#undef __ENUMERATE_ALIGNMENT_ROLE

#undef __ENUMERATE_FLAG_ROLE
#define __ENUMERATE_FLAG_ROLE(role) \
    data->flag[(int)FlagRole::role] = get_flag(#role);
    ENUMERATE_FLAG_ROLES(__ENUMERATE_FLAG_ROLE)
#undef __ENUMERATE_FLAG_ROLE

#undef __ENUMERATE_METRIC_ROLE
#define __ENUMERATE_METRIC_ROLE(role) \
    data->metric[(int)MetricRole::role] = get_metric(#role, (int)MetricRole::role);
    ENUMERATE_METRIC_ROLES(__ENUMERATE_METRIC_ROLE)
#undef __ENUMERATE_METRIC_ROLE

#define DO_PATH(x, allow_empty)                                                                                  \
    do {                                                                                                         \
        auto path = get_path(#x, (int)PathRole::x, allow_empty);                                                 \
        memcpy(data->path[(int)PathRole::x], path, min(strlen(path) + 1, sizeof(data->path[(int)PathRole::x]))); \
        data->path[(int)PathRole::x][sizeof(data->path[(int)PathRole::x]) - 1] = '\0';                           \
    } while (0)

    DO_PATH(TitleButtonIcons, false);
    DO_PATH(ActiveWindowShadow, true);
    DO_PATH(InactiveWindowShadow, true);
    DO_PATH(TaskbarShadow, true);
    DO_PATH(MenuShadow, true);
    DO_PATH(TooltipShadow, true);

    return buffer;
}

Core::AnonymousBuffer load_system_theme(String const& path)
{
    return load_system_theme(Core::ConfigFile::open(path).release_value_but_fixme_should_propagate_errors());
}

Vector<SystemThemeMetaData> list_installed_system_themes()
{
    Vector<SystemThemeMetaData> system_themes;
    Core::DirIterator dt("/res/themes", Core::DirIterator::SkipDots);
    while (dt.has_next()) {
        auto theme_name = dt.next_path();
        auto theme_path = String::formatted("/res/themes/{}", theme_name);
        system_themes.append({ LexicalPath::title(theme_name), theme_path });
    }
    quick_sort(system_themes, [](auto& a, auto& b) { return a.name < b.name; });
    return system_themes;
}

}
