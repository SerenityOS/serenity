/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "CursorTool.h"
#include "Editor.h"
#include "EditorWrapper.h"
#include "FindInFilesWidget.h"
#include "FormEditorWidget.h"
#include "FormWidget.h"
#include "Locator.h"
#include "Project.h"
#include "TerminalWrapper.h"
#include "WidgetTool.h"
#include "WidgetTreeModel.h"
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
#include <LibGUI/CppSyntaxHighlighter.h>
#include <LibGUI/FilePicker.h>
#include <LibGUI/InputBox.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Splitter.h>
#include <LibGUI/StackWidget.h>
#include <LibGUI/TabWidget.h>
#include <LibGUI/TableView.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/TextEditor.h>
#include <LibGUI/ToolBar.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <stdio.h>
#include <sys/wait.h>
#include <unistd.h>

NonnullRefPtrVector<EditorWrapper> g_all_editor_wrappers;
RefPtr<EditorWrapper> g_current_editor_wrapper;

String g_currently_open_file;
OwnPtr<Project> g_project;
RefPtr<GUI::Window> g_window;
RefPtr<GUI::TreeView> g_project_tree_view;
RefPtr<GUI::StackWidget> g_right_hand_stack;
RefPtr<GUI::Splitter> g_text_inner_splitter;
RefPtr<GUI::Widget> g_form_inner_container;
RefPtr<FormEditorWidget> g_form_editor_widget;

static RefPtr<GUI::TabWidget> s_action_tab_widget;

void add_new_editor(GUI::Widget& parent)
{
    auto wrapper = EditorWrapper::construct();
    if (s_action_tab_widget) {
        parent.insert_child_before(wrapper, *s_action_tab_widget);
    } else {
        parent.add_child(wrapper);
    }
    g_current_editor_wrapper = wrapper;
    g_all_editor_wrappers.append(wrapper);
    wrapper->editor().set_focus(true);
}

enum class EditMode {
    Text,
    Form,
};

void set_edit_mode(EditMode mode)
{
    if (mode == EditMode::Text) {
        g_right_hand_stack->set_active_widget(g_text_inner_splitter);
    } else if (mode == EditMode::Form) {
        g_right_hand_stack->set_active_widget(g_form_inner_container);
    }
}

EditorWrapper& current_editor_wrapper()
{
    ASSERT(g_current_editor_wrapper);
    return *g_current_editor_wrapper;
}

Editor& current_editor()
{
    return current_editor_wrapper().editor();
}

static void build(TerminalWrapper&);
static void run(TerminalWrapper&);
void open_file(const String&);
bool make_is_available();

