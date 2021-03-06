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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.stream.StreamSource;

import org.oclc.purl.dsdl.svrl.FailedAssert;
import org.oclc.purl.dsdl.svrl.SchematronOutputType;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import com.phloc.schematron.ISchematronResource;
import com.phloc.schematron.pure.SchematronResourcePure;

import de.uniba.dsg.bpmnspector.common.ValidationResult;
import de.uniba.dsg.bpmnspector.common.Violation;
import de.uniba.dsg.ppn.ba.helper.BpmnHelper;
import de.uniba.dsg.ppn.ba.helper.BpmnValidationException;
import de.uniba.dsg.ppn.ba.helper.PrintHelper;
import de.uniba.dsg.ppn.ba.helper.SetupHelper;
import de.uniba.dsg.ppn.ba.preprocessing.PreProcessResult;
import de.uniba.dsg.ppn.ba.preprocessing.PreProcessor;

/**
 * Implementation of BpmnValidator
 * <p>
 * More information : {@link BpmnValidator}
 * <p>
 * Does the validation process of the xsd and the schematron validation and
 * returns the results of the validation
 *
 * @author Philipp Neugebauer
 * @version 1.0
 */
public class SchematronBPMNValidator implements BpmnValidator {

    private final DocumentBuilder documentBuilder;
    private final PreProcessor preProcessor;
    private final XmlLocator xmlLocator;
    private final Ext001Checker ext001Checker;
    private final Ext002Checker ext002Checker;
    private final static Logger LOGGER;

    static {
        LOGGER = (Logger) LoggerFactory.getLogger(SchematronBPMNValidator.class
                .getSimpleName());
    }

    {
        documentBuilder = SetupHelper.setupDocumentBuilder();
        preProcessor = new PreProcessor();
        xmlLocator = new XmlLocator();
        ext001Checker = new Ext001Checker();
        ext002Checker = new Ext002Checker();
    }

    @Override
    public Level getLogLevel() {
        return LOGGER.getLevel();
    }

    @Override
    public void setLogLevel(Level logLevel) {
        ((Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME))
                .setLevel(logLevel);
    }

    @Override
    public List<ValidationResult> validateFiles(List<File> xmlFiles)
            throws BpmnValidationException {
        List<ValidationResult> validationResults = new ArrayList<>();
        for (File xmlFile : xmlFiles) {
            validationResults.add(validate(xmlFile));
        }
        return validationResults;
    }

    @Override
    public ValidationResult validate(File xmlFile)
            throws BpmnValidationException {
        final ISchematronResource schematronSchema = SchematronResourcePure
                .fromClassPath("validation.sch");
        if (!schematronSchema.isValidSchematron()) {
            LOGGER.debug("schematron file is invalid");
            throw new BpmnValidationException("Invalid Schematron file!");
        }

        LOGGER.info("Validating {}", xmlFile.getName());

        ValidationResult validationResult = new ValidationResult();

        try {
            Document headFileDocument = documentBuilder.parse(xmlFile);
            validationResult.getCheckedFiles().add(xmlFile.getAbsolutePath());
            File parentFolder = xmlFile.getParentFile();

            ext001Checker.checkConstraint001(xmlFile, parentFolder,
                    validationResult);
            ext002Checker.checkConstraint002(xmlFile, parentFolder,
                    validationResult);

            PreProcessResult preProcessResult = preProcessor.preProcess(
                    headFileDocument, parentFolder,
                    new HashMap<String, String>());

            SchematronOutputType schematronOutputType = schematronSchema
                    .applySchematronValidationToSVRL(new StreamSource(
                            DocumentTransformer
                            .transformToInputStream(headFileDocument)));
            for (int i = 0; i < schematronOutputType
                    .getActivePatternAndFiredRuleAndFailedAssertCount(); i++) {
                if (schematronOutputType
                        .getActivePatternAndFiredRuleAndFailedAssertAtIndex(i) instanceof FailedAssert) {
                    handleSchematronErrors(
                            xmlFile,
                            validationResult,
                            preProcessResult,
                            (FailedAssert) schematronOutputType
                            .getActivePatternAndFiredRuleAndFailedAssertAtIndex(i));
                }
            }

            for (int i = 0; i < validationResult.getCheckedFiles().size(); i++) {
                File f = new File(validationResult.getCheckedFiles().get(i));
                validationResult.getCheckedFiles().set(i, f.getName());
            }
        } catch (SAXParseException e) {
            // Occurs if Document is not well-formed
            validationResult.getViolations().add(
                    new Violation("XSD-Check", xmlFile.getName(), e
                            .getLineNumber(), "", e.getMessage()));
            validationResult.getCheckedFiles().add(xmlFile.getName());
            LOGGER.info("XML not well-formed in {} at line {}",
                    xmlFile.getName(), e.getLineNumber());
        } catch (SAXException | IOException e) {
            PrintHelper.printFileNotFoundLogs(LOGGER, e, xmlFile.getName());
            throw new BpmnValidationException(
                    "Given file couldn't be read or doesn't exist!");
        } catch (Exception e) { // NOPMD
            LOGGER.debug("exception at schematron validation. Cause: {}", e);
            throw new BpmnValidationException(
                    "Something went wrong during schematron validation!");
        }

        validationResult.setValid(validationResult.getViolations().isEmpty());
        LOGGER.info("Validating process successfully done, file is valid: {}",
                validationResult.isValid());

        return validationResult;
    }

