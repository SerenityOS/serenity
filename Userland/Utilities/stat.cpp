/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Shannon Booth <shannon@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/ArgsParser.h>
#include <LibCore/DateTime.h>
#include <LibCore/System.h>
#include <LibMain/Main.h>
#include <grp.h>
#include <pwd.h>
#include <sys/sysmacros.h>
#include <time.h>

static ErrorOr<int> stat(StringView file, bool should_follow_links)
{
    auto st = TRY(should_follow_links ? Core::System::stat(file) : Core::System::lstat(file));
    outln("    File: {}", file);
    outln("  Device: {}", st.st_dev);
    outln("   Inode: {}", st.st_ino);
    if (S_ISCHR(st.st_mode) || S_ISBLK(st.st_mode))
        outln("  Device: {},{}", major(st.st_rdev), minor(st.st_rdev));
    else
        outln("    Size: {}", st.st_size);
    outln("   Links: {}", st.st_nlink);
    outln("  Blocks: {}", st.st_blocks);
    out("     UID: {}", st.st_uid);
    if (auto* pwd = getpwuid(st.st_uid)) {
        out(" ({})", pwd->pw_name);
    }
    outln("");
    out("     GID: {}", st.st_gid);
    if (auto* grp = getgrgid(st.st_gid)) {
        out(" ({})", grp->gr_name);
    }
    outln("");
    out("    Mode: ({:o}/", st.st_mode);

    if (S_ISDIR(st.st_mode))
        out("d");
    else if (S_ISLNK(st.st_mode))
        out("l");
    else if (S_ISBLK(st.st_mode))
        out("b");
    else if (S_ISCHR(st.st_mode))
        out("c");
    else if (S_ISFIFO(st.st_mode))
        out("f");
    else if (S_ISSOCK(st.st_mode))
        out("s");
    else if (S_ISREG(st.st_mode))
        out("-");
    else
        out("?");

    out("{:c}{:c}{:c}{:c}{:c}{:c}{:c}{:c}",
        st.st_mode & S_IRUSR ? 'r' : '-',
        st.st_mode & S_IWUSR ? 'w' : '-',
        st.st_mode & S_ISUID ? 's' : (st.st_mode & S_IXUSR ? 'x' : '-'),
        st.st_mode & S_IRGRP ? 'r' : '-',
        st.st_mode & S_IWGRP ? 'w' : '-',
        st.st_mode & S_ISGID ? 's' : (st.st_mode & S_IXGRP ? 'x' : '-'),
        st.st_mode & S_IROTH ? 'r' : '-',
        st.st_mode & S_IWOTH ? 'w' : '-');

    if (st.st_mode & S_ISVTX)
        out("t");
    else
        out("{:c}", st.st_mode & S_IXOTH ? 'x' : '-');

    outln(")");

    auto print_time = [](timespec t) {
        outln("{}.{:09}", Core::DateTime::from_timestamp(t.tv_sec).to_byte_string(), t.tv_nsec);
    };

    out("Accessed: ");
    print_time(st.st_atim);
    out("Modified: ");
    print_time(st.st_mtim);
    out(" Changed: ");
    print_time(st.st_ctim);

    return 0;
}

ErrorOr<int> serenity_main(Main::Arguments arguments)
{
    TRY(Core::System::pledge("stdio rpath"));

    bool should_follow_links = false;
    Vector<StringView> files;

    auto args_parser = Core::ArgsParser();
    args_parser.add_option(should_follow_links, "Follow links to files", nullptr, 'L');
    args_parser.add_positional_argument(files, "File(s) to stat", "file", Core::ArgsParser::Required::Yes);
    args_parser.parse(arguments);

    bool had_error = false;
    for (auto& file : files) {
        auto r = stat(file, should_follow_links);
        if (r.is_error()) {
            had_error = true;
            warnln("stat: cannot stat '{}': {}", file, strerror(r.error().code()));
        }
    }

    return had_error;
}
