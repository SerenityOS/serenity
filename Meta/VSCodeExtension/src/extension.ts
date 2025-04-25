'use strict';
import * as vscode from 'vscode';
import {Range, Position, TextEdit} from 'vscode';
import * as fs from 'fs';
import * as tmp from 'tmp';
import {spawnSync} from 'child_process';

export function activate(context: vscode.ExtensionContext) {
  vscode.languages.registerDocumentFormattingEditProvider('serenity-gml', {
    provideDocumentFormattingEdits(document: vscode.TextDocument) {

      let filename = document.fileName;
      let rootPath = vscode.workspace.rootPath;
      let fileContent = document.getText()
      let eof = document.positionAt(fileContent.length);

      // FIXME: gml-format does not support files over stdin, yet,
      //        so we have to create a temporary file with the files content
      let tmpFile = tmp.fileSync();
      fs.writeSync(tmpFile.fd, fileContent);
      fs.closeSync(tmpFile.fd);

      let executablePath = rootPath + '/Build/lagom/gml-format'

      if (!fs.existsSync(executablePath)) {
        vscode.window.showErrorMessage(
            'Could not find gml-format. Please build lagom first.');
        return [];
      }

      let childOutput = spawnSync(executablePath, [tmpFile.name]);
      // Return Codes:
      // 0: Nothing changed
      // 1: Some lines changed
      // *: Error
      if (childOutput.status == 0)
        return [];

      if (childOutput.status && childOutput.status > 1) {
        vscode.window.showErrorMessage(
            `gml-format failed with exit-code: ${childOutput.status}`,
            childOutput.stderr.toString());
        return [];
      }
      console.log(childOutput.stdout);

      return [TextEdit.replace(
          new Range(new Position(0, 0), eof), childOutput.stdout.toString())];
    }
  });
}
