/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Model.h>
#include <QAbstractItemModel>

namespace Ladybird {

class ModelTranslator final : public QAbstractItemModel {
    Q_OBJECT
public:
    virtual ~ModelTranslator() override;

    void set_underlying_model(RefPtr<GUI::Model> model)
    {
        beginResetModel();
        m_model = model;
        endResetModel();
    }

    RefPtr<GUI::Model> underlying_model()
    {
        return m_model;
    }

    virtual int columnCount(QModelIndex const& parent) const override;
    virtual int rowCount(QModelIndex const& parent) const override;
    virtual QVariant data(QModelIndex const&, int role) const override;
    virtual QModelIndex index(int row, int column, QModelIndex const& parent) const override;
    virtual QModelIndex parent(QModelIndex const& index) const override;

    QModelIndex to_qt(GUI::ModelIndex const&) const;
    GUI::ModelIndex to_gui(QModelIndex const&) const;

private:
    RefPtr<GUI::Model> m_model;
};

}
