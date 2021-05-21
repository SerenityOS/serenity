/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/File.h>
#include <LibTest/TestCase.h>
#include <unistd.h>

TEST_CASE(file_readline)
{
    auto path = "/res/txt/long-line.txt";
    auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", path, strerror(errno));
        VERIFY_NOT_REACHED();
    }
    auto file = file_or_error.release_value();
    auto output_path = "/tmp/output.txt";
    auto outfile_or_error = Core::File::open(output_path, Core::OpenMode::WriteOnly);
    auto outputfile = outfile_or_error.release_value();
    while (file->can_read_line()) {
        outputfile->write(file->read_line());
        outputfile->write("\n");
    }
    struct stat src_stat;
    if (fstat(file->fd(), &src_stat) < 0) {
        VERIFY_NOT_REACHED();
    }
    struct stat dst_stat;
    if (fstat(outputfile->fd(), &dst_stat) < 0) {
        VERIFY_NOT_REACHED();
    }
    file->close();
    outputfile->close();
    EXPECT_EQ(src_stat.st_size, dst_stat.st_size);
}
