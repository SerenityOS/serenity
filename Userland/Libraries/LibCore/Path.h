/*
 * Copyright (c) 2023, Dan Klishch <danilklishch@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/ByteBuffer.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>
#include <LibCore/Forward.h>

#ifdef LIBCORE_DEPRECATE_OLD_PATH_API
#    define LIBCORE_DEPRECATED_PATH_API [[deprecated]]
#else
#    define LIBCORE_DEPRECATED_PATH_API
#endif

namespace Core {

namespace Detail {

class FileDescriptorOwner : public RefCounted<FileDescriptorOwner> {
    AK_MAKE_NONCOPYABLE(FileDescriptorOwner);
    AK_MAKE_NONMOVABLE(FileDescriptorOwner);

public:
    FileDescriptorOwner(int fd, bool should_close)
        : m_fd(fd)
        , m_should_close(should_close)
    {
    }

    ~FileDescriptorOwner();

    int fd() const { return m_fd; }

private:
    int m_fd;
    bool m_should_close;
};

enum class PathSegmentType {
    Absolute,
    Relative,
};

// FIXME(LexicalPath): Should this live in AK?
template<PathSegmentType segment_type>
class PathSegment {
public:
    consteval PathSegment(char const* path)
        : m_path(StringView { path, __builtin_strlen(path) })
        , m_is_zero_terminated_literal(true)
    {
        validate_compiletime_path_segment();
    }

    StringView path() const { return m_path; }
    bool is_zero_terminated_literal() const { return m_is_zero_terminated_literal; }

    PathSegment(Badge<Path>, StringView segment)
        : m_path(segment)
    {
    }

private:
    consteval void validate_compiletime_path_segment()
    {
        if (m_path.is_empty())
            AK::compiletime_fail("Path segment cannot be empty");

        if (m_path[m_path.length() - 1] == '/')
            AK::compiletime_fail("Path segment should not end with /");

        if (segment_type == PathSegmentType::Absolute) {
            if (m_path[0] != '/')
                AK::compiletime_fail("Absolute path segment should start with /");
        } else {
            if (m_path[0] == '/')
                AK::compiletime_fail("Relative path segment should not start with /");
        }

        for (size_t i = 1; i <= m_path.length(); ++i) {
            if (i == m_path.length() || m_path[i] == '/') {
                size_t j = i;
                while (j--)
                    if (m_path[j] == '/')
                        break;
                auto part = m_path.substring_view(j + 1, i - j - 1);
                if (part == "")
                    AK::compiletime_fail("Repeated slashes are not allowed");
                if (part == "..")
                    AK::compiletime_fail("Using .. with Core::Path is discouraged, use Core::Path::create_from_string if you _really_ need it");
            }
        }
    }

    StringView m_path;
    bool m_is_zero_terminated_literal = false;
};

using RelativePathSegment = PathSegment<PathSegmentType::Relative>;
using AbsolutePathSegment = PathSegment<PathSegmentType::Absolute>;

#ifdef AK_OS_SERENITY
inline constexpr bool make_paths_zero_terminated = false;
#else
inline constexpr bool make_paths_zero_terminated = true;
#endif

struct RuntimePath {
    RuntimePath(StringView view);
    RuntimePath(ByteBuffer&& buffer);

    StringView view() const;
    void append(RelativePathSegment segment);

    ByteBuffer data; // != "" and "\0", ends with '\0' if not on Serenity, last non-zero byte is not '/'
};

struct CompiletimeConstantPath {
    StringView view() const;

    StringView data; // Has '\0' character past the end, does not end with '/', except if it is "/"
};

struct ForeignCStringPath {
    int dirfd;
    char const* path;
};

// Path constructed from Directory
struct DirectoryPath {
    NonnullRefPtr<FileDescriptorOwner> directory;
};

// Absolute path
template<typename PathStorage>
struct AbsolutePath {
    PathStorage path; // Starts with '/'
};

using AbsoluteCompiletimePath = AbsolutePath<CompiletimeConstantPath>;
using AbsoluteRuntimePath = AbsolutePath<RuntimePath>;

// Path relative to a given directory
template<typename PathStorage>
struct RelativePath {
    NonnullRefPtr<FileDescriptorOwner> directory;
    PathStorage path;
};

using RelativeCompiletimePath = RelativePath<CompiletimeConstantPath>;
using RelativeRuntimePath = RelativePath<RuntimePath>;
using PathDataType = Variant<ForeignCStringPath, AbsoluteRuntimePath, AbsoluteCompiletimePath, RelativeRuntimePath, RelativeCompiletimePath, DirectoryPath>;

}

using Detail::AbsolutePathSegment;
using Detail::RelativePathSegment;

// Conceptually, Core::Path stores a pointer to a filesystem index node. After construction, it does
// not provide any getters except for the last path segment.
//
// The only intended purpose of `Path::last_segment` is to query filename, if a path instance refers
// to a file. For directories, calling `last_segment` might occasionally return . or .. instead of
// an actual directory name.
//
// Internally, Path uses a directory file descriptor and a path string, which is resolved relative
// to the aforementioned directory. Unfortunately, this design still leaves room for potential
// filesystem races if not used carefully. This is the reason why you should treat Path instances
// as an opaque pointers to something non-constant and rely on helpers from LibFileSystem to work
// with a filesystem.
class Path {
public:
    static Path root();

    // For relative paths, resolution is done relative to the initial working directory. "." will
    // be appended for paths ending with "/".
    static ErrorOr<Path> create_from_string(StringView path_string);
    static ErrorOr<Path> create_from_string(StringView path_string, Path const& base);

    // FIXME(LexicalPath): Should there be constructors from LexicalPath?

#ifdef AK_OS_SERENITY
    // NOTE: Should not be called outside of LibC. Caller should guarantee that the string
    //       outlives Path instances. Returned instances will be considered invalid and *will* cause
    //       assertion failures if passed to functions other than in Core::System.
    static Path create_from_c_string_without_copy_in_libc(char const* path);
    static Path create_from_c_string_without_copy_in_libc(int dirfd, char const* path);
#endif

    Path(AbsolutePathSegment path);
    Path(Directory directory);

    // FIXME(LexicalPath): Should this return RelativePathSegment or some "LexicalPathView"?
    StringView last_segment() const;

    // Path is considered "surely" a directory if it is constructed from NNRP<Directory> or using
    // Path::root() or its relative part ends with "/." or "/..".
    bool is_surely_a_directory() const;

    Path operator/(RelativePathSegment segment) const;

    int directory_fd_for_syscall() const;
#ifdef AK_OS_SERENITY
    StringView relative_path_for_syscall() const;
#else
    char const* relative_path_for_syscall() const;
#endif

    bool can_be_considered_standard_stream(Badge<File>) const;

    template<OneOf<Directory, File> VIP>
    Detail::PathDataType const& internal_representation(Badge<VIP>) const
    {
        return m_path;
    }

protected:
    Path(Detail::PathDataType&& path)
        : m_path(move(path))
    {
    }

    Detail::PathDataType m_path;
};

}
