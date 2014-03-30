package de.uniba.dsg.ppn.ba;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class Ext056 {

	// FIXME: error message due to ext.023 decision

	@Test
	public void testConstraintCallChoreographyFail() throws Exception {
		File f = new File(TestHelper.getTestFilePath()
				+ "056\\fail_call_choreography.bpmn");
		boolean valid = SchematronBPMNValidator.validateViaPureSchematron(f);
		assertEquals(valid, false);
		// assertEquals(SchematronBPMNValidator.getErrors(),
		// "//bpmn:subProcess[0]: A SubProcess must not contain Choreography Activities");
	}

	@Test
	public void testConstraintChoreographyTaskFail() throws Exception {
		File f = new File(TestHelper.getTestFilePath()
				+ "056\\fail_choreography_task.bpmn");
		boolean valid = SchematronBPMNValidator.validateViaPureSchematron(f);
		assertEquals(valid, false);
		// assertEquals(SchematronBPMNValidator.getErrors(),
		// "//bpmn:subProcess[0]: A SubProcess must not contain Choreography Activities");
	}

	// FIXME: for every $subprocess element does not work
	// @Test
	// public void testConstraintChoreographyTaskTransactionFail()
	// throws Exception {
	// File f = new File(TestHelper.getTestFilePath()
	// + "056\\fail_choreography_task_transaction.bpmn");
	// boolean valid = SchematronBPMNValidator.validateViaPureSchematron(f);
	// assertEquals(valid, false);
	// assertEquals(SchematronBPMNValidator.getErrors(),
	// "//bpmn:subProcess[0]: A SubProcess must not contain Choreography Activities");
	// }

	@Test
	public void testConstraintSubChoreographyFail() throws Exception {
		File f = new File(TestHelper.getTestFilePath()
				+ "056\\fail_sub_choreography.bpmn");
		boolean valid = SchematronBPMNValidator.validateViaPureSchematron(f);
		assertEquals(valid, false);
		// assertEquals(SchematronBPMNValidator.getErrors(),
		// "//bpmn:subProcess[0]: A SubProcess must not contain Choreography Activities");
	}
}
