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
    size_t file_offset = ftell(stream);
    fseek(stream, 0L, SEEK_END);
    char buffer[STRING_BUF_SIZE];
    int ret = snprintf(buffer, STRING_BUF_SIZE, "%s \t%s \t%s \t%s \t%d \t%d\n", mnt->mnt_fsname, mnt->mnt_dir,
        mnt->mnt_type, mnt->mnt_opts, mnt->mnt_freq, mnt->mnt_passno);
    size_t str_size = strlen(buffer);
    if (ret < 0 || ret >= STRING_BUF_SIZE)
        return 1;
    size_t written = fwrite(buffer, str_size, 1, stream);
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
    // Shouldn't we check for correctness of mnt_opts flags we check for?
    auto opt_pos = strcspn(mntbuf->mnt_opts, opt);
    if (opt_pos == strlen(mntbuf->mnt_opts))
        return nullptr;
    return mntbuf->mnt_opts + opt_pos--;
}

struct mntent* getmntent_r(FILE* streamp, struct mntent* mntbuf, char* buf, int buflen)
{
    regex_t regex;
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

        if (regcomp(&regex, g_regex_pattern, REG_EXTENDED | REG_NEWLINE) != 0)
            return nullptr;

        if (regexec(&regex, buf, NMATCH, pmatch, 0) != 0) [[unlikely]]
            continue;

        for (size_t i = 0; i < ARRAY_SIZE(g_indices_index); i++) {
            int ret;
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
                ret = sscanf(buf + pmatch[g_indices_index[i]].rm_so, "%u", &mntbuf->mnt_freq);
                if (ret != 1)
                    goto exit;
                break;
            case (MntEntries::mnt_passno):
                ret = sscanf(buf + pmatch[g_indices_index[i]].rm_so, "%u", &mntbuf->mnt_passno);
                if (ret != 1)
                    goto exit;
                break;
            }
        }
        break;
    }

exit:
    regfree(&regex);
    funlockfile(streamp);
    return mntbuf;
}
}
