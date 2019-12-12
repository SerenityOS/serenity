#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <LibCore/CDirIterator.h>
#include <LibGUI/GFileSystemModel.h>
#include <dirent.h>
#include <stdio.h>
#include <sys/stat.h>
#include <unistd.h>

struct GFileSystemModel::Node {
    String name;
    Node* parent { nullptr };
    Vector<Node*> children;
    enum Type {
        Unknown,
        Directory,
        File
    };
    Type type { Unknown };

    bool has_traversed { false };

    GModelIndex index(const GFileSystemModel& model) const
    {
        if (!parent)
            return model.create_index(0, 0, const_cast<Node*>(this));
        for (int row = 0; row < parent->children.size(); ++row) {
            if (parent->children[row] == this)
                return model.create_index(row, 0, const_cast<Node*>(this));
        }
        ASSERT_NOT_REACHED();
    }

    void cleanup()
    {
        for (auto& child: children) {
            child->cleanup();
            delete child;
        }
        children.clear();
    }

    void traverse_if_needed(const GFileSystemModel& model)
    {
        if (type != Node::Directory || has_traversed)
            return;
        has_traversed = true;

        auto full_path = this->full_path(model);
        CDirIterator di(full_path, CDirIterator::SkipDots);
        if (di.has_error()) {
            fprintf(stderr, "CDirIterator: %s\n", di.error_string());
            return;
        }

        while (di.has_next()) {
            String name = di.next_path();
            struct stat st;
            int rc = lstat(String::format("%s/%s", full_path.characters(), name.characters()).characters(), &st);
            if (rc < 0) {
                perror("lstat");
                continue;
            }
            if (model.m_mode == DirectoriesOnly && !S_ISDIR(st.st_mode))
                continue;
            auto* child = new Node;
            child->name = name;
            child->type = S_ISDIR(st.st_mode) ? Node::Type::Directory : Node::Type::File;
            child->parent = this;
            children.append(child);
        }
    }

    void reify_if_needed(const GFileSystemModel& model)
    {
        traverse_if_needed(model);
        if (type != Node::Type::Unknown)
            return;
        struct stat st;
        auto full_path = this->full_path(model);
        int rc = lstat(full_path.characters(), &st);
        dbgprintf("lstat(%s) = %d\n", full_path.characters(), rc);
        if (rc < 0) {
            perror("lstat");
            return;
        }
        type = S_ISDIR(st.st_mode) ? Node::Type::Directory : Node::Type::File;
    }

    String full_path(const GFileSystemModel& model) const
    {
        Vector<String, 32> lineage;
        for (auto* ancestor = parent; ancestor; ancestor = ancestor->parent) {
            lineage.append(ancestor->name);
        }
        StringBuilder builder;
        builder.append(model.root_path());
        for (int i = lineage.size() - 1; i >= 0; --i) {
            builder.append('/');
            builder.append(lineage[i]);
        }
        builder.append('/');
        builder.append(name);
        return canonicalized_path(builder.to_string());
    }
};

GModelIndex GFileSystemModel::index(const StringView& path) const
{
    FileSystemPath canonical_path(path);
    const Node* node = m_root;
    if (canonical_path.string() == "/")
        return m_root->index(*this);
    for (int i = 0; i < canonical_path.parts().size(); ++i) {
        auto& part = canonical_path.parts()[i];
        bool found = false;
        for (auto& child : node->children) {
            if (child->name == part) {
                child->reify_if_needed(*this);
                node = child;
                found = true;
                if (i == canonical_path.parts().size() - 1)
                    return node->index(*this);
                break;
            }
        }
        if (!found)
            return {};
    }
    return {};
}

String GFileSystemModel::path(const GModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& node = *(Node*)index.internal_data();
    node.reify_if_needed(*this);
    return node.full_path(*this);
}

GFileSystemModel::GFileSystemModel(const StringView& root_path, Mode mode)
    : m_root_path(canonicalized_path(root_path))
    , m_mode(mode)
{
    m_open_folder_icon = GIcon::default_icon("filetype-folder-open");
    m_closed_folder_icon = GIcon::default_icon("filetype-folder");
    m_file_icon = GIcon::default_icon("filetype-unknown");
    update();
}

GFileSystemModel::~GFileSystemModel()
{
}

void GFileSystemModel::update()
{
    cleanup();

    m_root = new Node;
    m_root->name = m_root_path;
    m_root->reify_if_needed(*this);

    did_update();
}

void GFileSystemModel::cleanup()
{
    if (m_root) {
        m_root->cleanup();
        delete m_root;
        m_root = nullptr;
    }
}

int GFileSystemModel::row_count(const GModelIndex& index) const
{
    if (!index.is_valid())
        return 1;
    auto& node = *(Node*)index.internal_data();
    node.reify_if_needed(*this);
    if (node.type == Node::Type::Directory)
        return node.children.size();
    return 0;
}

GModelIndex GFileSystemModel::index(int row, int column, const GModelIndex& parent) const
{
    if (!parent.is_valid())
        return create_index(row, column, m_root);
    auto& node = *(Node*)parent.internal_data();
    return create_index(row, column, node.children[row]);
}

GModelIndex GFileSystemModel::parent_index(const GModelIndex& index) const
{
    if (!index.is_valid())
        return {};
    auto& node = *(const Node*)index.internal_data();
    if (!node.parent) {
        ASSERT(&node == m_root);
        return {};
    }
    return node.parent->index(*this);
}

GVariant GFileSystemModel::data(const GModelIndex& index, Role role) const
{
    if (!index.is_valid())
        return {};
    auto& node = *(const Node*)index.internal_data();
    if (role == GModel::Role::Display)
        return node.name;
    if (role == GModel::Role::Icon) {
        if (node.type == Node::Directory)
            return m_closed_folder_icon;
        return m_file_icon;
    }
    return {};
}

int GFileSystemModel::column_count(const GModelIndex&) const
{
    return 1;
}
