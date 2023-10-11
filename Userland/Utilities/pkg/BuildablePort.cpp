/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "BuildablePort.h"
#include <AK/LexicalPath.h>
#include <AK/NonnullRefPtr.h>
#include <LibCore/File.h>
#include <LibCore/System.h>
#include <LibCrypto/BigInt/UnsignedBigInteger.h>
#include <LibCrypto/Hash/SHA2.h>
#include <LibFileSystem/FileSystem.h>
#include <LibLine/Editor.h>
#include <Shell/AST.h>
#include <Shell/NodeVisitor.h>
#include <Shell/Parser.h>
#include <Shell/PosixParser.h>
#include <Shell/Shell.h>
#include <unistd.h>

static ErrorOr<Vector<PortInputFile>> parse_file_list(Span<String> raw_file_list)
{
    Vector<PortInputFile> file_list;
    file_list.ensure_capacity(raw_file_list.size());

    for (auto const& raw_file : raw_file_list) {
        auto url = URL { raw_file };
        if (!url.is_valid())
            return Error::from_string_view("Invalid input file URL"sv);

        auto maybe_fragment = url.fragment();
        if (!maybe_fragment.has_value())
            return Error::from_string_view("Input file is missing a hash or git revision"sv);
        auto fragment = maybe_fragment.release_value();

        if (url.scheme() == "https" || url.scheme() == "http") {
            auto bigint_hash = Crypto::UnsignedBigInteger::from_base(16, fragment);
            VERIFY(bigint_hash.length() * Crypto::UnsignedBigInteger::BITS_IN_WORD / 8 == Crypto::Hash::SHA256::digest_size());
            Crypto::Hash::SHA256::DigestType hash;
            bigint_hash.export_data({ &hash.data, hash.data_length() });
            url.set_fragment({});
            file_list.append({ .url = move(url), .type_specific_info = PortInputFile::HTTPInfo { hash } });
        } else if (url.scheme() == "git+https") {
            url.set_fragment({});
            url.set_scheme("https"_string);
            file_list.append({ .url = move(url), .type_specific_info = PortInputFile::GitInfo { move(fragment) } });
        } else {
            return Error::from_string_view("Unsupported input file scheme"sv);
        }
    }

    return file_list;
}

ErrorOr<BuildablePort> BuildablePort::from_available_port(AvailablePort const& port)
{
    auto port_root = port.local_port_root();
    if (!FileSystem::is_directory(port_root.string()))
        return Error::from_string_view("Port is not available in the local file system"sv);

    auto package_base_path = LexicalPath::join(port_root.string(), "package.sh"sv);
    if (!FileSystem::exists(package_base_path.string()))
        return Error::from_string_view("Port is missing a package.sh script, it may be an empty directory"sv);

    auto shell = Shell::Shell::construct(true);
    {
        // FIXME: Find a nicer way of disabling Shell's output.
        auto old_stdout = TRY(Core::System::dup(STDOUT_FILENO));
        auto old_stderr = TRY(Core::System::dup(STDERR_FILENO));
        auto null_device = TRY(Core::System::open("/dev/null"sv, O_RDWR));
        TRY(Core::System::dup2(null_device, STDOUT_FILENO));
        TRY(Core::System::dup2(null_device, STDERR_FILENO));
        ScopeGuard restore_stdio { [&] {
            MUST(Core::System::dup2(old_stdout, STDOUT_FILENO));
            MUST(Core::System::dup2(old_stderr, STDERR_FILENO));
        } };

        auto was_successful = shell->run_file(package_base_path.string());
        if (!was_successful)
            return Error::from_string_view("Port package.sh couldn't be executed."sv);
    }

    BuildablePort buildable_port;
    buildable_port.m_absolute_path = port_root;

    buildable_port.m_name = TRY(String::from_deprecated_string(TRY(shell->local_variable_or("port"sv, ""))));
    if (buildable_port.m_name.is_empty())
        return Error::from_string_view("Port name is not a string"sv);

    buildable_port.m_version = TRY(String::from_deprecated_string(TRY(shell->local_variable_or("version"sv, ""))));
    if (buildable_port.m_version.is_empty())
        return Error::from_string_view("Port version is not a string"sv);

    // Dependencies and files list are not required, unlike version and port name.
    auto dependencies = TRY(shell->look_up_local_variable("depends"sv));
    if (dependencies != nullptr && dependencies->is_list())
        buildable_port.m_dependencies = TRY(const_cast<Shell::AST::Value&>(*dependencies).resolve_as_list(shell));

    auto files = TRY(shell->look_up_local_variable("files"sv));
    if (files != nullptr && files->is_list())
        buildable_port.m_input_files = TRY(parse_file_list(TRY(const_cast<Shell::AST::Value&>(*files).resolve_as_list(shell))));

    return buildable_port;
}
