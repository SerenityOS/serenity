/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HelpWindow.h"
#include "Spreadsheet.h"
#include "SpreadsheetWidget.h"
#include <AK/ScopeGuard.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/File.h>
#include <LibGUI/Application.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Window.h>
#include <unistd.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio recvfd sendfd rpath fattr unix cpath wpath thread", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    auto app = GUI::Application::construct(argc, argv);

    const char* filename = nullptr;

    Core::ArgsParser args_parser;
    args_parser.add_positional_argument(filename, "File to read from", "file", Core::ArgsParser::Required::No);

    args_parser.parse(argc, argv);

    if (filename) {
        if (!Core::File::exists(filename) || Core::File::is_directory(filename)) {
            warnln("File does not exist or is a directory: {}", filename);
            return 1;
        }
    }

    if (unveil("/tmp/portal/webcontent", "rw") < 0) {
        perror("unveil");
        return 1;
    }

    // For writing temporary files when exporting.
    if (unveil("/tmp", "crw") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/etc", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(Core::StandardPaths::home_directory().characters(), "rwc") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil("/res", "r") < 0) {
        perror("unveil");
        return 1;
    }

    if (unveil(nullptr, nullptr) < 0) {
        perror("unveil");
        return 1;
    }

    auto app_icon = GUI::Icon::default_icon("app-spreadsheet");
    auto window = GUI::Window::construct();
    window->set_title("Spreadsheet");
    window->resize(640, 480);
    window->set_icon(app_icon.bitmap_for_size(16));

    auto& spreadsheet_widget = window->set_main_widget<Spreadsheet::SpreadsheetWidget>(NonnullRefPtrVector<Spreadsheet::Sheet> {}, filename == nullptr);

    if (filename)
        spreadsheet_widget.load(filename);

    auto menubar = GUI::Menubar::construct();
    auto& file_menu = menubar->add_menu("&File");

    file_menu.add_action(GUI::Action::create("Add New Sheet", Gfx::Bitmap::load_from_file("/res/icons/16x16/new-tab.png"), [&](auto&) {
        spreadsheet_widget.add_sheet();
    }));
    file_menu.add_action(GUI::CommonActions::make_open_action([&](auto&) {
        Optional<String> load_path = GUI::FilePicker::get_open_filepath(window);
        if (!load_path.has_value())
            return;

        spreadsheet_widget.load(load_path.value());
    }));

    file_menu.add_action(GUI::CommonActions::make_save_action([&](auto&) {
        if (spreadsheet_widget.current_filename().is_empty()) {
            String name = "workbook";
            Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, name, "sheets");
            if (!save_path.has_value())
                return;

            spreadsheet_widget.save(save_path.value());
        } else {
            spreadsheet_widget.save(spreadsheet_widget.current_filename());
        }
    }));

    file_menu.add_action(GUI::CommonActions::make_save_as_action([&](auto&) {
        auto current_filename = spreadsheet_widget.current_filename();
        String name = "workbook";
        Optional<String> save_path = GUI::FilePicker::get_save_filepath(window, name, "sheets");
        if (!save_path.has_value())
            return;

        spreadsheet_widget.save(save_path.value());

        if (!current_filename.is_empty())
            spreadsheet_widget.set_filename(current_filename);
    }));
    file_menu.add_separator();

    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        if (!spreadsheet_widget.request_close())
            return;
        app->quit(0);
    }));

    window->on_close_request = [&]() -> GUI::Window::CloseRequestDecision {
        if (spreadsheet_widget.request_close())
            return GUI::Window::CloseRequestDecision::Close;
        return GUI::Window::CloseRequestDecision::StayOpen;
    };

    auto& edit_menu = menubar->add_menu("&Edit");

    auto clipboard_action = [&](bool is_cut) {
        /// text/x-spreadsheet-data:
        /// - action: copy/cut
        /// - currently selected cell
        /// - selected cell+
        auto* worksheet_ptr = spreadsheet_widget.current_worksheet_if_available();
        if (!worksheet_ptr) {
            GUI::MessageBox::show_error(window, "There are no active worksheets");
            return;
        }
        auto& worksheet = *worksheet_ptr;
        auto& cells = worksheet.selected_cells();
        VERIFY(!cells.is_empty());
        StringBuilder text_builder, url_builder;
        url_builder.append(is_cut ? "cut\n" : "copy\n");
        bool first = true;
        auto cursor = spreadsheet_widget.current_selection_cursor();
        if (cursor) {
            Spreadsheet::Position position { (size_t)cursor->column(), (size_t)cursor->row() };
            url_builder.append(position.to_url(worksheet).to_string());
            url_builder.append('\n');
        }

        for (auto& cell : cells) {
            if (first && !cursor) {
                url_builder.append(cell.to_url(worksheet).to_string());
                url_builder.append('\n');
            }

            url_builder.append(cell.to_url(worksheet).to_string());
            url_builder.append('\n');

            auto cell_data = worksheet.at(cell);
            if (!first)
                text_builder.append('\t');
            if (cell_data)
                text_builder.append(cell_data->data());
            first = false;
        }
        HashMap<String, String> metadata;
        metadata.set("text/x-spreadsheet-data", url_builder.to_string());
        dbgln(url_builder.to_string());

        GUI::Clipboard::the().set_data(text_builder.string_view().bytes(), "text/plain", move(metadata));
    };

    edit_menu.add_action(GUI::CommonActions::make_copy_action([&](auto&) { clipboard_action(false); }, window));
    edit_menu.add_action(GUI::CommonActions::make_cut_action([&](auto&) { clipboard_action(true); }, window));
    edit_menu.add_action(GUI::CommonActions::make_paste_action([&](auto&) {
        ScopeGuard update_after_paste { [&] { spreadsheet_widget.update(); } };

        auto* worksheet_ptr = spreadsheet_widget.current_worksheet_if_available();
        if (!worksheet_ptr) {
            GUI::MessageBox::show_error(window, "There are no active worksheets");
            return;
        }
        auto& sheet = *worksheet_ptr;
        auto& cells = sheet.selected_cells();
        VERIFY(!cells.is_empty());
        const auto& data = GUI::Clipboard::the().data_and_type();
        if (auto spreadsheet_data = data.metadata.get("text/x-spreadsheet-data"); spreadsheet_data.has_value()) {
            Vector<Spreadsheet::Position> source_positions, target_positions;
            auto lines = spreadsheet_data.value().split_view('\n');
            auto action = lines.take_first();

            for (auto& line : lines) {
                dbgln("Paste line '{}'", line);
                auto position = sheet.position_from_url(line);
                if (position.has_value())
                    source_positions.append(position.release_value());
            }

            for (auto& position : sheet.selected_cells())
                target_positions.append(position);

            if (source_positions.is_empty())
                return;

            auto first_position = source_positions.take_first();
            sheet.copy_cells(move(source_positions), move(target_positions), first_position, action == "cut" ? Spreadsheet::Sheet::CopyOperation::Cut : Spreadsheet::Sheet::CopyOperation::Copy);
        } else {
            for (auto& cell : sheet.selected_cells())
                sheet.ensure(cell).set_data(StringView { data.data.data(), data.data.size() });
            spreadsheet_widget.update();
        }
    },
        window));

    auto& help_menu = menubar->add_menu("&Help");

    help_menu.add_action(GUI::Action::create(
        "&Functions Help", [&](auto&) {
            if (auto* worksheet_ptr = spreadsheet_widget.current_worksheet_if_available()) {
                auto docs = worksheet_ptr->gather_documentation();
                auto help_window = Spreadsheet::HelpWindow::the(window);
                help_window->set_docs(move(docs));
                help_window->show();
            } else {
                GUI::MessageBox::show_error(window, "Cannot prepare documentation/help without an active worksheet");
            }
        },
        window));

    help_menu.add_action(GUI::CommonActions::make_about_action("Spreadsheet", app_icon, window));

    window->set_menubar(move(menubar));

    window->show();

    return app->exec();
}
