serenity_component(
    Profiler
    RECOMMENDED
    TARGETS Profiler
)

set(SOURCES
        DisassemblyModel.cpp
        main.cpp
        IndividualSampleModel.cpp
        FlameGraphView.cpp
        FilesystemEventModel.cpp
        Gradient.cpp
        Process.cpp
        Profile.cpp
        ProfileModel.cpp
        SamplesModel.cpp
        SignpostsModel.cpp
        SourceModel.cpp
        TimelineContainer.cpp
        TimelineHeader.cpp
        TimelineTrack.cpp
        TimelineView.cpp
        )

serenity_app(Profiler ICON app-profiler)
target_link_libraries(Profiler PRIVATE LibCore LibDebug LibELF LibFileSystem LibGfx LibGUI LibDesktop LibDisassembly LibSymbolication LibMain LibURL)
