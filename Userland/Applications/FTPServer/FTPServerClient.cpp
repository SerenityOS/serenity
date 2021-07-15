/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "FTPServerClient.h"
#include "Transports/TLS/TLSTransport.h"
#include <AK/MappedFile.h>
#include <LibCore/Account.h>
#include <LibCore/DirIterator.h>
#include <LibCore/File.h>
#include <LibCore/FileStream.h>
#include <LibCrypto/Checksum/CRC32.h>
#include <unistd.h>
#include <utime.h>

#undef FTP_DEBUG
#define FTP_DEBUG 1

FTPServerClient::FTPServerClient(u32 id, RefPtr<Core::TCPSocket> socket, AK::JsonObject json_settings)
    : m_id(id)
    , m_source_address(socket->source_address().ipv4_address())
    , m_control_connection(move(socket))
    , m_json_settings(json_settings)
{
    m_transport = new RawTransport();
    m_control_connection->on_ready_to_read = [this] { drain_socket(); };
}

void FTPServerClient::drain_socket()
{
    NonnullRefPtr<FTPServerClient> protect(*this);

    while (!m_should_die && m_control_connection && m_control_connection->can_read()) {
        if (!m_control_connection || m_control_connection->eof()) {
            quit();
            break;
        }

        auto buf = receive_on_transport(4 * KiB);

        if (!m_transport->is_init()) {
            m_transport->init(buf, m_control_connection);
            continue;
        }

        if (buf.is_empty()) {
            quit();
            break;
        }

        // remove \r\n
        String data = String(buf, AK::NoChomp);
        dbgln_if(FTP_DEBUG, "Received parsed: {}", data.substring_view(0, data.length() - 2));

        data = data.substring(0, data.length() - 2);

        if (on_receive_command)
            on_receive_command(this, data);

        handle_command(data);
    }

    if (!m_control_connection || m_control_connection->eof()) {
        quit();
        return;
    }
}

void FTPServerClient::handle_command(String input)
{
    Vector<String> parts = input.split(' ');
    String command = parts.take_first();

    if (command.equals_ignoring_case("AUTH")) {
        handle_auth_command(parts);
        return;
    }

    if (command.equals_ignoring_case("FEAT")) {
        handle_feat_command();
        return;
    }

    if (command.equals_ignoring_case("PWD") || command.equals_ignoring_case("XPWD")) {
        handle_pwd_command();
        return;
    }

    if (command.equals_ignoring_case("SYST")) {
        handle_syst_command();
        return;
    }

    if (command.equals_ignoring_case("RETR")) {
        handle_retr_command(parts);
        return;
    }

    if (command.equals_ignoring_case("STOR")) {
        handle_stor_command(parts);
        return;
    }

    if (command.equals_ignoring_case("STOU")) {
        handle_stou_command(parts);
        return;
    }

    if (command.equals_ignoring_case("MKD") || command.equals_ignoring_case("XMKD")) {
        handle_mkd_command(parts);
        return;
    }

    if (command.equals_ignoring_case("RMD") || command.equals_ignoring_case("XRMD")) {
        handle_rmd_command(parts);
        return;
    }

    if (command.equals_ignoring_case("DELE")) {
        handle_dele_command(parts);
        return;
    }

    if (command.equals_ignoring_case("CWD") || command.equals_ignoring_case("XCWD")) {
        handle_cwd_command(parts);
        return;
    }

    if (command.equals_ignoring_case("CDUP") || command.equals_ignoring_case("XCUP")) {
        handle_cdup_command();
        return;
    }

    if (command.equals_ignoring_case("LIST")) {
        handle_list_command(parts);
        return;
    }

    if (command.equals_ignoring_case("NLST")) {
        handle_nlst_command(parts);
        return;
    }

    if (command.equals_ignoring_case("TYPE")) {
        handle_type_command(parts);
        return;
    }

    if (command.equals_ignoring_case("STRU")) {
        handle_stru_command(parts);
        return;
    }

    if (command.equals_ignoring_case("PASV")) {
        handle_pasv_command();
        return;
    }

    if (command.equals_ignoring_case("USER")) {
        handle_user_command(parts);
        return;
    }

    if (command.equals_ignoring_case("PASS")) {
        handle_pass_command(parts);
        return;
    }

    if (command.equals_ignoring_case("QUIT")) {
        handle_quit_command();
        return;
    }

    if (command.equals_ignoring_case("APPE")) {
        handle_appe_command(parts);
        return;
    }

    if (command.equals_ignoring_case("RNFR")) {
        handle_rnfr_command(parts);
        return;
    }

    if (command.equals_ignoring_case("RNTO")) {
        handle_rnto_command(parts);
        return;
    }

    if (command.equals_ignoring_case("NOOP")) {
        handle_noop_command();
        return;
    }

    if (command.equals_ignoring_case("REIN")) {
        handle_noop_command();
        return;
    }

    if (command.equals_ignoring_case("MDTM")) {
        handle_mdtm_command(parts);
        return;
    }

    if (command.equals_ignoring_case("SIZE")) {
        handle_size_command(parts);
        return;
    }

    if (command.equals_ignoring_case("MODE")) {
        handle_mode_command(parts);
        return;
    }

    if (command.equals_ignoring_case("XCRC")) {
        handle_xcrc_command(parts);
        return;
    }

    if (command.equals_ignoring_case("MLST")) {
        handle_mlst_command(parts);
        return;
    }

    if (command.equals_ignoring_case("MLSD")) {
        handle_mlsd_command(parts);
        return;
    }

    if (command.equals_ignoring_case("SITE")) {
        handle_site_command();
        return;
    }

    if (command.equals_ignoring_case("MFCT")) {
        handle_mfct_command();
        return;
    }

    dbgln_if(FTP_DEBUG, "Unhandled command: {}", command);

    if (on_info)
        on_info(this, String::formatted("Tried invalid command {} {}\n", command, String::join(" ", parts)));

    send_command_not_implemented();
}

