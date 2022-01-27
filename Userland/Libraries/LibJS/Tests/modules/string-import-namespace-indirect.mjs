import * as indirectNs from "./default-and-star-export-indirect.mjs";

export const passed =
    indirectNs["*"] === "starExportValue" &&
    indirectNs[""] === "empty" &&
    indirectNs.default === undefined;
