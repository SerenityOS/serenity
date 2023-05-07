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

namespace Core {

///
/// Use of Core::File for reading/writing data is deprecated.
/// Please use Core::File and Core::InputBufferedFile instead.
///
class DeprecatedFile final : public IODevice {
    C_OBJECT(DeprecatedFile)
public:
    virtual ~DeprecatedFile() override;

    static ErrorOr<NonnullRefPtr<DeprecatedFile>> open(DeprecatedString filename, OpenMode, mode_t = 0644);

    DeprecatedString filename() const { return m_filename; }
    void set_filename(const DeprecatedString filename) { m_filename = move(filename); }

    bool is_directory() const;
    bool is_device() const;
    bool is_block_device() const;
    bool is_char_device() const;
    bool is_link() const;
    bool looks_like_shared_library() const;

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

    static ErrorOr<void, CopyError> copy_file(DeprecatedString const& dst_path, struct stat const& src_stat, DeprecatedFile& source, PreserveMode = PreserveMode::Nothing);
    static ErrorOr<void, CopyError> copy_directory(DeprecatedString const& dst_path, DeprecatedString const& src_path, struct stat const& src_stat, LinkMode = LinkMode::Disallowed, PreserveMode = PreserveMode::Nothing);
    static ErrorOr<void, CopyError> copy_file_or_directory(DeprecatedString const& dst_path, DeprecatedString const& src_path, RecursionMode = RecursionMode::Allowed, LinkMode = LinkMode::Disallowed, AddDuplicateFileMarker = AddDuplicateFileMarker::Yes, PreserveMode = PreserveMode::Nothing);

    static DeprecatedString real_path_for(DeprecatedString const& filename);
    static ErrorOr<DeprecatedString> read_link(DeprecatedString const& link_path);

    virtual bool open(OpenMode) override;

    enum class ShouldCloseFileDescriptor {
        No = 0,
        Yes
    };
    bool open(int fd, OpenMode, ShouldCloseFileDescriptor);
    [[nodiscard]] int leak_fd();

    static NonnullRefPtr<DeprecatedFile> standard_input();
    static NonnullRefPtr<DeprecatedFile> standard_output();
    static NonnullRefPtr<DeprecatedFile> standard_error();

    static Optional<DeprecatedString> resolve_executable_from_environment(StringView filename);

private:
    DeprecatedFile(Object* parent = nullptr)
        : IODevice(parent)
    {
    }
    explicit DeprecatedFile(DeprecatedString filename, Object* parent = nullptr);

    bool open_impl(OpenMode, mode_t);

    DeprecatedString m_filename;
    ShouldCloseFileDescriptor m_should_close_file_descriptor { ShouldCloseFileDescriptor::Yes };
};

AK_ENUM_BITWISE_OPERATORS(DeprecatedFile::PreserveMode);

}
