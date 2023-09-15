/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <AK/Stream.h>
#include <LibCore/ArgsParser.h>
#include <LibCore/EventLoop.h>
#include <LibCore/File.h>
#include <LibCore/Proxy.h>
#include <LibCore/System.h>
#include <LibFileSystem/FileSystem.h>
#include <LibHTTP/HttpResponse.h>
#include <LibMain/Main.h>
#include <LibMarkdown/Document.h>
#include <LibMarkdown/Table.h>
#include <LibProtocol/Request.h>
#include <LibProtocol/RequestClient.h>

#include "AvailablePort.h"
#include "MarkdownTableFinder.h"

#include <Shell/AST.h>
#include <Shell/Formatter.h>
#include <Shell/NodeVisitor.h>
#include <Shell/PosixParser.h>
#include <Shell/Shell.h>

static bool is_installed(HashMap<String, InstalledPort>& installed_ports_database, StringView package_name)
{
    auto port = installed_ports_database.find(package_name);
    return port != installed_ports_database.end();
}

static Optional<AvailablePort&> find_port_package(HashMap<String, AvailablePort>& available_ports, StringView package_name)
{
    auto port = available_ports.find(package_name);
    if (port == available_ports.end())
        return {};
    return port->value;
}

ErrorOr<int> AvailablePort::query_details_for_package(HashMap<String, AvailablePort>& available_ports, HashMap<String, InstalledPort>& installed_ports, StringView package_name, bool verbose)
{
    auto possible_available_port = find_port_package(available_ports, package_name);
    if (!possible_available_port.has_value()) {
        outln("pkg: No match for queried name \"{}\"", package_name);
        return 0;
    }

    auto& available_port = possible_available_port.release_value();

    outln("{}: {}, {}", available_port.name(), available_port.version(), available_port.website());
    if (verbose) {
        out("Installed: ");
        if (is_installed(installed_ports, package_name))
            outln("Yes");
        else
            outln("No");
    }
    return 0;
}

static Optional<Markdown::Table::Column const&> get_column_in_table(Markdown::Table const& ports_table, StringView column_name)
{
    for (auto& column : ports_table.columns()) {
        if (column_name == column.header.render_for_terminal())
            return column;
    }
    return {};
}

ErrorOr<int> AvailablePort::update_available_ports_list_file()
{
    if (auto error_or_void = Core::System::mkdir("/usr/Ports/"sv, 0655); error_or_void.is_error()) {
        if (error_or_void.error().code() != EEXIST)
            return error_or_void.release_error();
    }
    if (!Core::System::access("/usr/Ports/AvailablePorts.md"sv, R_OK).is_error() && FileSystem::remove("/usr/Ports/AvailablePorts.md"sv, FileSystem::RecursionMode::Disallowed).is_error()) {
        outln("pkg: /usr/Ports/AvailablePorts.md exists, but can't delete it before updating it!");
        return 0;
    }
    RefPtr<Protocol::Request> request;
    auto protocol_client = TRY(Protocol::RequestClient::try_create());
    HashMap<DeprecatedString, DeprecatedString, CaseInsensitiveStringTraits> request_headers;
    Core::ProxyData proxy_data {};

    auto output_stream = TRY(Core::File::open("/usr/Ports/AvailablePorts.md"sv, Core::File::OpenMode::ReadWrite, 0644));
    Core::EventLoop loop;

    URL url("https://raw.githubusercontent.com/SerenityOS/serenity/master/Ports/AvailablePorts.md");
    DeprecatedString method = "GET";
    outln("pkg: Syncing packages database...");
    request = protocol_client->start_request(method, url, request_headers, ReadonlyBytes {}, proxy_data);
    request->on_finish = [&](bool success, auto) {
        if (!success)
            outln("pkg: Syncing packages database failed.");
        else
            outln("pkg: Syncing packages database done.");
        loop.quit(success ? 0 : 1);
    };
    request->stream_into(*output_stream);
    return loop.exec();
}

static ErrorOr<String> extract_port_name_from_column(Markdown::Table::Column const& column, size_t row_index)
{
    struct : public Markdown::Visitor {
        virtual RecursionDecision visit(Markdown::Text::LinkNode const& node) override
        {
            text_node = node.text.ptr();
            return RecursionDecision::Break;
        }

    public:
        Markdown::Text::Node* text_node;
    } text_node_find_visitor;

    column.rows[row_index].walk(text_node_find_visitor);
    VERIFY(text_node_find_visitor.text_node);
    StringBuilder string_builder;
    text_node_find_visitor.text_node->render_for_raw_print(string_builder);
    return string_builder.to_string();
}

ErrorOr<HashMap<String, AvailablePort>> AvailablePort::read_available_ports_list()
{
    if (Core::System::access("/usr/Ports/AvailablePorts.md"sv, R_OK).is_error()) {
        warnln("pkg: /usr/Ports/AvailablePorts.md doesn't exist, did you run pkg -u first?");
        return Error::from_errno(ENOENT);
    }
    auto available_ports_file = TRY(Core::File::open("/usr/Ports/AvailablePorts.md"sv, Core::File::OpenMode::Read, 0600));
    auto content_buffer = TRY(available_ports_file->read_until_eof());
    auto content = StringView(content_buffer);
    auto document = Markdown::Document::parse(content);
    auto finder = MarkdownTableFinder::analyze(*document);
    if (finder.table_count() != 1)
        return Error::from_string_literal("Invalid tables count in /usr/Ports/AvailablePorts.md");

    VERIFY(finder.tables()[0]);
    auto possible_port_name_column = get_column_in_table(*finder.tables()[0], "Port"sv);
    auto possible_port_version_column = get_column_in_table(*finder.tables()[0], "Version"sv);
    auto possible_port_website_column = get_column_in_table(*finder.tables()[0], "Website"sv);

    if (!possible_port_name_column.has_value())
        return Error::from_string_literal("pkg: Port column not found /usr/Ports/AvailablePorts.md");
    if (!possible_port_version_column.has_value())
        return Error::from_string_literal("pkg: Version column not found /usr/Ports/AvailablePorts.md");
    if (!possible_port_website_column.has_value())
        return Error::from_string_literal("pkg: Website column not found /usr/Ports/AvailablePorts.md");

    auto& port_name_column = possible_port_name_column.release_value();
    auto& port_version_column = possible_port_version_column.release_value();
    auto& port_website_column = possible_port_website_column.release_value();

    VERIFY(port_name_column.rows.size() == port_version_column.rows.size());
    VERIFY(port_version_column.rows.size() == port_website_column.rows.size());

    HashMap<String, AvailablePort> available_ports;
    for (size_t port_index = 0; port_index < port_name_column.rows.size(); port_index++) {
        auto name = TRY(extract_port_name_from_column(port_name_column, port_index));
        auto website = TRY(String::from_deprecated_string(port_website_column.rows[port_index].render_for_terminal()));
        if (website.is_empty())
            website = "n/a"_string;

        auto version = TRY(String::from_deprecated_string(port_version_column.rows[port_index].render_for_terminal()));
        if (version.is_empty())
            version = "n/a"_string;

        TRY(available_ports.try_set(name, AvailablePort { name, version, website }));
    }
    return available_ports;
}
