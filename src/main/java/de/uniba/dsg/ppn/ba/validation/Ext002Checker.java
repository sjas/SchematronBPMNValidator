/**
 *
 * BPMN Validation Project to validate special BPMN Constraints, see \README.md
 *
 * Copyright (C) 2014 Philipp Neugebauer
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package de.uniba.dsg.ppn.ba.validation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import de.uniba.dsg.bpmnspector.common.ValidationResult;
import de.uniba.dsg.bpmnspector.common.Violation;
import de.uniba.dsg.ppn.ba.helper.BpmnHelper;
import de.uniba.dsg.ppn.ba.helper.ImportedFilesCrawler;
import de.uniba.dsg.ppn.ba.helper.PrintHelper;
import de.uniba.dsg.ppn.ba.helper.SetupHelper;
import de.uniba.dsg.ppn.ba.preprocessing.ImportedFile;

/**
 * This class is resposible for the check of the EXT.002 constraint
 *
 * @author Philipp Neugebauer
 * @version 1.0
 *
 */
public class Ext002Checker {

    private final DocumentBuilder documentBuilder;
    private final XPathExpression xPathExpression;
    private final XmlLocator xmlLocator;
    private final static Logger LOGGER;
    private static final String CONSTRAINTNUMBER = "EXT.002";

    static {
        LOGGER = LoggerFactory.getLogger(Ext002Checker.class.getSimpleName());
    }

    {
        documentBuilder = SetupHelper.setupDocumentBuilder();
        xPathExpression = SetupHelper.setupXPathExpression();
        xmlLocator = new XmlLocator();
    }

    /**
     * checks, if there are violations of the EXT.002 constraint
     *
     * @param headFile
     *            the file which should be checked
     * @param folder
     *            the parent folder of the file
     * @param validationResult
     *            the current validation result of validating process for adding
     *            found violations
     * @throws XPathExpressionException
     *             if there's an invalid xpath expression used
     */
    public void checkConstraint002(File headFile, File folder,
            ValidationResult validationResult) throws XPathExpressionException {
        List<File> importedFileList = searchForImports(headFile, folder,
                validationResult);

        for (int i = 0; i < importedFileList.size(); i++) {
            File file1 = importedFileList.get(i);
            try {
                Document document1 = documentBuilder.parse(file1);
                BpmnHelper.removeBPMNDINode(document1);
                String namespace1 = document1.getDocumentElement()
                        .getAttribute("targetNamespace");
                for (int j = i + 1; j < importedFileList.size(); j++) {
                    File file2 = importedFileList.get(j);
                    try {
                        Document document2 = documentBuilder.parse(file2);
                        BpmnHelper.removeBPMNDINode(document2);
                        String namespace2 = document2.getDocumentElement()
                                .getAttribute("targetNamespace");
                        if (namespace1.equals(namespace2)) {
                            checkNamespacesAndIdDuplicates(file1, file2,
                                    document1, document2, validationResult);
                        }
                    } catch (IOException | SAXException e) {
                        PrintHelper.printFileNotFoundLogs(LOGGER, e,
                                file2.getName());
                    }
                }
            } catch (IOException | SAXException e) {
                PrintHelper.printFileNotFoundLogs(LOGGER, e, file1.getName());
            }
        }
    }

    /**
     * searches for all existing files, which are imported in the given file and
     * their imports and so on
     *
     * @param file
     *            where the imports are searched
     * @param folder
     *            parent folder of file
     * @param validationResult
     *            to add all imported files to the checked file list
     * @return List<File> including all imported files in file
     */
    private List<File> searchForImports(File file, File folder,
            ValidationResult validationResult) {
        List<File> importedFileList = new ArrayList<>();
        try {
            Document document = documentBuilder.parse(file);
            List<ImportedFile> importedFiles = ImportedFilesCrawler
                    .selectImportedFiles(document, folder, 0, true);
            importedFileList.add(file);

            for (ImportedFile importedFile : importedFiles) {
                File impFile = importedFile.getFile();
                if (impFile.exists()) {
                    validationResult.getCheckedFiles().add(
                            impFile.getAbsolutePath());
                    importedFileList.addAll(searchForImports(impFile, folder,
                            validationResult));
                }
            }
        } catch (SAXParseException e) {
            validationResult.getViolations().add(
                    new Violation("XSD-Check", file.getName(), e
                            .getLineNumber(), "", e.getMessage()));
            validationResult.getCheckedFiles().add(file.getName());
            LOGGER.info("XML not well-formed in {} at line {}", file.getName(),
                    e.getLineNumber());
        } catch (IOException | SAXException e) {
            PrintHelper.printFileNotFoundLogs(LOGGER, e, file.getName());
        }

        return importedFileList;
    }

    /**
     * checks, if there are namespace and id duplicates in these two checked
     * files
     *
     * @param file1
     *            first file to check
     * @param file2
     *            second file to check
     * @param document1
     *            parsed file1
     * @param document2
     *            parsed file2
     * @param validationResult
     *            for adding violations to the current validation result
     * @throws XPathExpressionException
     *             if there are invalid xpath expressions
     */
    private void checkNamespacesAndIdDuplicates(File file1, File file2,
            Document document1, Document document2,
            ValidationResult validationResult) throws XPathExpressionException {
        NodeList foundNodes1 = (NodeList) xPathExpression.evaluate(document1,
                XPathConstants.NODESET);
        NodeList foundNodes2 = (NodeList) xPathExpression.evaluate(document2,
                XPathConstants.NODESET);
        for (int k = 1; k < foundNodes1.getLength(); k++) {
            String importedFile1Id = foundNodes1.item(k).getNodeValue();
            for (int l = 1; l < foundNodes2.getLength(); l++) {
                String importedFile2Id = foundNodes2.item(l).getNodeValue();
                if (importedFile1Id.equals(importedFile2Id)) {
                    String xpathLocation = BpmnHelper
                            .createIdBpmnExpression(importedFile1Id);
                    validationResult.getViolations().add(
                            new Violation(CONSTRAINTNUMBER, file1.getName(),
                                    xmlLocator.findLine(file1, xpathLocation),
                                    xpathLocation + "[0]",
                                    "Files have id duplicates"));
                    validationResult.getViolations().add(
                            new Violation(CONSTRAINTNUMBER, file2.getName(),
                                    xmlLocator.findLine(file2, xpathLocation),
                                    xpathLocation + "[0]",
                                    "Files have id duplicates"));
                    LOGGER.info("violation of constraint {} found.",
                            CONSTRAINTNUMBER);
                }
            }
        }
    }
}
