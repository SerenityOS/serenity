/*
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Tree.h"
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Queue.h>
#include <AK/QuickSort.h>
#include <LibCore/Directory.h>

#include <fcntl.h>
#include <sys/stat.h>

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
    QueueEntry(ByteString path, TreeNode* node)
        : path(move(path))
        , node(node) {};
    ByteString path;
    TreeNode* node { nullptr };
};

static MountInfo* find_mount_for_path(ByteString path, Vector<MountInfo>& mounts)
{
    MountInfo* result = nullptr;
    size_t length = 0;
    for (auto& mount_info : mounts) {
        ByteString& mount_point = mount_info.mount_point;
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

    auto log_error = [&](Error& error) {
        auto error_code = error.code();
        int error_sum = error_accumulator.get(error_code).value_or(0);
        error_accumulator.set(error_code, error_sum + 1);
    };

    StringBuilder builder = StringBuilder();
    builder.append(m_name);
    builder.append('/');
    MountInfo* root_mount_info = find_mount_for_path(builder.to_byte_string(), mounts);
    if (!root_mount_info) {
        return error_accumulator;
    }
    while (!queue.is_empty()) {
        QueueEntry queue_entry = queue.dequeue();

        builder.clear();
        builder.append(queue_entry.path);
        builder.append('/');

        MountInfo* mount_info = find_mount_for_path(builder.to_byte_string(), mounts);
        if (!mount_info || (mount_info != root_mount_info && mount_info->source != root_mount_info->source)) {
            continue;
        }

        auto directory_or_error = Core::Directory::create(builder.string_view(), Core::Directory::CreateDirectories::No);
        if (directory_or_error.is_error()) {
            log_error(directory_or_error.error());
        } else {
            auto directory = directory_or_error.release_value();
            queue_entry.node->m_children = make<Vector<TreeNode>>();

            auto iteration_result = Core::Directory::for_each_entry(builder.string_view(), Core::DirIterator::SkipParentAndBaseDir, [&](auto& entry, auto&) -> ErrorOr<IterationDecision> {
                TRY(queue_entry.node->m_children->try_append(TreeNode(entry.name)));
                return IterationDecision::Continue;
            });
            if (iteration_result.is_error())
                log_error(iteration_result.error());

            for (auto& child : *queue_entry.node->m_children) {
                files_encountered_count += 1;
                if (!(files_encountered_count % FILES_ENCOUNTERED_UPDATE_STEP_SIZE))
                    on_progress(files_encountered_count);

                builder.append(child.m_name);
                auto st_or_error = directory.stat(child.m_name, AT_SYMLINK_NOFOLLOW);
                if (st_or_error.is_error()) {
                    log_error(st_or_error.error());
                } else {
                    auto st = st_or_error.release_value();
                    if (S_ISDIR(st.st_mode)) {
                        queue.enqueue(QueueEntry(builder.to_byte_string(), &child));
                    } else {
                        child.m_area = st.st_size;
                    }
                }
                builder.trim(child.m_name.length());
            }
        }
    }

    update_totals();
    return error_accumulator;
}

Optional<TreeNode const&> TreeNode::child_with_name(ByteString name) const
{
    for (auto& child : *m_children) {
        if (child.name() == name)
            return child;
    }

    return {};
}
