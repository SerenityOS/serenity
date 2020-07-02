set(SOURCES
    DisassemblyModel.cpp
    main.cpp
    ProcessChooser.cpp
    Profile.cpp
    ProfileModel.cpp
    ProfileTimelineWidget.cpp
    RunningProcessesModel.cpp
)

serenity_bin(Profiler)
target_link_libraries(Profiler LibGUI LibX86)
