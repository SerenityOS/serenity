serenity_component(
    PackageManager
    RECOMMENDED
    TARGETS PackageManager
    DEPENDS FileSystemAccessServer
)

set(SOURCES
    AvailablePort.cpp
    InstalledPort.cpp
    main.cpp
)

serenity_app(PackageManager ICON app-assistant)
target_link_libraries(PackageManager PRIVATE LibCore LibMain LibFileSystem LibProtocol LibHTTP LibMarkdown LibShell)
