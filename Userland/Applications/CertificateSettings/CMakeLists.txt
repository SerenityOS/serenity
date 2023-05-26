serenity_component(
    CertificateSettings
    REQUIRED
    TARGETS CertificateSettings
)

compile_gml(CertificateStoreWidget.gml CertificateStoreWidgetGML.cpp)

set(SOURCES
    CertificateStoreWidget.cpp
    CertificateStoreWidgetGML.cpp
    main.cpp
)

serenity_app(CertificateSettings ICON certificate)
target_link_libraries(CertificateSettings PRIVATE LibCore LibCrypto LibFileSystem LibFileSystemAccessClient LibGfx LibGUI LibMain LibTLS)
