set(SOURCES
    main.cpp
    DevicesModel.cpp
    GraphWidget.cpp
    MemoryStatsWidget.cpp
    NetworkStatisticsWidget.cpp
    ProcessFileDescriptorMapWidget.cpp
    ProcessMemoryMapWidget.cpp
    ProcessModel.cpp
    ProcessStacksWidget.cpp
    ProcessTableView.cpp
    ProcessUnveiledPathsWidget.cpp
)

serenity_app(SystemMonitor)
target_link_libraries(SystemMonitor LibGUI LibPCIDB)
