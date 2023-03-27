/*
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CertificateStore.h"
#include <Applications/CertificateSettings/CertificateStoreGML.h>
#include <LibCrypto/ASN1/PEM.h>

namespace CertificateSettings {

NonnullRefPtr<CertificateStoreModel> CertificateStoreModel::create() { return adopt_ref(*new CertificateStoreModel); }

ErrorOr<void> CertificateStoreModel::load()
{
    // FIXME: In the future, we will allow users to import their own certificates. To support this, we would need to change this logic
    auto cacert_file = TRY(Core::File::open("/etc/cacert.pem"sv, Core::File::OpenMode::Read));
    auto data = TRY(cacert_file->read_until_eof());
    m_certificates = TRY(DefaultRootCACertificates::the().reload_certificates(data));

    return {};
}

DeprecatedString CertificateStoreModel::column_name(int column) const
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

GUI::Variant CertificateStoreModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
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

ErrorOr<NonnullRefPtr<CertificateStoreWidget>> CertificateStoreWidget::try_create()
{
    auto widget = TRY(adopt_nonnull_ref_or_enomem(new (nothrow) CertificateStoreWidget()));
    TRY(widget->initialize());
    return widget;
}

ErrorOr<void> CertificateStoreWidget::initialize()
{
    TRY(load_from_gml(certificate_store_gml));

    m_root_ca_tableview = find_descendant_of_type_named<GUI::TableView>("root_ca_tableview");
    m_root_ca_tableview->set_highlight_selected_rows(true);
    m_root_ca_tableview->set_alternating_row_colors(false);

    m_root_ca_model = CertificateStoreModel::create();
    TRY(m_root_ca_model->load());
    m_root_ca_tableview->set_model(m_root_ca_model);
    m_root_ca_tableview->set_column_width(CertificateStoreModel::Column::IssuedTo, 150);
    m_root_ca_tableview->set_column_width(CertificateStoreModel::Column::IssuedBy, 150);

    return {};
}
}
