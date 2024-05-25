serenity_component(
    PackageManager
    RECOMMENDED
    TARGETS PackageManager
    DEPENDS FileSystemAccessServer
)

set(SOURCES
    AvailablePortDatabase.cpp
    InstalledPort.cpp
    InstalledPortDatabase.cpp
    main.cpp
)

serenity_app(PackageManager ICON app-assistant)
target_link_libraries(PackageManager PRIVATE LibCore LibSemVer LibMain LibFileSystem LibProtocol LibHTTP LibMarkdown LibShell LibURL)
