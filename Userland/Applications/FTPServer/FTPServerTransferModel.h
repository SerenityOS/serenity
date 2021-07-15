/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/Model.h>

class FTPServer;

class FTPServerTransferModel final : public GUI::Model {
public:
    enum Column {
        Client,
        File,
        Bytes
    };

    static NonnullRefPtr<FTPServerTransferModel> create(FTPServer& server) { return adopt_ref(*new FTPServerTransferModel(server)); }
    virtual ~FTPServerTransferModel() override;

    virtual int row_count(const GUI::ModelIndex&) const override;
    virtual int column_count(const GUI::ModelIndex&) const override;
    virtual String column_name(int column) const override;
    virtual GUI::Variant data(const GUI::ModelIndex&, GUI::ModelRole) const override;
    virtual void update() override;

private:
    explicit FTPServerTransferModel(FTPServer&);

    FTPServer& m_server;
};
