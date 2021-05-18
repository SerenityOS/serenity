/*
 * Copyright (c) 2021, Fabian Blatz <fabianblatz@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FuzzyHaystackModel.h"
#include <AK/FuzzyMatch.h>
#include <AK/HashMap.h>
#include <AK/QuickSort.h>
#include <Applications/FuzzySearch/FuzzySearchGML.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Label.h>
#include <LibGUI/ListView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <stdbool.h>
#include <stdint.h>
#include <unistd.h>

static void list_files_recursive(const String& path, Vector<HaystackEntry>& data)
{
    Core::DirIterator dir_iterator(path, Core::DirIterator::Flags::SkipParentAndBaseDir);
    if (dir_iterator.has_error())
        return;

    while (dir_iterator.has_next()) {
        auto path = dir_iterator.next_full_path();
        if (Core::File::is_directory(path)) {
            data.append(HaystackEntry { path, 0 });
            list_files_recursive(path, data);
        } else {
            data.append(HaystackEntry { path, 0 });
        }
    }
}

static void load_haystack(Vector<HaystackEntry>& data)
{
    auto input = Core::File::standard_input();
    while (input->can_read_line()) {
        auto line = input->read_line();
        if (!line.is_empty() && !line.is_whitespace())
            data.append(HaystackEntry { line, 0 });
    }
    if (data.is_empty())
        list_files_recursive(".", data);
}

int main(int argc, char* argv[])
{
    if (pledge("stdio recvfd sendfd accept rpath unix cpath fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    if (pledge("stdio recvfd sendfd rpath accept", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Vector<HaystackEntry> haystack;

    bool frameless = false;
    FuzzyMatchOptions options { false };
    Core::ArgsParser args_parser;
    args_parser.set_general_help("Fuzzy search on stdin");
    args_parser.add_option(options.ignore_case, "ignore case distinctions in patterns and data", "ignore-case", 'i');
    args_parser.add_option(frameless, "display window frameless", "frameless", 'f');
    args_parser.parse(argc, argv);

    load_haystack(haystack);

    auto window = GUI::Window::construct();
    auto app_icon = GUI::Icon::default_icon("app-fuzzy-search");

    if (frameless)
        window->set_frameless(true);

    window->set_title("FuzzySearch");
    window->resize(480, 480);
    window->set_minimum_size(160, 200);
    window->center_on_screen();
    window->set_icon(app_icon.bitmap_for_size(16));
    auto model = FuzzyHaystackModel::create(haystack, options);

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.load_from_gml(fuzzy_search_gml);

    auto haystack_view = main_widget.find_descendant_of_type_named<GUI::ListView>("haystack_listview");
    auto needle_textbox = main_widget.find_descendant_of_type_named<GUI::TextBox>("needle_textbox");
    auto needle_label = main_widget.find_descendant_of_type_named<GUI::Label>("needle_label");

    auto refresh_label = [&] {
        needle_label->set_text(String::formatted("({}/{}) >", model->row_count(), model->unfiltered_row_count()));
    };

    haystack_view->set_alternating_row_colors(false);
    haystack_view->set_model(model);
    haystack_view->set_cursor(model->index(model->row_count() - 1), GUI::AbstractView::SelectionUpdate::Set);

    haystack_view->on_activation = [&](const GUI::ModelIndex& index) {
        out(stdout, index.data().as_string());
        app->quit(0);
    };

    haystack_view->on_escape_pressed = [&] { app->quit(1); };

    needle_textbox->on_change = [&] {
        model->set_needle(needle_textbox->text());
        haystack_view->set_cursor(model->index(model->row_count() - 1), GUI::AbstractView::SelectionUpdate::Set);
        refresh_label();
    };

    needle_textbox->on_up_pressed = [&] {
        haystack_view->move_cursor(GUI::AbstractView::CursorMovement::Up, GUI::AbstractView::SelectionUpdate::Set);
    };
    needle_textbox->on_down_pressed = [&] {
        haystack_view->move_cursor(GUI::AbstractView::CursorMovement::Down, GUI::AbstractView::SelectionUpdate::Set);
    };
    needle_textbox->on_pagedown_pressed = [&] {
        haystack_view->move_cursor(GUI::AbstractView::CursorMovement::PageDown, GUI::AbstractView::SelectionUpdate::Set);
    };
    needle_textbox->on_pageup_pressed = [&] {
        haystack_view->move_cursor(GUI::AbstractView::CursorMovement::PageUp, GUI::AbstractView::SelectionUpdate::Set);
    };
    needle_textbox->on_return_pressed = [&] {
        out(stdout, model->data(haystack_view->cursor_index()).as_string());
        app->quit(0);
    };

    needle_textbox->on_escape_pressed = [&] { app->quit(1); };

    needle_textbox->set_focus(true);
    refresh_label();
    window->show();
    return app->exec();
}
