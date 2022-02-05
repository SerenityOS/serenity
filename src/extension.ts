'use strict';
import * as vscode from 'vscode';
import * as fs from 'fs';
import {spawnSync} from 'child_process';

export function activate(context: vscode.ExtensionContext) {
  vscode.languages.registerDocumentFormattingEditProvider('serenity-gml', {
    provideDocumentFormattingEdits(document: vscode.TextDocument) {
      // FIXME: gml-format does not support files over stdin, yet,
      //        so we have to force-save it for now
      // FIXME: Allow partial re-formatting
      document.save();
      let filename = document.fileName;
      let rootPath = vscode.workspace.rootPath;
      let eof = document.positionAt(document.getText().length);

      let executablePath = rootPath + '/Build/lagom/gml-format'

      if (!fs.existsSync(executablePath)) {
        vscode.window.showErrorMessage(
            'Could not find gml-format. Please build lagom first.');
        return [];
      }

      let childOutput = spawnSync(executablePath, ['-i', filename]);
      if (childOutput.status != 0) {
        vscode.window.showErrorMessage(
            `gml-format failed with exit-code: ${childOutput.status}`,
            childOutput.stderr.toString());
      }

      return [];
    }
  });
}
