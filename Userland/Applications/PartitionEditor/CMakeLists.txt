serenity_component(
    PartitionEditor
    TARGETS PartitionEditor
)

set(SOURCES
    main.cpp
    PartitionModel.cpp
)

serenity_app(PartitionEditor ICON app-partition-editor)
target_link_libraries(PartitionEditor PRIVATE LibCore LibFileSystem LibGfx LibMain LibGUI LibPartition)
