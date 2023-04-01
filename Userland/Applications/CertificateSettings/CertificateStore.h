/*
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/TableView.h>
#include <LibTLS/Certificate.h>

namespace CertificateSettings {

class CertificateStoreModel : public GUI::Model {
public:
    enum Column {
        IssuedTo,
        IssuedBy,
        Expire,
        __Count
    };

    static NonnullRefPtr<CertificateStoreModel> create();
    virtual ~CertificateStoreModel() override = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return m_certificates.size(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual DeprecatedString column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual ErrorOr<void> load();
    virtual ErrorOr<size_t> add(Vector<Certificate> const&);

private:
    Vector<Certificate> m_certificates;
};

class CertificateStoreWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(CertStoreWidget)
public:
    virtual ~CertificateStoreWidget() override = default;
    static ErrorOr<NonnullRefPtr<CertificateStoreWidget>> try_create();
    virtual void apply_settings() override {};

private:
    CertificateStoreWidget() = default;
    ErrorOr<void> initialize();
    ErrorOr<void> import_pem();

    RefPtr<CertificateStoreModel> m_root_ca_model;
    RefPtr<GUI::TableView> m_root_ca_tableview;
    RefPtr<GUI::Button> m_import_ca_button;
};
}
