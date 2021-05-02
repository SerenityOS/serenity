set(SOURCES
    Mesh.cpp
    WavefrontOBJLoader.cpp
    main.cpp
)

serenity_app(GLTeapot ICON app-teapot)
target_link_libraries(GLTeapot LibGUI LibGL)
