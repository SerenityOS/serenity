/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "TreeMapWidget.h"
#include <AK/LexicalPath.h>
#include <AK/Queue.h>
#include <AK/QuickSort.h>
#include <AK/URL.h>
#include <Applications/SpaceAnalyzer/SpaceAnalyzerGML.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibDesktop/Launcher.h>
#include <LibGUI/Application.h>
#include <LibGUI/BoxLayout.h>
#include <LibGUI/Breadcrumbbar.h>
#include <LibGUI/Clipboard.h>
#include <LibGUI/FileIconProvider.h>
#include <LibGUI/Icon.h>
#include <LibGUI/Label.h>
#include <LibGUI/Menu.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Statusbar.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>

static const char* APP_NAME = "Space Analyzer";
static constexpr size_t FILES_ENCOUNTERED_UPDATE_STEP_SIZE = 25;

struct TreeNode : public SpaceAnalyzer::TreeMapNode {
    TreeNode(String name)
        : m_name(move(name)) {};

    virtual String name() const override { return m_name; }
    virtual i64 area() const override { return m_area; }
    virtual size_t num_children() const override
    {
        if (m_children) {
            return m_children->size();
        }
        return 0;
    }
    virtual const TreeNode& child_at(size_t i) const override { return m_children->at(i); }
    virtual void sort_children_by_area() const override
    {
        if (m_children) {
            Vector<TreeNode>* children = const_cast<Vector<TreeNode>*>(m_children.ptr());
            quick_sort(*children, [](auto& a, auto& b) { return b.m_area < a.m_area; });
        }
    }

    String m_name;
    i64 m_area { 0 };
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
    auto file = Core::File::construct("/proc/df");
    if (!file->open(Core::OpenMode::ReadOnly)) {
        warnln("Failed to open {}: {}", file->name(), file->error_string());
        return;
    }

    auto content = file->read_all();
    auto json = JsonValue::from_string(content).release_value_but_fixme_should_propagate_errors();

