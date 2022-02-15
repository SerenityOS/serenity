/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Michał Lach <misspelledcocteau@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <ctype.h>
#include <limits.h>
#include <mntent.h>
#include <regex.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#ifndef ARRAY_SIZE
#    define ARRAY_SIZE(x) (sizeof(x) / sizeof((x)[0]))
#endif

#ifndef STRINGIFY_LITERAL
#    define STRINGIFY_LITERAL(x) #x
#endif

#ifndef STRINGIFY
#    define STRINGIFY(x) STRINGIFY_LITERAL(x)
#endif

#define STRING_BUF_SIZE 4096
#define NMATCH 14

#define REGEX_INDENT "[ \t]+"
#define REGEX_EOLINDENT "[ \t]*"
#define REGEX_FS_SPEC_ABSPATH "/(([^/ \t]{1," STRINGIFY(NAME_MAX) "}/)*[^/ \t]{1," STRINGIFY(NAME_MAX) "})?"
#define REGEX_FS_SPEC_LABEL "([A-Z]+=[A-Za-z0-9_=-]+)"
#define REGEX_FS_SPEC_PSEUDO "([a-z_-]+)"
#define REGEX_FS_SPEC REGEX_FS_SPEC_ABSPATH "|" REGEX_FS_SPEC_LABEL "|" REGEX_FS_SPEC_PSEUDO
#define REGEX_FS_FILE "[a-z0-9A-Z_/-]+"
#define REGEX_FS_VFSTYPE "[[:alnum:]]+"
#define REGEX_FS_MNTOPTS "[a-z0-9,=-]+"
#define REGEX_FS_FREQ "[0-2]"
#define REGEX_FS_PASSNO "[0-2]"

#define REGEX_OPTIONAL "*"

#define REGEX_FSTAB                                        \
    "^"                                                    \
    "(" REGEX_FS_SPEC ")" REGEX_INDENT                     \
    "(" REGEX_FS_FILE ")" REGEX_INDENT                     \
    "(" REGEX_FS_VFSTYPE ")" REGEX_INDENT                  \
    "(" REGEX_FS_MNTOPTS ")" REGEX_EOLINDENT               \
    "(" REGEX_FS_FREQ ")" REGEX_OPTIONAL REGEX_EOLINDENT   \
    "(" REGEX_FS_PASSNO ")" REGEX_OPTIONAL REGEX_EOLINDENT \
    "$"

static const char* g_regex_pattern = REGEX_FSTAB;
static const int g_indices_index[] = { 1, 6, 7, 8, 9, 10 };

enum MntEntries {
    mnt_fsname = 0,
    mnt_dir,
    mnt_type,
    mnt_opts,
    mnt_freq,
    mnt_passno,
};

static bool comment_or_blank(const char* line, size_t len)
{
    size_t i;
    for (i = 0; i < len && isblank(*line); i++)
        ;
    return i == len || *line == '#';
}

extern "C" {

struct mntent* getmntent(FILE* stream)
{
    struct mntent* mntbuf = (struct mntent*)malloc(sizeof(struct mntent));
    char* strbuf = (char*)malloc(STRING_BUF_SIZE);
    return getmntent_r(stream, mntbuf, strbuf, STRING_BUF_SIZE);
}

FILE* setmntent(char const* filename, char const* type)
{
    return fopen(filename, type);
}

int addmntent(FILE* __restrict__ stream, const struct mntent* __restrict__ mnt)
{
    auto file_offset = ftell(stream);
    fseek(stream, 0L, SEEK_END);
    char buffer[STRING_BUF_SIZE];
    long written = (long)snprintf(buffer, STRING_BUF_SIZE, "%s \t%s \t%s \t%s \t%d \t%d\n", mnt->mnt_fsname, mnt->mnt_dir,
        mnt->mnt_type, mnt->mnt_opts, mnt->mnt_freq, mnt->mnt_passno);
    auto str_size = strlen(buffer);
    if (written < 0 || written >= STRING_BUF_SIZE)
        return 1;
    written = fwrite(buffer, str_size, 1, stream);
    fseek(stream, file_offset, SEEK_SET);
    return 0;
}

int endmntent(FILE* streamp)
{
    if (streamp)
        fclose(streamp);
    return 1;
}

char* hasmntopt(const struct mntent* mntbuf, const char* opt)
{
    // Shouldn't we check if opt is a valid flag?
    auto opt_pos = strcspn(mntbuf->mnt_opts, opt);
    if (opt_pos == strlen(mntbuf->mnt_opts))
        return nullptr;
    return mntbuf->mnt_opts + opt_pos--;
}

struct mntent* getmntent_r(FILE* streamp, struct mntent* mntbuf, char* buf, int buflen)
{
    regex_t expr;
    regmatch_t pmatch[NMATCH];
    memset(mntbuf, 0, sizeof(struct mntent));
    flockfile(streamp);
    for (;;) {
        if (fgets_unlocked(buf, buflen, streamp) == nullptr)
            return nullptr;

        auto line_len = strlen(buf);
        auto newline_pos = strcspn(buf, "\n");
        if (newline_pos != line_len) [[likely]] {
            buf[newline_pos] = '\0';
            line_len = strlen(buf);
        }

        if (comment_or_blank(buf, line_len))
            continue;

        if (regcomp(&expr, g_regex_pattern, REG_EXTENDED | REG_NEWLINE) != 0)
            return nullptr;

        if (regexec(&expr, buf, NMATCH, pmatch, 0) != 0) [[unlikely]]
            continue;

        for (size_t i = 0; i < ARRAY_SIZE(g_indices_index); i++) {
            int written;
            auto* substr_ptr = buf + pmatch[g_indices_index[i]].rm_so;
            buf[pmatch[g_indices_index[i]].rm_eo] = '\0';
            if (i == MntEntries::mnt_freq && strcmp(substr_ptr, "") == 0)
                break;

            switch (i) {
            case (MntEntries::mnt_fsname):
                mntbuf->mnt_fsname = substr_ptr;
                break;
            case (MntEntries::mnt_dir):
                mntbuf->mnt_dir = substr_ptr;
                break;
            case (MntEntries::mnt_type):
                mntbuf->mnt_type = substr_ptr;
                break;
            case (MntEntries::mnt_opts):
                mntbuf->mnt_opts = substr_ptr;
                break;
            case (MntEntries::mnt_freq):
                written = sscanf(substr_ptr, "%u", &mntbuf->mnt_freq);
                if (written != 1)
                    goto exit;
                break;
            case (MntEntries::mnt_passno):
                written = sscanf(substr_ptr, "%u", &mntbuf->mnt_passno);
                if (written != 1)
                    goto exit;
                break;
            }
        }
        break;
    }

exit:
    regfree(&expr);
    funlockfile(streamp);
    return mntbuf;
}
}
