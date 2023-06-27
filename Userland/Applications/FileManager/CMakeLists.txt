serenity_component(
    FileManager
    REQUIRED
    TARGETS FileManager
    DEPENDS FileOperation
)

compile_gml(FileManagerWindow.gml FileManagerWindowGML.h file_manager_window_gml)
compile_gml(FileOperationProgress.gml FileOperationProgressGML.h file_operation_progress_gml)
compile_gml(PropertiesWindowAudioTab.gml PropertiesWindowAudioTabGML.h properties_window_audio_tab_gml)
compile_gml(PropertiesWindowFontTab.gml PropertiesWindowFontTabGML.h properties_window_font_tab_gml)
compile_gml(PropertiesWindowGeneralTab.gml PropertiesWindowGeneralTabGML.h properties_window_general_tab_gml)
compile_gml(PropertiesWindowImageTab.gml PropertiesWindowImageTabGML.h properties_window_image_tab_gml)

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
    PropertiesWindowAudioTabGML.h
    PropertiesWindowFontTabGML.h
    PropertiesWindowGeneralTabGML.h
    PropertiesWindowImageTabGML.h
)

serenity_app(FileManager ICON app-file-manager)
target_link_libraries(FileManager PRIVATE LibAudio LibCore LibFileSystem LibGfx LibGUI LibDesktop LibConfig LibMain LibThreading)
