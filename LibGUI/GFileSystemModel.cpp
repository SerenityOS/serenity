#include <LibGUI/GFileSystemModel.h>
#include <AK/FileSystemPath.h>
#include <AK/StringBuilder.h>
#include <sys/stat.h>
#include <dirent.h>
#include <unistd.h>
#include <stdio.h>

struct GFileSystemModel::Node {
    String name;
    Node* parent { nullptr };
    Vector<Node*> children;
    enum Type { Unknown, Directory, File };
    Type type { Unknown };

    bool has_traversed { false };

    GModelIndex index(const GFileSystemModel& model) const
    {
        if (!parent)
            return model.create_index(0, 0, (void*)this);
        for (int row = 0; row < parent->children.size(); ++row) {
            if (parent->children[row] == this)
                return model.create_index(row, 0, (void*)this);
        }
        ASSERT_NOT_REACHED();
    }

    void traverse_if_needed(const GFileSystemModel& model)
    {
        if (type != Node::Directory || has_traversed)
            return;
        has_traversed = true;

        auto full_path = this->full_path(model);
        DIR* dirp = opendir(full_path.characters());
        if (!dirp)
            return;

        while (auto* de = readdir(dirp)) {
            if (!strcmp(de->d_name, ".") || !strcmp(de->d_name, ".."))
                continue;
            struct stat st;
            int rc = lstat(String::format("%s/%s", full_path.characters(), de->d_name).characters(), &st);
            if (rc < 0) {
                perror("lstat");
                continue;
            }
            if (model.m_mode == DirectoriesOnly && !S_ISDIR(st.st_mode))
                continue;
            auto* child = new Node;
            child->name = de->d_name;
            child->type = S_ISDIR(st.st_mode) ? Node::Type::Directory : Node::Type::File;
            child->parent = this;
            children.append(child);
        }

        closedir(dirp);
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
        Vector<String> lineage;
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
        return FileSystemPath(builder.to_string()).string();
    }
};

GFileSystemModel::GFileSystemModel(const String& root_path, Mode mode)
    : m_root_path(FileSystemPath(root_path).string())
    , m_mode(mode)
{
    update();
}

GFileSystemModel::~GFileSystemModel()
{
}

void GFileSystemModel::update()
{
    // FIXME: Support refreshing the model!
    if (m_root)
        return;

    m_root = new Node;
    m_root->name = m_root_path;
    m_root->reify_if_needed(*this);
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
        return { };
    auto& node = *(const Node*)index.internal_data();
    if (!node.parent) {
        ASSERT(&node == m_root);
        return { };
    }
    return node.parent->index(*this);
}

GVariant GFileSystemModel::data(const GModelIndex& index, Role role) const
{
    if (!index.is_valid())
        return { };
    auto& node = *(const Node*)index.internal_data();
    if (role == GModel::Role::Display)
        return node.name;
    if (role == GModel::Role::Icon) {
        if (node.type == Node::Directory)
            return GIcon::default_icon("filetype-folder");
        return GIcon::default_icon("filetype-unknown");
    }
    return { };
}

void GFileSystemModel::activate(const GModelIndex&)
{
}

int GFileSystemModel::column_count(const GModelIndex&) const
{
    return 1;
}
