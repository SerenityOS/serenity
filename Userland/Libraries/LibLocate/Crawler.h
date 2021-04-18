/*
 * Copyright (c) 2021, Aron Lander <aron@aronlander.se>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Queue.h>
#include <AK/String.h>
#include <LibCore/DirIterator.h>
#include <LibLocate/Types.h>
#include <sys/stat.h>

namespace Locate {

class Crawler {
public:
    Crawler(String path);
    NonnullOwnPtr<Locate::DirectoryInfo> index_next_directory();
    size_t directories_in_queue();

private:
    Queue<NonnullOwnPtr<Locate::DirectoryInfo>> m_directory_queue;
    uint32_t m_identifier_counter = 1;
};
}
