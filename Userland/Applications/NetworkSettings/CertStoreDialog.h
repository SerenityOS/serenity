/*
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>
#include <LibGUI/Model.h>
#include <LibGUI/TableView.h>
#include <LibTLS/Certificate.h>

namespace NetworkSettings {

class CertStoreModel : public GUI::Model {
public:
    enum Column {
        IssuedTo,
        IssuedBy,
        Expire,
        __Count
    };

    static NonnullRefPtr<CertStoreModel> create();
    virtual ~CertStoreModel() override = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return m_certificates.size(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual DeprecatedString column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;
    virtual void load();

private:
    Vector<Certificate> m_certificates;
};

class CertStoreDialog final : public GUI::Dialog {
    C_OBJECT(CertStoreDialog)
public:
    virtual ~CertStoreDialog() override = default;
    static ErrorOr<NonnullRefPtr<CertStoreDialog>> try_create(Window* parent_window);

    void open_cert_store();

private:
    CertStoreDialog(Window* parent_window)
        : Dialog(parent_window) {};

    RefPtr<CertStoreModel> m_root_ca_model;
    RefPtr<GUI::TableView> m_root_ca_tableview;
};
}
