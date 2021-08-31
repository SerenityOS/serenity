serenity_component(
    3DFileViewer
    RECOMMENDED
    TARGETS 3DFileViewer
)

set(SOURCES
    main.cpp
)

serenity_app(3DFileViewer ICON app-3d-file-viewer)
target_link_libraries(3DFileViewer LibGUI LibGL LibThreeDee)
