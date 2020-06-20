set(SOURCES
    Bindings/CanvasRenderingContext2DWrapper.cpp
    Bindings/DocumentWrapper.cpp
    Bindings/DocumentWrapper.h
    Bindings/ElementWrapper.cpp
    Bindings/EventListenerWrapper.cpp
    Bindings/EventTargetWrapper.cpp
    Bindings/EventWrapper.cpp
    Bindings/HTMLCanvasElementWrapper.cpp
    Bindings/HTMLImageElementWrapper.cpp
    Bindings/ImageDataWrapper.cpp
    Bindings/LocationObject.cpp
    Bindings/MouseEventWrapper.cpp
    Bindings/NavigatorObject.cpp
    Bindings/NodeWrapper.cpp
    Bindings/NodeWrapper.h
    Bindings/NodeWrapperFactory.cpp
    Bindings/WindowObject.cpp
    Bindings/Wrappable.cpp
    Bindings/XMLHttpRequestConstructor.cpp
    Bindings/XMLHttpRequestPrototype.cpp
    Bindings/XMLHttpRequestWrapper.cpp
    CSS/Length.cpp
    CSS/Selector.cpp
    CSS/SelectorEngine.cpp
    CSS/StyleDeclaration.cpp
    CSS/StyleProperties.cpp
    CSS/StyleResolver.cpp
    CSS/StyleRule.cpp
    CSS/StyleSheet.cpp
    CSS/StyleSheetList.cpp
    CSS/StyleValue.cpp
    DOM/AttributeNames.cpp
    DOM/CanvasRenderingContext2D.cpp
    DOM/CharacterData.cpp
    DOM/Comment.cpp
    DOM/Document.cpp
    DOM/DocumentType.cpp
    DOM/Element.cpp
    DOM/ElementFactory.cpp
    DOM/EventListener.cpp
    DOM/EventTarget.cpp
    DOM/HTMLAnchorElement.cpp
    DOM/HTMLBlinkElement.cpp
    DOM/HTMLBodyElement.cpp
    DOM/HTMLBRElement.cpp
    DOM/HTMLCanvasElement.cpp
    DOM/HTMLElement.cpp
    DOM/HTMLFontElement.cpp
    DOM/HTMLFormElement.cpp
    DOM/HTMLHeadElement.cpp
    DOM/HTMLHeadingElement.cpp
    DOM/HTMLHRElement.cpp
    DOM/HTMLHtmlElement.cpp
    DOM/HTMLIFrameElement.cpp
    DOM/HTMLImageElement.cpp
    DOM/HTMLInputElement.cpp
    DOM/HTMLObjectElement.cpp
    DOM/HTMLLinkElement.cpp
    DOM/HTMLScriptElement.cpp
    DOM/HTMLStyleElement.cpp
    DOM/HTMLTableElement.cpp
    DOM/HTMLTableCellElement.cpp
    DOM/HTMLTableRowElement.cpp
    DOM/HTMLTitleElement.cpp
    DOM/ImageData.cpp
    DOM/Node.cpp
    DOM/ParentNode.cpp
    DOM/TagNames.cpp
    DOM/Text.cpp
    DOM/Window.cpp
    DOM/XMLHttpRequest.cpp
    DOMTreeModel.cpp
    Dump.cpp
    FontCache.cpp
    Frame/EventHandler.cpp
    Frame/Frame.cpp
    Layout/BoxModelMetrics.cpp
    Layout/LayoutBlock.cpp
    Layout/LayoutBox.cpp
    Layout/LayoutBreak.cpp
    Layout/LayoutCanvas.cpp
    Layout/LayoutDocument.cpp
    Layout/LayoutFrame.cpp
    Layout/LayoutImage.cpp
    Layout/LayoutInline.cpp
    Layout/LayoutListItem.cpp
    Layout/LayoutListItemMarker.cpp
    Layout/LayoutNode.cpp
    Layout/LayoutReplaced.cpp
    Layout/LayoutTable.cpp
    Layout/LayoutTableCell.cpp
    Layout/LayoutTableRow.cpp
    Layout/LayoutTableRowGroup.cpp
    Layout/LayoutText.cpp
    Layout/LayoutTreeBuilder.cpp
    Layout/LayoutWidget.cpp
    Layout/LineBox.cpp
    Layout/LineBoxFragment.cpp
    LayoutTreeModel.cpp
    Loader/FrameLoader.cpp
    Loader/ImageLoader.cpp
    Loader/ImageResource.cpp
    Loader/Resource.cpp
    Loader/ResourceLoader.cpp
    Page.cpp
    PageView.cpp
    Painting/StackingContext.cpp
    Parser/CSSParser.cpp
    Parser/Entities.cpp
    Parser/HTMLDocumentParser.cpp
    Parser/HTMLParser.cpp
    Parser/HTMLToken.cpp
    Parser/HTMLTokenizer.cpp
    Parser/ListOfActiveFormattingElements.cpp
    Parser/StackOfOpenElements.cpp
    StylePropertiesModel.cpp
    URLEncoder.cpp
    CSS/PropertyID.h
    CSS/PropertyID.cpp
    CSS/DefaultStyleSheetSource.cpp
)

