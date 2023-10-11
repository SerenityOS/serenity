/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AvailablePort.h"
#include <AK/LexicalPath.h>
#include <AK/String.h>
#include <AK/URL.h>
#include <AK/Utf8View.h>
#include <AK/Variant.h>
#include <LibCrypto/Hash/SHA2.h>

struct PortInputFile {
    URL url;

    struct GitInfo {
        String revision;
    };
    struct HTTPInfo {
        Crypto::Hash::SHA256::DigestType hash;
    };

    Variant<GitInfo, HTTPInfo> type_specific_info;

    bool is_git() const { return type_specific_info.has<GitInfo>(); }
    bool is_http() const { return type_specific_info.has<HTTPInfo>(); }
};

class BuildablePort {
public:
    // Requires the port to be downloaded and accessible locally.
    static ErrorOr<BuildablePort> from_available_port(AvailablePort const& port);

    Utf8View name() const { return m_name.code_points(); }
    Utf8View version() const { return m_version.code_points(); }
    ReadonlySpan<String> dependencies() const { return m_dependencies.span(); }
    ReadonlySpan<PortInputFile> input_files() const { return m_input_files.span(); }

private:
    BuildablePort() = default;

    String m_name;
    LexicalPath m_absolute_path { "" };
    String m_version;
    Vector<String> m_dependencies;
    Vector<PortInputFile> m_input_files;
};

namespace AK {

template<>
struct Formatter<PortInputFile> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, PortInputFile const& property_descriptor)
    {
        return Formatter<StringView>::format(builder,
            TRY(String::formatted("{} ({})", property_descriptor.url,
                TRY(property_descriptor.type_specific_info.visit(
                    [](PortInputFile::GitInfo const& info) { return String::formatted("Git, rev {}", info.revision); },
                    [](PortInputFile::HTTPInfo const& info) { return String::formatted("HTTP, SHA256 {}", info.hash); })))));
    }
};

}
