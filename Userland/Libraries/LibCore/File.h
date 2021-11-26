/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/String.h>
#include <LibCore/IODevice.h>
#include <sys/stat.h>

namespace Core {

class File final : public IODevice {
    C_OBJECT(File)
public:
    virtual ~File() override;

    static ErrorOr<NonnullRefPtr<File>> open(String filename, OpenMode, mode_t = 0644);

    String filename() const { return m_filename; }
    void set_filename(const String filename) { m_filename = move(filename); }

    bool is_directory() const;
    static bool is_directory(String const& filename);

    bool is_device() const;
    static bool is_device(String const& filename);

    bool is_link() const;
    static bool is_link(String const& filename);

    bool looks_like_shared_library() const;
    static bool looks_like_shared_library(String const& filename);

    static bool exists(String const& filename);
    static ErrorOr<size_t> size(String const& filename);
    static bool ensure_parent_directories(String const& path);
    static String current_working_directory();
    static String absolute_path(String const& path);

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
        Nothing,
        PermissionsOwnershipTimestamps,
    };

    struct CopyError : public Error {
        CopyError(int error_code, bool t)
            : Error(error_code)
            , tried_recursing(t)
        {
        }
        bool tried_recursing;
    };

    static ErrorOr<void, CopyError> copy_file(String const& dst_path, struct stat const& src_stat, File& source, PreserveMode = PreserveMode::Nothing);
    static ErrorOr<void, CopyError> copy_directory(String const& dst_path, String const& src_path, struct stat const& src_stat, LinkMode = LinkMode::Disallowed, PreserveMode = PreserveMode::Nothing);
    static ErrorOr<void, CopyError> copy_file_or_directory(String const& dst_path, String const& src_path, RecursionMode = RecursionMode::Allowed, LinkMode = LinkMode::Disallowed, AddDuplicateFileMarker = AddDuplicateFileMarker::Yes, PreserveMode = PreserveMode::Nothing);

    static String real_path_for(String const& filename);
    static String read_link(String const& link_path);
    static ErrorOr<void> link_file(String const& dst_path, String const& src_path);

    struct RemoveError : public Error {
        RemoveError(String f, int error_code)
            : Error(error_code)
            , file(move(f))
        {
        }
        String file;
    };
    static ErrorOr<void, RemoveError> remove(String const& path, RecursionMode, bool force);

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

private:
    File(Object* parent = nullptr)
        : IODevice(parent)
    {
    }
    explicit File(String filename, Object* parent = nullptr);

    bool open_impl(OpenMode, mode_t);

    String m_filename;
    ShouldCloseFileDescriptor m_should_close_file_descriptor { ShouldCloseFileDescriptor::Yes };
};

}
