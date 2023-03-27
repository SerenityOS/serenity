/*
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/NetworkSettings/CertStoreDialog.h>
#include <Applications/NetworkSettings/CertStoreDialogGML.h>
#include <LibCrypto/ASN1/PEM.h>

namespace NetworkSettings {

NonnullRefPtr<CertStoreModel> CertStoreModel::create() { return adopt_ref(*new CertStoreModel); }

void CertStoreModel::load()
{
    // FIXME: In the future, we will allow users to import their own certificates. To support this, we would need to change this logic
    auto cacert_result = Core::File::open("/etc/cacert.pem"sv, Core::File::OpenMode::Read);
    if (cacert_result.is_error()) {
        dbgln("Failed to load CA Certificates: {}", cacert_result.error());
        return;
    }

    auto cacert_file = cacert_result.release_value();
    auto data_result = cacert_file->read_until_eof();
    if (data_result.is_error()) {
        dbgln("Failed to load CA Certificates: {}", data_result.error());
        return;
    }

    auto data = data_result.release_value();
    auto load_result = DefaultRootCACertificates::the().reload_certificates(data);
    if (load_result.is_error()) {
        dbgln("Failed to load CA Certificates: {}", load_result.error());
        return;
    }

    m_certificates = load_result.release_value();
}

DeprecatedString CertStoreModel::column_name(int column) const
{
    switch (column) {
    case Column::IssuedTo:
        return "Issued To";
    case Column::IssuedBy:
        return "Issued By";
    case Column::Expire:
        return "Expiration Date";
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant CertStoreModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role != GUI::ModelRole::Display)
        return {};
    if (m_certificates.is_empty())
        return {};

    auto cert = m_certificates.at(index.row());

    switch (index.column()) {
    case Column::IssuedTo:
        return cert.subject.subject.is_empty() ? cert.subject.unit : cert.subject.subject;
    case Column::IssuedBy:
        return cert.issuer.subject.is_empty() ? cert.issuer.unit : cert.issuer.subject;
    case Column::Expire:
        return cert.not_after.to_deprecated_string("%Y-%m-%d"sv);
    default:
        VERIFY_NOT_REACHED();
    }

    return {};
}

void CertStoreDialog::open_cert_store()
{
    auto icon = GUI::Icon::default_icon("certificate"sv);
    set_icon(icon.bitmap_for_size(16));
    set_title("Certificate Store");

    m_root_ca_tableview = find_descendant_of_type_named<GUI::TableView>("root_ca_tableview");
    m_root_ca_tableview->set_highlight_selected_rows(true);
    m_root_ca_tableview->set_alternating_row_colors(false);

    m_root_ca_model = CertStoreModel::create();
    m_root_ca_model->load();
    m_root_ca_tableview->set_model(m_root_ca_model);
    m_root_ca_tableview->set_column_width(CertStoreModel::Column::IssuedTo, 150);
    m_root_ca_tableview->set_column_width(CertStoreModel::Column::IssuedBy, 150);

    auto* close_button = find_descendant_of_type_named<GUI::Button>("close_button");
    close_button->on_click = [&](auto) {
        done(ExecResult::OK);
    };

    exec();
}

ErrorOr<NonnullRefPtr<CertStoreDialog>> CertStoreDialog::try_create(Window* parent_window)
{
    auto dialog = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) CertStoreDialog(parent_window)));
    auto widget = TRY(dialog->set_main_widget<GUI::Widget>());

    TRY(widget->load_from_gml(cert_store_dialog_gml));

    return dialog;
}
}
