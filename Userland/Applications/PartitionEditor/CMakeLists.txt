serenity_component(
    PartitionEditor
    TARGETS PartitionEditor
)

compile_gml(PartitionEditorWindow.gml PartitionEditorWindowGML.h partition_editor_window_gml)

set(SOURCES
    main.cpp
    PartitionEditorWindowGML.h
    PartitionModel.cpp
)

serenity_app(PartitionEditor ICON app-space-analyzer)
target_link_libraries(PartitionEditor LibMain LibGUI LibPartition)
