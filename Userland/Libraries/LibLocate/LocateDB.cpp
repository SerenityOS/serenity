/*
 * Copyright (c) 2021, Aron Lander <aron@aronlander.se>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibLocate/LocateDB.h>

namespace Locate {

LocateDB::LocateDB(String const& path, LocateDbMode mode)
{
    if (!Core::File::exists(locate_db_path)) {
        auto file_handle = Core::File::open(locate_db_path, Core::IODevice::WriteOnly, 0640);
        if (file_handle.is_error()) {
            outln("Got error while trying to create locate db file: {}", file_handle.error());
            VERIFY_NOT_REACHED();
        }
        bool close_result = file_handle.value()->close();
        VERIFY(close_result);
        int chown_result = chown(locate_db_path, 0, 18);
        VERIFY(chown_result == 0);
    }

    m_mode = mode;
    m_file_handle = fopen(path.characters(), mode == LocateDbMode::Write ? "wb" : "rb");
    VERIFY(m_file_handle);
    int seek_result = fseek(m_file_handle, 0, SEEK_END);
    VERIFY(seek_result == 0);
    m_file_size = ftell(m_file_handle);
    VERIFY(m_file_size >= 0);
    seek_result = fseek(m_file_handle, 0, SEEK_SET);
    VERIFY(seek_result == 0);
}

LocateDB::~LocateDB()
{
    fclose(m_file_handle);
}

void LocateDB::write_header()
{
    VERIFY(m_mode == LocateDbMode::Write);
    size_t write_result = fwrite(magic_header, sizeof(uint8_t), strlen(magic_header) + 1, m_file_handle);
    VERIFY(write_result > 0);
}

void LocateDB::write_directory(DirectoryInfo& directory_info)
{
    VERIFY(m_mode == LocateDbMode::Write);

    struct DirStartEndHeader dir_header {
        .type = (uint8_t)ChunkType::DirectoryStart,
        .db_id = directory_info.db_id,
        .parent_db_id = directory_info.parent_db_id,
        .path_size = directory_info.path.length() + 1
    };
    size_t write_result = fwrite(bit_cast<char*>(&dir_header), sizeof(struct DirStartEndHeader), 1, m_file_handle);
    VERIFY(write_result > 0);
    write_result = fwrite(directory_info.path.characters(), sizeof(uint8_t), directory_info.path.length() + 1, m_file_handle);
    VERIFY(write_result > 0);

    for (auto& child : directory_info.children) {
        ChildFileData cur_child {
            .type = (uint8_t)(child->type == FileType::File ? ChunkType::File : ChunkType::Directory),
            .db_id = child->db_id,
            .parent_db_id = child->parent_db_id,
            .name_size = child->name.length() + 1
        };

        write_result = fwrite(bit_cast<char*>(&cur_child), sizeof(struct ChildFileData), 1, m_file_handle);
        VERIFY(write_result > 0);
        write_result = fwrite(child->name.characters(), sizeof(uint8_t), child->name.length() + 1, m_file_handle);
        VERIFY(write_result > 0);
    }

    dir_header.type = (uint8_t)ChunkType::DirectoryEnd;
    write_result = fwrite(bit_cast<char*>(&dir_header), sizeof(struct DirStartEndHeader), 1, m_file_handle);
    VERIFY(write_result > 0);
    write_result = fwrite(directory_info.path.characters(), sizeof(uint8_t), directory_info.path.length() + 1, m_file_handle);
    VERIFY(write_result > 0);
}

bool LocateDB::verify_header()
{
    VERIFY(m_mode == LocateDbMode::Read);

    char buffer[BUFSIZ];
    size_t bytes_read = fread(&buffer, sizeof(char), strlen(magic_header) + 1, m_file_handle);
    VERIFY(bytes_read > 0);
    if (strcmp(magic_header, buffer) == 0)
        return true;
    return false;
}

LocateDB::ChunkType LocateDB::identify_chunk()
{
    VERIFY(m_mode == LocateDbMode::Read);

    uint8_t type_byte;
    size_t bytes_read = fread(&type_byte, sizeof(uint8_t), 1, m_file_handle);
    VERIFY(bytes_read == 1);
    VERIFY(feof(m_file_handle) == 0);
    int seek_result = fseek(m_file_handle, -1, SEEK_CUR);
    VERIFY(seek_result == 0);

    VERIFY(type_byte < (uint8_t)ChunkType::__Count);
    return ChunkType(type_byte);
}

void LocateDB::parse_directory_info(DirectoryInfo& directory_info)
{
    VERIFY(m_mode == LocateDbMode::Read);

    struct DirStartEndHeader header;
    size_t bytes_read = fread(&header, sizeof(struct DirStartEndHeader), 1, m_file_handle);
    VERIFY(bytes_read > 0);
    directory_info.db_id = header.db_id;
    directory_info.parent_db_id = header.parent_db_id;

    VERIFY(header.path_size < BUFSIZ);
    char path_from_file[BUFSIZ];
    bytes_read = fread(&path_from_file, sizeof(uint8_t), header.path_size, m_file_handle);
    VERIFY(bytes_read > 0);
    directory_info.path = String(path_from_file);
}

NonnullOwnPtr<ChildInfo> LocateDB::parse_child_info()
{
    VERIFY(m_mode == LocateDbMode::Read);

    NonnullOwnPtr<ChildInfo> child_info = make<ChildInfo>();

    struct ChildFileData child_file_data;
    size_t bytes_read = fread(&child_file_data, sizeof(struct ChildFileData), 1, m_file_handle);
    VERIFY(bytes_read > 0);
    VERIFY(child_file_data.name_size < BUFSIZ);
    char name_from_file[BUFSIZ];
    bytes_read = fread(&name_from_file, sizeof(uint8_t), child_file_data.name_size, m_file_handle);
    VERIFY(bytes_read > 0);

    child_info->type = ChunkType(child_file_data.type) == ChunkType::File ? FileType::File : FileType::Directory;
    child_info->db_id = child_file_data.db_id;
    child_info->parent_db_id = child_file_data.parent_db_id;
    child_info->name = String(name_from_file);

    return child_info;
}

OwnPtr<DirectoryInfo> LocateDB::get_next_directory()
{
    OwnPtr<DirectoryInfo> current_directory = make<DirectoryInfo>();

    while (true) {
        long current_position = ftell(m_file_handle);
        VERIFY(current_position != -1);
        if (current_position >= m_file_size) {
            break;
        } else {
            VERIFY(!feof(m_file_handle));
        }
        ChunkType row_type = identify_chunk();

        if (row_type == ChunkType::DirectoryStart) {
            VERIFY(current_directory->path.length() == 0);
            parse_directory_info(*current_directory);
            if (current_directory->path == "/") {
                NonnullOwnPtr<PermissionInfo> permission_info = make<PermissionInfo>(PermissionInfo {
                    .parent_id = 0,
                    .path = "/" });
                m_path_relations.set(1, move(permission_info));
            }
        } else if (row_type == ChunkType::DirectoryEnd) {
            DirectoryInfo verification_dir;
            parse_directory_info(verification_dir);
            VERIFY(current_directory->path.length() != 0 && current_directory->path == verification_dir.path);
            return current_directory;
        } else {
            auto child = parse_child_info();
            String path_string;
            if (current_directory->path == "/")
                path_string = String::formatted("{}{}", current_directory->path, child->name);
            else
                // Don't waste memory on file names in the path hierarchy.
                path_string = String::formatted("{}/{}", current_directory->path, child->type == FileType::Directory ? child->name : "");

            NonnullOwnPtr<PermissionInfo> permission_info = make<PermissionInfo>(
                PermissionInfo { child->parent_db_id, String::formatted(path_string, current_directory->path, child->name) });
            m_path_relations.set(child->db_id, move(permission_info));
            current_directory->children.append(move(child));
        }
    }

    current_directory = nullptr;
    return current_directory;
}

const PermissionInfo* LocateDB::get_permission_info(uint32_t db_id)
{
    VERIFY(m_path_relations.contains(db_id));
    return m_path_relations.get(db_id).value_or(nullptr);
}

}
