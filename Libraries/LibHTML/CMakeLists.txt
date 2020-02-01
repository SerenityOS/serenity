set(SOURCES
    CSS/Selector.cpp
    CSS/SelectorEngine.cpp
    CSS/StyleDeclaration.cpp
    CSS/StyleProperties.cpp
    CSS/StyleResolver.cpp
    CSS/StyleRule.cpp
    CSS/StyleSheet.cpp
    CSS/StyleValue.cpp
    DOM/CharacterData.cpp
    DOM/Comment.cpp
    DOM/Document.cpp
    DOM/DocumentType.cpp
    DOM/Element.cpp
    DOM/ElementFactory.cpp
    DOM/HTMLAnchorElement.cpp
    DOM/HTMLBlinkElement.cpp
    DOM/HTMLBodyElement.cpp
    DOM/HTMLBRElement.cpp
    DOM/HTMLElement.cpp
    DOM/HTMLFontElement.cpp
    DOM/HTMLFormElement.cpp
    DOM/HTMLHeadElement.cpp
    DOM/HTMLHeadingElement.cpp
    DOM/HTMLHRElement.cpp
    DOM/HTMLHtmlElement.cpp
    DOM/HTMLImageElement.cpp
    DOM/HTMLInputElement.cpp
    DOM/HTMLLinkElement.cpp
    DOM/HTMLStyleElement.cpp
    DOM/HTMLTitleElement.cpp
    DOM/Node.cpp
    DOM/ParentNode.cpp
    DOM/Text.cpp
    DOMTreeModel.cpp
    Dump.cpp
    FontCache.cpp
    Frame.cpp
    StylePropertiesModel.cpp
    HtmlView.cpp
    Layout/BoxModelMetrics.cpp
    Layout/LayoutBlock.cpp
    Layout/LayoutBox.cpp
    Layout/LayoutBreak.cpp
    Layout/LayoutDocument.cpp
    Layout/LayoutImage.cpp
    Layout/LayoutInline.cpp
    Layout/LayoutListItem.cpp
    Layout/LayoutListItemMarker.cpp
    Layout/LayoutNode.cpp
    Layout/LayoutReplaced.cpp
    Layout/LayoutTableCell.cpp
    Layout/LayoutTable.cpp
    Layout/LayoutTableRow.cpp
    Layout/LayoutText.cpp
    Layout/LayoutTreeBuilder.cpp
    Layout/LayoutWidget.cpp
    Layout/LineBox.cpp
    Layout/LineBoxFragment.cpp
    Parser/CSSParser.cpp
    Parser/HTMLParser.cpp
    ResourceLoader.cpp
)

add_subdirectory(CodeGenerators/Generate_CSS_PropertyID_cpp)
add_subdirectory(CodeGenerators/Generate_CSS_PropertyID_h)

add_custom_command(
    OUTPUT CSS_DefaultStyleSheetSource.cpp
    COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/Scripts/GenerateStyleSheetSource.sh default_stylesheet_source ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Default.css > ${CMAKE_CURRENT_BINARY_DIR}/CSS_DefaultStyleSheetSource.cpp
    DEPENDS CSS/Default.css
)

add_custom_command(
    OUTPUT CSS_PropertyID.cpp
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/CodeGenerators/Generate_CSS_PropertyID_cpp/Generate_CSS_PropertyID_cpp ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Properties.json > ${CMAKE_CURRENT_BINARY_DIR}/CSS_PropertyID.cpp
    DEPENDS CSS/Properties.json Generate_CSS_PropertyID_cpp
)

add_custom_command(
    OUTPUT CSS_PropertyID.h
    COMMAND ${CMAKE_CURRENT_BINARY_DIR}/CodeGenerators/Generate_CSS_PropertyID_h/Generate_CSS_PropertyID_h ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Properties.json > ${CMAKE_CURRENT_BINARY_DIR}/CSS_PropertyID.h
    DEPENDS CSS/Properties.json Generate_CSS_PropertyID_h
)

set(SOURCES ${SOURCES} CSS_DefaultStyleSheetSource.cpp CSS_PropertyID.cpp CSS_PropertyID.h)
serenity_lib(LibHTML html)
target_link_libraries(LibHTML LibProtocol LibGUI)
target_include_directories(LibHTML PRIVATE ${CMAKE_CURRENT_BINARY_DIR}/../../Servers)
target_include_directories(LibHTML PUBLIC ${CMAKE_CURRENT_BINARY_DIR}/..)
