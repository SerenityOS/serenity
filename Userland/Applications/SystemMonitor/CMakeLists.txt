serenity_component(
    SystemMonitor
    REQUIRED
    TARGETS SystemMonitor Profiler Inspector
)

set(SOURCES
    GraphWidget.cpp
    main.cpp
    MemoryStatsWidget.cpp
    NetworkStatisticsWidget.cpp
    ProcessFileDescriptorMapWidget.cpp
    ProcessMemoryMapWidget.cpp
    ProcessModel.cpp
    ProcessUnveiledPathsWidget.cpp
    ProcessStateWidget.cpp
    ThreadStackWidget.cpp
)

serenity_app(SystemMonitor ICON app-system-monitor)
target_link_libraries(SystemMonitor LibGUI LibSymbolication LibPCIDB LibMain)
