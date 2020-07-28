set(SOURCES
    DisassemblyModel.cpp
    main.cpp
    Profile.cpp
    ProfileModel.cpp
    ProfileTimelineWidget.cpp
)

serenity_bin(Profiler)
target_link_libraries(Profiler LibGUI LibX86)