void FTPServerClient::handle_size_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    auto filename = params.at(0);
    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(filename);
    auto path = LexicalPath::canonicalized_path(builder.to_string());

    if (!Core::File::exists(path)) {
        send_file_unavailable();
        return;
    }

    struct stat stat;
    int rc = lstat(builder.to_string().characters(), &stat);
    if (rc < 0) {
        perror("lstat");
        memset(&stat, 0, sizeof(stat));
        send_request_aborted_local_error();
    }

    send_file_status(String::formatted("{}", stat.st_size));
}

void FTPServerClient::handle_mdtm_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    auto filename = params.at(0);
    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(filename);
    auto path = LexicalPath::canonicalized_path(builder.to_string());

    if (!Core::File::exists(path)) {
        send_file_unavailable();
        return;
    }

    struct stat stat;
    int rc = lstat(builder.to_string().characters(), &stat);
    if (rc < 0) {
        perror("lstat");
        memset(&stat, 0, sizeof(stat));
        send_request_aborted_local_error();
    }

    send_file_status(Core::DateTime::from_timestamp(stat.st_mtime).to_string("%Y%m%d%H%M%S"));
}

void FTPServerClient::handle_rein_command()
{
    m_username = {};
    m_is_logged_in = false;
    m_working_dir = { "/" };
    m_transfer_type = { "I" };
    m_transfer_mode = { "S" };
    m_file_structure = { "F" };
    m_rename_from = {};
    m_is_passive = {};

    send_ok();
}

void FTPServerClient::handle_noop_command()
{
    send_ok();
}

void FTPServerClient::handle_rnfr_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    auto old_name = params.at(0);
    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(old_name);
    auto from_path = LexicalPath::canonicalized_path(builder.to_string());

    if (!Core::File::exists(from_path)) {
        send_file_action_not_taken();
        return;
    }

    m_rename_from = from_path;
    send_file_action_needs_additional_command();
    return;
}

void FTPServerClient::handle_rnto_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    if (m_rename_from.is_empty()) {
        send_bad_sequence_of_commands();
        return;
    }

    auto new_name = params.at(0);

    if (!Core::File::exists(m_rename_from)) {
        send_file_action_not_taken();
        return;
    }

    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(new_name);
    auto to_path = LexicalPath::canonicalized_path(builder.to_string());

    if (Core::File::exists(to_path)) {
        send_file_action_not_taken();
        return;
    }

    auto rc = rename(m_rename_from.characters(), to_path.characters());
    if (rc < 0) {
        if (errno == EXDEV) {
            auto result = Core::File::copy_file_or_directory(
                m_rename_from, to_path,
                Core::File::RecursionMode::Allowed,
                Core::File::LinkMode::Disallowed,
                Core::File::AddDuplicateFileMarker::No);

            if (result.is_error()) {
                send_request_aborted_local_error();
                return;
            }
            rc = unlink(m_rename_from.characters());
            if (rc < 0) {
                send_request_aborted_local_error();
                return;
            }
        } else {
            send_request_aborted_local_error();
            return;
        }
    }

    m_rename_from = {};
    send_file_action_ok();
    return;
}