set(GENERATED_SOURCES
    ../../Services/ProtocolServer/ProtocolClientEndpoint.h
    ../../Services/ProtocolServer/ProtocolServerEndpoint.h
)

add_custom_command(
    OUTPUT Bindings/NodeWrapper.h
    COMMAND /bin/mkdir -p Bindings
    COMMAND WrapperGenerator --header ${CMAKE_CURRENT_SOURCE_DIR}/DOM/Node.idl > Bindings/NodeWrapper.h
    VERBATIM
    DEPENDS WrapperGenerator
    MAIN_DEPENDENCY DOM/Node.idl
)
add_custom_target(generate_NodeWrapper.h DEPENDS Bindings/NodeWrapper.h)

add_custom_command(
    OUTPUT Bindings/NodeWrapper.cpp
    COMMAND /bin/mkdir -p Bindings
    COMMAND WrapperGenerator --implementation ${CMAKE_CURRENT_SOURCE_DIR}/DOM/Node.idl > Bindings/NodeWrapper.cpp
    VERBATIM
    DEPENDS WrapperGenerator
    MAIN_DEPENDENCY DOM/Node.idl
)
add_custom_target(generate_NodeWrapper.cpp DEPENDS Bindings/NodeWrapper.cpp)

add_custom_command(
    OUTPUT Bindings/DocumentWrapper.h
    COMMAND /bin/mkdir -p Bindings
    COMMAND WrapperGenerator --header ${CMAKE_CURRENT_SOURCE_DIR}/DOM/Document.idl > Bindings/DocumentWrapper.h
    VERBATIM
    DEPENDS WrapperGenerator
    MAIN_DEPENDENCY DOM/Document.idl
)
add_custom_target(generate_DocumentWrapper.h DEPENDS Bindings/DocumentWrapper.h)

add_custom_command(
    OUTPUT Bindings/DocumentWrapper.cpp
    COMMAND /bin/mkdir -p Bindings
    COMMAND WrapperGenerator --implementation ${CMAKE_CURRENT_SOURCE_DIR}/DOM/Document.idl > Bindings/DocumentWrapper.cpp
    VERBATIM
    DEPENDS WrapperGenerator
    MAIN_DEPENDENCY DOM/Document.idl
)
add_custom_target(generate_DocumentWrapper.cpp DEPENDS Bindings/DocumentWrapper.cpp)

add_custom_command(
    OUTPUT CSS/PropertyID.h
    COMMAND /bin/mkdir -p CSS
    COMMAND Generate_CSS_PropertyID_h ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Properties.json > CSS/PropertyID.h
    VERBATIM
    DEPENDS Generate_CSS_PropertyID_h
    MAIN_DEPENDENCY CSS/Properties.json
)
add_custom_target(generate_PropertyID.h DEPENDS CSS/PropertyID.h)

add_custom_command(
    OUTPUT CSS/PropertyID.cpp
    COMMAND /bin/mkdir -p CSS
    COMMAND Generate_CSS_PropertyID_cpp ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Properties.json > CSS/PropertyID.cpp
    VERBATIM
    DEPENDS Generate_CSS_PropertyID_cpp
    MAIN_DEPENDENCY CSS/Properties.json
)

add_custom_command(
    OUTPUT CSS/DefaultStyleSheetSource.cpp
    COMMAND /bin/mkdir -p CSS
    COMMAND ${CMAKE_CURRENT_SOURCE_DIR}/Scripts/GenerateStyleSheetSource.sh default_stylesheet_source
            ${CMAKE_CURRENT_SOURCE_DIR}/CSS/Default.css > CSS/DefaultStyleSheetSource.cpp
    VERBATIM
    DEPENDS Scripts/GenerateStyleSheetSource.sh
    MAIN_DEPENDENCY CSS/Default.css
)

serenity_lib(LibWeb web)
target_link_libraries(LibWeb LibCore LibJS LibMarkdown LibGemini LibGUI LibGfx LibTextCodec LibProtocol)
