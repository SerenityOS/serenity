/*
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CertificateStoreWidget.h"
#include <AK/String.h>
#include <LibCore/DateTime.h>
#include <LibCrypto/ASN1/PEM.h>
#include <LibFileSystem/FileSystem.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/SortingProxyModel.h>

namespace CertificateSettings {

NonnullRefPtr<CertificateStoreModel> CertificateStoreModel::create()
{
    return adopt_ref(*new CertificateStoreModel);
}

void CertificateStoreProxyModel::sort(int column, GUI::SortOrder sort_order)
{
    SortingProxyModel::sort(column, sort_order);
    m_parent_table_view->set_column_width(CertificateStoreModel::Column::IssuedTo, 150);
    m_parent_table_view->set_column_width(CertificateStoreModel::Column::IssuedBy, 150);
}

ErrorOr<void> CertificateStoreModel::load()
{
    m_certificates = TRY(DefaultRootCACertificates::load_certificates());

    return {};
}

ErrorOr<String> CertificateStoreModel::column_name(int column) const
{
    switch (column) {
    case Column::IssuedTo:
        return "Issued To"_string;
    case Column::IssuedBy:
        return "Issued By"_string;
    case Column::Expire:
        return "Expiration Date"_string;
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant CertificateStoreModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role != GUI::ModelRole::Display)
        return {};
    if (m_certificates.is_empty())
        return {};

    auto cert = m_certificates.at(index.row());

    switch (index.column()) {
    case Column::IssuedTo: {
        auto issued_to = cert.subject.common_name();
        if (issued_to.is_empty()) {
            issued_to = cert.subject.organizational_unit();
        }

        return issued_to;
    }
    case Column::IssuedBy: {
        auto issued_by = cert.issuer.common_name();
        if (issued_by.is_empty()) {
            issued_by = cert.issuer.organizational_unit();
        }

        return issued_by;
    }
    case Column::Expire:
        return Core::DateTime::from_timestamp(cert.validity.not_after.seconds_since_epoch()).to_byte_string("%Y-%m-%d"sv);
    default:
        VERIFY_NOT_REACHED();
    }

    return {};
}

ErrorOr<size_t> CertificateStoreModel::add(Vector<Certificate> const& certificates)
{
    auto size = m_certificates.size();
    TRY(m_certificates.try_extend(certificates));
    return m_certificates.size() - size;
}

ErrorOr<void> CertificateStoreWidget::import_pem()
{
    FileSystemAccessClient::OpenFileOptions options {
        .window_title = "Import"sv,
        .allowed_file_types = Vector {
            GUI::FileTypeFilter { "Certificate Files", { { "pem", "crt" } } },
        },
    };
    auto fsac_result = FileSystemAccessClient::Client::the().open_file(window(), options);
    if (fsac_result.is_error())
        return {};

    auto fsac_file = fsac_result.release_value();
    auto data = TRY(fsac_file.release_stream()->read_until_eof());
    auto count = TRY(m_root_ca_model->add(TRY(DefaultRootCACertificates::parse_pem_root_certificate_authorities(data))));

    if (count == 0) {
        return Error::from_string_view("No valid CA found to import."sv);
    }

    auto cert_file = TRY(Core::File::open(TRY(String::formatted("{}/.config/certs.pem", Core::StandardPaths::home_directory())), Core::File::OpenMode::Write | Core::File::OpenMode::Append));
    TRY(cert_file->write_until_depleted(data.bytes()));
    cert_file->close();

    m_root_ca_model->invalidate();
    m_root_ca_tableview->set_column_width(CertificateStoreModel::Column::IssuedTo, 150);
    m_root_ca_tableview->set_column_width(CertificateStoreModel::Column::IssuedBy, 150);

    GUI::MessageBox::show(window(), TRY(String::formatted("Successfully imported {} CAs.", count)), "Success"sv);

    return {};
}

Certificate CertificateStoreModel::get(int index)
{
    return m_certificates.at(index);
}

ErrorOr<void> CertificateStoreWidget::export_pem()
{
    auto index = m_root_ca_tableview->selection().first();
    auto real_index = m_root_ca_proxy_model->map_to_source(index);
    auto cert = m_root_ca_model->get(real_index.row());

    String filename = cert.subject.common_name();
    if (filename.is_empty()) {
        filename = cert.subject.organizational_unit();
    }

    filename = TRY(filename.replace(" "sv, "_"sv, ReplaceMode::All));

    auto file = FileSystemAccessClient::Client::the().save_file(window(), filename.to_byte_string(), "pem"sv);
    if (file.is_error())
        return {};

    auto data = TRY(Crypto::encode_pem(cert.original_asn1));

    TRY(file.release_value().release_stream()->write_until_depleted(data));

    return {};
}

ErrorOr<void> CertificateStoreWidget::initialize()
{
    m_root_ca_tableview = find_descendant_of_type_named<GUI::TableView>("root_ca_tableview");
    m_root_ca_tableview->set_highlight_selected_rows(true);
    m_root_ca_tableview->set_alternating_row_colors(false);

    m_root_ca_model = CertificateStoreModel::create();
    m_root_ca_proxy_model = TRY(CertificateStoreProxyModel::create(*m_root_ca_model, *m_root_ca_tableview));
    m_root_ca_proxy_model->set_sort_role(GUI::ModelRole::Display);
    TRY(m_root_ca_model->load());
    m_root_ca_tableview->set_model(m_root_ca_proxy_model);
    m_root_ca_tableview->set_column_width(CertificateStoreModel::Column::IssuedTo, 150);
    m_root_ca_tableview->set_column_width(CertificateStoreModel::Column::IssuedBy, 150);

    m_root_ca_tableview->on_selection_change = [&]() {
        m_export_ca_button->set_enabled(m_root_ca_tableview->selection().size() == 1);
    };

    m_import_ca_button = find_descendant_of_type_named<GUI::Button>("import_button");
    m_import_ca_button->on_click = [&](auto) {
        auto import_result = import_pem();
        if (import_result.is_error()) {
            GUI::MessageBox::show_error(window(), ByteString::formatted("{}", import_result.release_error()));
        }
    };

    m_export_ca_button = find_descendant_of_type_named<GUI::Button>("export_button");
    m_export_ca_button->on_click = [&](auto) {
        auto export_result = export_pem();
        if (export_result.is_error()) {
            GUI::MessageBox::show_error(window(), ByteString::formatted("{}", export_result.release_error()));
        }
    };

    return {};
}
}