void FTPServerClient::handle_auth_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    String type = params.at(0);
    if (type.equals_ignoring_case("TLS")) {
        // FIXME: Add TLS support (LibTLS needs support for upgrading a socket?)
        // send_command_not_implemented_for_parameter();
        send_auth_ok();
        m_transport_type = TransportType::TLS;
        m_transport = new TLSTransport();
        return;
    }

    if (type.equals_ignoring_case("SSL")) {
        // FIXME: Add SSL support
        send_command_not_implemented_for_parameter();
        return;
    }

    send_command_not_implemented();
    return;
}

void FTPServerClient::handle_stou_command(Vector<String> params)
{
    String file = String::join(" ", params);

    auto connection = create_data_socket();
    if (!connection) {
        return;
    }

    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(file);
    auto path = LexicalPath::canonicalized_path(builder.to_string());

    while (Core::File::exists(path)) {
        builder.append(".1");
        path = LexicalPath::canonicalized_path(builder.to_string());
    }

    send(String::formatted("150 FILE:{}\r\n", path));

    auto outstream = Core::OutputFileStream::open(path);

    if (outstream.is_error()) {
        send_request_aborted_local_error();
        return;
    }

    connection->set_blocking(true);
    send_initiating_transfer(builder.to_string());

    while (true) {
        auto data = connection->read(4 * KiB);

        if (data.size() == 0) {
            break;
        }

        outstream.value().write(data);

        if (on_data_transfer_update)
            on_data_transfer_update(m_id, data.size());
    }

    connection->close();
    send_transfer_success();
}

void FTPServerClient::handle_appe_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    String file = String::join(" ", params);

    auto connection = create_data_socket();
    if (!connection) {
        return;
    }

    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(file);
    auto path = LexicalPath::canonicalized_path(builder.to_string());
    auto outstream = Core::OutputFileStream::open(path);

    if (outstream.is_error()) {
        send_request_aborted_local_error();
        return;
    }

    connection->set_blocking(true);
    send_initiating_transfer(builder.to_string());

    while (true) {
        auto data = connection->read(4 * KiB);

        if (data.size() == 0) {
            break;
        }

        outstream.value().write(data);

        if (on_data_transfer_update)
            on_data_transfer_update(m_id, data.size());
    }

    connection->close();
    send_transfer_success();
}

void FTPServerClient::handle_feat_command()
{
    send_system_status();
}

void FTPServerClient::handle_pwd_command()
{
    send_current_working_directory();
}

void FTPServerClient::handle_syst_command()
{
    send_system_info();
}

void FTPServerClient::handle_retr_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    String file = params.at(0);

    auto connection = create_data_socket();
    if (!connection) {
        return;
    }

    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(file);

    auto stream_or_error = Core::InputFileStream::open(builder.to_string());
    if (stream_or_error.is_error()) {
        send_request_aborted_local_error();
        return;
    }

    auto& stream = stream_or_error.value();

    connection->set_blocking(true);
    send_initiating_transfer(builder.to_string());

    // FIXME: 4*KiB
    auto buffer = ByteBuffer::create_uninitialized(100);
    while (!stream.has_any_error() && buffer.size() > 0) {
        auto nread = stream.read(buffer);
        buffer.resize(nread);
        connection->send(buffer);

        if (on_data_transfer_update)
            on_data_transfer_update(m_id, nread);

        // FIXME: Remove sleep
        sleep(1);
    }

    connection->close();
    send_transfer_success();
}

