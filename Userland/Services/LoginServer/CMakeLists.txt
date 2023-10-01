serenity_component(
    LoginServer
    REQUIRED
    TARGETS LoginServer
)

compile_gml(LoginWindow.gml LoginWindowGML.cpp)

set(SOURCES
    LoginWindow.cpp
    LoginWindowGML.cpp
    main.cpp
)

serenity_bin(LoginServer)
target_link_libraries(LoginServer PRIVATE LibCore LibGfx LibGUI LibMain)
