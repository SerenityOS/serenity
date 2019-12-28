LIBHTML_OBJS = \
    CSS/DefaultStyleSheetSource.o \
    CSS/PropertyID.o \
    CSS/Selector.o \
    CSS/SelectorEngine.o \
    CSS/StyleDeclaration.o \
    CSS/StyleProperties.o \
    CSS/StyleResolver.o \
    CSS/StyleRule.o \
    CSS/StyleSheet.o \
    CSS/StyleValue.o \
    DOM/CharacterData.o \
    DOM/Comment.o \
    DOM/Document.o \
    DOM/DocumentType.o \
    DOM/Element.o \
    DOM/ElementFactory.o \
    DOM/HTMLAnchorElement.o \
    DOM/HTMLBRElement.o \
    DOM/HTMLBlinkElement.o \
    DOM/HTMLBodyElement.o \
    DOM/HTMLElement.o \
    DOM/HTMLFontElement.o \
    DOM/HTMLFormElement.o \
    DOM/HTMLHRElement.o \
    DOM/HTMLHeadElement.o \
    DOM/HTMLHeadingElement.o \
    DOM/HTMLHtmlElement.o \
    DOM/HTMLImageElement.o \
    DOM/HTMLInputElement.o \
    DOM/HTMLLinkElement.o \
    DOM/HTMLStyleElement.o \
    DOM/HTMLTitleElement.o \
    DOM/Node.o \
    DOM/ParentNode.o \
    DOM/Text.o \
    DOMTreeModel.o \
    Dump.o \
    FontCache.o \
    Frame.o \
    HtmlView.o \
    Layout/BoxModelMetrics.o \
    Layout/LayoutBlock.o \
    Layout/LayoutBox.o \
    Layout/LayoutBreak.o \
    Layout/LayoutDocument.o \
    Layout/LayoutImage.o \
    Layout/LayoutInline.o \
    Layout/LayoutListItem.o \
    Layout/LayoutListItemMarker.o \
    Layout/LayoutNode.o \
    Layout/LayoutReplaced.o \
    Layout/LayoutTable.o \
    Layout/LayoutTableCell.o \
    Layout/LayoutTableRow.o \
    Layout/LayoutText.o \
    Layout/LayoutTreeBuilder.o \
    Layout/LayoutWidget.o \
    Layout/LineBox.o \
    Layout/LineBoxFragment.o \
    Parser/CSSParser.o \
    Parser/HTMLParser.o \
    ResourceLoader.o

EXTRA_SOURCES = \
    CSS/DefaultStyleSheetSource.cpp \
    CSS/PropertyID.h \
    CSS/PropertyID.cpp

GENERATE_CSS_PROPERTYID_CPP = CodeGenerators/Generate_CSS_PropertyID_cpp/Generate_CSS_PropertyID_cpp
GENERATE_CSS_PROPERTYID_H = CodeGenerators/Generate_CSS_PropertyID_h/Generate_CSS_PropertyID_h

$(GENERATE_CSS_PROPERTYID_H):
	@flock $(dir $(GENERATE_CSS_PROPERTYID_H)) $(MAKE) -C $(dir $(GENERATE_CSS_PROPERTYID_H))

$(GENERATE_CSS_PROPERTYID_CPP):
	@flock $(dir $(GENERATE_CSS_PROPERTYID_CPP)) $(MAKE) -C $(dir $(GENERATE_CSS_PROPERTYID_CPP))

CSS/DefaultStyleSheetSource.cpp: CSS/Default.css Scripts/GenerateStyleSheetSource.sh
	@echo "GENERATE $@"
	$(QUIET) Scripts/GenerateStyleSheetSource.sh default_stylesheet_source $< > $@

CSS/PropertyID.h: CSS/Properties.json $(GENERATE_CSS_PROPERTYID_H)
	@echo "GENERATE $@"
	$(QUIET) flock CSS $(GENERATE_CSS_PROPERTYID_H) $< > $@

CSS/PropertyID.cpp: CSS/Properties.json $(GENERATE_CSS_PROPERTYID_CPP)
	@echo "GENERATE $@"
	$(QUIET) flock CSS $(GENERATE_CSS_PROPERTYID_CPP) $< > $@

ResourceLoader.cpp: ../../Servers/ProtocolServer/ProtocolClientEndpoint.h
../../Servers/ProtocolServer/ProtocolClientEndpoint.h:
	@$(MAKE) -C $(dir $(@))

EXTRA_CLEAN = CSS/DefaultStyleSheetSource.cpp CSS/PropertyID.h CSS/PropertyID.cpp

OBJS = $(EXTRA_OBJS) $(LIBHTML_OBJS)

LIBRARY = libhtml.a

install:
	for dir in . Parser DOM CSS Layout; do \
	    mkdir -p $(SERENITY_BASE_DIR)/Root/usr/include/LibHTML/$$dir; \
	    cp $$dir/*.h $(SERENITY_BASE_DIR)/Root/usr/include/LibHTML/$$dir/; \
	done
	cp $(LIBRARY) $(SERENITY_BASE_DIR)/Root/usr/lib/

include ../../Makefile.common

SUBDIRS = CodeGenerators

include ../../Makefile.subdir
