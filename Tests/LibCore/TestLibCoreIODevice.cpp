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
    auto path = "long_lines.txt";
    auto file_or_error = Core::File::open(path, Core::OpenMode::ReadOnly);
    if (file_or_error.is_error()) {
        warnln("Failed to open {}: {}", path, file_or_error.error());
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
    file->close();
    outputfile->close();

    // Open files again for comparison since otherwise read_all returns empty (even when not closing the file)
    file = Core::File::construct(path);
    if (!file->open(Core::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();
    outputfile = Core::File::construct(output_path);
    if (!outputfile->open(Core::OpenMode::ReadOnly))
        VERIFY_NOT_REACHED();
    auto inputData = file->read_all();
    auto outputData = outputfile->read_all();
    EXPECT(inputData.size() > 0);
    EXPECT_EQ(inputData.size(), outputData.size());

    // Compare char by char
    for (size_t i = 0; i < inputData.size(); i++) {
        EXPECT_EQ(inputData[i], outputData[i]);
    }
}

TEST_CASE(file_get_read_position)
{
    const String path = "10kb.txt";
    auto file = Core::File::open(path, Core::OpenMode::ReadOnly).release_value();

    const size_t step_size = 98;
    for (size_t i = 0; i < 10240 - step_size; i += step_size) {
        auto read_buffer = file->read(step_size);
        EXPECT_EQ(read_buffer.size(), step_size);

        for (size_t j = 0; j < read_buffer.size(); j++) {
            EXPECT_EQ(static_cast<u32>(read_buffer[j] - '0'), (i + j) % 10);
        }

        off_t offset = 0;
        VERIFY(file->seek(0, Core::SeekMode::FromCurrentPosition, &offset));
        EXPECT_EQ(offset, static_cast<off_t>(i + step_size));
    }

    {
        off_t offset = 0;
        VERIFY(file->seek(0, Core::SeekMode::FromEndPosition, &offset));
        EXPECT_EQ(offset, 10240);
    }

    {
        off_t offset = 0;
        VERIFY(file->seek(0, Core::SeekMode::SetPosition, &offset));
        EXPECT_EQ(offset, 0);
    }
}