int main(int argc, char** argv)
{
    if (pledge("stdio tty accept rpath cpath wpath shared_buffer proc exec unix fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    GUI::Application app(argc, argv);

    if (pledge("stdio tty accept rpath cpath wpath shared_buffer proc exec fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    Function<void()> update_actions;

    g_window = GUI::Window::construct();
    g_window->set_rect(90, 90, 840, 600);
    g_window->set_title("HackStudio");

    auto widget = GUI::Widget::construct();
    g_window->set_main_widget(widget);

    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GUI::VerticalBoxLayout>());
    widget->layout()->set_spacing(0);

    StringBuilder path;
    path.append(getenv("PATH"));
    if (path.length())
        path.append(":");
    path.append("/bin:/usr/bin:/usr/local/bin");
    setenv("PATH", path.to_string().characters(), true);

    if (!make_is_available())
        GUI::MessageBox::show("The 'make' command is not available. You probably want to install the binutils, gcc, and make ports from the root of the Serenity repository.", "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, g_window);

    if (chdir("/home/anon/little") < 0) {
        perror("chdir");
        return 1;
    }
    g_project = Project::load_from_file("little.files");
    ASSERT(g_project);

    auto toolbar = widget->add<GUI::ToolBar>();

    auto selected_file_names = [&] {
        Vector<String> files;
        g_project_tree_view->selection().for_each_index([&](const GUI::ModelIndex& index) {
            files.append(g_project->model().data(index).as_string());
        });
        return files;
    };

    auto new_action = GUI::Action::create("Add new file to project...", { Mod_Ctrl, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"), [&](const GUI::Action&) {
        auto input_box = g_window->add<GUI::InputBox>("Enter name of new file:", "Add new file to project");
        if (input_box->exec() == GUI::InputBox::ExecCancel)
            return;
        auto filename = input_box->text_value();
        auto file = Core::File::construct(filename);
        if (!file->open((Core::IODevice::OpenMode)(Core::IODevice::WriteOnly | Core::IODevice::MustBeNew))) {
            GUI::MessageBox::show(String::format("Failed to create '%s'", filename.characters()), "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, g_window);
            return;
        }
        if (!g_project->add_file(filename)) {
            GUI::MessageBox::show(String::format("Failed to add '%s' to project", filename.characters()), "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, g_window);
            // FIXME: Should we unlink the file here maybe?
            return;
        }
        open_file(filename);
    });

    auto add_existing_file_action = GUI::Action::create("Add existing file to project...", Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"), [&](auto&) {
        auto result = GUI::FilePicker::get_open_filepath("Add existing file to project");
        if (!result.has_value())
            return;
        auto& filename = result.value();
        if (!g_project->add_file(filename)) {
            GUI::MessageBox::show(String::format("Failed to add '%s' to project", filename.characters()), "Error", GUI::MessageBox::Type::Error, GUI::MessageBox::InputType::OK, g_window);
            return;
        }
        open_file(filename);
    });

    auto delete_action = GUI::CommonActions::make_delete_action([&](const GUI::Action& action) {
        (void)action;

        auto files = selected_file_names();
        if (files.is_empty())
            return;

        String message;
        if (files.size() == 1) {
            message = String::format("Really remove %s from the project?", FileSystemPath(files[0]).basename().characters());
        } else {
            message = String::format("Really remove %d files from the project?", files.size());
        }

        auto result = GUI::MessageBox::show(
            message,
            "Confirm deletion",
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel,
            g_window);
        if (result == GUI::MessageBox::ExecCancel)
            return;

        for (auto& file : files) {
            if (!g_project->remove_file(file)) {
                GUI::MessageBox::show(
                    String::format("Removing file %s from the project failed.", file.characters()),
                    "Removal failed",
                    GUI::MessageBox::Type::Error,
                    GUI::MessageBox::InputType::OK,
                    g_window);
                break;
            }
        }
    });
    delete_action->set_enabled(false);

    auto project_tree_view_context_menu = GUI::Menu::construct("Project Files");
    project_tree_view_context_menu->add_action(new_action);
    project_tree_view_context_menu->add_action(add_existing_file_action);
    project_tree_view_context_menu->add_action(delete_action);

    auto outer_splitter = widget->add<GUI::HorizontalSplitter>();
    g_project_tree_view = outer_splitter->add<GUI::TreeView>();
    g_project_tree_view->set_model(g_project->model());
    g_project_tree_view->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    g_project_tree_view->set_preferred_size(140, 0);

    g_project_tree_view->on_context_menu_request = [&](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid()) {
            project_tree_view_context_menu->popup(event.screen_position());
        }
    };

    g_project_tree_view->on_selection_change = [&] {
        delete_action->set_enabled(!g_project_tree_view->selection().is_empty());
    };

    g_right_hand_stack = outer_splitter->add<GUI::StackWidget>();

    g_form_inner_container = g_right_hand_stack->add<GUI::Widget>();
    g_form_inner_container->set_layout(make<GUI::HorizontalBoxLayout>());
    auto form_widgets_toolbar = g_form_inner_container->add<GUI::ToolBar>(Orientation::Vertical, 26);
    form_widgets_toolbar->set_preferred_size(38, 0);

    GUI::ActionGroup tool_actions;
    tool_actions.set_exclusive(true);

    auto cursor_tool_action = GUI::Action::create("Cursor", Gfx::Bitmap::load_from_file("/res/icons/widgets/Cursor.png"), [&](auto&) {
        g_form_editor_widget->set_tool(make<CursorTool>(*g_form_editor_widget));
    });
    cursor_tool_action->set_checkable(true);
    cursor_tool_action->set_checked(true);
    tool_actions.add_action(cursor_tool_action);

    form_widgets_toolbar->add_action(cursor_tool_action);

    GUI::WidgetClassRegistration::for_each([&](const GUI::WidgetClassRegistration& reg) {
        auto icon_path = String::format("/res/icons/widgets/G%s.png", reg.class_name().characters());
        auto action = GUI::Action::create(reg.class_name(), Gfx::Bitmap::load_from_file(icon_path), [&reg](auto&) {
            g_form_editor_widget->set_tool(make<WidgetTool>(*g_form_editor_widget, reg));
            auto widget = reg.construct();
            g_form_editor_widget->form_widget().add_child(widget);
            widget->set_relative_rect(30, 30, 30, 30);
            g_form_editor_widget->model().update();
        });
        action->set_checkable(true);
        action->set_checked(false);
        tool_actions.add_action(action);
        form_widgets_toolbar->add_action(move(action));
    });

    auto form_editor_inner_splitter = g_form_inner_container->add<GUI::HorizontalSplitter>();

    g_form_editor_widget = form_editor_inner_splitter->add<FormEditorWidget>();

    auto form_editing_pane_container = form_editor_inner_splitter->add<GUI::VerticalSplitter>();
    form_editing_pane_container->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    form_editing_pane_container->set_preferred_size(190, 0);
    form_editing_pane_container->set_layout(make<GUI::VerticalBoxLayout>());

    auto add_properties_pane = [&](auto& text, auto pane_widget) {
        auto wrapper = form_editing_pane_container->add<GUI::Widget>();
        wrapper->set_layout(make<GUI::VerticalBoxLayout>());
        auto label = wrapper->add<GUI::Label>(text);
        label->set_fill_with_background_color(true);
        label->set_text_alignment(Gfx::TextAlignment::CenterLeft);
        label->set_font(Gfx::Font::default_bold_font());
        label->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        label->set_preferred_size(0, 16);
        wrapper->add_child(pane_widget);
    };

    auto form_widget_tree_view = GUI::TreeView::construct();
    form_widget_tree_view->set_model(g_form_editor_widget->model());
    form_widget_tree_view->on_selection_change = [&] {
        g_form_editor_widget->selection().disable_hooks();
        g_form_editor_widget->selection().clear();
        form_widget_tree_view->selection().for_each_index([&](auto& index) {
            // NOTE: Make sure we don't add the FormWidget itself to the selection,
            //       since that would allow you to drag-move the FormWidget.
            if (index.internal_data() != &g_form_editor_widget->form_widget())
                g_form_editor_widget->selection().add(*(GUI::Widget*)index.internal_data());
        });
        g_form_editor_widget->update();
        g_form_editor_widget->selection().enable_hooks();
    };

    g_form_editor_widget->selection().on_add = [&](auto& widget) {
        form_widget_tree_view->selection().add(g_form_editor_widget->model().index_for_widget(widget));
    };
    g_form_editor_widget->selection().on_remove = [&](auto& widget) {
        form_widget_tree_view->selection().remove(g_form_editor_widget->model().index_for_widget(widget));
    };
    g_form_editor_widget->selection().on_clear = [&] {
        form_widget_tree_view->selection().clear();
    };

    add_properties_pane("Form widget tree:", form_widget_tree_view);
    add_properties_pane("Widget properties:", GUI::TableView::construct());

    g_text_inner_splitter = g_right_hand_stack->add<GUI::VerticalSplitter>();
    g_text_inner_splitter->layout()->set_margins({ 0, 3, 0, 0 });
    add_new_editor(*g_text_inner_splitter);

    auto switch_to_next_editor = GUI::Action::create("Switch to next editor", { Mod_Ctrl, Key_E }, [&](auto&) {
        if (g_all_editor_wrappers.size() <= 1)
            return;
        Vector<EditorWrapper*> wrappers;
        g_text_inner_splitter->for_each_child_of_type<EditorWrapper>([&](auto& child) {
            wrappers.append(&child);
            return IterationDecision::Continue;
        });
        for (int i = 0; i < wrappers.size(); ++i) {
            if (g_current_editor_wrapper.ptr() == wrappers[i]) {
                if (i == wrappers.size() - 1)
                    wrappers[0]->editor().set_focus(true);
                else
                    wrappers[i + 1]->editor().set_focus(true);
            }
        }
    });

    auto switch_to_previous_editor = GUI::Action::create("Switch to previous editor", { Mod_Ctrl | Mod_Shift, Key_E }, [&](auto&) {
        if (g_all_editor_wrappers.size() <= 1)
            return;
        Vector<EditorWrapper*> wrappers;
        g_text_inner_splitter->for_each_child_of_type<EditorWrapper>([&](auto& child) {
            wrappers.append(&child);
            return IterationDecision::Continue;
        });
        for (int i = wrappers.size() - 1; i >= 0; --i) {
            if (g_current_editor_wrapper.ptr() == wrappers[i]) {
                if (i == 0)
                    wrappers.last()->editor().set_focus(true);
                else
                    wrappers[i - 1]->editor().set_focus(true);
            }
        }
    });

    auto remove_current_editor_action = GUI::Action::create("Remove current editor", { Mod_Alt | Mod_Shift, Key_E }, [&](auto&) {
        if (g_all_editor_wrappers.size() <= 1)
            return;
        auto wrapper = g_current_editor_wrapper;
        switch_to_next_editor->activate();
        g_text_inner_splitter->remove_child(*wrapper);
        g_all_editor_wrappers.remove_first_matching([&](auto& entry) { return entry == wrapper.ptr(); });
        update_actions();
    });

    auto save_action = GUI::Action::create("Save", { Mod_Ctrl, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"), [&](auto&) {
        if (g_currently_open_file.is_empty())
            return;
        current_editor().write_to_file(g_currently_open_file);
    });

    toolbar->add_action(new_action);
    toolbar->add_action(add_existing_file_action);
    toolbar->add_action(save_action);
    toolbar->add_action(delete_action);
    toolbar->add_separator();

    toolbar->add_action(GUI::CommonActions::make_cut_action([&](auto&) { current_editor().cut_action().activate(); }));
    toolbar->add_action(GUI::CommonActions::make_copy_action([&](auto&) { current_editor().copy_action().activate(); }));
    toolbar->add_action(GUI::CommonActions::make_paste_action([&](auto&) { current_editor().paste_action().activate(); }));
    toolbar->add_separator();
    toolbar->add_action(GUI::CommonActions::make_undo_action([&](auto&) { current_editor().undo_action().activate(); }));
    toolbar->add_action(GUI::CommonActions::make_redo_action([&](auto&) { current_editor().redo_action().activate(); }));
    toolbar->add_separator();

    g_project_tree_view->on_activation = [&](auto& index) {
        auto filename = g_project_tree_view->model()->data(index, GUI::Model::Role::Custom).to_string();
        open_file(filename);
    };

    s_action_tab_widget = g_text_inner_splitter->add<GUI::TabWidget>();

    s_action_tab_widget->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    s_action_tab_widget->set_preferred_size(0, 24);

    auto reveal_action_tab = [&](auto& widget) {
        if (s_action_tab_widget->preferred_size().height() < 200)
            s_action_tab_widget->set_preferred_size(0, 200);
        s_action_tab_widget->set_active_widget(widget);
    };

    auto hide_action_tabs = [&] {
        s_action_tab_widget->set_preferred_size(0, 24);
    };

    auto hide_action_tabs_action = GUI::Action::create("Hide action tabs", { Mod_Ctrl | Mod_Shift, Key_X }, [&](auto&) {
        hide_action_tabs();
    });

    auto add_editor_action = GUI::Action::create("Add new editor", { Mod_Ctrl | Mod_Alt, Key_E }, [&](auto&) {
        add_new_editor(*g_text_inner_splitter);
        update_actions();
    });

    auto find_in_files_widget = FindInFilesWidget::construct();
    s_action_tab_widget->add_widget("Find in files", find_in_files_widget);

    auto terminal_wrapper = TerminalWrapper::construct();
    s_action_tab_widget->add_widget("Console", terminal_wrapper);

    auto locator = widget->add<Locator>();

    auto open_locator_action = GUI::Action::create("Open Locator...", { Mod_Ctrl, Key_K }, [&](auto&) {
        locator->open();
    });

    auto menubar = make<GUI::MenuBar>();
    auto app_menu = GUI::Menu::construct("HackStudio");
    app_menu->add_action(save_action);
    app_menu->add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    auto project_menu = GUI::Menu::construct("Project");
    project_menu->add_action(new_action);
    project_menu->add_action(add_existing_file_action);
    menubar->add_menu(move(project_menu));

    auto edit_menu = GUI::Menu::construct("Edit");
    edit_menu->add_action(GUI::Action::create("Find in files...", { Mod_Ctrl | Mod_Shift, Key_F }, [&](auto&) {
        reveal_action_tab(find_in_files_widget);
        find_in_files_widget->focus_textbox_and_select_all();
    }));
    menubar->add_menu(move(edit_menu));

    auto stop_action = GUI::Action::create("Stop", Gfx::Bitmap::load_from_file("/res/icons/16x16/stop.png"), [&](auto&) {
        terminal_wrapper->kill_running_command();
    });

    stop_action->set_enabled(false);
    terminal_wrapper->on_command_exit = [&] {
        stop_action->set_enabled(false);
    };

    auto build_action = GUI::Action::create("Build", { Mod_Ctrl, Key_B }, Gfx::Bitmap::load_from_file("/res/icons/16x16/build.png"), [&](auto&) {
        reveal_action_tab(terminal_wrapper);
        build(terminal_wrapper);
        stop_action->set_enabled(true);
    });
    toolbar->add_action(build_action);

    auto run_action = GUI::Action::create("Run", { Mod_Ctrl, Key_R }, Gfx::Bitmap::load_from_file("/res/icons/16x16/play.png"), [&](auto&) {
        reveal_action_tab(terminal_wrapper);
        run(terminal_wrapper);
        stop_action->set_enabled(true);
    });
    toolbar->add_action(run_action);
    toolbar->add_action(stop_action);

    auto build_menu = GUI::Menu::construct("Build");
    build_menu->add_action(build_action);
    build_menu->add_action(run_action);
    build_menu->add_action(stop_action);
    menubar->add_menu(move(build_menu));

    auto view_menu = GUI::Menu::construct("View");
    view_menu->add_action(hide_action_tabs_action);
    view_menu->add_action(open_locator_action);
    view_menu->add_separator();
    view_menu->add_action(add_editor_action);
    view_menu->add_action(remove_current_editor_action);
    menubar->add_menu(move(view_menu));

    auto small_icon = Gfx::Bitmap::load_from_file("/res/icons/16x16/app-hack-studio.png");

    auto help_menu = GUI::Menu::construct("Help");
    help_menu->add_action(GUI::Action::create("About", [&](auto&) {
        GUI::AboutDialog::show("HackStudio", small_icon, g_window);
    }));
    menubar->add_menu(move(help_menu));

    app.set_menubar(move(menubar));

    g_window->set_icon(small_icon);

    g_window->show();

    update_actions = [&]() {
        remove_current_editor_action->set_enabled(g_all_editor_wrappers.size() > 1);
    };

    open_file("main.cpp");

    update_actions();
    return app.exec();
}

void build(TerminalWrapper& wrapper)
{
    wrapper.run_command("make");
}

void run(TerminalWrapper& wrapper)
{
    wrapper.run_command("make run");
}

void open_file(const String& filename)
{
    auto file = g_project->get_file(filename);
    current_editor().set_document(const_cast<GUI::TextDocument&>(file->document()));

    if (filename.ends_with(".cpp") || filename.ends_with(".h"))
        current_editor().set_syntax_highlighter(make<GUI::CppSyntaxHighlighter>());

    if (filename.ends_with(".frm")) {
        set_edit_mode(EditMode::Form);
    } else {
        set_edit_mode(EditMode::Text);
    }

    g_currently_open_file = filename;
    g_window->set_title(String::format("%s - HackStudio", g_currently_open_file.characters()));
    g_project_tree_view->update();

    current_editor_wrapper().filename_label().set_text(filename);

    current_editor().set_focus(true);
}

bool make_is_available()
{
    auto pid = fork();
    if (pid < 0)
        return false;

    if (!pid) {
        int rc = execlp("make", "make", "--version", nullptr);
        ASSERT(rc < 0);
        perror("execl");
        exit(127);
    }

    int wstatus;
    waitpid(pid, &wstatus, 0);
    return WEXITSTATUS(wstatus) == 0;
}
