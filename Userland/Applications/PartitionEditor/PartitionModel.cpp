/*
 * Copyright (c) 2022, Samuel Bowman <sam@sambowman.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Applications/PartitionEditor/PartitionModel.h>
#include <LibPartition/EBRPartitionTable.h>
#include <LibPartition/GUIDPartitionTable.h>
#include <LibPartition/MBRPartitionTable.h>

namespace PartitionEditor {

String PartitionModel::column_name(int column) const
{
    switch (column) {
    case Column::Partition:
        return "Partition";
    case Column::StartBlock:
        return "Start Block";
    case Column::EndBlock:
        return "End Block";
    default:
        VERIFY_NOT_REACHED();
    }
}

GUI::Variant PartitionModel::data(GUI::ModelIndex const& index, GUI::ModelRole role) const
{
    if (role != GUI::ModelRole::Display)
        return {};
    if (!m_partition_table)
        return {};

    auto optional_partition = m_partition_table->partition(index.row());
    if (optional_partition.has_value()) {
        auto partition = optional_partition.release_value();

        switch (index.column()) {
        case Column::Partition:
            return index.row() + 1;
        case Column::StartBlock:
            return partition.start_block();
        case Column::EndBlock:
            return partition.end_block();
        default:
            VERIFY_NOT_REACHED();
        }
    }

    return {};
}

ErrorOr<void> PartitionModel::set_device_path(String const& path)
{
    auto file = TRY(Core::File::open(path, Core::OpenMode::ReadOnly));

    auto mbr_table_or_error = Partition::MBRPartitionTable::try_to_initialize(file);
    if (!mbr_table_or_error.is_error()) {
        dbgln("Found MBR partition table on {}", path);
        m_partition_table = move(mbr_table_or_error.value());
        invalidate();
        return {};
    }

    auto ebr_table_or_error = Partition::EBRPartitionTable::try_to_initialize(file);
    if (!ebr_table_or_error.is_error()) {
        dbgln("Found EBR partition table on {}", path);
        m_partition_table = move(ebr_table_or_error.value());
        invalidate();
        return {};
    }

    auto guid_table_or_error = Partition::GUIDPartitionTable::try_to_initialize(file);
    if (!guid_table_or_error.is_error()) {
        dbgln("Found GUID partition table on {}", path);
        m_partition_table = move(guid_table_or_error.value());
        invalidate();
        return {};
    }

    return Error::from_errno(ENOTSUP);
}

}