    json.as_array().for_each([&output](auto& value) {
        auto& filesystem_object = value.as_object();
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

static NonnullRefPtr<GUI::Window> create_progress_window()
{
    auto window = GUI::Window::construct();

    window->set_title(APP_NAME);
    window->set_resizable(false);
    window->set_closeable(false);
    window->resize(240, 50);
    window->center_on_screen();

    auto& main_widget = window->set_main_widget<GUI::Widget>();
    main_widget.set_fill_with_background_color(true);
    main_widget.set_layout<GUI::VerticalBoxLayout>();

    auto& label = main_widget.add<GUI::Label>("Analyzing storage space...");
    label.set_fixed_height(22);

    auto& progresslabel = main_widget.add<GUI::Label>();
    progresslabel.set_name("progresslabel");
    progresslabel.set_fixed_height(22);

    return window;
}

static void update_progress_label(GUI::Label& progresslabel, size_t files_encountered_count)
{
    auto text = String::formatted("{} files...", files_encountered_count);
    progresslabel.set_text(text);

    Core::EventLoop::current().pump(Core::EventLoop::WaitMode::PollForEvents);
}

struct QueueEntry {
    QueueEntry(String path, TreeNode* node)
        : path(move(path))
        , node(node) {};
    String path;
    TreeNode* node { nullptr };
};

static void populate_filesize_tree(TreeNode& root, Vector<MountInfo>& mounts, HashMap<int, int>& error_accumulator, GUI::Label& progresslabel)
{
    VERIFY(!root.m_name.ends_with("/"));

    Queue<QueueEntry> queue;
    queue.enqueue(QueueEntry(root.m_name, &root));
    size_t files_encountered_count = 0;

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
                files_encountered_count += 1;
                if (!(files_encountered_count % FILES_ENCOUNTERED_UPDATE_STEP_SIZE))
                    update_progress_label(progresslabel, files_encountered_count);

                String& name = child.m_name;
                int name_len = name.length();
                builder.append(name);
                struct stat st;
                int stat_result = fstatat(dir_iterator.fd(), name.characters(), &st, AT_SYMLINK_NOFOLLOW);
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

static void analyze(RefPtr<Tree> tree, SpaceAnalyzer::TreeMapWidget& treemapwidget, GUI::Statusbar& statusbar)
{
    statusbar.set_text("");
    auto progress_window = create_progress_window();
    progress_window->show();

    auto& progresslabel = *progress_window->main_widget()->find_descendant_of_type_named<GUI::Label>("progresslabel");
    update_progress_label(progresslabel, 0);

    // Build an in-memory tree mirroring the filesystem and for each node
    // calculate the sum of the file size for all its descendants.
    TreeNode* root = &tree->m_root;
    Vector<MountInfo> mounts;
    fill_mounts(mounts);
    HashMap<int, int> error_accumulator;
    populate_filesize_tree(*root, mounts, error_accumulator, progresslabel);

    progress_window->close();

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
    int access_result = access(LexicalPath::dirname(absolute_path).characters(), W_OK);
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

    RefPtr<Tree> tree = adopt_ref(*new Tree(""));

    // Configure application window.
    auto app_icon = GUI::Icon::default_icon("app-space-analyzer");
    auto window = GUI::Window::construct();
    window->set_title(APP_NAME);
    window->resize(640, 480);
    window->set_icon(app_icon.bitmap_for_size(16));

    // Load widgets.
    auto& mainwidget = window->set_main_widget<GUI::Widget>();
    mainwidget.load_from_gml(space_analyzer_gml);
    auto& breadcrumbbar = *mainwidget.find_descendant_of_type_named<GUI::Breadcrumbbar>("breadcrumbbar");
    auto& treemapwidget = *mainwidget.find_descendant_of_type_named<SpaceAnalyzer::TreeMapWidget>("tree_map");
    auto& statusbar = *mainwidget.find_descendant_of_type_named<GUI::Statusbar>("statusbar");

    auto& file_menu = window->add_menu("&File");
    file_menu.add_action(GUI::Action::create("&Analyze", [&](auto&) {
        analyze(tree, treemapwidget, statusbar);
    }));
    file_menu.add_separator();
    file_menu.add_action(GUI::CommonActions::make_quit_action([&](auto&) {
        app->quit();
    }));

    auto& help_menu = window->add_menu("&Help");
    help_menu.add_action(GUI::CommonActions::make_about_action(APP_NAME, app_icon, window));

    // Configure the nodes context menu.
    auto open_folder_action = GUI::Action::create("Open Folder", { Mod_Ctrl, Key_O }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        Desktop::Launcher::open(URL::create_with_file_protocol(get_absolute_path_to_selected_node(treemapwidget)));
    });
    auto open_containing_folder_action = GUI::Action::create("Open Containing Folder", { Mod_Ctrl, Key_O }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/open.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
        LexicalPath path { get_absolute_path_to_selected_node(treemapwidget) };
        Desktop::Launcher::open(URL::create_with_file_protocol(path.dirname(), path.basename()));
    });
    auto copy_path_action = GUI::Action::create("Copy Path to Clipboard", { Mod_Ctrl, Key_C }, Gfx::Bitmap::try_load_from_file("/res/icons/16x16/edit-copy.png").release_value_but_fixme_should_propagate_errors(), [&](auto&) {
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
                        static_cast<Error const&>(deletion_result.error())),
                    "Deletion failed",
                    GUI::MessageBox::Type::Error,
                    GUI::MessageBox::InputType::YesNo);
                if (retry_message_result == GUI::MessageBox::ExecYes) {
                    try_again = true;
                }
            } else {
                GUI::MessageBox::show(window,
                    String::formatted("Successfully deleted \"{}\".", selected_node_path),
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
        StringBuilder builder;

        breadcrumbbar.clear_segments();
        for (size_t k = 0; k < treemapwidget.path_size(); k++) {
            if (k == 0) {
                breadcrumbbar.append_segment("/", GUI::FileIconProvider::icon_for_path("/").bitmap_for_size(16), "/", "/");
                continue;
            }

            const SpaceAnalyzer::TreeMapNode* node = treemapwidget.path_node(k);

            builder.append("/");
            builder.append(node->name());

            breadcrumbbar.append_segment(node->name(), GUI::FileIconProvider::icon_for_path(builder.string_view()).bitmap_for_size(16), builder.string_view(), builder.string_view());
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
