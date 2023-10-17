/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Directory.h>
#include <LibCore/Path.h>
#include <LibTest/TestCase.h>

using Core::Path, Core::AbsolutePathSegment, Core::RelativePathSegment;

template<typename T>
struct ConstexprConstructibleChecker {
    template<typename F, int = (T(F {}()), 0)>
    consteval static bool check(F) { return true; }
    consteval static bool check(...) { return false; }
};

#define IS_CONSTEXPR_CONSTRUCTIBLE(T, argument) ConstexprConstructibleChecker<T>::check([] { return argument; })

#define CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS(path)                              \
    static_assert(IS_CONSTEXPR_CONSTRUCTIBLE(AbsolutePathSegment, "/" path)); \
    static_assert(IS_CONSTEXPR_CONSTRUCTIBLE(RelativePathSegment, path))

#define CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT(path)                       \
    static_assert(!IS_CONSTEXPR_CONSTRUCTIBLE(AbsolutePathSegment, "/" path)); \
    static_assert(!IS_CONSTEXPR_CONSTRUCTIBLE(RelativePathSegment, path))

CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS("a");
CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS("aaaaa");
CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS("usr/bin");
CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS("usr/bin/.");
CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS("directory/.");
CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS("./lib");
CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS("foo/./bar");
CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS("foo/././bar");
CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS(".");

CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("");
CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("/");
CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("//");
CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("..");
CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("../directory");
CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("directory/");
CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("directory/..");
CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("a//b");
CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("a//");
CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT("/foo");

#undef CHECK_SEGMENT_CONSTEXPR_CONSTRUCTS
#undef CHECK_SEGMENT_DOES_NOT_CONSTEXPR_CONSTRUCT

#ifdef AK_OS_SERENITY
static StringView as_string_view(StringView s)
{
    return s;
}
#else
static StringView as_string_view(char const* s)
{
    return StringView(s, __builtin_strlen(s));
}
#endif

#define EXPECT_PATH_EQ(path, fd, relative_part, last_segment_, is_directory)    \
    EXPECT_EQ(path.directory_fd_for_syscall(), fd);                             \
    EXPECT_EQ(as_string_view(path.relative_path_for_syscall()), relative_part); \
    EXPECT_EQ(path.last_segment(), last_segment_);                              \
    EXPECT(path.is_surely_a_directory() == is_directory);

TEST_CASE(simple)
{
    // Absolute paths
    auto root = Path::root();
    EXPECT_PATH_EQ(root, -1, "/.", ".", true);

    auto root_usr = root / "usr";
    EXPECT_PATH_EQ(root_usr, -1, "/./usr", "usr", false);

    auto root_usr_bin = root_usr / "bin";
    EXPECT_PATH_EQ(root_usr_bin, -1, "/./usr/bin", "bin", false);

    auto root_usr_bin_2 = root / "usr/bin";
    EXPECT_PATH_EQ(root_usr_bin, -1, "/./usr/bin", "bin", false);

    Path root_usr_3("/usr");
    EXPECT_PATH_EQ(root_usr_3, -1, "/usr", "usr", false);

    auto root_usr_bin_3 = root_usr_3 / "bin";
    EXPECT_PATH_EQ(root_usr_bin_3, -1, "/usr/bin", "bin", false);

    // Paths relative to the initial working directory
    auto cwd = Core::Directory::initial_working_directory();
    int cwd_fd = cwd.fd();

    Path cwd_path(cwd);
    EXPECT_PATH_EQ(cwd_path, cwd_fd, ".", ".", true);

    auto dot_usr = cwd_path / "usr";
    EXPECT_PATH_EQ(dot_usr, cwd_fd, "usr", "usr", false);

    auto dot_usr_bin = dot_usr / "bin";
    EXPECT_PATH_EQ(dot_usr_bin, cwd_fd, "usr/bin", "bin", false);

    auto dot_usr_bin_dot = dot_usr / "bin/.";
    EXPECT_PATH_EQ(dot_usr_bin_dot, cwd_fd, "usr/bin/.", ".", true);

    // Path relative to an open directory
    auto root_directory = Core::Directory::initial_working_directory();
    int root_fd = root_directory.fd();

    auto opened_root_usr_bin = Path(root_directory) / "directory";
    EXPECT_PATH_EQ(opened_root_usr_bin, root_fd, "directory", "directory", false);
}

TEST_CASE(path_from_string)
{
    auto cwd = Core::Directory::initial_working_directory();
    int cwd_fd = cwd.fd();
    EXPECT_NE(cwd_fd, -1);

    auto empty_string_error = Path::create_from_string(""sv);
    EXPECT(empty_string_error.is_error());

    auto root = MUST(Path::create_from_string("/"sv));
    EXPECT_PATH_EQ(root, -1, "/.", ".", true);

    auto cwd_path = MUST(Path::create_from_string("."sv));
    EXPECT_PATH_EQ(cwd_path, cwd_fd, ".", ".", true);

    auto dot_dot = MUST(Path::create_from_string(".."sv));
    EXPECT_PATH_EQ(dot_dot, cwd_fd, "..", "..", true);

    auto dot_dot_slash = MUST(Path::create_from_string("../"sv));
    EXPECT_PATH_EQ(dot_dot_slash, cwd_fd, "../.", ".", true);

    auto usr_bin = MUST(Path::create_from_string("/usr/bin"sv));
    EXPECT_PATH_EQ(usr_bin, -1, "/usr/bin", "bin", false);

    auto usr_bin_slash = MUST(Path::create_from_string("/usr/bin/"sv));
    EXPECT_PATH_EQ(usr_bin_slash, -1, "/usr/bin/.", ".", true);

    auto cringe = MUST(Path::create_from_string("../usr/./../foo/bar"sv));
    EXPECT_PATH_EQ(cringe, cwd_fd, "../usr/./../foo/bar", "bar", false);

    auto root_directory = Core::Directory::initial_working_directory();
    int root_fd = root_directory.fd();

    auto root_root_path = Path(root_directory) / "root";
    EXPECT_PATH_EQ(root_root_path, root_fd, "root", "root", false);
}

template<typename T>
__attribute__((no_sanitize("undefined"))) Badge<T> fake_badge()
{
    // FIXME: Find out a way to construct fake badges without invoking UB.
    void* fn_address = reinterpret_cast<void*>(+[] {});
    AK::taint_for_optimizer(fn_address);
    return reinterpret_cast<Badge<T> (*)()>(fn_address)();
}

TEST_CASE(can_be_considered_standard_stream)
{
    auto slash_minus = MUST(Path::create_from_string("/-"sv));
    EXPECT(!slash_minus.can_be_considered_standard_stream(fake_badge<Core::File>()));

    auto minus = MUST(Path::create_from_string("-"sv));
    EXPECT(minus.can_be_considered_standard_stream(fake_badge<Core::File>()));

    auto minus_slash = MUST(Path::create_from_string("-/"sv));
    EXPECT(!minus_slash.can_be_considered_standard_stream(fake_badge<Core::File>()));
}
