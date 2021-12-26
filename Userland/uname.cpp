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

#include <stdio.h>
#include <sys/utsname.h>
#include <unistd.h>

int main(int argc, char** argv)
{
    if (pledge("stdio", nullptr) < 0) {
        perror("pledge");
        return 1;
    }

    utsname uts;
    int rc = uname(&uts);
    if (rc < 0) {
        perror("uname() failed");
        return 0;
    }
    bool flag_s = false;
    bool flag_n = false;
    bool flag_r = false;
    bool flag_m = false;
    if (argc == 1) {
        flag_s = true;
    } else {
        for (int i = 1; i < argc; ++i) {
            if (argv[i][0] == '-') {
                for (const char* o = &argv[i][1]; *o; ++o) {
                    switch (*o) {
                    case 's':
                        flag_s = true;
                        break;
                    case 'n':
                        flag_n = true;
                        break;
                    case 'r':
                        flag_r = true;
                        break;
                    case 'm':
                        flag_m = true;
                        break;
                    case 'a':
                        flag_s = flag_n = flag_r = flag_m = true;
                        break;
                    }
                }
            }
        }
    }
    if (!flag_s && !flag_n && !flag_r && !flag_m)
        flag_s = true;
    if (flag_s)
        printf("%s ", uts.sysname);
    if (flag_n)
        printf("%s ", uts.nodename);
    if (flag_r)
        printf("%s ", uts.release);
    if (flag_m)
        printf("%s ", uts.machine);
    printf("\n");
    return 0;
}
