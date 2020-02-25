/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/String.h>
#include <AK/Vector.h>
#include <grp.h>
#include <pwd.h>
#include <stdio.h>
#include <string.h>
#include <sys/stat.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio rpath chown", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    if (argc < 3) {
        printf("usage: chown <uid[:gid]> <path>\n");
        return 0;
    }

    uid_t new_uid = -1;
    gid_t new_gid = -1;

    auto parts = String(argv[1]).split(':');
    if (parts.is_empty()) {
        fprintf(stderr, "Empty uid/gid spec\n");
        return 1;
    }

    bool ok;
    new_uid = parts[0].to_uint(ok);
    if (!ok) {
        new_uid = getpwnam(parts[0].characters())->pw_uid;

        if (!new_uid) {
            fprintf(stderr, "Invalid uid: '%s'\n", parts[0].characters());
            return 1;
        }
    }

    if (parts.size() == 2) {
        new_gid = parts[1].to_uint(ok);
        if (!ok) {
            new_gid = getgrnam(parts[1].characters())->gr_gid;

            if (!new_gid) {
                fprintf(stderr, "Invalid gid: '%s'\n", parts[1].characters());
                return 1;
            }
        }
    }

    int rc = chown(argv[2], new_uid, new_gid);
    if (rc < 0) {
        perror("chown");
        return 1;
    }

    return 0;
}