void FTPServerClient::handle_stor_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    String file = String::join(" ", params);

    auto connection = create_data_socket();
    if (!connection) {
        return;
    }

    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(file);
    auto path = LexicalPath::canonicalized_path(builder.to_string());

    if (Core::File::exists(path)) {
        auto removed_or_error = Core::File::remove(path, Core::File::RecursionMode::Disallowed, false);
        if (removed_or_error.is_error()) {
            send_request_aborted_local_error();
            return;
        }
    }

    auto outstream = Core::OutputFileStream::open(path);

    if (outstream.is_error()) {
        send_request_aborted_local_error();
        return;
    }

    connection->set_blocking(true);
    send_initiating_transfer(builder.to_string());

    while (true) {
        auto data = connection->read(4 * KiB);

        if (data.size() == 0) {
            break;
        }

        outstream.value().write(data);

        if (on_data_transfer_update)
            on_data_transfer_update(m_id, data.size());
    }

    connection->close();
    send_transfer_success();
}

void FTPServerClient::handle_mkd_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    String file = String::join(" ", params);

    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(file);
    auto path = LexicalPath::canonicalized_path(builder.to_string());

    if (mkdir(path.characters(), 0755) < 0) {
        perror("mkdir");
        send_file_action_not_taken();
        return;
    }

    send_file_action_ok();
}

void FTPServerClient::handle_rmd_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    String file = String::join(" ", params);

    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(file);
    auto path = LexicalPath::canonicalized_path(builder.to_string());

    if (rmdir(path.characters()) < 0) {
        perror("rmdir");
        send_file_action_not_taken();
        return;
    }

    send_file_action_ok();
}

void FTPServerClient::handle_dele_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    String file = String::join(" ", params);

    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(file);
    auto path = LexicalPath::canonicalized_path(builder.to_string());

    auto result = Core::File::remove(path, Core::File::RecursionMode::Disallowed, false);
    if (result.is_error()) {
        send_file_action_not_taken();
        return;
    }

    send_file_action_ok();
}

void FTPServerClient::handle_cwd_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_command_not_implemented_for_parameter();
        return;
    }

    if (!params.at(0).starts_with("/")) {
        StringBuilder builder;
        builder.append(m_working_dir);
        builder.append("/");
        builder.append(params.at(0));
        (void)params.take_first();

        params.prepend(builder.to_string());
    }

    String path = String::join(" ", params);

    m_working_dir = LexicalPath::canonicalized_path(path);
    send_file_action_ok();
}

void FTPServerClient::handle_cdup_command()
{
    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/..");
    m_working_dir = LexicalPath::canonicalized_path(builder.to_string());
    send_file_action_ok();
}

void FTPServerClient::handle_list_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_directory_content(m_working_dir, true, DirectoryTransferFormat::LS);
        return;
    }

    String path = String::join(" ", params);

    if (path.starts_with('-')) {
        send_directory_content(m_working_dir, true, DirectoryTransferFormat::LS);
        return;
    }

    send_directory_content(path, true, DirectoryTransferFormat::LS);
}

void FTPServerClient::handle_nlst_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_directory_content(m_working_dir, true, DirectoryTransferFormat::NAME_ONLY);
        return;
    }

    String path = String::join(" ", params);

    if (path.starts_with('-')) {
        send_directory_content(m_working_dir, true, DirectoryTransferFormat::NAME_ONLY);
        return;
    }

    send_directory_content(path, true, DirectoryTransferFormat::NAME_ONLY);
}

void FTPServerClient::handle_type_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_invalid_parameters();
        return;
    }

    String type = params.at(0);

    if (!type.equals_ignoring_case("I") && !type.equals_ignoring_case("A")) {
        send_command_not_implemented_for_parameter();
        return;
    }

    m_transfer_type = type;
    send_ok();
}

void FTPServerClient::handle_stru_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_invalid_parameters();
        return;
    }

    String type = params.at(0);

    if (!type.equals_ignoring_case("F")) {
        send_command_not_implemented_for_parameter();
        return;
    }

    m_file_structure = type;
    send_ok();
}

void FTPServerClient::handle_mode_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_invalid_parameters();
        return;
    }

    String mode = params.at(0);

    if (!mode.equals_ignoring_case("S")) {
        send_command_not_implemented_for_parameter();
        return;
    }

    m_transfer_mode = mode;
    send_ok();
}

