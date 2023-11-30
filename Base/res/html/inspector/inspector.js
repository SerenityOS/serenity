let selectedTopTab = null;
let selectedTopTabButton = null;

let selectedBottomTab = null;
let selectedBottomTabButton = null;

let selectedDOMNode = null;

const selectTab = (tabButton, tabID, selectedTab, selectedTabButton) => {
    let tab = document.getElementById(tabID);

    if (selectedTab === tab) {
        return selectedTab;
    }
    if (selectedTab !== null) {
        selectedTab.style.display = "none";
        selectedTabButton.classList.remove("active");
    }

    tab.style.display = "block";
    tabButton.classList.add("active");

    return tab;
};

const selectTopTab = (tabButton, tabID) => {
    selectedTopTab = selectTab(tabButton, tabID, selectedTopTab, selectedTopTabButton);
    selectedTopTabButton = tabButton;
};

const selectBottomTab = (tabButton, tabID) => {
    selectedBottomTab = selectTab(tabButton, tabID, selectedBottomTab, selectedBottomTabButton);
    selectedBottomTabButton = tabButton;
};

let initialTopTabButton = document.getElementById("dom-tree-button");
selectTopTab(initialTopTabButton, "dom-tree");

let initialBottomTabButton = document.getElementById("computed-style-button");
selectBottomTab(initialBottomTabButton, "computed-style");

const scrollToElement = element => {
    // Include an offset to prevent the element being placed behind the fixed `tab-controls` header.
    const offset = 45;

    let position = element.getBoundingClientRect().top;
    position += window.pageYOffset - offset;

    window.scrollTo(0, position);
};

inspector.loadDOMTree = tree => {
    let domTree = document.getElementById("dom-tree");
    domTree.innerHTML = atob(tree);

    let domNodes = domTree.querySelectorAll(".hoverable");

    for (let domNode of domNodes) {
        domNode.addEventListener("click", event => {
            inspectDOMNode(domNode);
            event.preventDefault();
        });
    }
};

inspector.loadAccessibilityTree = tree => {
    let accessibilityTree = document.getElementById("accessibility-tree");
    accessibilityTree.innerHTML = atob(tree);
};

inspector.inspectDOMNodeID = nodeID => {
    let domNodes = document.querySelectorAll(`[data-id="${nodeID}"]`);
    if (domNodes.length !== 1) {
        return;
    }

    for (let domNode = domNodes[0]; domNode; domNode = domNode.parentNode) {
        if (domNode.tagName === "DETAILS") {
            domNode.setAttribute("open", "");
        }
    }

    inspectDOMNode(domNodes[0]);
    scrollToElement(selectedDOMNode);
};

inspector.clearInspectedDOMNode = () => {
    if (selectedDOMNode !== null) {
        selectedDOMNode.classList.remove("selected");
        selectedDOMNode = null;
    }
};

inspector.createPropertyTables = (computedStyle, resolvedStyle, customProperties) => {
    const createPropertyTable = (tableID, properties) => {
        let oldTable = document.getElementById(tableID);

        let newTable = document.createElement("tbody");
        newTable.setAttribute("id", tableID);

        Object.keys(properties)
            .sort()
            .forEach(name => {
                let row = newTable.insertRow();

                let nameColumn = row.insertCell();
                nameColumn.innerText = name;

                let valueColumn = row.insertCell();
                valueColumn.innerText = properties[name];
            });

        oldTable.parentNode.replaceChild(newTable, oldTable);
    };

    createPropertyTable("computed-style-table", JSON.parse(computedStyle));
    createPropertyTable("resolved-style-table", JSON.parse(resolvedStyle));
    createPropertyTable("custom-properties-table", JSON.parse(customProperties));
};

const inspectDOMNode = domNode => {
    if (selectedDOMNode === domNode) {
        return;
    }

    inspector.clearInspectedDOMNode();

    domNode.classList.add("selected");
    selectedDOMNode = domNode;

    inspector.inspectDOMNode(domNode.dataset.id, domNode.dataset.pseudoElement);
};

document.addEventListener("DOMContentLoaded", () => {
    inspector.inspectorLoaded();
});
