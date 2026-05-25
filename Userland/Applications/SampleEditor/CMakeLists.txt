serenity_component(
    SampleEditor
    RECOMMENDED
    TARGETS SampleEditor
)

set(SOURCES
    SampleFile.cpp
    SampleBlock.cpp
    SampleDocument.cpp
    SamplePainter.cpp
    SampleEditorPalette.cpp
    SampleWidget.cpp
    MainWidget.cpp
    main.cpp
)

serenity_app(SampleEditor ICON app-sample-editor)

target_link_libraries(SampleEditor PRIVATE LibAudio LibCore LibDesktop LibFileSystem LibFileSystemAccessClient LibGfx LibGUI LibIPC LibMain LibConfig LibThreading LibURL)