void FTPServerClient::handle_xcrc_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_invalid_parameters();
        return;
    }

    auto filename = params.at(0);
    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(filename);
    auto path = LexicalPath::canonicalized_path(builder.to_string());

    if (!Core::File::exists(path)) {
        send_file_unavailable();
        return;
    }

    auto stream_or_error = Core::InputFileStream::open(path);
    if (stream_or_error.is_error()) {
        send_request_aborted_local_error();
        return;
    }

    auto& stream = stream_or_error.value();

    auto crc = Crypto::Checksum::CRC32();
    auto buffer = ByteBuffer::create_uninitialized(4 * KiB);
    while (!stream.has_any_error() && buffer.size() > 0) {
        auto nread = stream.read(buffer);
        buffer.resize(nread);
        crc.update(buffer);
    }

    send_file_action_ok(String::formatted("{}", crc.digest()));
}

void FTPServerClient::handle_pasv_command()
{

    m_data_connection = Core::TCPServer::construct();
    m_data_connection->set_blocking(true);

    while (!m_data_connection->listen(m_source_address, 0)) {
        if (on_receive_command)
            on_receive_command(this, String::formatted("Failed to open passive socket on {}:{} failed, port taken?", m_source_address, m_data_connection->local_port().value()));
    }

    if (!m_data_connection->is_listening()) {
        send_request_aborted_local_error();
        return;
    }

    m_is_passive = true;

    if (on_info)
        on_info(this, String::formatted("Opened passive socket on {}:{}", m_source_address, m_data_connection->local_port().value()));

    send_entering_passive_mode(m_data_connection->local_address().value(), m_data_connection->local_port().value());
}

void FTPServerClient::handle_user_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_invalid_parameters();
        return;
    }

    m_username = params.at(0);

    send_user_ok_need_password();
}

void FTPServerClient::handle_pass_command(Vector<String> params)
{
    if (m_is_logged_in) {
        send_command_not_needed();
        return;
    }

    if (params.size() < 1) {
        send_invalid_parameters();
        return;
    }

    if (m_username.is_empty()) {
        send_bad_sequence_of_commands();
        return;
    }

    auto password = params.at(0);

    auto allow_anonymous = m_json_settings.get("allow_anonymous_logins").to_bool(false);
    if (!allow_anonymous) {
        if (m_username.equals_ignoring_case("anonymous")) {
            send_not_logged_in();
            return;
        }

        auto result = Core::Account::from_name(m_username.characters());
        if (result.is_error()) {
            send_not_logged_in();
            return;
        }

        if (!result.value().authenticate(password.characters())) {
            send_not_logged_in();
            return;
        }
    }

    auto default_work_dir = m_json_settings.get("default_home_directory").as_string_or("/");
    auto users_settings = m_json_settings.get("users").as_object();
    if (users_settings.has(m_username)) {
        auto current_user_settings = users_settings.get(m_username).as_object();
        m_working_dir = current_user_settings.get("home_directory").as_string_or(default_work_dir);
    }

    m_is_logged_in = true;
    send_user_logged_in();
}

void FTPServerClient::handle_quit_command()
{
    quit();
}

void FTPServerClient::handle_mlst_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_directory_content(m_working_dir, false, DirectoryTransferFormat::MLSX);
        return;
    }

    String path = String::join(" ", params);

    if (path.starts_with('-')) {
        send_directory_content(m_working_dir, false, DirectoryTransferFormat::MLSX);
        return;
    }

    send_directory_content(path, false, DirectoryTransferFormat::MLSX);
}

void FTPServerClient::handle_mlsd_command(Vector<String> params)
{
    if (params.size() < 1) {
        send_directory_content(m_working_dir, true, DirectoryTransferFormat::MLSX);
        return;
    }

    String path = String::join(" ", params);

    if (path.starts_with('-')) {
        send_directory_content(m_working_dir, true, DirectoryTransferFormat::MLSX);
        return;
    }

    send_directory_content(path, true, DirectoryTransferFormat::MLSX);
}

void FTPServerClient::handle_site_command()
{
    send_command_not_needed();
}

void FTPServerClient::handle_help_command()
{
    send_help_message();
}

void FTPServerClient::handle_mfct_command()
{
    send_command_not_needed();
}

void FTPServerClient::handle_mfmt_command(Vector<String> params)
{
    if (params.size() < 2) {
        send_invalid_parameters();
        return;
    }

    auto time = params.at(0);
    auto file = params.at(1);

    StringBuilder builder;
    builder.append(m_working_dir);
    builder.append("/");
    builder.append(file);
    auto path = LexicalPath::canonicalized_path(builder.to_string());

    if (!Core::File::exists(path)) {
        send_file_unavailable();
        return;
    }

    auto new_time = Core::DateTime::parse("%Y%m%d%H%M%S", time);
    if (!new_time.has_value()) {
        send_invalid_parameters();
    }

    utimbuf buf = { new_time->timestamp(), new_time->timestamp() };
    if (utime(path.characters(), &buf) < 0) {
        perror("utime");
        send_request_aborted_local_error();
    }

    send_file_attribute_change_ok();
}

