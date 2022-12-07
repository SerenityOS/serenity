/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Error.h>
#include <LibCore/IODevice.h>
#include <sys/stat.h>

// FIXME: Make this a bit prettier.
#define DEFAULT_PATH "/usr/local/sbin:/usr/local/bin:/usr/bin:/bin"
#define DEFAULT_PATH_SV "/usr/local/sbin:/usr/local/bin:/usr/bin:/bin"sv

namespace Core {

///
/// Use of Core::File for reading/writing data is deprecated.
/// Please use Core::Stream::File and Core::Stream::BufferedFile instead.
///
class File final : public IODevice {
    C_OBJECT(File)
public:
    virtual ~File() override;

    static ErrorOr<NonnullRefPtr<File>> open(DeprecatedString filename, OpenMode, mode_t = 0644);

    DeprecatedString filename() const { return m_filename; }
    void set_filename(const DeprecatedString filename) { m_filename = move(filename); }

    bool is_directory() const;
    static bool is_directory(DeprecatedString const& filename);

    bool is_device() const;
    static bool is_device(DeprecatedString const& filename);
    bool is_block_device() const;
    static bool is_block_device(DeprecatedString const& filename);
    bool is_char_device() const;
    static bool is_char_device(DeprecatedString const& filename);

    bool is_link() const;
    static bool is_link(DeprecatedString const& filename);

    bool looks_like_shared_library() const;
    static bool looks_like_shared_library(DeprecatedString const& filename);

    static bool exists(StringView filename);
    static ErrorOr<size_t> size(DeprecatedString const& filename);
    static DeprecatedString current_working_directory();
    static DeprecatedString absolute_path(DeprecatedString const& path);

    enum class RecursionMode {
        Allowed,
        Disallowed
    };

    enum class LinkMode {
        Allowed,
        Disallowed
    };

    enum class AddDuplicateFileMarker {
        Yes,
        No,
    };

    enum class PreserveMode {
        Nothing = 0,
        Permissions = (1 << 0),
        Ownership = (1 << 1),
        Timestamps = (1 << 2),
    };

    struct CopyError : public Error {
        CopyError(int error_code, bool t)
            : Error(error_code)
            , tried_recursing(t)
        {
        }
        bool tried_recursing;
    };

    static ErrorOr<void, CopyError> copy_file(DeprecatedString const& dst_path, struct stat const& src_stat, File& source, PreserveMode = PreserveMode::Nothing);
    static ErrorOr<void, CopyError> copy_directory(DeprecatedString const& dst_path, DeprecatedString const& src_path, struct stat const& src_stat, LinkMode = LinkMode::Disallowed, PreserveMode = PreserveMode::Nothing);
    static ErrorOr<void, CopyError> copy_file_or_directory(DeprecatedString const& dst_path, DeprecatedString const& src_path, RecursionMode = RecursionMode::Allowed, LinkMode = LinkMode::Disallowed, AddDuplicateFileMarker = AddDuplicateFileMarker::Yes, PreserveMode = PreserveMode::Nothing);

    static DeprecatedString real_path_for(DeprecatedString const& filename);
    static ErrorOr<DeprecatedString> read_link(DeprecatedString const& link_path);
    static ErrorOr<void> link_file(DeprecatedString const& dst_path, DeprecatedString const& src_path);

    struct RemoveError : public Error {
        RemoveError(DeprecatedString f, int error_code)
            : Error(error_code)
            , file(move(f))
        {
        }
        DeprecatedString file;
    };
    static ErrorOr<void, RemoveError> remove(DeprecatedString const& path, RecursionMode, bool force);

    virtual bool open(OpenMode) override;

    enum class ShouldCloseFileDescriptor {
        No = 0,
        Yes
    };
    bool open(int fd, OpenMode, ShouldCloseFileDescriptor);
    [[nodiscard]] int leak_fd();

    static NonnullRefPtr<File> standard_input();
    static NonnullRefPtr<File> standard_output();
    static NonnullRefPtr<File> standard_error();

    static Optional<DeprecatedString> resolve_executable_from_environment(StringView filename);

private:
    File(Object* parent = nullptr)
        : IODevice(parent)
    {
    }
    explicit File(DeprecatedString filename, Object* parent = nullptr);

    bool open_impl(OpenMode, mode_t);

    DeprecatedString m_filename;
    ShouldCloseFileDescriptor m_should_close_file_descriptor { ShouldCloseFileDescriptor::Yes };
};

AK_ENUM_BITWISE_OPERATORS(File::PreserveMode);

}
