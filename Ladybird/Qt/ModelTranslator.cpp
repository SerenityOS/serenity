/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ModelTranslator.h"
#include "StringUtils.h"
#include <QIcon>

namespace Ladybird {

ModelTranslator::~ModelTranslator() = default;

int ModelTranslator::columnCount(QModelIndex const& parent) const
{
    if (!m_model)
        return 0;
    return m_model->column_count(to_gui(parent));
}

int ModelTranslator::rowCount(QModelIndex const& parent) const
{
    if (!m_model)
        return 0;
    return m_model->row_count(to_gui(parent));
}

static QVariant convert_variant(GUI::Variant const& value)
{
    if (value.is_string())
        return qstring_from_ak_deprecated_string(value.as_string());
    if (value.is_icon()) {
        auto const& gui_icon = value.as_icon();
        auto bitmap = gui_icon.bitmap_for_size(16);
        VERIFY(bitmap);
        auto qt_image = QImage(bitmap->scanline_u8(0), 16, 16, QImage::Format_ARGB32);
        QIcon qt_icon;
        qt_icon.addPixmap(QPixmap::fromImage(qt_image.convertToFormat(QImage::Format::Format_ARGB32_Premultiplied)));
        return qt_icon;
    }
    return {};
}

QVariant ModelTranslator::data(QModelIndex const& index, int role) const
{
    VERIFY(m_model);
    switch (role) {
    case Qt::DisplayRole:
        return convert_variant(m_model->data(to_gui(index), GUI::ModelRole::Display));
    case Qt::DecorationRole:
        return convert_variant(m_model->data(to_gui(index), GUI::ModelRole::Icon));
    default:
        return {};
    }
}

QModelIndex ModelTranslator::index(int row, int column, QModelIndex const& parent) const
{
    VERIFY(m_model);
    return to_qt(m_model->index(row, column, to_gui(parent)));
}

QModelIndex ModelTranslator::parent(QModelIndex const& index) const
{
    VERIFY(m_model);
    return to_qt(m_model->parent_index(to_gui(index)));
}

QModelIndex ModelTranslator::to_qt(GUI::ModelIndex const& index) const
{
    if (!index.is_valid())
        return {};
    return createIndex(index.row(), index.column(), index.internal_data());
}

GUI::ModelIndex ModelTranslator::to_gui(QModelIndex const& index) const
{
    VERIFY(m_model);
    if (!index.isValid())
        return {};
    return m_model->unsafe_create_index(index.row(), index.column(), index.internalPointer());
}

}