void FTPServerClient::quit()
{
    if (m_control_connection)
        m_control_connection->close();

    // TODO: close the data connection?

    if (on_exit)
        on_exit();

    m_should_die = true;
}

void FTPServerClient::send_directory_content(String path, bool use_data_socket, DirectoryTransferFormat format)
{
    RefPtr<Core::TCPSocket> connection;

    if (use_data_socket) {
        connection = create_data_socket();
        if (!connection) {
            send_request_aborted_local_error();
            return;
        }

        connection->set_blocking(true);
        send_initiating_transfer(path);
    }

    dbgln_if(FTP_DEBUG, "Sending content of directory \"{}\"", path);

    Core::DirIterator di(path, Core::DirIterator::SkipDots);
    while (di.has_next()) {
        String name = di.next_path();

        StringBuilder builder;
        builder.append(path);
        builder.append('/');
        builder.append(name);

        auto path = LexicalPath(builder.to_string());
        auto data = format_to_transfer_format(path, format);

        if (!data.has_value()) {
            send_request_aborted_local_error();
            return;
        }

        auto& data_value = data.value();

        dbgln_if(FTP_DEBUG, "{}", data_value);

        if (use_data_socket) {
            connection->send(data_value.bytes());
        } else {
            send(data_value);
        }
    }

    if (use_data_socket) {
        connection->close();
        send_transfer_success();
    }
}

RefPtr<Core::TCPSocket> FTPServerClient::create_data_socket()
{
    if (!m_is_passive.has_value()) {
        send_unable_to_open_data_connection();
        return {};
    }

    auto is_passive = m_is_passive.value();

    if (!is_passive) {
        TODO();
        return {};
    }

    auto connection = m_data_connection->accept();
    if (!connection) {
        perror("accept");
        send_unable_to_open_data_connection();
        return {};
    }

    connection->set_blocking(true);
    return connection;
}

Optional<String> FTPServerClient::format_to_transfer_format(LexicalPath path, DirectoryTransferFormat format)
{
    switch (format) {
    case DirectoryTransferFormat::LS:
        return format_for_ls(path);
    case DirectoryTransferFormat::NAME_ONLY:
        return format_for_name_only(path);
    case DirectoryTransferFormat::MLSX:
        return format_for_mlsx(path);
    default:
        VERIFY_NOT_REACHED();
    }
}

Optional<String> FTPServerClient::format_for_name_only(LexicalPath path)
{
    return path.basename();
}

Optional<String> FTPServerClient::format_for_ls(LexicalPath path)
{
    struct stat st;
    int rc = lstat(path.string().characters(), &st);
    if (rc < 0) {
        perror("lstat");
        memset(&st, 0, sizeof(st));
        return {};
    }

    StringBuilder builder;

    if (S_ISDIR(st.st_mode))
        builder.append("d");
    else if (S_ISLNK(st.st_mode))
        builder.append("l");
    else if (S_ISBLK(st.st_mode))
        builder.append("b");
    else if (S_ISCHR(st.st_mode))
        builder.append("c");
    else if (S_ISFIFO(st.st_mode))
        builder.append("f");
    else if (S_ISSOCK(st.st_mode))
        builder.append("s");
    else if (S_ISREG(st.st_mode))
        builder.append("-");
    else
        builder.append("?");

    builder.appendff("{}{}{}{}{}{}{}{}",
        st.st_mode & S_IRUSR ? 'r' : '-',
        st.st_mode & S_IWUSR ? 'w' : '-',
        st.st_mode & S_ISUID ? 's' : (st.st_mode & S_IXUSR ? 'x' : '-'),
        st.st_mode & S_IRGRP ? 'r' : '-',
        st.st_mode & S_IWGRP ? 'w' : '-',
        st.st_mode & S_ISGID ? 's' : (st.st_mode & S_IXGRP ? 'x' : '-'),
        st.st_mode & S_IROTH ? 'r' : '-',
        st.st_mode & S_IWOTH ? 'w' : '-');

    if (st.st_mode & S_ISVTX)
        builder.append("t");
    else
        builder.appendff("{}", st.st_mode & S_IXOTH ? 'x' : '-');

    builder.appendff(" {}", st.st_nlink);
    builder.appendff(" {}", st.st_uid);
    builder.appendff(" {}", st.st_gid);

    if (S_ISCHR(st.st_mode) || S_ISBLK(st.st_mode)) {
        builder.appendff("  {},{} ", major(st.st_rdev), minor(st.st_rdev));
    } else {
        builder.appendff(" {} ", (uint64_t)st.st_size);
    }

    builder.appendff("  {}  ", Core::DateTime::from_timestamp(st.st_mtime).to_string("%h %d  %Y"));

    builder.append(path.basename());
    builder.append("\r\n");

    return builder.to_string();
}

