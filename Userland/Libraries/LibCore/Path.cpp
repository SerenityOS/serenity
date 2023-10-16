/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/Directory.h>
#include <LibCore/Path.h>
#include <LibCore/System.h>

using namespace Core;
using namespace Core::Detail;

FileDescriptorOwner::~FileDescriptorOwner()
{
    if (m_fd != -1 && m_should_close) {
        MUST(Core::System::close(m_fd));
        m_fd = -1;
    }
}

RuntimePath::RuntimePath(StringView view)
{
    data.append(view.characters_without_null_termination(), view.length());
    if (view.ends_with('/'))
        data.append('.');
    if (make_paths_zero_terminated)
        data.append(0);
}

StringView RuntimePath::view() const
{
    if (make_paths_zero_terminated) {
        StringView result = data;
        return result.substring_view(0, result.length() - 1);
    }
    return data;
}

void RuntimePath::append(RelativePathSegment segment)
{
    if (make_paths_zero_terminated)
        data[data.size() - 1] = '/';
    else
        data.append('/');

    StringView segment_view = segment.path();
    data.append(segment_view.characters_without_null_termination(), segment_view.length());

    if (segment_view.ends_with('/'))
        data.append('.');

    if (make_paths_zero_terminated)
        data.append(0);
}

StringView CompiletimeConstantPath::view() const
{
    return data;
}

Path Path::root()
{
    return Path(AbsoluteCompiletimePath { .path = { "/."sv } });
}

ErrorOr<Path> Path::create_from_string(StringView path_string)
{
    return create_from_string(path_string, Directory::initial_working_directory());
}

ErrorOr<Path> Path::create_from_string(StringView path_string, Path const& base)
{
    if (path_string.is_empty())
        return Error::from_string_literal("Path cannot be empty");

    if (path_string.starts_with('/'))
        return Path(AbsoluteRuntimePath({ path_string }));
    return base / RelativePathSegment({}, path_string);
}

#ifdef AK_OS_SERENITY
Path Path::create_from_c_string_without_copy_in_libc(char const* path)
{
    return Path(ForeignCStringPath { .dirfd = AT_FDCWD, .path = path });
}

Path Path::create_from_c_string_without_copy_in_libc(int dirfd, char const* path)
{
    return Path(ForeignCStringPath { .dirfd = dirfd, .path = path });
}
#endif

Path::Path(AbsolutePathSegment path)
    : m_path(ForeignCStringPath {}) // m_path does not have null state
{
    if (path.is_zero_terminated_literal())
        m_path = AbsoluteCompiletimePath({ path.path() });
    else
        m_path = AbsoluteRuntimePath({ path.path() });
}

Path::Path(Directory directory)
    : m_path(DirectoryPath { .directory = directory.fd_owner({}) })
{
}

StringView Path::last_segment() const
{
    return m_path.visit(
        [](ForeignCStringPath const&) -> StringView { VERIFY_NOT_REACHED(); },
        [](DirectoryPath const&) { return "."sv; },
        [](auto const& path) {
            StringView relative_part = path.path.view();
            return relative_part.substring_view(relative_part.find_last('/').value_or(-1) + 1);
        });
}

bool Path::is_surely_a_directory() const
{
    auto segment = last_segment();
    return segment == "." || segment == "..";
}

Path Path::operator/(RelativePathSegment segment) const
{
    return m_path.visit(
        [&](ForeignCStringPath const&) -> Path { VERIFY_NOT_REACHED(); },
        [&](DirectoryPath const& directory_path) {
            if (segment.is_zero_terminated_literal())
                return Path(RelativeCompiletimePath { directory_path.directory, { segment.path() } });
            return Path(RelativeRuntimePath { directory_path.directory, { segment.path() } });
        },
        [&](OneOf<AbsoluteRuntimePath, RelativeRuntimePath> auto path) {
            path.path.append(segment);
            return Path(move(path));
        },
        [&](AbsoluteCompiletimePath const& absolute_path) {
            auto new_path = AbsoluteRuntimePath({ absolute_path.path.data });
            new_path.path.append(segment);
            return Path(move(new_path));
        },
        [&](RelativeCompiletimePath const& relative_path) {
            auto new_path = RelativeRuntimePath { relative_path.directory, { relative_path.path.data } };
            new_path.path.append(segment);
            return Path(move(new_path));
        });
}

int Path::directory_fd_for_syscall() const
{
    return m_path.visit(
        [&](ForeignCStringPath const& foreign_path) {
            return foreign_path.dirfd;
        },
        [&](OneOf<DirectoryPath, RelativeRuntimePath, RelativeCompiletimePath> auto const& path) {
            return path.directory->fd();
        },
        [&](OneOf<AbsoluteRuntimePath, AbsoluteCompiletimePath> auto const&) {
            return -1;
        });
}

#ifdef AK_OS_SERENITY
StringView Path::relative_path_for_syscall() const
{
    return m_path.visit(
        [&](ForeignCStringPath const& foreign_path) {
            return StringView { foreign_path.path, __builtin_strlen(foreign_path.path) };
        },
        [&](DirectoryPath const&) {
            return "."sv;
        },
        [&](auto const& path) {
            return path.path.view();
        });
}
#else
char const* Path::relative_path_for_syscall() const
{
    return m_path.visit(
        [&](ForeignCStringPath const& foreign_path) {
            return foreign_path.path;
        },
        [&](DirectoryPath const&) {
            return ".";
        },
        [&](auto const& path) {
            return path.path.view().characters_without_null_termination();
        });
}
#endif

bool Path::can_be_considered_standard_stream(Badge<File>) const
{
    return m_path.visit(
        [&](ForeignCStringPath const&) -> bool { VERIFY_NOT_REACHED(); },
        [&](RelativeRuntimePath const& path) { return path.path.view() == "-"sv; },
        [&](auto const&) { return false; });
}
