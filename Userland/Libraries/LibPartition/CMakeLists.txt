set(SOURCES
    DiskPartitionMetadata.cpp
    EBRPartitionTable.cpp
    GUIDPartitionTable.cpp
    MBRPartitionTable.cpp
    PartitionTable.cpp
)

serenity_lib(LibPartition partition)
target_link_libraries(LibPartition LibCore)