Optional<String> FTPServerClient::format_for_mlsx(LexicalPath path)
{
    StringBuilder builder;

    struct stat stat;
    int rc = lstat(path.string().characters(), &stat);
    if (rc < 0) {
        perror("lstat");
        memset(&stat, 0, sizeof(stat));
        return {};
    }

    // FIXME: split into file, dir, cdir, pdir
    builder.appendff("Type={};", S_ISDIR(stat.st_mode) ? "dir" : "file");

    builder.appendff("Size={};", (uint64_t)stat.st_size);
    builder.appendff("Modify={};", Core::DateTime::from_timestamp(stat.st_mtime).to_string("%Y%m%d%H%M%S"));
    builder.appendff("Unique={};", stat.st_ino);

    /*
    a + type=file;  append is ok
    c + type=dir;   store is ok
    d;              delete is ok
    e + type=dir;   CD to the dir is ok
    f;              rename is ok
    l + type=dir;   listing files is ok
    m + type=dir;   create new dir is ok
    p + type=dir;   directory contents can be deleted
    r + type=file;  file can be downloaded
    w + type=file;  file can be uploaded
    */
    // FIXME: Currently every client has the same permissions as the main thread, so we just fake this for now
    // Note: This does not imply the actions are guaranteed to work, just that it might.
    builder.appendff("Perm={};", "acdeflmprw");

    return builder.to_string();
}

void FTPServerClient::send_restart_marker()
{
    // 110
    TODO();
}

void FTPServerClient::send_service_ready_in_minutes([[maybe_unused]] u32 minutes)
{
    // 120
    TODO();
}

void FTPServerClient::send_data_connection_already_open()
{
    send("125 Data connection already opened; transfer starting\r\n");
}

void FTPServerClient::send_initiating_transfer(String path)
{
    if (on_data_transfer_start)
        on_data_transfer_start(m_id, path);
    send("150 File status okay; about to open data connection\r\n");
}

void FTPServerClient::send_ok()
{
    send("200 OK\r\n");
}

void FTPServerClient::send_command_not_needed()
{
    send("202 Command not implemented, superfluous at this site\r\n");
}

void FTPServerClient::send_system_status()
{
    send("211 System status, or system help reply\r\n");
}

void FTPServerClient::send_directory_status()
{
    send("212 Directory status\r\n");
}

void FTPServerClient::send_file_status(String status)
{
    send(String::formatted("213 {}\r\n", status));
}

void FTPServerClient::send_help_message()
{
    send("214 System status, or system help reply\r\n");
}

void FTPServerClient::send_system_info()
{
    send("215 SerenityOS\r\n");
}

void FTPServerClient::send_welcome()
{
    send("220 Ready\r\n");
}

void FTPServerClient::send_closing_control_connection()
{
    send("221 Service closing control connection\r\n");
}

void FTPServerClient::send_data_connection_open_no_transfer_in_progress()
{
    send("225 Data connection open; no transfer in progress\r\n");
}

void FTPServerClient::send_transfer_success()
{
    if (on_data_transfer_end)
        on_data_transfer_end(m_id);
    send("226 Closing data connection; transfer ok\r\n");
}

void FTPServerClient::send_entering_passive_mode(IPv4Address address, u16 port)
{
    StringBuilder builder;
    builder.appendff("227 Entering Passive Mode ({},{},{},{},{},{})\r\n", address[0], address[1], address[2], address[3], port >> 8, port & 0xFF);
    send(builder.to_string());
}

void FTPServerClient::send_user_logged_in()
{
    send("230 User logged in\r\n");
}

