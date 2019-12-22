#include "CppLexer.h"
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
#include <LibCore/CFile.h>
#include <LibGUI/GAboutDialog.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GActionGroup.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GBoxLayout.h>
#include <LibGUI/GButton.h>
#include <LibGUI/GFilePicker.h>
#include <LibGUI/GInputBox.h>
#include <LibGUI/GLabel.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GMenuBar.h>
#include <LibGUI/GMessageBox.h>
#include <LibGUI/GSplitter.h>
#include <LibGUI/GStackWidget.h>
#include <LibGUI/GTabWidget.h>
#include <LibGUI/GTableView.h>
#include <LibGUI/GTextBox.h>
#include <LibGUI/GTextEditor.h>
#include <LibGUI/GToolBar.h>
#include <LibGUI/GTreeView.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <stdio.h>
#include <unistd.h>

NonnullRefPtrVector<EditorWrapper> g_all_editor_wrappers;
RefPtr<EditorWrapper> g_current_editor_wrapper;

String g_currently_open_file;
OwnPtr<Project> g_project;
RefPtr<GWindow> g_window;
RefPtr<GTreeView> g_project_tree_view;
RefPtr<GStackWidget> g_right_hand_stack;
RefPtr<GSplitter> g_text_inner_splitter;
RefPtr<GWidget> g_form_inner_container;
RefPtr<FormEditorWidget> g_form_editor_widget;

static RefPtr<GTabWidget> s_action_tab_widget;

