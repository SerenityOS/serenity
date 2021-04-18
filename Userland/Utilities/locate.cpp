/*
 * Copyright (c) 2021, Aron Lander <aron@aronlander.se>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibCore/ArgsParser.h>
#include <LibLocate/LocateDB.h>
#include <LibLocate/Types.h>
#include <unistd.h>

bool check_permissions(uint32_t db_id, Locate::LocateDB& locate_db);

bool check_permissions(uint32_t db_id, Locate::LocateDB& locate_db)
{
    uint32_t next_identifier = db_id;
    while (true) {
        const Locate::PermissionInfo* permission_info = locate_db.get_permission_info(next_identifier);
        // Reached /, thus, the readable path for the user is not broken along the way, thus, the file can be shown.
        if (permission_info->parent_id == 0)
            return true;
        if (access(permission_info->path.characters(), R_OK) != 0) {
            return false;
        }
        next_identifier = permission_info->parent_id;
    }
}

int main(int argc, char** argv)
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil(Locate::locate_db_path, "r") < 0) {
        perror("unveil");
        return 1;
    }

    String keyword;
    bool o_case_insensitive = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(o_case_insensitive, "Ignore case during keyword matching", "ignore-case", 'i');
    args_parser.add_positional_argument(keyword, "Keyword to search for within the file db", "keyword", Core::ArgsParser::Required::Yes);
    args_parser.parse(argc, argv);

    auto locate_db = Locate::LocateDB(Locate::locate_db_path, Locate::LocateDbMode::Read);
    if (!locate_db.verify_header()) {
        warnln("Couldn't verify the locate file database. Please run \"updatedb\" as root to regenerate it.");
        return 1;
    }

    CaseSensitivity case_sensitivity = o_case_insensitive ? CaseSensitivity::CaseInsensitive : CaseSensitivity::CaseSensitive;

    while (auto directory = locate_db.get_next_directory()) {

        if (directory->path.contains(keyword, case_sensitivity)) {
            if (check_permissions(directory->db_id, locate_db)) {
                for (auto& child : directory->children) {
                    outln("{}/{}", directory->path, child->name);
                }
            }
        } else {
            bool directory_accessible = false;
            for (auto& child : directory->children) {
                if (child->name.contains(keyword, case_sensitivity)) {
                    if (directory_accessible || check_permissions(directory->db_id, locate_db)) {
                        directory_accessible = true;
                        outln("{}/{}", directory->path, child->name);
                    }
                }
            }
        }
    }
}
