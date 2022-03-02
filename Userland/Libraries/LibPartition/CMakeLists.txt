set(SOURCES
    DiskPartitionMetadata.cpp
    EBRPartitionTable.cpp
    MBRPartitionTable.cpp
    PartitionTable.cpp
)

serenity_lib(LibPartition partition)
