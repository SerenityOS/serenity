serenity_component(
    Presenter
    RECOMMENDED
    TARGETS Presenter
    DEPENDS ImageDecoder FileSystemAccessServer
)

compile_gml(PresenterSettingsFooter.gml PresenterSettingsFooterGML.h presenter_settings_footer_gml)

set(SOURCES
    main.cpp
    Presentation.cpp
    PresenterSettings.cpp
    PresenterWidget.cpp
    Slide.cpp
    SlideObject.cpp
)

set(GENERATED_SOURCES
    PresenterSettingsFooterGML.h
)

serenity_app(Presenter ICON app-display-settings)
target_link_libraries(Presenter PRIVATE LibImageDecoderClient LibGUI LibGfx LibFileSystemAccessClient LibConfig LibCore LibMain LibThreading)
