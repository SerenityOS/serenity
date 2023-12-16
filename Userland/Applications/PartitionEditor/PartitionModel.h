/*
 * Copyright (c) 2022, Samuel Bowman <sam@sambowman.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibCore/File.h>
#include <LibGUI/Model.h>
#include <LibPartition/PartitionTable.h>

namespace PartitionEditor {

class PartitionModel final : public GUI::Model {
public:
    enum Column {
        Partition,
        StartBlock,
        EndBlock,
        TotalBlocks,
        Size,
        __Count,
    };

    static NonnullRefPtr<PartitionModel> create();
    virtual ~PartitionModel() override = default;

    virtual int row_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return m_partition_table->partitions_count(); }
    virtual int column_count(GUI::ModelIndex const& = GUI::ModelIndex()) const override { return Column::__Count; }
    virtual ErrorOr<String> column_name(int) const override;
    virtual GUI::Variant data(GUI::ModelIndex const&, GUI::ModelRole) const override;

    ErrorOr<void> set_device_path(ByteString const&);

private:
    PartitionModel() = default;

    OwnPtr<Partition::PartitionTable> m_partition_table;
    OwnPtr<Core::File> m_backing_file;
};

}
