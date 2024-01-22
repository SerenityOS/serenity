/*
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <LibGUI/SettingsWindow.h>
#include <LibGUI/SortingProxyModel.h>
#include <LibGUI/TableView.h>
#include <LibTLS/Certificate.h>

namespace CertificateSettings {

class CertificateStoreProxyModel : public GUI::SortingProxyModel {
public:
    static ErrorOr<NonnullRefPtr<CertificateStoreProxyModel>> create(NonnullRefPtr<Model> source, NonnullRefPtr<GUI::TableView> view)
    {
        return adopt_nonnull_ref_or_enomem(new (nothrow) CertificateStoreProxyModel(move(source), move(view)));
    }
    virtual void sort(int column, GUI::SortOrder) override;

private:
    CertificateStoreProxyModel(NonnullRefPtr<Model> source, NonnullRefPtr<GUI::TableView> view)
        : SortingProxyModel(move(source))
        , m_parent_table_view(move(view)) {};

    NonnullRefPtr<GUI::TableView> m_parent_table_view;
};

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
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual ErrorOr<void> load();
    virtual ErrorOr<size_t> add(Vector<Certificate> const&);
    virtual Certificate get(int index);

private:
    Vector<Certificate> m_certificates;
};

class CertificateStoreWidget : public GUI::SettingsWindow::Tab {
    C_OBJECT_ABSTRACT(CertStoreWidget)
public:
    virtual ~CertificateStoreWidget() override = default;
    static ErrorOr<NonnullRefPtr<CertificateStoreWidget>> try_create();
    ErrorOr<void> initialize();
    virtual void apply_settings() override {};

private:
    CertificateStoreWidget() = default;

    ErrorOr<void> import_pem();
    ErrorOr<void> export_pem();

    RefPtr<CertificateStoreModel> m_root_ca_model;
    RefPtr<CertificateStoreProxyModel> m_root_ca_proxy_model;
    RefPtr<GUI::TableView> m_root_ca_tableview;
    RefPtr<GUI::Button> m_import_ca_button;
    RefPtr<GUI::Button> m_export_ca_button;
};
}
