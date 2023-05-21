set(SOURCES
    DiskPartitionMetadata.cpp
    EBRPartitionTable.cpp
    GUIDPartitionTable.cpp
    MBRPartitionTable.cpp
    PartitionableDevice.cpp
    PartitionTable.cpp
)

serenity_lib(LibPartition partition)
target_link_libraries(LibPartition PRIVATE LibCore)