void FTPServerClient::send_auth_ok()
{
    send("234 AUTH command OK. Initializing connection\r\n");
}

void FTPServerClient::send_file_action_ok_start(String data)
{
    send(String::formatted("250- {}\r\n", data));
}

void FTPServerClient::send_file_action_ok(String data)
{
    send(String::formatted("250 {}\r\n", data));
}

void FTPServerClient::send_file_action_ok_stop()
{
    send("250 End\r\n");
}

void FTPServerClient::send_file_attribute_change_ok()
{
    send("253 Attributes changed ok.\r\n");
}

void FTPServerClient::send_current_working_directory()
{
    StringBuilder builder;
    builder.appendff("257 \"{}\"\r\n", m_working_dir);
    send(builder.to_string());
}

void FTPServerClient::send_user_ok_need_password()
{
    send("331 Username okay, need password\r\n");
}

void FTPServerClient::send_need_account_for_login()
{
    send("332 Need account for login\r\n");
}

void FTPServerClient::send_auth_security_data(String base64data)
{
    StringBuilder builder;
    builder.append("334 ");
    builder.append("[ADAT=");
    builder.append(base64data);
    builder.append("]\r\n");
    send(builder.to_string());
}

void FTPServerClient::send_file_action_needs_additional_command()
{
    send("350 Requested file action pending further information\r\n");
}

void FTPServerClient::send_service_unavailable()
{
    send("421 Service not available, closing control connection\r\n");
}

void FTPServerClient::send_unable_to_open_data_connection()
{
    send("425 Unable to open data connection\r\n");
}

void FTPServerClient::send_connection_closed_transfer_aborted()
{
    send("426 Connection closed; transfer aborted\r\n");
}

void FTPServerClient::send_security_resource_unavailable()
{
    send("431 Need unavailable resource to process security\r\n");
}

void FTPServerClient::send_file_action_not_taken()
{
    send("450 Requested file action not taken\r\n");
}

void FTPServerClient::send_request_aborted_local_error()
{
    send("451 Requested action aborted: local error in processing\r\n");
}

void FTPServerClient::send_request_aborted_not_enough_filesystem_space()
{
    send("452 Requested action not taken; insufficient storage space\r\n");
}

void FTPServerClient::send_command_unrecognized()
{
    send("500 Syntax error, command unrecognized\r\n");
}

void FTPServerClient::send_invalid_parameters()
{
    send("501 Syntax error in parameters or argument\r\n");
}

void FTPServerClient::send_command_not_implemented()
{
    send("502 Command not implemented\r\n");
}

void FTPServerClient::send_bad_sequence_of_commands()
{
    send("503 Bad sequence of commands\r\n");
}

void FTPServerClient::send_command_not_implemented_for_parameter()
{
    send("504 Command not implemented for that parameter\r\n");
}

void FTPServerClient::send_not_logged_in()
{
    send("530 Not logged in\r\n");
}

void FTPServerClient::send_need_account_to_store_files()
{
    send("532 Need account for storing files\r\n");
}

void FTPServerClient::send_request_denied_due_to_policy()
{
    send("534 Request denied for policy reasons\r\n");
}

void FTPServerClient::send_failed_security_check()
{
    send("535 Failed security check\r\n");
}

void FTPServerClient::send_file_unavailable()
{
    send("550 Requested action not taken; file unavailable\r\n");
}

void FTPServerClient::send_page_type_unknown()
{
    send("551 Requested action aborted: page type unknown\r\n");
}

void FTPServerClient::send_exceeded_storage_allocation()
{
    send("552 Requested file action aborted; Exceeded storage allocation\r\n");
}

void FTPServerClient::send_filename_not_allowed()
{
    send("553 Requested action not taken; File name not allowed\r\n");
}

void FTPServerClient::send(String data)
{
    dbgln_if(FTP_DEBUG, "Sending: {}", data.substring_view(0, data.length() - 2));

    if (on_send_command)
        on_send_command(this, data);

    if (!m_control_connection) {
        quit();
        return;
    }

    send_on_transport(data.bytes());
}

bool FTPServerClient::send_on_transport(ReadonlyBytes data)
{
    return m_transport->send(data, m_control_connection);
}

ByteBuffer FTPServerClient::receive_on_transport(int max_size)
{
    return m_transport->receive(max_size, m_control_connection);
}
