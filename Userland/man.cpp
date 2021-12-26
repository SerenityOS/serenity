/*
 * Copyright (c) 2019-2020, Sergey Bugaev <bugaevc@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/ByteBuffer.h>
#include <AK/String.h>
#include <LibCore/File.h>
#include <LibMarkdown/MDDocument.h>
#include <stdio.h>
#include <unistd.h>

int main(int argc, char* argv[])
{
    if (pledge("stdio rpath", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (unveil("/usr/share/man", "r") < 0) {
        perror("unveil");
        return 1;
    }

    unveil(nullptr, nullptr);

    String name;
    String section;

    if (argc < 2 || argc > 3) {
        fprintf(stderr, "Usage:\t%s <name>\n\t%s <section <name>\n", argv[0], argv[0]);
        exit(1);
    }

    if (argc == 2) {
        name = argv[1];
    } else {
        section = argv[1];
        name = argv[2];
    }

    auto make_path = [&](String s) {
        return String::format("/usr/share/man/man%s/%s.md", s.characters(), name.characters());
    };
    if (section.is_null()) {
        String sections[] = {
            "1",
            "2",
            "3",
            "4",
            "5",
            "6",
            "7",
            "8"
        };
        for (auto& s : sections) {
            String path = make_path(s);
            if (access(path.characters(), R_OK) == 0) {
                section = s;
                break;
            }
        }
        if (section.is_null()) {
            fprintf(stderr, "No man page for %s\n", name.characters());
            exit(1);
        }
    }

    auto file = Core::File::construct();
    file->set_filename(make_path(section));

    if (!file->open(Core::IODevice::OpenMode::ReadOnly)) {
        perror("Failed to open man page file");
        exit(1);
    }

    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    dbg() << "Loading man page from " << file->filename();
    auto buffer = file->read_all();
    String source { (const char*)buffer.data(), (size_t)buffer.size() };

    printf("%s(%s)\t\tSerenity manual\n", name.characters(), section.characters());

    MDDocument document;
    bool success = document.parse(source);
    ASSERT(success);

    String rendered = document.render_for_terminal();
    printf("%s", rendered.characters());
}
