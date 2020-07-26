set(SOURCES
    Bindings/EventListenerWrapper.cpp
    Bindings/EventWrapperFactory.cpp
    Bindings/LocationObject.cpp
    Bindings/NavigatorObject.cpp
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
    DOM/CharacterData.cpp
    DOM/Comment.cpp
    DOM/Document.cpp
    DOM/DocumentType.cpp
    DOM/Element.cpp
    DOM/ElementFactory.cpp
    DOM/EventListener.cpp
    DOM/EventTarget.cpp
    DOM/Node.cpp
    DOM/ParentNode.cpp
    DOM/TagNames.cpp
    DOM/Text.cpp
    DOM/Timer.cpp
    DOM/Window.cpp
    DOM/XMLHttpRequest.cpp
    DOMTreeModel.cpp
    Dump.cpp
    FontCache.cpp
    Frame/EventHandler.cpp
    Frame/Frame.cpp
    HTML/CanvasRenderingContext2D.cpp
    HTML/HTMLAnchorElement.cpp
    HTML/HTMLBRElement.cpp
    HTML/HTMLBlinkElement.cpp
    HTML/HTMLBodyElement.cpp
    HTML/HTMLCanvasElement.cpp
    HTML/HTMLElement.cpp
    HTML/HTMLFontElement.cpp
    HTML/HTMLFormElement.cpp
    HTML/HTMLHRElement.cpp
    HTML/HTMLHeadElement.cpp
    HTML/HTMLHeadingElement.cpp
    HTML/HTMLHtmlElement.cpp
    HTML/HTMLIFrameElement.cpp
    HTML/HTMLImageElement.cpp
    HTML/HTMLInputElement.cpp
    HTML/HTMLLinkElement.cpp
    HTML/HTMLObjectElement.cpp
    HTML/HTMLScriptElement.cpp
    HTML/HTMLStyleElement.cpp
    HTML/HTMLTableCellElement.cpp
    HTML/HTMLTableElement.cpp
    HTML/HTMLTableRowElement.cpp
    HTML/HTMLTitleElement.cpp
    HTML/ImageData.cpp
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
    Layout/LayoutPosition.cpp
    Layout/LayoutReplaced.cpp
    Layout/LayoutSVG.cpp
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
    Parser/HTMLToken.cpp
    Parser/HTMLTokenizer.cpp
    Parser/ListOfActiveFormattingElements.cpp
    Parser/StackOfOpenElements.cpp
    StylePropertiesModel.cpp
    SVG/SVGElement.cpp
    SVG/SVGGeometryElement.cpp
    SVG/SVGGraphicsElement.cpp
    SVG/SVGPathElement.cpp
    SVG/SVGSVGElement.cpp
    SVG/TagNames.cpp
    URLEncoder.cpp
    CSS/PropertyID.h
    CSS/PropertyID.cpp
    CSS/DefaultStyleSheetSource.cpp
    WebContentView.cpp
    WebContentClient.cpp
)

set(GENERATED_SOURCES
    ../../Services/ProtocolServer/ProtocolClientEndpoint.h
    ../../Services/ProtocolServer/ProtocolServerEndpoint.h
    ../../Services/WebContent/WebContentClientEndpoint.h
    ../../Services/WebContent/WebContentServerEndpoint.h
)

set_property(GLOBAL PROPERTY wrapper_sources)
function(add_wrapper_sources)
    get_property(tmp GLOBAL PROPERTY wrapper_sources)
    foreach(arg ${ARGV})
        set(tmp ${tmp}
            ${arg}
        )
    endforeach()
    set_property(GLOBAL PROPERTY wrapper_sources "${tmp}")
endfunction(add_wrapper_sources)

function(libweb_js_wrapper class)
    get_filename_component(basename ${class} NAME)
    add_wrapper_sources(Bindings/${basename}Wrapper.cpp Bindings/${basename}Wrapper.h)
    add_custom_command(
        OUTPUT Bindings/${basename}Wrapper.h
        COMMAND /bin/mkdir -p Bindings
        COMMAND WrapperGenerator --header ${CMAKE_CURRENT_SOURCE_DIR}/${class}.idl > Bindings/${basename}Wrapper.h
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY ${class}.idl
    )
    add_custom_command(
        OUTPUT Bindings/${basename}Wrapper.cpp
        COMMAND /bin/mkdir -p Bindings
        COMMAND WrapperGenerator --implementation ${CMAKE_CURRENT_SOURCE_DIR}/${class}.idl > Bindings/${basename}Wrapper.cpp
        VERBATIM
        DEPENDS WrapperGenerator
        MAIN_DEPENDENCY ${class}.idl
    )
    add_custom_target(generate_${basename}Wrapper.h DEPENDS Bindings/${class}Wrapper.h)
    add_custom_target(generate_${basename}Wrapper.cpp DEPENDS Bindings/${class}Wrapper.cpp)
endfunction()

libweb_js_wrapper(DOM/Document)
libweb_js_wrapper(DOM/DocumentType)
libweb_js_wrapper(DOM/Element)
libweb_js_wrapper(DOM/Event)
libweb_js_wrapper(DOM/EventTarget)
libweb_js_wrapper(DOM/MouseEvent)
libweb_js_wrapper(DOM/Node)
libweb_js_wrapper(HTML/CanvasRenderingContext2D)
libweb_js_wrapper(HTML/HTMLCanvasElement)
libweb_js_wrapper(HTML/HTMLElement)
libweb_js_wrapper(HTML/HTMLImageElement)
libweb_js_wrapper(HTML/ImageData)

get_property(WRAPPER_SOURCES GLOBAL PROPERTY wrapper_sources)
set(SOURCES ${SOURCES} ${WRAPPER_SOURCES})

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
target_link_libraries(LibWeb LibCore LibJS LibMarkdown LibGemini LibGUI LibGfx LibTextCodec LibProtocol LibImageDecoderClient)
