/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Transports/Transport.h"
#include <AK/JsonObject.h>
#include <AK/LexicalPath.h>
#include <LibCore/TCPServer.h>
#include <LibCore/TCPSocket.h>
#include <LibTLS/TLSv12.h>

enum class DirectoryTransferFormat {
    NAME_ONLY,
    LS,
    MLSX
};

class FTPServerClient : public RefCounted<FTPServerClient> {
public:
    static NonnullRefPtr<FTPServerClient> create(u32 id, RefPtr<Core::TCPSocket> socket, AK::JsonObject json_settings)
    {
        return adopt_ref(*new FTPServerClient(id, move(socket), json_settings));
    }

    Function<void()> on_exit;
    Function<void(FTPServerClient*, String)> on_info;
    Function<void(FTPServerClient*, String)> on_receive_command;
    Function<void(FTPServerClient*, String)> on_send_command;
    Function<void(u32, String)> on_data_transfer_start;
    Function<void(u32, u32)> on_data_transfer_update;
    Function<void(u32)> on_data_transfer_end;

    void send_auth_ok();
    void send_auth_security_data(String);
    void send_bad_sequence_of_commands();
    void send_closing_control_connection();
    void send_command_not_implemented_for_parameter();
    void send_command_not_implemented();
    void send_command_not_needed();
    void send_command_unrecognized();
    void send_connection_closed_transfer_aborted();
    void send_current_working_directory();
    void send_data_connection_already_open();
    void send_data_connection_open_no_transfer_in_progress();
    void send_directory_content(String, bool, DirectoryTransferFormat);
    void send_directory_status();
    void send_entering_passive_mode(IPv4Address, u16);
    void send_exceeded_storage_allocation();
    void send_failed_security_check();
    void send_file_action_needs_additional_command();
    void send_file_action_not_taken();
    void send_file_action_ok(String data = "250 Requested file action okay, completed");
    void send_file_action_ok_start(String);
    void send_file_action_ok_stop();
    void send_file_attribute_change_ok();
    void send_file_status(String);
    void send_file_unavailable();
    void send_filename_not_allowed();
    void send_help_message();
    void send_initiating_transfer(String);
    void send_invalid_parameters();
    void send_need_account_for_login();
    void send_need_account_to_store_files();
    void send_not_logged_in();
    void send_ok();
    void send_page_type_unknown();
    void send_request_aborted_local_error();
    void send_request_aborted_not_enough_filesystem_space();
    void send_request_denied_due_to_policy();
    void send_restart_marker();
    void send_security_resource_unavailable();
    void send_service_ready_in_minutes(u32);
    void send_service_unavailable();
    void send_system_info();
    void send_system_status();
    void send_transfer_success();
    void send_unable_to_open_data_connection();
    void send_user_logged_in();
    void send_user_ok_need_password();
    void send_welcome();

    String user() { return m_username.is_empty() ? "NO_USERNAME" : m_username; };
    u32 id() { return m_id; };

protected:
    FTPServerClient(u32 id, RefPtr<Core::TCPSocket> socket, AK::JsonObject json_settings);

    void send(String);
    void handle_command(String);
    void drain_socket();
    RefPtr<Core::TCPSocket> create_data_socket();
    void quit();

private:
    void handle_appe_command(Vector<String>);
    void handle_auth_command(Vector<String>);
    void handle_cdup_command();
    void handle_cwd_command(Vector<String>);
    void handle_dele_command(Vector<String>);
    void handle_feat_command();
    void handle_help_command();
    void handle_list_command(Vector<String>);
    void handle_mdtm_command(Vector<String>);
    void handle_mfct_command();
    void handle_mfmt_command(Vector<String>);
    void handle_mkd_command(Vector<String>);
    void handle_mlsd_command(Vector<String>);
    void handle_mlst_command(Vector<String>);
    void handle_mode_command(Vector<String>);
    void handle_nlst_command(Vector<String>);
    void handle_noop_command();
    void handle_pass_command(Vector<String>);
    void handle_pasv_command();
    void handle_pwd_command();
    void handle_quit_command();
    void handle_rein_command();
    void handle_retr_command(Vector<String>);
    void handle_rmd_command(Vector<String>);
    void handle_rnfr_command(Vector<String>);
    void handle_rnto_command(Vector<String>);
    void handle_site_command();
    void handle_size_command(Vector<String>);
    void handle_stor_command(Vector<String>);
    void handle_stou_command(Vector<String>);
    void handle_stru_command(Vector<String>);
    void handle_syst_command();
    void handle_type_command(Vector<String>);
    void handle_user_command(Vector<String>);
    void handle_xcrc_command(Vector<String>);

    Optional<String> format_to_transfer_format(LexicalPath, DirectoryTransferFormat format = DirectoryTransferFormat::LS);
    Optional<String> format_for_name_only(LexicalPath path);
    Optional<String> format_for_ls(LexicalPath path);
    Optional<String> format_for_mlsx(LexicalPath path);

    ByteBuffer receive_on_transport(int max_size);
    bool send_on_transport(ReadonlyBytes);

    u32 m_id { 0 };
    bool m_should_die { false };
    IPv4Address m_source_address {};

    RefPtr<Core::Socket> m_control_connection;
    RefPtr<Core::TCPServer> m_data_connection;

    TransportType m_transport_type = { TransportType::RAW };
    TransportBase* m_transport;

    String m_username {};
    bool m_is_logged_in { false };
    String m_working_dir {};
    String m_transfer_type { "I" };
    String m_transfer_mode { "S" };
    String m_file_structure { "F" };
    String m_rename_from {};
    Optional<bool> m_is_passive {};

    AK::JsonObject m_json_settings;
};
