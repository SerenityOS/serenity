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

#include <Kernel/API/Syscall.h>
#include <LibCore/ArgsParser.h>
#include <serenity.h>
#include <stdio.h>
#include <string.h>

int main(int argc, char** argv)
{
    int mode = 0;

    bool purge_all_volatile = false;
    bool purge_all_clean_inode = false;

    Core::ArgsParser args_parser;
    args_parser.add_option(purge_all_volatile, "Mode PURGE_ALL_VOLATILE", nullptr, 'v');
    args_parser.add_option(purge_all_clean_inode, "Mode PURGE_ALL_CLEAN_INODE", nullptr, 'c');
    args_parser.parse(argc, argv);

    if (!purge_all_volatile && !purge_all_clean_inode)
        purge_all_volatile = purge_all_clean_inode = true;

    if (purge_all_volatile)
        mode |= PURGE_ALL_VOLATILE;
    if (purge_all_clean_inode)
        mode |= PURGE_ALL_CLEAN_INODE;

    int purged_page_count = purge(mode);
    if (purged_page_count < 0) {
        perror("purge");
        return 1;
    }
    printf("Purged page count: %d\n", purged_page_count);
    return 0;
}
