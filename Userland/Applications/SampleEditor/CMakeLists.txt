serenity_component(
    SampleEditor
    REQUIRED
    TARGETS SampleEditor
)

set(SOURCES
    SampleSourceFile.cpp
    SampleEditorPalette.cpp
    SampleBlock.cpp
    SampleFileBlock.cpp
    SampleBlockContainer.cpp
    SampleRenderer.cpp
    SampleWidget.cpp
    MainWidget.cpp
    main.cpp
)

serenity_app(SampleEditor ICON app-sample-editor)

target_link_libraries(SampleEditor PRIVATE LibAudio LibCore LibDesktop LibFileSystem LibFileSystemAccessClient LibGfx LibGUI LibIPC LibMain LibConfig LibURL)