void add_new_editor(GWidget& parent)
{
    auto wrapper = EditorWrapper::construct(nullptr);
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

int main(int argc, char** argv)
{
    GApplication app(argc, argv);

    Function<void()> update_actions;

    g_window = GWindow::construct();
    g_window->set_rect(90, 90, 840, 600);
    g_window->set_title("HackStudio");

    auto widget = GWidget::construct();
    g_window->set_main_widget(widget);

    widget->set_fill_with_background_color(true);
    widget->set_layout(make<GBoxLayout>(Orientation::Vertical));
    widget->layout()->set_spacing(0);

    if (chdir("/home/anon/little") < 0) {
        perror("chdir");
        return 1;
    }
    g_project = Project::load_from_file("little.files");
    ASSERT(g_project);

    auto toolbar = GToolBar::construct(widget);

    auto outer_splitter = GSplitter::construct(Orientation::Horizontal, widget);
    g_project_tree_view = GTreeView::construct(outer_splitter);
    g_project_tree_view->set_model(g_project->model());
    g_project_tree_view->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    g_project_tree_view->set_preferred_size(140, 0);

    g_right_hand_stack = GStackWidget::construct(outer_splitter);

    g_form_inner_container = GWidget::construct(g_right_hand_stack);
    g_form_inner_container->set_layout(make<GBoxLayout>(Orientation::Horizontal));
    auto form_widgets_toolbar = GToolBar::construct(Orientation::Vertical, 26, g_form_inner_container);
    form_widgets_toolbar->set_preferred_size(38, 0);

    GActionGroup tool_actions;
    tool_actions.set_exclusive(true);

    auto cursor_tool_action = GAction::create("Cursor", GraphicsBitmap::load_from_file("/res/icons/widgets/Cursor.png"), [&](auto&) {
        g_form_editor_widget->set_tool(make<CursorTool>(*g_form_editor_widget));
    });
    cursor_tool_action->set_checkable(true);
    cursor_tool_action->set_checked(true);
    tool_actions.add_action(cursor_tool_action);

    form_widgets_toolbar->add_action(cursor_tool_action);

    GWidgetClassRegistration::for_each([&](const GWidgetClassRegistration& reg) {
        auto icon_path = String::format("/res/icons/widgets/%s.png", reg.class_name().characters());
        auto action = GAction::create(reg.class_name(), GraphicsBitmap::load_from_file(icon_path), [&reg](auto&) {
            g_form_editor_widget->set_tool(make<WidgetTool>(*g_form_editor_widget, reg));
            auto widget = reg.construct(&g_form_editor_widget->form_widget());
            widget->set_relative_rect(30, 30, 30, 30);
            g_form_editor_widget->model().update();
        });
        action->set_checkable(true);
        action->set_checked(false);
        tool_actions.add_action(action);
        form_widgets_toolbar->add_action(move(action));
    });

    auto form_editor_inner_splitter = GSplitter::construct(Orientation::Horizontal, g_form_inner_container);

    g_form_editor_widget = FormEditorWidget::construct(form_editor_inner_splitter);

    auto form_editing_pane_container = GSplitter::construct(Orientation::Vertical, form_editor_inner_splitter);
    form_editing_pane_container->set_size_policy(SizePolicy::Fixed, SizePolicy::Fill);
    form_editing_pane_container->set_preferred_size(190, 0);
    form_editing_pane_container->set_layout(make<GBoxLayout>(Orientation::Vertical));

    auto add_properties_pane = [&](auto& text, auto pane_widget) {
        auto wrapper = GWidget::construct(form_editing_pane_container.ptr());
        wrapper->set_layout(make<GBoxLayout>(Orientation::Vertical));
        auto label = GLabel::construct(text, wrapper);
        label->set_fill_with_background_color(true);
        label->set_text_alignment(TextAlignment::CenterLeft);
        label->set_font(Font::default_bold_font());
        label->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
        label->set_preferred_size(0, 16);
        wrapper->add_child(pane_widget);
    };

    auto form_widget_tree_view = GTreeView::construct(nullptr);
    form_widget_tree_view->set_model(g_form_editor_widget->model());
    form_widget_tree_view->on_selection_change = [&] {
        g_form_editor_widget->selection().disable_hooks();
        g_form_editor_widget->selection().clear();
        form_widget_tree_view->selection().for_each_index([&](auto& index) {
            // NOTE: Make sure we don't add the FormWidget itself to the selection,
            //       since that would allow you to drag-move the FormWidget.
            if (index.internal_data() != &g_form_editor_widget->form_widget())
                g_form_editor_widget->selection().add(*(GWidget*)index.internal_data());
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
    add_properties_pane("Widget properties:", GTableView::construct(nullptr));

    g_text_inner_splitter = GSplitter::construct(Orientation::Vertical, g_right_hand_stack);
    g_text_inner_splitter->layout()->set_margins({ 0, 3, 0, 0 });
    add_new_editor(*g_text_inner_splitter);

    auto new_action = GAction::create("Add new file to project...", { Mod_Ctrl, Key_N }, GraphicsBitmap::load_from_file("/res/icons/16x16/new.png"), [&](const GAction&) {
        auto input_box = GInputBox::construct("Enter name of new file:", "Add new file to project", g_window);
        if (input_box->exec() == GInputBox::ExecCancel)
            return;
        auto filename = input_box->text_value();
        auto file = CFile::construct(filename);
        if (!file->open((CIODevice::OpenMode)(CIODevice::WriteOnly | CIODevice::MustBeNew))) {
            GMessageBox::show(String::format("Failed to create '%s'", filename.characters()), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, g_window);
            return;
        }
        if (!g_project->add_file(filename)) {
            GMessageBox::show(String::format("Failed to add '%s' to project", filename.characters()), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, g_window);
            // FIXME: Should we unlink the file here maybe?
            return;
        }
        open_file(filename);
    });

    auto add_existing_file_action = GAction::create("Add existing file to project...", GraphicsBitmap::load_from_file("/res/icons/16x16/open.png"), [&](auto&) {
        auto result = GFilePicker::get_open_filepath("Add existing file to project");
        if (!result.has_value())
            return;
        auto& filename = result.value();
        if (!g_project->add_file(filename)) {
            GMessageBox::show(String::format("Failed to add '%s' to project", filename.characters()), "Error", GMessageBox::Type::Error, GMessageBox::InputType::OK, g_window);
            return;
        }
        open_file(filename);
    });

    auto switch_to_next_editor = GAction::create("Switch to next editor", { Mod_Ctrl, Key_E }, [&](auto&) {
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

    auto switch_to_previous_editor = GAction::create("Switch to previous editor", { Mod_Ctrl | Mod_Shift, Key_E }, [&](auto&) {
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

    auto remove_current_editor_action = GAction::create("Remove current editor", { Mod_Alt | Mod_Shift, Key_E }, [&](auto&) {
        if (g_all_editor_wrappers.size() <= 1)
            return;
        auto wrapper = g_current_editor_wrapper;
        switch_to_next_editor->activate();
        g_text_inner_splitter->remove_child(*wrapper);
        g_all_editor_wrappers.remove_first_matching([&](auto& entry) { return entry == wrapper.ptr(); });
        update_actions();
    });

    auto save_action = GAction::create("Save", { Mod_Ctrl, Key_S }, GraphicsBitmap::load_from_file("/res/icons/16x16/save.png"), [&](auto&) {
        if (g_currently_open_file.is_empty())
            return;
        current_editor().write_to_file(g_currently_open_file);
    });

    toolbar->add_action(new_action);
    toolbar->add_action(add_existing_file_action);
    toolbar->add_action(save_action);
    toolbar->add_separator();

    toolbar->add_action(GCommonActions::make_cut_action([&](auto&) { current_editor().cut_action().activate(); }));
    toolbar->add_action(GCommonActions::make_copy_action([&](auto&) { current_editor().copy_action().activate(); }));
    toolbar->add_action(GCommonActions::make_paste_action([&](auto&) { current_editor().paste_action().activate(); }));
    toolbar->add_separator();
    toolbar->add_action(GCommonActions::make_undo_action([&](auto&) { current_editor().undo_action().activate(); }));
    toolbar->add_action(GCommonActions::make_redo_action([&](auto&) { current_editor().redo_action().activate(); }));
    toolbar->add_separator();

    g_project_tree_view->on_activation = [&](auto& index) {
        auto filename = g_project_tree_view->model()->data(index, GModel::Role::Custom).to_string();
        open_file(filename);
    };

    s_action_tab_widget = GTabWidget::construct(g_text_inner_splitter);

    s_action_tab_widget->set_size_policy(SizePolicy::Fill, SizePolicy::Fixed);
    s_action_tab_widget->set_preferred_size(0, 24);

    auto reveal_action_tab = [&](auto& widget) {
        if (s_action_tab_widget->preferred_size().height() < 200)
            s_action_tab_widget->set_preferred_size(0, 200);
        s_action_tab_widget->set_active_widget(widget);
    };

    auto hide_action_tabs = [&] {
        s_action_tab_widget->set_preferred_size(0, 24);
    };

    auto hide_action_tabs_action = GAction::create("Hide action tabs", { Mod_Ctrl | Mod_Shift, Key_X }, [&](auto&) {
        hide_action_tabs();
    });

    auto add_editor_action = GAction::create("Add new editor", { Mod_Ctrl | Mod_Alt, Key_E }, [&](auto&) {
        add_new_editor(*g_text_inner_splitter);
        update_actions();
    });

    auto find_in_files_widget = FindInFilesWidget::construct(nullptr);
    s_action_tab_widget->add_widget("Find in files", find_in_files_widget);

    auto terminal_wrapper = TerminalWrapper::construct(nullptr);
    s_action_tab_widget->add_widget("Console", terminal_wrapper);

    auto locator = Locator::construct(widget);

    auto open_locator_action = GAction::create("Open Locator...", { Mod_Ctrl, Key_K }, [&](auto&) {
        locator->open();
    });

    auto menubar = make<GMenuBar>();
    auto app_menu = GMenu::construct("HackStudio");
    app_menu->add_action(save_action);
    app_menu->add_action(GCommonActions::make_quit_action([&](auto&) {
        app.quit();
    }));
    menubar->add_menu(move(app_menu));

    auto project_menu = GMenu::construct("Project");
    project_menu->add_action(new_action);
    project_menu->add_action(add_existing_file_action);
    menubar->add_menu(move(project_menu));

    auto edit_menu = GMenu::construct("Edit");
    edit_menu->add_action(GAction::create("Find in files...", { Mod_Ctrl | Mod_Shift, Key_F }, [&](auto&) {
        reveal_action_tab(find_in_files_widget);
        find_in_files_widget->focus_textbox_and_select_all();
    }));
    menubar->add_menu(move(edit_menu));

    auto stop_action = GAction::create("Stop", GraphicsBitmap::load_from_file("/res/icons/16x16/stop.png"), [&](auto&) {
        terminal_wrapper->kill_running_command();
    });

    stop_action->set_enabled(false);
    terminal_wrapper->on_command_exit = [&] {
        stop_action->set_enabled(false);
    };

    auto build_action = GAction::create("Build", { Mod_Ctrl, Key_B }, GraphicsBitmap::load_from_file("/res/icons/16x16/build.png"), [&](auto&) {
        reveal_action_tab(terminal_wrapper);
        build(terminal_wrapper);
        stop_action->set_enabled(true);
    });
    toolbar->add_action(build_action);

    auto run_action = GAction::create("Run", { Mod_Ctrl, Key_R }, GraphicsBitmap::load_from_file("/res/icons/16x16/play.png"), [&](auto&) {
        reveal_action_tab(terminal_wrapper);
        run(terminal_wrapper);
        stop_action->set_enabled(true);
    });
    toolbar->add_action(run_action);
    toolbar->add_action(stop_action);

    auto build_menu = GMenu::construct("Build");
    build_menu->add_action(build_action);
    build_menu->add_action(run_action);
    build_menu->add_action(stop_action);
    menubar->add_menu(move(build_menu));

    auto view_menu = GMenu::construct("View");
    view_menu->add_action(hide_action_tabs_action);
    view_menu->add_action(open_locator_action);
    view_menu->add_separator();
    view_menu->add_action(add_editor_action);
    view_menu->add_action(remove_current_editor_action);
    menubar->add_menu(move(view_menu));

    auto small_icon = GraphicsBitmap::load_from_file("/res/icons/16x16/app-hack-studio.png");

    auto help_menu = GMenu::construct("Help");
    help_menu->add_action(GAction::create("About", [&](auto&) {
        GAboutDialog::show("HackStudio", small_icon, g_window);
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

struct TextStyle {
    Color color;
    const Font* font { nullptr };
};

static TextStyle style_for_token_type(CppToken::Type type)
{
    switch (type) {
    case CppToken::Type::Keyword:
        return { Color::Black, &Font::default_bold_fixed_width_font() };
    case CppToken::Type::KnownType:
        return { Color::from_rgb(0x929200), &Font::default_bold_fixed_width_font() };
    case CppToken::Type::Identifier:
        return { Color::from_rgb(0x000092) };
    case CppToken::Type::DoubleQuotedString:
    case CppToken::Type::SingleQuotedString:
    case CppToken::Type::Number:
        return { Color::from_rgb(0x920000) };
    case CppToken::Type::PreprocessorStatement:
        return { Color::from_rgb(0x009292) };
    case CppToken::Type::Comment:
        return { Color::from_rgb(0x009200) };
    default:
        return { Color::Black };
    }
}

static void rehighlight()
{
    auto text = current_editor().text();
    CppLexer lexer(text);
    auto tokens = lexer.lex();

    Vector<GTextDocumentSpan> spans;
    for (auto& token : tokens) {
#ifdef DEBUG_SYNTAX_HIGHLIGHTING
        dbg() << token.to_string() << " @ " << token.m_start.line << ":" << token.m_start.column << " - " << token.m_end.line << ":" << token.m_end.column;
#endif
        GTextDocumentSpan span;
        span.range.set_start({ token.m_start.line, token.m_start.column });
        span.range.set_end({ token.m_end.line, token.m_end.column });
        auto style = style_for_token_type(token.m_type);
        span.color = style.color;
        span.font = style.font;
        span.is_skippable = token.m_type == CppToken::Type::Whitespace;
        span.data = (void*)token.m_type;
        spans.append(span);
    }
    current_editor().document().set_spans(spans);
    static_cast<Editor&>(current_editor()).notify_did_rehighlight();
    current_editor().update();
}

void open_file(const String& filename)
{
    auto file = g_project->get_file(filename);
    current_editor().set_document(const_cast<GTextDocument&>(file->document()));

    if (filename.ends_with(".cpp") || filename.ends_with(".h")) {
        current_editor().on_change = [] { rehighlight(); };
        rehighlight();
    } else {
        current_editor().on_change = nullptr;
    }

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
