serenity_component(
    FileManager
    REQUIRED
    TARGETS FileManager
    DEPENDS FileOperation
)

stringify_gml(FileManagerWindow.gml FileManagerWindowGML.h file_manager_window_gml)
stringify_gml(FileOperationProgress.gml FileOperationProgressGML.h file_operation_progress_gml)
stringify_gml(PropertiesWindowArchiveTab.gml PropertiesWindowArchiveTabGML.h properties_window_archive_tab_gml)
stringify_gml(PropertiesWindowAudioTab.gml PropertiesWindowAudioTabGML.h properties_window_audio_tab_gml)
stringify_gml(PropertiesWindowFontTab.gml PropertiesWindowFontTabGML.h properties_window_font_tab_gml)
stringify_gml(PropertiesWindowGeneralTab.gml PropertiesWindowGeneralTabGML.h properties_window_general_tab_gml)
stringify_gml(PropertiesWindowImageTab.gml PropertiesWindowImageTabGML.h properties_window_image_tab_gml)
stringify_gml(PropertiesWindowPDFTab.gml PropertiesWindowPDFTabGML.h properties_window_pdf_tab_gml)

set(SOURCES
    DesktopWidget.cpp
    DirectoryView.cpp
    FileOperationProgressWidget.cpp
    FileUtils.cpp
    main.cpp
    PropertiesWindow.cpp
)

set(GENERATED_SOURCES
    FileManagerWindowGML.h
    FileOperationProgressGML.h
    PropertiesWindowArchiveTabGML.h
    PropertiesWindowAudioTabGML.h
    PropertiesWindowFontTabGML.h
    PropertiesWindowGeneralTabGML.h
    PropertiesWindowImageTabGML.h
    PropertiesWindowPDFTabGML.h
)

serenity_app(FileManager ICON app-file-manager)
target_link_libraries(FileManager PRIVATE LibArchive LibAudio LibConfig LibCore LibDesktop LibFileSystem LibGfx LibGUI LibMain LibMaps LibPDF LibThreading LibURL)
