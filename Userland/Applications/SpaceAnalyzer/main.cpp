/*
 * Copyright (c) 2021, the SerenityOS developers.
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
#include "TreeMapWidget.h"
#include <AK/LexicalPath.h>
#include <AK/Queue.h>
#include <AK/QuickSort.h>
#include <AK/RefCounted.h>
#include <AK/URL.h>
#include <Applications/SpaceAnalyzer/SpaceAnalyzerGML.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/AboutDialog.h>
#include <LibGUI/Application.h>
#include <LibGUI/BreadcrumbBar.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Menu.h>
#include <LibGUI/MenuBar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/StatusBar.h>
#include <sys/stat.h>

static const char* APP_NAME = "Space Analyzer";

struct TreeNode : public SpaceAnalyzer::TreeMapNode {
    TreeNode(String name)
        : m_name(move(name)) {};

    virtual String name() const { return m_name; }
    virtual int64_t area() const { return m_area; }
    virtual size_t num_children() const
    {
        if (m_children) {
            return m_children->size();
        }
        return 0;
    }
    virtual const TreeNode& child_at(size_t i) const { return m_children->at(i); }
    virtual void sort_children_by_area() const
    {
        if (m_children) {
            Vector<TreeNode>* children = const_cast<Vector<TreeNode>*>(m_children.ptr());
            quick_sort(*children, [](auto& a, auto& b) { return b.m_area < a.m_area; });
        }
    }

    String m_name;
    int64_t m_area { 0 };
    OwnPtr<Vector<TreeNode>> m_children;
};

struct Tree : public SpaceAnalyzer::TreeMap {
    Tree(String root_name)
        : m_root(move(root_name)) {};
    virtual ~Tree() {};
    TreeNode m_root;
    virtual const SpaceAnalyzer::TreeMapNode& root() const override
    {
        return m_root;
    };
};

struct MountInfo {
    String mount_point;
    String source;
};

static void fill_mounts(Vector<MountInfo>& output)
{
    // Output info about currently mounted filesystems.
    auto df = Core::File::construct("/proc/df");
    if (!df->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Failed to open /proc/df: %s\n", df->error_string());
        return;
    }

    auto content = df->read_all();
    auto json = JsonValue::from_string(content);
    VERIFY(json.has_value());

    json.value().as_array().for_each([&output](auto& value) {
        auto filesystem_object = value.as_object();
        MountInfo mount_info;
        mount_info.mount_point = filesystem_object.get("mount_point").to_string();
        mount_info.source = filesystem_object.get("source").as_string_or("none");
        output.append(mount_info);
    });
}

static MountInfo* find_mount_for_path(String path, Vector<MountInfo>& mounts)
{
    MountInfo* result = nullptr;
    size_t length = 0;
    for (auto& mount_info : mounts) {
        String& mount_point = mount_info.mount_point;
        if (path.starts_with(mount_point)) {
            if (!result || mount_point.length() > length) {
                result = &mount_info;
                length = mount_point.length();
            }
        }
    }
    return result;
}

static long long int update_totals(TreeNode& node)
{
    long long int result = 0;
    if (node.m_children) {
        for (auto& child : *node.m_children) {
            result += update_totals(child);
        }
        node.m_area = result;
    } else {
        result = node.m_area;
    }
    return result;
}

struct QueueEntry {
    QueueEntry(String path, TreeNode* node)
        : path(move(path))
        , node(node) {};
    String path;
    TreeNode* node { nullptr };
};

static void populate_filesize_tree(TreeNode& root, Vector<MountInfo>& mounts, HashMap<int, int>& error_accumulator)
{
    VERIFY(!root.m_name.ends_with("/"));

    Queue<QueueEntry> queue;
    queue.enqueue(QueueEntry(root.m_name, &root));

    StringBuilder builder = StringBuilder();
    builder.append(root.m_name);
    builder.append("/");
    MountInfo* root_mount_info = find_mount_for_path(builder.to_string(), mounts);
    if (!root_mount_info) {
        return;
    }
    while (!queue.is_empty()) {
        QueueEntry queue_entry = queue.dequeue();

        builder.clear();
        builder.append(queue_entry.path);
        builder.append("/");

        MountInfo* mount_info = find_mount_for_path(builder.to_string(), mounts);
        if (!mount_info || (mount_info != root_mount_info && mount_info->source != root_mount_info->source)) {
            continue;
        }

        Core::DirIterator dir_iterator(builder.to_string(), Core::DirIterator::SkipParentAndBaseDir);
        if (dir_iterator.has_error()) {
            int error_sum = error_accumulator.get(dir_iterator.error()).value_or(0);
            error_accumulator.set(dir_iterator.error(), error_sum + 1);
        } else {
            queue_entry.node->m_children = make<Vector<TreeNode>>();
            while (dir_iterator.has_next()) {
                queue_entry.node->m_children->append(TreeNode(dir_iterator.next_path()));
            }
            for (auto& child : *queue_entry.node->m_children) {
                String& name = child.m_name;
                int name_len = name.length();
                builder.append(name);
                struct stat st;
                int stat_result = lstat(builder.to_string().characters(), &st);
                if (stat_result < 0) {
                    int error_sum = error_accumulator.get(errno).value_or(0);
                    error_accumulator.set(errno, error_sum + 1);
                } else {
                    if (S_ISDIR(st.st_mode)) {
                        queue.enqueue(QueueEntry(builder.to_string(), &child));
                    } else {
                        child.m_area = st.st_size;
                    }
                }
                builder.trim(name_len);
            }
        }
    }

    update_totals(root);
}

static void analyze(RefPtr<Tree> tree, SpaceAnalyzer::TreeMapWidget& treemapwidget, GUI::StatusBar& statusbar)
{
    // Build an in-memory tree mirroring the filesystem and for each node
    // calculate the sum of the file size for all its descendants.
    TreeNode* root = &tree->m_root;
    Vector<MountInfo> mounts;
    fill_mounts(mounts);
    HashMap<int, int> error_accumulator;
    populate_filesize_tree(*root, mounts, error_accumulator);

    // Display an error summary in the statusbar.
    if (!error_accumulator.is_empty()) {
        StringBuilder builder;
        bool first = true;
        builder.append("Some directories were not analyzed: ");
        for (auto& key : error_accumulator.keys()) {
            if (!first) {
                builder.append(", ");
            }
            builder.append(strerror(key));
            builder.append(" (");
            int value = error_accumulator.get(key).value();
            builder.append(String::number(value));
            if (value == 1) {
                builder.append(" time");
            } else {
                builder.append(" times");
            }
            builder.append(")");
            first = false;
        }
        statusbar.set_text(builder.to_string());
    } else {
        statusbar.set_text("No errors");
    }
    treemapwidget.set_tree(tree);
}

static bool is_removable(const String& absolute_path)
{
    VERIFY(!absolute_path.is_empty());
    int access_result = access(absolute_path.characters(), W_OK);
    if (access_result != 0 && errno != EACCES)
        perror("access");
    return access_result == 0;
}

static String get_absolute_path_to_selected_node(const SpaceAnalyzer::TreeMapWidget& treemapwidget, bool include_last_node = true)
{
    StringBuilder path_builder;
    for (size_t k = 0; k < treemapwidget.path_size() - (include_last_node ? 0 : 1); k++) {
        if (k != 0) {
            path_builder.append('/');
        }
        const SpaceAnalyzer::TreeMapNode* node = treemapwidget.path_node(k);
        path_builder.append(node->name());
    }
    return path_builder.build();
}

int main(int argc, char* argv[])
{
    auto app = GUI::Application::construct(argc, argv);

    RefPtr<Tree> tree = adopt(*new Tree(""));

    // Configure application window.
    auto app_icon = GUI::Icon::default_icon("app-space-analyzer");
    auto window = GUI::Window::construct();
    window->set_title(APP_NAME);
    window->resize(640, 480);
    window->set_icon(app_icon.bitmap_for_size(16));

    // Load widgets.
    auto& mainwidget = window->set_main_widget<GUI::Widget>();
    mainwidget.load_from_gml(space_analyzer_gml);
    auto& breadcrumbbar = *mainwidget.find_descendant_of_type_named<GUI::BreadcrumbBar>("breadcrumb_bar");
    auto& treemapwidget = *mainwidget.find_descendant_of_type_named<SpaceAnalyzer::TreeMapWidget>("tree_map");
    auto& statusbar = *mainwidget.find_descendant_of_type_named<GUI::StatusBar>("status_bar");

    // Configure the menubar.
    auto menubar = GUI::MenuBar::construct();
    auto& app_menu = menubar->add_menu(APP_NAME);
    app_menu.add_action(GUI::Action::create("Analyze", [&](auto&) {
        analyze(tree, treemapwidget, statusbar);
    }));
    app_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));
    auto& help_menu = menubar->add_menu("Help");
    help_menu.add_action(GUI::CommonActions::make_about_action(APP_NAME, app_icon, window));
    app->set_menubar(move(menubar));

    // Configure the nodes context menu.
    auto open_folder_action = GUI::Action::create("Open Folder", { Mod_Ctrl, Key_O }, Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"), [&](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol(get_absolute_path_to_selected_node(treemapwidget)));
    });
    auto open_containing_folder_action = GUI::Action::create("Open Containing Folder", { Mod_Ctrl, Key_O }, Gfx::Bitmap::load_from_file("/res/icons/16x16/open.png"), [&](auto&) {
        LexicalPath path { get_absolute_path_to_selected_node(treemapwidget) };
        Desktop::Launcher::open(URL::create_with_file_protocol(path.dirname(), path.basename()));
    });
    auto copy_path_action = GUI::Action::create("Copy Path to Clipboard", { Mod_Ctrl, Key_C }, Gfx::Bitmap::load_from_file("/res/icons/16x16/edit-copy.png"), [&](auto&) {
        GUI::Clipboard::the().set_plain_text(get_absolute_path_to_selected_node(treemapwidget));
    });
    auto delete_action = GUI::CommonActions::make_delete_action([&](auto&) {
        String selected_node_path = get_absolute_path_to_selected_node(treemapwidget);
        bool try_again = true;
        while (try_again) {
            try_again = false;

            auto deletion_result = Core::File::remove(selected_node_path, Core::File::RecursionMode::Allowed, true);
            if (deletion_result.is_error()) {
                auto retry_message_result = GUI::MessageBox::show(window,
                    String::formatted("Failed to delete \"{}\": {}. Retry?",
                        deletion_result.error().file,
                        deletion_result.error().error_code.string()),
                    "Deletion failed",
                    GUI::MessageBox::Type::Error,
                    GUI::MessageBox::InputType::YesNo);
                if (retry_message_result == GUI::MessageBox::ExecYes) {
                    try_again = true;
                }
            } else {
                GUI::MessageBox::show(window,
                    String::formatted("Successfuly deleted \"{}\".", selected_node_path),
                    "Deletion completed",
                    GUI::MessageBox::Type::Information,
                    GUI::MessageBox::InputType::OK);
            }
        }

        // TODO: Refreshing data always causes resetting the viewport back to "/".
        // It would be great if we found a way to preserve viewport across refreshes.
        analyze(tree, treemapwidget, statusbar);
    });
    // TODO: Both these menus could've been implemented as one, but it's impossible to change action text after it's shown once.
    auto folder_node_context_menu = GUI::Menu::construct();
    folder_node_context_menu->add_action(*open_folder_action);
    folder_node_context_menu->add_action(*copy_path_action);
    folder_node_context_menu->add_action(*delete_action);
    auto file_node_context_menu = GUI::Menu::construct();
    file_node_context_menu->add_action(*open_containing_folder_action);
    file_node_context_menu->add_action(*copy_path_action);
    file_node_context_menu->add_action(*delete_action);

    // Configure event handlers.
    breadcrumbbar.on_segment_click = [&](size_t index) {
        VERIFY(index < treemapwidget.path_size());
        treemapwidget.set_viewpoint(index);
    };
    treemapwidget.on_path_change = [&]() {
        breadcrumbbar.clear_segments();
        for (size_t k = 0; k < treemapwidget.path_size(); k++) {
            if (k == 0) {
                breadcrumbbar.append_segment("/");
            } else {
                const SpaceAnalyzer::TreeMapNode* node = treemapwidget.path_node(k);
                breadcrumbbar.append_segment(node->name());
            }
        }
        breadcrumbbar.set_selected_segment(treemapwidget.viewpoint());
    };
    treemapwidget.on_context_menu_request = [&](const GUI::ContextMenuEvent& event) {
        String selected_node_path = get_absolute_path_to_selected_node(treemapwidget);
        if (selected_node_path.is_empty())
            return;
        delete_action->set_enabled(is_removable(selected_node_path));
        if (Core::File::is_directory(selected_node_path)) {
            folder_node_context_menu->popup(event.screen_position());
        } else {
            file_node_context_menu->popup(event.screen_position());
        }
    };

    // At startup automatically do an analysis of root.
    analyze(tree, treemapwidget, statusbar);

    window->show();
    return app->exec();
}
