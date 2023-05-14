/*
 * Copyright (c) 2022, Samuel Bowman <sam@sambowman.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NumberFormat.h>
#include <Applications/PartitionEditor/PartitionModel.h>
#include <LibCore/DeprecatedFile.h>
#include <LibPartition/EBRPartitionTable.h>
#include <LibPartition/GUIDPartitionTable.h>
#include <LibPartition/MBRPartitionTable.h>

namespace PartitionEditor {

NonnullRefPtr<PartitionModel> PartitionModel::create()
{
    return adopt_ref(*new PartitionModel);
}

String PartitionModel::column_name(int column) const
{
    switch (column) {
    case Column::Partition:
        return "Partition"_string.release_value_but_fixme_should_propagate_errors();
    case Column::StartBlock:
        return "Start Block"_string.release_value_but_fixme_should_propagate_errors();
    case Column::EndBlock:
        return "End Block"_string.release_value_but_fixme_should_propagate_errors();
    case Column::TotalBlocks:
        return "Total Blocks"_string.release_value_but_fixme_should_propagate_errors();
    case Column::Size:
        return "Size"_short_string;
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
        case Column::TotalBlocks:
            return partition.end_block() - partition.start_block() + 1;
        case Column::Size:
            return human_readable_size((partition.end_block() - partition.start_block() + 1) * m_partition_table->block_size());
        default:
            VERIFY_NOT_REACHED();
        }
    }

    return {};
}

ErrorOr<void> PartitionModel::set_device_path(DeprecatedString const& path)
{
    auto file = TRY(Core::DeprecatedFile::open(path, Core::OpenMode::ReadOnly));

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
