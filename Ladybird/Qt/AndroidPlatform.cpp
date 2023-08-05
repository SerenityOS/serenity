/*
 * Copyright (c) 2022, Andrew Kaster <akaster@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/LexicalPath.h>
#include <AK/Platform.h>
#include <AK/ScopeGuard.h>
#include <LibArchive/Tar.h>
#include <LibArchive/TarStream.h>
#include <LibCompress/Gzip.h>
#include <LibCore/Directory.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibMain/Main.h>

#include <QCoreApplication>
#include <QJniObject>
#include <QSslSocket>

#ifndef AK_OS_ANDROID
#    error This file is for Android only, check CMake config!
#endif

// HACK ALERT, we need to include LibMain manually here because the Qt build system doesn't include LibMain.a in the actual executable,
//    nor include it in libladybird_<arch>.so
#include <LibMain/Main.cpp> // NOLINT(bugprone-suspicious-include)

extern DeprecatedString s_serenity_resource_root;

void android_platform_init();
static void extract_ladybird_resources();
static ErrorOr<void> extract_tar_archive(String archive_file, DeprecatedString output_directory);

void android_platform_init()
{
    qDebug() << "Device supports OpenSSL: " << QSslSocket::supportsSsl();

    QJniObject res = QJniObject::callStaticMethod<jstring>("org/serenityos/ladybird/TransferAssets",
        "transferAssets",
        "(Landroid/content/Context;)Ljava/lang/String;",
        QNativeInterface::QAndroidApplication::context());
    s_serenity_resource_root = res.toString().toUtf8().data();

    extract_ladybird_resources();
}

void extract_ladybird_resources()
{
    qDebug() << "serenity resource root is " << s_serenity_resource_root.characters();
    auto file_or_error = Core::System::open(DeprecatedString::formatted("{}/res/icons/16x16/app-browser.png", s_serenity_resource_root), O_RDONLY);
    if (file_or_error.is_error()) {
        qDebug() << "Unable to open test file file as expected, extracting asssets...";

        MUST(extract_tar_archive(MUST(String::formatted("{}/ladybird-assets.tar", s_serenity_resource_root)), s_serenity_resource_root));
    } else {
        qDebug() << "Opened app-browser.png test file, good to go!";
        qDebug() << "Hopefully no developer changed the asset files and expected them to be re-extracted!";
    }
}

ErrorOr<void> extract_tar_archive(String archive_file, DeprecatedString output_directory)
{
    constexpr size_t buffer_size = 4096;

    auto file = TRY(Core::InputBufferedFile::create(TRY(Core::File::open(archive_file, Core::File::OpenMode::Read))));

    DeprecatedString old_pwd = TRY(Core::System::getcwd());

    TRY(Core::System::chdir(output_directory));
    ScopeGuard go_back = [&old_pwd] { MUST(Core::System::chdir(old_pwd)); };

    auto tar_stream = TRY(Archive::TarInputStream::construct(make<Compress::GzipCompressor>(move(file))));

    HashMap<DeprecatedString, DeprecatedString> global_overrides;
    HashMap<DeprecatedString, DeprecatedString> local_overrides;

    auto get_override = [&](StringView key) -> Optional<DeprecatedString> {
        Optional<DeprecatedString> maybe_local = local_overrides.get(key);

        if (maybe_local.has_value())
            return maybe_local;

        Optional<DeprecatedString> maybe_global = global_overrides.get(key);

        if (maybe_global.has_value())
            return maybe_global;

        return {};
    };

    while (!tar_stream->finished()) {
        Archive::TarFileHeader const& header = tar_stream->header();

        // Handle meta-entries earlier to avoid consuming the file content stream.
        if (header.content_is_like_extended_header()) {
            switch (header.type_flag()) {
            case Archive::TarFileType::GlobalExtendedHeader: {
                TRY(tar_stream->for_each_extended_header([&](StringView key, StringView value) {
                    if (value.length() == 0)
                        global_overrides.remove(key);
                    else
                        global_overrides.set(key, value);
                }));
                break;
            }
            case Archive::TarFileType::ExtendedHeader: {
                TRY(tar_stream->for_each_extended_header([&](StringView key, StringView value) {
                    local_overrides.set(key, value);
                }));
                break;
            }
            default:
                warnln("Unknown extended header type '{}' of {}", (char)header.type_flag(), header.filename());
                VERIFY_NOT_REACHED();
            }

            TRY(tar_stream->advance());
            continue;
        }

        Archive::TarFileStream file_stream = tar_stream->file_contents();

        // Handle other header types that don't just have an effect on extraction.
        switch (header.type_flag()) {
        case Archive::TarFileType::LongName: {
            StringBuilder long_name;

            Array<u8, buffer_size> buffer;

            while (!file_stream.is_eof()) {
                auto slice = TRY(file_stream.read_some(buffer));
                long_name.append(reinterpret_cast<char*>(slice.data()), slice.size());
            }

            local_overrides.set("path", long_name.to_deprecated_string());
            TRY(tar_stream->advance());
            continue;
        }
        default:
            // None of the relevant headers, so continue as normal.
            break;
        }

        LexicalPath path = LexicalPath(header.filename());
        if (!header.prefix().is_empty())
            path = path.prepend(header.prefix());
        DeprecatedString filename = get_override("path"sv).value_or(path.string());

        DeprecatedString absolute_path = TRY(FileSystem::absolute_path(filename)).to_deprecated_string();
        auto parent_path = LexicalPath(absolute_path).parent();
        auto header_mode = TRY(header.mode());

        switch (header.type_flag()) {
        case Archive::TarFileType::NormalFile:
        case Archive::TarFileType::AlternateNormalFile: {
            MUST(Core::Directory::create(parent_path, Core::Directory::CreateDirectories::Yes));

            int fd = TRY(Core::System::open(absolute_path, O_CREAT | O_WRONLY, header_mode));

            Array<u8, buffer_size> buffer;
            while (!file_stream.is_eof()) {
                auto slice = TRY(file_stream.read_some(buffer));
                TRY(Core::System::write(fd, slice));
            }

            TRY(Core::System::close(fd));
            break;
        }
        case Archive::TarFileType::SymLink: {
            MUST(Core::Directory::create(parent_path, Core::Directory::CreateDirectories::Yes));

            TRY(Core::System::symlink(header.link_name(), absolute_path));
            break;
        }
        case Archive::TarFileType::Directory: {
            MUST(Core::Directory::create(parent_path, Core::Directory::CreateDirectories::Yes));

            auto result_or_error = Core::System::mkdir(absolute_path, header_mode);
            if (result_or_error.is_error() && result_or_error.error().code() != EEXIST)
                return result_or_error.release_error();
            break;
        }
        default:
            // FIXME: Implement other file types
            warnln("file type '{}' of {} is not yet supported", (char)header.type_flag(), header.filename());
            VERIFY_NOT_REACHED();
        }

        // Non-global headers should be cleared after every file.
        local_overrides.clear();

        TRY(tar_stream->advance());
    }
    return {};
}