    /**
     * tries to locate errors in the specific files
     *
     * @param xmlFile
     *            the file where the error must be located with the help of the
     *            {@link XmlLocator}
     * @param validationResult
     *            the result of the validation to add new found errors
     * @param preProcessResult
     *            the result of the preprocessing step to be able to detect
     *            file-across errors after the merging in the preprocessing step
     * @param failedAssert
     *            the error of the schematron validation
     */
    private void handleSchematronErrors(File xmlFile,
            ValidationResult validationResult,
            PreProcessResult preProcessResult, FailedAssert failedAssert) {
        String message = failedAssert.getText().trim();
        String constraint = message.substring(0, message.indexOf('|'));
        String errorMessage = message.substring(message.indexOf('|') + 1);
        int line = xmlLocator.findLine(xmlFile, failedAssert.getLocation());
        String fileName = xmlFile.getName();
        String location = failedAssert.getLocation();

        if (line == -1) {
            try {
                String xpathId = "";
                if (failedAssert.getDiagnosticReferenceCount() > 0) {
                    xpathId = failedAssert.getDiagnosticReference().get(0)
                            .getText().trim();
                }
                String[] result = searchForViolationFile(xpathId,
                        validationResult, preProcessResult.getNamespaceTable());
                fileName = result[0];
                line = Integer.parseInt(result[1]);
                location = result[2];
            } catch (BpmnValidationException e) {
                fileName = e.getMessage();
                LOGGER.error("Line of affected Element could not be determined.");
            } catch (StringIndexOutOfBoundsException e) {
                fileName = "Element couldn't be found!";
                LOGGER.error("Line of affected Element could not be determined.");
            }
        }

        String logText = String.format(
                "violation of constraint %s found in %s at line %s.",
                constraint, fileName, line);
        LOGGER.info(logText);
        validationResult.getViolations().add(
                new Violation(constraint, fileName, line, location,
                        errorMessage));
    }

    /**
     * searches for the file and line, where the violation occured
     *
     * @param xpathExpression
     *            the expression, through which the file and line should be
     *            identified
     * @param validationResult
     *            for getting all checked files
     * @param namespaceTable
     *            to identify the file, where the violation occured
     * @return string array with filename, line and xpath expression to find the
     *         element
     * @throws BpmnValidationException
     *             if no element can be found
     */
    private String[] searchForViolationFile(String xpathExpression,
            ValidationResult validationResult,
            Map<String, String> namespaceTable) throws BpmnValidationException {
        String fileName = "";
        String line = "-1";
        String xpathObjectId = "";

        String namespacePrefix = xpathExpression.substring(0,
                xpathExpression.indexOf('_'));
        String namespace = "";
        for (Map.Entry<String, String> entry : namespaceTable.entrySet()) {
            if (entry.getValue().equals(namespacePrefix)) {
                namespace = entry.getKey();
            }
        }

        for (String checkedFilePath : validationResult.getCheckedFiles()) {
            File checkedFile = new File(checkedFilePath);
            try {
                Document document = documentBuilder.parse(checkedFile);
                if (document.getDocumentElement()
                        .getAttribute("targetNamespace").equals(namespace)) {
                    xpathObjectId = BpmnHelper
                            .createIdBpmnExpression(xpathExpression
                                    .substring(xpathExpression.indexOf('_') + 1));
                    line = String.valueOf(xmlLocator.findLine(checkedFile,
                            xpathObjectId));
                    xpathObjectId += "[0]"; // NOPMD
                    fileName = checkedFile.getName();
                    break;
                }
            } catch (SAXException | IOException e) {
                PrintHelper.printFileNotFoundLogs(LOGGER, e,
                        checkedFile.getName());
            }
        }

        if ("-1".equals(line)) {
            throw new BpmnValidationException("BPMN Element couldn't be found!");
        }

        return new String[] { fileName, line, xpathObjectId };
    }
}
