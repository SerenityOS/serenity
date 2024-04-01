embed_as_string_view(
    "WaylandDtd.cpp"
    "${CMAKE_CURRENT_SOURCE_DIR}/wayland.dtd"
    "WaylandDtd.cpp"
    "wayland_dtd_xml"
    NAMESPACE "Wayland")

set(SOURCES
    main.cpp
    WaylandDtd.cpp
)

lagom_tool(WaylandTranspiler LIBS LibMain LibXML)
