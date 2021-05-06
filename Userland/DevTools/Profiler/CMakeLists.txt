set(SOURCES
    DisassemblyModel.cpp
    main.cpp
    IndividualSampleModel.cpp
    Process.cpp
    ProcessPickerWidget.cpp
    Profile.cpp
    ProfileModel.cpp
        TimelineTrack.cpp
        SamplesModel.cpp
        TimelineView.cpp
)

serenity_app(Profiler ICON app-profiler)
target_link_libraries(Profiler LibGUI LibDesktop LibX86)
