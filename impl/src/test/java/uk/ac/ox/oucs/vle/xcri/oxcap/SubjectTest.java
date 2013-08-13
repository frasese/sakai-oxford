package uk.ac.ox.oucs.vle.xcri.oxcap;

import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Test;

import java.io.StringReader;

import static org.junit.Assert.*;

public class SubjectTest {

	@Test
	public void testSimpleRDF() throws Exception {
		Subject subject = createSubject(Subject.RDF.getURI(), "CD", "Career Development");
		assertEquals("CD", subject.getIdentifier());
		assertEquals("Career Development", subject.getValue());
		assertTrue(subject.isRDFCategory());
		assertFalse(subject.isJACSCategory());
		assertFalse(subject.isRMCategory());
		assertTrue(subject.isValid());
	}

	@Test
	public void testSimpleRM() throws Exception {
		Subject subject = createSubject(Subject.RM.getURI(), "QL", "Qualitative");
		assertEquals("QL", subject.getIdentifier());
		assertEquals("Qualitative", subject.getValue());
		assertFalse(subject.isRDFCategory());
		assertTrue(subject.isRMCategory());
		assertFalse(subject.isJACSCategory());
		assertTrue(subject.isValid());
	}

	@Test
	public void testSimpleJACS() throws Exception {
		Subject subject = createSubject(Subject.JACS.getURI(), "A200", "Pre-clinical Dentistry");
		assertEquals("A200", subject.getIdentifier());
		assertEquals("Pre-clinical Dentistry", subject.getValue());
		assertFalse(subject.isRDFCategory());
		assertFalse(subject.isRMCategory());
		assertTrue(subject.isJACSCategory());
		assertTrue(subject.isValid());
	}

	@Test
	public void testUnknown() throws Exception {
		Subject subject = createSubject("http://unknown.domain/namespace", "ID", "Value");
		assertEquals("ID", subject.getIdentifier());
		assertEquals("Value", subject.getValue());
		assertFalse(subject.isRDFCategory());
		assertFalse(subject.isRMCategory());
		assertFalse(subject.isJACSCategory());
		assertFalse(subject.isValid());
	}

	@Test
	public void testDifferentXml() throws Exception {
		// Here we have a different prefix for the xsi namespace.
		Subject subject = createSubject("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
				"<dc:subject " +
				"xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
				"xmlns:xsi2=\"http://www.w3.org/2001/XMLSchema-instance\" "+
				"xmlns:ns=\"https://data.ox.ac.uk/id/ox-rdf/\" " +
				"xsi2:type=\"ns:notation\" identifier=\"ID\">Value</dc:subject>");
		assertEquals("ID", subject.getIdentifier());
		assertEquals("Value", subject.getValue());
		assertTrue(subject.isRDFCategory());
		assertFalse(subject.isRMCategory());
		assertFalse(subject.isJACSCategory());
		assertTrue(subject.isValid());
	}


	private Subject createSubject(String namespace, String identifier, String value) throws Exception {
		Subject subject = createSubject("<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
				"<dc:subject " +
				"xmlns:dc=\"http://purl.org/dc/elements/1.1/\" " +
				"xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
				"xmlns:ns=\"" + namespace + "\" " +
				"xsi:type=\"ns:notation\" identifier=\"" + identifier + "\">" + value + "</dc:subject>");
		return subject;
	}

	private Subject createSubject(String xml) throws Exception {
		SAXBuilder builder = new SAXBuilder();
		builder.setValidation(false);
		Document document = builder.build(new StringReader(xml));
		Subject subject = new Subject();
		subject.fromXml(document.getRootElement());
		return subject;
	}

}
