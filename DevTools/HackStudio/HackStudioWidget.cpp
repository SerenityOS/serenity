/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2020, the SerenityOS developers
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

#include "HackStudioWidget.h"
#include "CursorTool.h"
#include "Debugger/DebugInfoWidget.h"
#include "Debugger/Debugger.h"
#include "Debugger/DisassemblyWidget.h"
#include "Editor.h"
#include "EditorWrapper.h"
#include "FindInFilesWidget.h"
#include "FormEditorWidget.h"
#include "FormWidget.h"
#include "Git/DiffViewer.h"
#include "Git/GitWidget.h"
#include "HackStudio.h"
#include "HackStudioWidget.h"
#include "Locator.h"
#include "Project.h"
#include "TerminalWrapper.h"
#include "WidgetTool.h"
#include "WidgetTreeModel.h"
#include <AK/StringBuilder.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/Event.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibDebug/DebugSession.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Action.h>
#include <LibGUI/ActionGroup.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Button.h>
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
#include <LibGUI/ToolBarContainer.h>
#include <LibGUI/TreeView.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibThread/Lock.h>
#include <LibThread/Thread.h>
#include <LibVT/TerminalWidget.h>
#include <fcntl.h>
#include <spawn.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>

namespace HackStudio {

HackStudioWidget::HackStudioWidget(const String& path_to_project)
{
    set_fill_with_background_color(true);
    set_layout<GUI::VerticalBoxLayout>();
    layout()->set_spacing(2);

    open_project(path_to_project);

    auto& toolbar_container = add<GUI::ToolBarContainer>();

    auto& outer_splitter = add<GUI::HorizontalSplitter>();
    create_project_tree_view(outer_splitter);
    m_project_tree_view_context_menu = create_project_tree_view_context_menu();

    m_right_hand_splitter = outer_splitter.add<GUI::VerticalSplitter>();
    m_right_hand_stack = m_right_hand_splitter->add<GUI::StackWidget>();
    create_form_editor(*m_right_hand_stack);

    m_diff_viewer = m_right_hand_stack->add<DiffViewer>();

    m_editors_splitter = m_right_hand_stack->add<GUI::VerticalSplitter>();
    m_editors_splitter->layout()->set_margins({ 0, 3, 0, 0 });
    add_new_editor(*m_editors_splitter);

    m_switch_to_next_editor = create_switch_to_next_editor_action();
    m_switch_to_previous_editor = create_switch_to_previous_editor_action();

    m_remove_current_editor_action = create_remove_current_editor_action();
    m_open_action = create_open_action();
    m_save_action = create_save_action();

    create_action_tab(*m_right_hand_splitter);

    m_add_editor_action = create_add_editor_action();
    m_add_terminal_action = create_add_terminal_action();
    m_remove_current_terminal_action = create_remove_current_terminal_action();

    m_locator = add<Locator>();

    m_terminal_wrapper->on_command_exit = [this] {
        m_stop_action->set_enabled(false);
    };

    m_build_action = create_build_action();
    m_run_action = create_run_action();
    m_stop_action = create_stop_action();
    m_debug_action = create_debug_action();

    initialize_debugger();

    create_toolbar(toolbar_container);
}

void HackStudioWidget::update_actions()
{
    auto is_remove_terminal_enabled = [this]() {
        auto widget = m_action_tab_widget->active_widget();
        if (!widget)
            return false;
        if (StringView { "TerminalWrapper" } != widget->class_name())
            return false;
        if (!reinterpret_cast<TerminalWrapper*>(widget)->user_spawned())
            return false;
        return true;
    };

    m_remove_current_editor_action->set_enabled(m_all_editor_wrappers.size() > 1);
    m_remove_current_terminal_action->set_enabled(is_remove_terminal_enabled());
}

void HackStudioWidget::on_action_tab_change()
{
    update_actions();
    auto git_widget = m_action_tab_widget->active_widget();
    if (!git_widget)
        return;
    if (StringView { "GitWidget" } != git_widget->class_name())
        return;
    reinterpret_cast<GitWidget*>(git_widget)->refresh();
}

void HackStudioWidget::open_project(String filename)
{
    LexicalPath lexical_path(filename);
    if (chdir(lexical_path.dirname().characters()) < 0) {
        perror("chdir");
        exit(1);
    }
    m_project = Project::load_from_file(filename);
    ASSERT(m_project);
    if (m_project_tree_view) {
        m_project_tree_view->set_model(m_project->model());
        m_project_tree_view->toggle_index(m_project_tree_view->model()->index(0, 0));
        m_project_tree_view->update();
    }
    if (Debugger::is_initialized()) {
        Debugger::the().reset_breakpoints();
    }
}

Vector<String> HackStudioWidget::selected_file_names() const
{
    Vector<String> files;
    m_project_tree_view->selection().for_each_index([&](const GUI::ModelIndex& index) {
        files.append(index.data().as_string());
    });
    return files;
}

void HackStudioWidget::open_file(const String& filename)
{
    auto project_file = m_project->get_file(filename);
    if (project_file) {
        current_editor().set_document(const_cast<GUI::TextDocument&>(project_file->document()));
        current_editor().set_mode(GUI::TextEditor::Editable);
    } else {
        auto external_file = ProjectFile::construct_with_name(filename);
        current_editor().set_document(const_cast<GUI::TextDocument&>(external_file->document()));
        current_editor().set_mode(GUI::TextEditor::ReadOnly);
    }

    if (filename.ends_with(".frm")) {
        set_edit_mode(EditMode::Form);
    } else {
        set_edit_mode(EditMode::Text);
    }

    m_currently_open_file = filename;
    window()->set_title(String::formatted("{} - HackStudio", m_currently_open_file));
    m_project_tree_view->update();

    current_editor_wrapper().filename_label().set_text(filename);

    current_editor().set_focus(true);
}

EditorWrapper& HackStudioWidget::current_editor_wrapper()
{
    ASSERT(m_current_editor_wrapper);
    return *m_current_editor_wrapper;
}

GUI::TextEditor& HackStudioWidget::current_editor()
{
    return current_editor_wrapper().editor();
}

void HackStudioWidget::set_edit_mode(EditMode mode)
{
    if (mode == EditMode::Text) {
        m_right_hand_stack->set_active_widget(m_editors_splitter);
    } else if (mode == EditMode::Form) {
        m_right_hand_stack->set_active_widget(m_form_inner_container);
    } else if (mode == EditMode::Diff) {
        m_right_hand_stack->set_active_widget(m_diff_viewer);
    } else {
        ASSERT_NOT_REACHED();
    }
    m_right_hand_stack->active_widget()->update();
}

NonnullRefPtr<GUI::Menu> HackStudioWidget::create_project_tree_view_context_menu()
{
    m_open_selected_action = create_open_selected_action();
    m_new_action = create_new_action();
    m_add_existing_file_action = create_add_existing_file_action();
    m_delete_action = create_delete_action();
    auto project_tree_view_context_menu = GUI::Menu::construct("Project Files");
    project_tree_view_context_menu->add_action(*m_open_selected_action);
    // TODO: Rename, cut, copy, duplicate with new name, show containing folder ...
    project_tree_view_context_menu->add_separator();
    project_tree_view_context_menu->add_action(*m_new_action);
    project_tree_view_context_menu->add_action(*m_add_existing_file_action);
    project_tree_view_context_menu->add_action(*m_delete_action);
    return project_tree_view_context_menu;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_new_action()
{
    return GUI::Action::create("Add new file to project...", { Mod_Ctrl, Key_N }, Gfx::Bitmap::load_from_file("/res/icons/16x16/new.png"), [this](const GUI::Action&) {
        String filename;
        if (GUI::InputBox::show(filename, window(), "Enter name of new file:", "Add new file to project") != GUI::InputBox::ExecOK)
            return;
        auto file = Core::File::construct(filename);
        if (!file->open((Core::IODevice::OpenMode)(Core::IODevice::WriteOnly | Core::IODevice::MustBeNew))) {
            GUI::MessageBox::show(window(), String::formatted("Failed to create '{}'", filename), "Error", GUI::MessageBox::Type::Error);
            return;
        }
        if (!m_project->add_file(filename)) {
            GUI::MessageBox::show(window(), String::formatted("Failed to add '{}' to project", filename), "Error", GUI::MessageBox::Type::Error);
            // FIXME: Should we unlink the file here maybe?
            return;
        }
        m_project_tree_view->toggle_index(m_project_tree_view->model()->index(0, 0));
        open_file(filename);
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_open_selected_action()
{

    auto open_selected_action = GUI::Action::create("Open", [this](const GUI::Action&) {
        auto files = selected_file_names();
        for (auto& file : files)
            open_file(file);
    });
    open_selected_action->set_enabled(true);
    return open_selected_action;
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_add_existing_file_action()
{
    return GUI::Action::create("Add existing file to project...", Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"), [this](auto&) {
        auto result = GUI::FilePicker::get_open_filepath(window(), "Add existing file to project");
        if (!result.has_value())
            return;
        auto& filename = result.value();
        if (!m_project->add_file(filename)) {
            GUI::MessageBox::show(window(), String::formatted("Failed to add '{}' to project", filename), "Error", GUI::MessageBox::Type::Error);
            return;
        }
        m_project_tree_view->toggle_index(m_project_tree_view->model()->index(0, 0));
        open_file(filename);
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_delete_action()
{

    auto delete_action = GUI::CommonActions::make_delete_action([this](const GUI::Action&) {
        auto files = selected_file_names();
        if (files.is_empty())
            return;

        String message;
        if (files.size() == 1) {
            message = String::formatted("Really remove {} from the project?", LexicalPath(files[0]).basename());
        } else {
            message = String::formatted("Really remove {} files from the project?", files.size());
        }

        auto result = GUI::MessageBox::show(window(),
            message,
            "Confirm deletion",
            GUI::MessageBox::Type::Warning,
            GUI::MessageBox::InputType::OKCancel);
        if (result == GUI::MessageBox::ExecCancel)
            return;

        for (auto& file : files) {
            if (!m_project->remove_file(file)) {
                GUI::MessageBox::show(window(),
                    String::formatted("Removing file {} from the project failed.", file),
                    "Removal failed",
                    GUI::MessageBox::Type::Error);
                break;
            }
        }
    });
    delete_action->set_enabled(false);
    return delete_action;
}

void HackStudioWidget::add_new_editor(GUI::Widget& parent)
{
    auto wrapper = EditorWrapper::construct();
    if (m_action_tab_widget) {
        parent.insert_child_before(wrapper, *m_action_tab_widget);
    } else {
        parent.add_child(wrapper);
    }
    m_current_editor_wrapper = wrapper;
    m_all_editor_wrappers.append(wrapper);
    wrapper->editor().set_focus(true);
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_switch_to_next_editor_action()
{
    return GUI::Action::create("Switch to next editor", { Mod_Ctrl, Key_E }, [this](auto&) {
        if (m_all_editor_wrappers.size() <= 1)
            return;
        Vector<EditorWrapper*> wrappers;
        m_editors_splitter->for_each_child_of_type<EditorWrapper>([this, &wrappers](auto& child) {
            wrappers.append(&child);
            return IterationDecision::Continue;
        });
        for (size_t i = 0; i < wrappers.size(); ++i) {
            if (m_current_editor_wrapper.ptr() == wrappers[i]) {
                if (i == wrappers.size() - 1)
                    wrappers[0]->editor().set_focus(true);
                else
                    wrappers[i + 1]->editor().set_focus(true);
            }
        }
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_switch_to_previous_editor_action()
{
    return GUI::Action::create("Switch to previous editor", { Mod_Ctrl | Mod_Shift, Key_E }, [this](auto&) {
        if (m_all_editor_wrappers.size() <= 1)
            return;
        Vector<EditorWrapper*> wrappers;
        m_editors_splitter->for_each_child_of_type<EditorWrapper>([this, &wrappers](auto& child) {
            wrappers.append(&child);
            return IterationDecision::Continue;
        });
        for (int i = wrappers.size() - 1; i >= 0; --i) {
            if (m_current_editor_wrapper.ptr() == wrappers[i]) {
                if (i == 0)
                    wrappers.last()->editor().set_focus(true);
                else
                    wrappers[i - 1]->editor().set_focus(true);
            }
        }
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_remove_current_editor_action()
{
    return GUI::Action::create("Remove current editor", { Mod_Alt | Mod_Shift, Key_E }, [this](auto&) {
        if (m_all_editor_wrappers.size() <= 1)
            return;
        auto wrapper = m_current_editor_wrapper;
        m_switch_to_next_editor->activate();
        m_editors_splitter->remove_child(*wrapper);
        m_all_editor_wrappers.remove_first_matching([&wrapper](auto& entry) { return entry == wrapper.ptr(); });
        update_actions();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_open_action()
{
    return GUI::Action::create("Open project...", { Mod_Ctrl | Mod_Shift, Key_O }, Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"), [this](auto&) {
        auto open_path = GUI::FilePicker::get_open_filepath(window(), "Open project");
        if (!open_path.has_value())
            return;
        open_project(open_path.value());
        open_file(m_project->default_file());
        update_actions();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_save_action()
{
    return GUI::Action::create("Save", { Mod_Ctrl, Key_S }, Gfx::Bitmap::load_from_file("/res/icons/16x16/save.png"), [this](auto&) {
        if (m_currently_open_file.is_empty())
            return;

        current_editor().write_to_file(m_currently_open_file);

        if (m_git_widget->initialized())
            m_git_widget->refresh();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_remove_current_terminal_action()
{
    return GUI::Action::create("Remove current Terminal", { Mod_Alt | Mod_Shift, Key_T }, [this](auto&) {
        auto widget = m_action_tab_widget->active_widget();
        if (!widget)
            return;
        if (strcmp(widget->class_name(), "TerminalWrapper") != 0)
            return;
        auto terminal = reinterpret_cast<TerminalWrapper*>(widget);
        if (!terminal->user_spawned())
            return;

        m_action_tab_widget->remove_tab(*terminal);
        update_actions();
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_add_editor_action()
{
    return GUI::Action::create("Add new editor", { Mod_Ctrl | Mod_Alt, Key_E },
        Gfx::Bitmap::load_from_file("/res/icons/16x16/app-text-editor.png"),
        [this](auto&) {
            add_new_editor(*m_editors_splitter);
            update_actions();
        });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_add_terminal_action()
{
    return GUI::Action::create("Add new Terminal", { Mod_Ctrl | Mod_Alt, Key_T },
        Gfx::Bitmap::load_from_file("/res/icons/16x16/app-terminal.png"),
        [this](auto&) {
            auto& terminal = m_action_tab_widget->add_tab<TerminalWrapper>("Terminal");
            reveal_action_tab(terminal);
            update_actions();
            terminal.terminal()->set_focus(true);
        });
}

void HackStudioWidget::reveal_action_tab(GUI::Widget& widget)
{
    if (m_action_tab_widget->preferred_size().height() < 200)
        m_action_tab_widget->set_preferred_size(0, 200);
    m_action_tab_widget->set_active_widget(&widget);
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_debug_action()
{
    return GUI::Action::create("Debug", Gfx::Bitmap::load_from_file("/res/icons/16x16/debug-run.png"), [this](auto&) {
        if (m_project->type() != ProjectType::Cpp) {
            GUI::MessageBox::show(window(), "Cannot debug current project type", "Error", GUI::MessageBox::Type::Error);
            return;
        }
        if (!GUI::FilePicker::file_exists(get_project_executable_path())) {
            GUI::MessageBox::show(window(), String::formatted("Could not find file: {}. (did you build the project?)", get_project_executable_path()), "Error", GUI::MessageBox::Type::Error);
            return;
        }
        if (Debugger::the().session()) {
            GUI::MessageBox::show(window(), "Debugger is already running", "Error", GUI::MessageBox::Type::Error);
            return;
        }
        Debugger::the().set_executable_path(get_project_executable_path());
        m_debugger_thread = adopt(*new LibThread::Thread(Debugger::start_static));
        m_debugger_thread->start();
    });
}

void HackStudioWidget::initialize_debugger()
{
    Debugger::initialize(
        [this](const PtraceRegisters& regs) {
            ASSERT(Debugger::the().session());
            const auto& debug_session = *Debugger::the().session();
            auto source_position = debug_session.debug_info().get_source_position(regs.eip);
            if (!source_position.has_value()) {
                dbgln("Could not find source position for address: {:p}", regs.eip);
                return Debugger::HasControlPassedToUser::No;
            }

            Core::EventLoop::main().post_event(
                *window(),
                make<Core::DeferredInvocationEvent>(
                    [this, source_position, &regs](auto&) {
                        m_current_editor_in_execution = get_editor_of_file(source_position.value().file_path);
                        m_current_editor_in_execution->editor().set_execution_position(source_position.value().line_number - 1);
                        m_debug_info_widget->update_state(*Debugger::the().session(), regs);
                        m_debug_info_widget->set_debug_actions_enabled(true);
                        m_disassembly_widget->update_state(*Debugger::the().session(), regs);
                        HackStudioWidget::reveal_action_tab(*m_debug_info_widget);
                    }));
            Core::EventLoop::wake();

            return Debugger::HasControlPassedToUser::Yes;
        },
        [this]() {
            Core::EventLoop::main().post_event(*window(), make<Core::DeferredInvocationEvent>([this](auto&) {
                m_debug_info_widget->set_debug_actions_enabled(false);
                if (m_current_editor_in_execution) {
                    m_current_editor_in_execution->editor().clear_execution_position();
                }
            }));
            Core::EventLoop::wake();
        },
        [this]() {
            Core::EventLoop::main().post_event(*window(), make<Core::DeferredInvocationEvent>([this](auto&) {
                m_debug_info_widget->program_stopped();
                m_disassembly_widget->program_stopped();
                HackStudioWidget::hide_action_tabs();
                GUI::MessageBox::show(window(), "Program Exited", "Debugger", GUI::MessageBox::Type::Information);
            }));
            Core::EventLoop::wake();
        });
}

String HackStudioWidget::get_full_path_of_serenity_source(const String& file)
{
    auto path_parts = LexicalPath(file).parts();
    ASSERT(path_parts[0] == "..");
    path_parts.remove(0);
    StringBuilder relative_path_builder;
    relative_path_builder.join("/", path_parts);
    constexpr char SERENITY_LIBS_PREFIX[] = "/usr/src/serenity";
    LexicalPath serenity_sources_base(SERENITY_LIBS_PREFIX);
    return String::formatted("{}/{}", serenity_sources_base, relative_path_builder.to_string());
}

NonnullRefPtr<EditorWrapper> HackStudioWidget::get_editor_of_file(const String& file_name)
{

    String file_path = file_name;

    // TODO: We can probably do a more specific condition here, something like
    // "if (file.starts_with("../Libraries/") || file.starts_with("../AK/"))"
    if (file_name.starts_with("../")) {
        file_path = get_full_path_of_serenity_source(file_name);
    }

    open_file(file_path);
    return current_editor_wrapper();
}

String HackStudioWidget::get_project_executable_path() const
{
    // e.g /my/project.hsp => /my/project
    // TODO: Perhaps a Makefile rule for getting the value of $(PROGRAM) would be better?
    return m_project->path().substring(0, m_project->path().index_of(".").value());
}

void HackStudioWidget::build(TerminalWrapper& wrapper)
{
    if (m_project->type() == ProjectType::JavaScript && m_currently_open_file.ends_with(".js"))
        wrapper.run_command(String::formatted("js -A {}", m_currently_open_file));
    else
        wrapper.run_command("make");
}

void HackStudioWidget::run(TerminalWrapper& wrapper)
{
    if (m_project->type() == ProjectType::JavaScript && m_currently_open_file.ends_with(".js"))
        wrapper.run_command(String::formatted("js {}", m_currently_open_file));
    else
        wrapper.run_command("make run");
}

void HackStudioWidget::hide_action_tabs()
{
    m_action_tab_widget->set_preferred_size(0, 24);
};

Project& HackStudioWidget::project()
{
    return *m_project;
}

void HackStudioWidget::set_current_editor_wrapper(RefPtr<EditorWrapper> editor_wrapper)
{
    m_current_editor_wrapper = editor_wrapper;
}

void HackStudioWidget::create_project_tree_view(GUI::Widget& parent)
{
    m_project_tree_view = parent.add<GUI::TreeView>();
    m_project_tree_view->set_model(m_project->model());
    m_project_tree_view->set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    m_project_tree_view->set_preferred_size(140, 0);
    m_project_tree_view->toggle_index(m_project_tree_view->model()->index(0, 0));

    m_project_tree_view->on_context_menu_request = [this](const GUI::ModelIndex& index, const GUI::ContextMenuEvent& event) {
        if (index.is_valid()) {
            m_project_tree_view_context_menu->popup(event.screen_position(), m_open_selected_action);
        }
    };

    m_project_tree_view->on_selection_change = [this] {
        m_open_selected_action->set_enabled(!m_project_tree_view->selection().is_empty());
        m_delete_action->set_enabled(!m_project_tree_view->selection().is_empty());
    };

    m_project_tree_view->on_activation = [this](auto& index) {
        auto filename = index.data(GUI::ModelRole::Custom).to_string();
        open_file(filename);
    };
}

void HackStudioWidget::create_form_editor(GUI::Widget& parent)
{
    m_form_inner_container = parent.add<GUI::Widget>();
    m_form_inner_container->set_layout<GUI::HorizontalBoxLayout>();
    auto& form_widgets_toolbar = m_form_inner_container->add<GUI::ToolBar>(Orientation::Vertical, 26);
    form_widgets_toolbar.set_preferred_size(38, 0);

    GUI::ActionGroup tool_actions;
    tool_actions.set_exclusive(true);

    auto cursor_tool_action = GUI::Action::create_checkable("Cursor", Gfx::Bitmap::load_from_file("/res/icons/hackstudio/Cursor.png"), [this](auto&) {
        m_form_editor_widget->set_tool(make<CursorTool>(*m_form_editor_widget));
    });
    cursor_tool_action->set_checked(true);
    tool_actions.add_action(cursor_tool_action);

    form_widgets_toolbar.add_action(cursor_tool_action);

    GUI::WidgetClassRegistration::for_each([&, this](const GUI::WidgetClassRegistration& reg) {
        constexpr size_t gui_namespace_prefix_length = sizeof("GUI::") - 1;
        auto icon_path = String::formatted("/res/icons/hackstudio/G{}.png",
            reg.class_name().substring(gui_namespace_prefix_length, reg.class_name().length() - gui_namespace_prefix_length));
        if (!Core::File::exists(icon_path))
            return;

        auto action = GUI::Action::create_checkable(reg.class_name(), Gfx::Bitmap::load_from_file(icon_path), [&reg, this](auto&) {
            m_form_editor_widget->set_tool(make<WidgetTool>(*m_form_editor_widget, reg));
            auto widget = reg.construct();
            m_form_editor_widget->form_widget().add_child(widget);
            widget->set_relative_rect(30, 30, 30, 30);
            m_form_editor_widget->model().update();
        });
        action->set_checked(false);
        tool_actions.add_action(action);
        form_widgets_toolbar.add_action(move(action));
    });

    auto& form_editor_inner_splitter = m_form_inner_container->add<GUI::HorizontalSplitter>();

    m_form_editor_widget = form_editor_inner_splitter.add<FormEditorWidget>();

    auto& form_editing_pane_container = form_editor_inner_splitter.add<GUI::VerticalSplitter>();
    form_editing_pane_container.set_size_policy(GUI::SizePolicy::Fixed, GUI::SizePolicy::Fill);
    form_editing_pane_container.set_preferred_size(190, 0);
    form_editing_pane_container.set_layout<GUI::VerticalBoxLayout>();

    auto add_properties_pane = [&](auto& text, auto& pane_widget) {
        auto& wrapper = form_editing_pane_container.add<GUI::Widget>();
        wrapper.set_layout<GUI::VerticalBoxLayout>();
        auto& label = wrapper.add<GUI::Label>(text);
        label.set_fill_with_background_color(true);
        label.set_text_alignment(Gfx::TextAlignment::CenterLeft);
        label.set_font(Gfx::Font::default_bold_font());
        label.set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
        label.set_preferred_size(0, 16);
        wrapper.add_child(pane_widget);
    };

    m_form_widget_tree_view = GUI::TreeView::construct();
    m_form_widget_tree_view->set_model(m_form_editor_widget->model());
    m_form_widget_tree_view->on_selection_change = [this] {
        m_form_editor_widget->selection().disable_hooks();
        m_form_editor_widget->selection().clear();
        m_form_widget_tree_view->selection().for_each_index([this](auto& index) {
            // NOTE: Make sure we don't add the FormWidget itself to the selection,
            //       since that would allow you to drag-move the FormWidget.
            if (index.internal_data() != &m_form_editor_widget->form_widget())
                m_form_editor_widget->selection().add(*(GUI::Widget*)index.internal_data());
        });
        m_form_editor_widget->update();
        m_form_editor_widget->selection().enable_hooks();
    };

    m_form_editor_widget->selection().on_add = [this](auto& widget) {
        m_form_widget_tree_view->selection().add(m_form_editor_widget->model().index_for_widget(widget));
    };
    m_form_editor_widget->selection().on_remove = [this](auto& widget) {
        m_form_widget_tree_view->selection().remove(m_form_editor_widget->model().index_for_widget(widget));
    };
    m_form_editor_widget->selection().on_clear = [this] {
        m_form_widget_tree_view->selection().clear();
    };

    add_properties_pane("Form widget tree:", *m_form_widget_tree_view);
    add_properties_pane("Widget properties:", *GUI::TableView::construct());
}

void HackStudioWidget::create_toolbar(GUI::Widget& parent)
{
    auto& toolbar = parent.add<GUI::ToolBar>();
    toolbar.add_action(*m_new_action);
    toolbar.add_action(*m_add_existing_file_action);
    toolbar.add_action(*m_save_action);
    toolbar.add_action(*m_delete_action);
    toolbar.add_separator();

    toolbar.add_action(GUI::CommonActions::make_cut_action([this](auto&) { current_editor().cut_action().activate(); }));
    toolbar.add_action(GUI::CommonActions::make_copy_action([this](auto&) { current_editor().copy_action().activate(); }));
    toolbar.add_action(GUI::CommonActions::make_paste_action([this](auto&) { current_editor().paste_action().activate(); }));
    toolbar.add_separator();
    toolbar.add_action(GUI::CommonActions::make_undo_action([this](auto&) { current_editor().undo_action().activate(); }));
    toolbar.add_action(GUI::CommonActions::make_redo_action([this](auto&) { current_editor().redo_action().activate(); }));
    toolbar.add_separator();

    toolbar.add_action(*m_build_action);
    toolbar.add_separator();

    toolbar.add_action(*m_run_action);
    toolbar.add_action(*m_stop_action);
    toolbar.add_separator();

    toolbar.add_action(*m_debug_action);
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_build_action()
{
    return GUI::Action::create("Build", { Mod_Ctrl, Key_B }, Gfx::Bitmap::load_from_file("/res/icons/16x16/build.png"), [this](auto&) {
        reveal_action_tab(*m_terminal_wrapper);
        build(*m_terminal_wrapper);
        m_stop_action->set_enabled(true);
    });
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_run_action()
{
    return GUI::Action::create("Run", { Mod_Ctrl, Key_R }, Gfx::Bitmap::load_from_file("/res/icons/16x16/program-run.png"), [this](auto&) {
        reveal_action_tab(*m_terminal_wrapper);
        run(*m_terminal_wrapper);
        m_stop_action->set_enabled(true);
    });
}

void HackStudioWidget::create_action_tab(GUI::Widget& parent)
{
    m_action_tab_widget = parent.add<GUI::TabWidget>();

    m_action_tab_widget->set_size_policy(GUI::SizePolicy::Fill, GUI::SizePolicy::Fixed);
    m_action_tab_widget->set_preferred_size(0, 24);
    m_action_tab_widget->on_change = [this](auto&) {
        on_action_tab_change();

        static bool first_time = true;
        if (!first_time)
            m_action_tab_widget->set_preferred_size(0, 200);
        first_time = false;
    };

    m_find_in_files_widget = m_action_tab_widget->add_tab<FindInFilesWidget>("Find in files");
    m_terminal_wrapper = m_action_tab_widget->add_tab<TerminalWrapper>("Build", false);
    m_debug_info_widget = m_action_tab_widget->add_tab<DebugInfoWidget>("Debug");
    m_disassembly_widget = m_action_tab_widget->add_tab<DisassemblyWidget>("Disassembly");
    m_git_widget = m_action_tab_widget->add_tab<GitWidget>("Git", LexicalPath(m_project->root_directory()));
    m_git_widget->set_view_diff_callback([this](const auto& original_content, const auto& diff) {
        m_diff_viewer->set_content(original_content, diff);
        set_edit_mode(EditMode::Diff);
    });
}

void HackStudioWidget::create_app_menubar(GUI::MenuBar& menubar)
{
    auto& app_menu = menubar.add_menu("HackStudio");
    app_menu.add_action(*m_open_action);
    app_menu.add_action(*m_save_action);
    app_menu.add_separator();
    app_menu.add_action(GUI::CommonActions::make_quit_action([this](auto&) {
        GUI::Application::the()->quit();
    }));
}

void HackStudioWidget::create_project_menubar(GUI::MenuBar& menubar)
{
    auto& project_menu = menubar.add_menu("Project");
    project_menu.add_action(*m_new_action);
    project_menu.add_action(*m_add_existing_file_action);
}

void HackStudioWidget::create_edit_menubar(GUI::MenuBar& menubar)
{
    auto& edit_menu = menubar.add_menu("Edit");
    edit_menu.add_action(GUI::Action::create("Find in files...", { Mod_Ctrl | Mod_Shift, Key_F }, Gfx::Bitmap::load_from_file("/res/icons/16x16/find.png"), [this](auto&) {
        reveal_action_tab(*m_find_in_files_widget);
        m_find_in_files_widget->focus_textbox_and_select_all();
    }));
}

void HackStudioWidget::create_build_menubar(GUI::MenuBar& menubar)
{
    auto& build_menu = menubar.add_menu("Build");
    build_menu.add_action(*m_build_action);
    build_menu.add_separator();
    build_menu.add_action(*m_run_action);
    build_menu.add_action(*m_stop_action);
    build_menu.add_separator();
    build_menu.add_action(*m_debug_action);
}

void HackStudioWidget::create_view_menubar(GUI::MenuBar& menubar)
{
    auto hide_action_tabs_action = GUI::Action::create("Hide action tabs", { Mod_Ctrl | Mod_Shift, Key_X }, [this](auto&) {
        hide_action_tabs();
    });
    auto open_locator_action = GUI::Action::create("Open Locator...", { Mod_Ctrl, Key_K }, [this](auto&) {
        m_locator->open();
    });

    auto& view_menu = menubar.add_menu("View");
    view_menu.add_action(hide_action_tabs_action);
    view_menu.add_action(open_locator_action);
    view_menu.add_separator();
    view_menu.add_action(*m_add_editor_action);
    view_menu.add_action(*m_remove_current_editor_action);
    view_menu.add_action(*m_add_terminal_action);
    view_menu.add_action(*m_remove_current_terminal_action);
}

void HackStudioWidget::create_help_menubar(GUI::MenuBar& menubar)
{
    auto& help_menu = menubar.add_menu("Help");
    help_menu.add_action(GUI::Action::create("About", [this](auto&) {
        GUI::AboutDialog::show("HackStudio", Gfx::Bitmap::load_from_file("/res/icons/32x32/app-hack-studio.png"), window());
    }));
}

NonnullRefPtr<GUI::Action> HackStudioWidget::create_stop_action()
{
    auto action = GUI::Action::create("Stop", Gfx::Bitmap::load_from_file("/res/icons/16x16/program-stop.png"), [this](auto&) {
        m_terminal_wrapper->kill_running_command();
    });

    action->set_enabled(false);
    return action;
}

void HackStudioWidget::initialize_menubar(GUI::MenuBar& menubar)
{
    create_app_menubar(menubar);
    create_project_menubar(menubar);
    create_edit_menubar(menubar);
    create_build_menubar(menubar);
    create_view_menubar(menubar);
    create_help_menubar(menubar);
}

HackStudioWidget::~HackStudioWidget()
{
    if (!m_debugger_thread.is_null()) {
        Debugger::the().set_requested_debugger_action(Debugger::DebuggerAction::Exit);
        void* retval;
        dbgln("Waiting for debugger thread to terminate");
        int rc = pthread_join(m_debugger_thread->tid(), &retval);
        if (rc < 0) {
            perror("pthread_join");
            dbgln("error joining debugger thread");
        }
    }
}

}
