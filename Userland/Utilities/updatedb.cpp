/*
 * Copyright (c) 2021, Aron Lander <aron@aronlander.se>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <LibLocate/Crawler.h>
#include <LibLocate/LocateDB.h>

int main()
{
    if (pledge("stdio rpath wpath cpath chown fattr", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (getuid() != 0) {
        warnln("You need root privileges to run updatedb, exiting.");
        return 1;
    }

    NonnullOwnPtr<Locate::Crawler> crawler = make<Locate::Crawler>(String("/"));
    NonnullOwnPtr<Locate::LocateDB> locate_db = make<Locate::LocateDB>(Locate::locate_db_path, Locate::LocateDbMode::Write);
    locate_db->write_header();

    while (crawler->directories_in_queue() > 0) {
        auto dir_info = crawler->index_next_directory();
        locate_db->write_directory(*dir_info);
    }
}
