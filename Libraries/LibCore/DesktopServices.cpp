/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/URL.h>
#include <LibCore/DesktopServices.h>
#include <stdio.h>
#include <sys/stat.h>

namespace Core {

static bool open_file_url(const URL&);
static bool spawn(String executable, String argument);

bool DesktopServices::open(const URL& url)
{
    if (url.protocol() == "file")
        return open_file_url(url);

    return spawn("/bin/Browser", url.to_string());
}

bool spawn(String executable, String argument)
{
    if (fork() == 0) {
        if (execl(executable.characters(), executable.characters(), argument.characters(), nullptr) < 0) {
            perror("execl");
            return false;
        }
        ASSERT_NOT_REACHED();
    }
    return true;
}

bool open_file_url(const URL& url)
{
    struct stat st;
    if (stat(url.path().characters(), &st) < 0) {
        perror("stat");
        return false;
    }

    if (S_ISDIR(st.st_mode))
        return spawn("/bin/FileManager", url.path());

    if (st.st_mode & (S_IXUSR | S_IXGRP | S_IXOTH))
        return spawn(url.path(), {});

    if (url.path().to_lowercase().ends_with(".png"))
        return spawn("/bin/QuickShow", url.path());

    if (url.path().to_lowercase().ends_with(".html"))
        return spawn("/bin/Browser", url.path());

    if (url.path().to_lowercase().ends_with(".wav"))
        return spawn("/bin/SoundPlayer", url.path());

    return spawn("/bin/TextEditor", url.path());
}

}
