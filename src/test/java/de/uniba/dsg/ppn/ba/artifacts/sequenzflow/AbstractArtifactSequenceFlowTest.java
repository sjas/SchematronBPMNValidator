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
package de.uniba.dsg.ppn.ba.artifacts.sequenzflow;

import org.junit.Test;

import de.uniba.dsg.bpmnspector.common.Violation;
import de.uniba.dsg.ppn.ba.TestCase;
import de.uniba.dsg.ppn.ba.helper.BpmnValidationException;

/**
 * Abstract test class for simplifying the testing of the Constraints EXT.006
 * and EXT.007
 *
 * @author Philipp Neugebauer
 * @version 1.0
 *
 */
abstract public class AbstractArtifactSequenceFlowTest extends TestCase {

    @Test
    public void testConstraintAssociationFail() throws BpmnValidationException {
        assertTests("Fail_association.bpmn", 11);

    }

    @Test
    public void testConstraintGroupFail() throws BpmnValidationException {
        assertTests("Fail_group.bpmn", 8);
    }

    @Test
    public void testConstraintTextAnnotationFail()
            throws BpmnValidationException {
        assertTests("Fail_text_annotation.bpmn", 8);
    }

    private void assertTests(String fileName, int line)
            throws BpmnValidationException {
        de.uniba.dsg.bpmnspector.common.ValidationResult result = verifyInValidResult(
                createFile(fileName), 3);
        assertFirstViolation(result.getViolations().get(0), fileName);
        assertSecondViolation(result.getViolations().get(1), fileName, line);
        assertThirdViolation(result.getViolations().get(2), fileName);
    }

    protected void assertFirstViolation(Violation v, String fileName) {
        throw new UnsupportedOperationException(
                "must be overriden by every child class!");
    }

    protected void assertSecondViolation(Violation v, String fileName, int line) {
        throw new UnsupportedOperationException(
                "must be overriden by every child class!");
    }

    protected void assertThirdViolation(Violation v, String fileName) {
        throw new UnsupportedOperationException(
                "must be overriden by every child class!");
    }

}
