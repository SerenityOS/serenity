/*
 * Copyright (c) 2021, Aron Lander <aron@aronlander.se>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibLocate/Crawler.h>

namespace Locate {

Crawler::Crawler(String path)
{
    m_directory_queue.enqueue(make<Locate::DirectoryInfo>(Locate::DirectoryInfo {
        .path = path,
        .db_id = m_identifier_counter,
        .parent_db_id = 0,
        .children = SinglyLinkedListWithCount<NonnullOwnPtr<Locate::ChildInfo>>() }));
    m_identifier_counter++;
}

NonnullOwnPtr<Locate::DirectoryInfo> Crawler::index_next_directory()
{
    auto current_directory_info = m_directory_queue.dequeue();
    String current_path = current_directory_info->path.characters();
    Core::DirIterator directory_iterator(current_path, Core::DirIterator::SkipParentAndBaseDir);

    while (directory_iterator.has_next()) {
        auto child = make<Locate::ChildInfo>();
        child->name = directory_iterator.next_path();

        String full_path;
        if (current_path == "/")
            full_path = String(String::formatted("{}{}", current_path, child->name));
        else
            full_path = String(String::formatted("{}/{}", current_path, child->name));

        child->db_id = m_identifier_counter;
        child->parent_db_id = current_directory_info->db_id;

        struct stat stat_info;
        int stat_result = lstat(full_path.characters(), &stat_info);
        VERIFY(stat_result >= 0);
        if (S_ISDIR(stat_info.st_mode)) {
            child->type = Locate::FileType::Directory;
            m_directory_queue.enqueue(make<Locate::DirectoryInfo>(Locate::DirectoryInfo {
                .path = full_path,
                .db_id = m_identifier_counter,
                .parent_db_id = current_directory_info->db_id,
                .children = SinglyLinkedListWithCount<NonnullOwnPtr<Locate::ChildInfo>>() }));
        } else if (S_ISBLK(stat_info.st_mode)) {
            continue;
        } else {
            child->type = Locate::FileType::File;
        }

        current_directory_info->children.append(child);
        m_identifier_counter++;
    }

    return current_directory_info;
}

size_t Crawler::directories_in_queue()
{
    return m_directory_queue.size();
}

}
