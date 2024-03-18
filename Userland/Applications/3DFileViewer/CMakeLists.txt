serenity_component(
    3DFileViewer
    RECOMMENDED
    TARGETS 3DFileViewer
    DEPENDS FileSystemAccessServer
)

set(SOURCES
    Mesh.cpp
    WavefrontOBJLoader.cpp
    main.cpp
)

serenity_app(3DFileViewer ICON app-3d-file-viewer)
target_link_libraries(3DFileViewer PRIVATE LibCore LibDesktop LibGfx LibGUI LibGL LibFileSystemAccessClient LibMain LibURL)
