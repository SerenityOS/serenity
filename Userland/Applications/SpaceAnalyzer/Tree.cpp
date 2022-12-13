/*
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tree.h"
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Queue.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>

#include <fcntl.h>
#include <sys/stat.h>
#include <unistd.h>

static constexpr size_t FILES_ENCOUNTERED_UPDATE_STEP_SIZE = 25;

long long int TreeNode::update_totals()
{
    long long int result = 0;
    if (m_children) {
        for (auto& child : *m_children) {
            result += child.update_totals();
        }
        m_area = result;
    } else {
        result = m_area;
    }
    return result;
}

void TreeNode::sort_children_by_area() const
{
    if (m_children) {
        Vector<TreeNode>* children = const_cast<Vector<TreeNode>*>(m_children.ptr());
        quick_sort(*children, [](auto& a, auto& b) { return b.m_area < a.m_area; });
    }
}

struct QueueEntry {
    QueueEntry(DeprecatedString path, TreeNode* node)
        : path(move(path))
        , node(node) {};
    DeprecatedString path;
    TreeNode* node { nullptr };
};

static MountInfo* find_mount_for_path(DeprecatedString path, Vector<MountInfo>& mounts)
{
    MountInfo* result = nullptr;
    size_t length = 0;
    for (auto& mount_info : mounts) {
        DeprecatedString& mount_point = mount_info.mount_point;
        if (path.starts_with(mount_point)) {
            if (!result || mount_point.length() > length) {
                result = &mount_info;
                length = mount_point.length();
            }
        }
    }
    return result;
}

HashMap<int, int> TreeNode::populate_filesize_tree(Vector<MountInfo>& mounts, Function<void(size_t)> on_progress)
{
    VERIFY(!m_name.ends_with('/'));

    Queue<QueueEntry> queue;
    queue.enqueue(QueueEntry(m_name, this));
    size_t files_encountered_count = 0;
    HashMap<int, int> error_accumulator;

    StringBuilder builder = StringBuilder();
    builder.append(m_name);
    builder.append('/');
    MountInfo* root_mount_info = find_mount_for_path(builder.to_deprecated_string(), mounts);
    if (!root_mount_info) {
        return error_accumulator;
    }
    while (!queue.is_empty()) {
        QueueEntry queue_entry = queue.dequeue();

        builder.clear();
        builder.append(queue_entry.path);
        builder.append('/');

        MountInfo* mount_info = find_mount_for_path(builder.to_deprecated_string(), mounts);
        if (!mount_info || (mount_info != root_mount_info && mount_info->source != root_mount_info->source)) {
            continue;
        }

        Core::DirIterator dir_iterator(builder.to_deprecated_string(), Core::DirIterator::SkipParentAndBaseDir);
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
                    on_progress(files_encountered_count);

                DeprecatedString& name = child.m_name;
                int name_len = name.length();
                builder.append(name);
                struct stat st;
                int stat_result = fstatat(dir_iterator.fd(), name.characters(), &st, AT_SYMLINK_NOFOLLOW);
                if (stat_result < 0) {
                    int error_sum = error_accumulator.get(errno).value_or(0);
                    error_accumulator.set(errno, error_sum + 1);
                } else {
                    if (S_ISDIR(st.st_mode)) {
                        queue.enqueue(QueueEntry(builder.to_deprecated_string(), &child));
                    } else {
                        child.m_area = st.st_size;
                    }
                }
                builder.trim(name_len);
            }
        }
    }

    update_totals();
    return error_accumulator;
}
