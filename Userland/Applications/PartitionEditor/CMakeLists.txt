serenity_component(
    PartitionEditor
    TARGETS PartitionEditor
)

stringify_gml(PartitionEditorWindow.gml PartitionEditorWindowGML.h partition_editor_window_gml)

set(SOURCES
    main.cpp
    PartitionModel.cpp
)

set(GENERATED_SOURCES
    PartitionEditorWindowGML.h
)

serenity_app(PartitionEditor ICON app-partition-editor)
target_link_libraries(PartitionEditor PRIVATE LibCore LibFileSystem LibGfx LibMain LibGUI LibPartition)
