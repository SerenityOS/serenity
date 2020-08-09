set(SOURCES
    DevicesModel.cpp
    GraphWidget.cpp
    main.cpp
    MemoryStatsWidget.cpp
    NetworkStatisticsWidget.cpp
    ProcessFileDescriptorMapWidget.cpp
    ProcessMemoryMapWidget.cpp
    ProcessModel.cpp
    ProcessStackWidget.cpp
    ProcessUnveiledPathsWidget.cpp
)

serenity_bin(SystemMonitor)
target_link_libraries(SystemMonitor LibGUI LibPCIDB)
