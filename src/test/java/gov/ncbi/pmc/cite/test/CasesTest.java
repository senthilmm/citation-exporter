package gov.ncbi.pmc.cite.test;

import static gov.ncbi.pmc.cite.test.TestUtils.serializeXml;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nonnull;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.helger.commons.xml.serialize.read.DOMInputStreamProvider;
import com.helger.schematron.ISchematronResource;
import com.helger.schematron.pure.SchematronResourcePure;

import gov.ncbi.pmc.cite.App;
import gov.ncbi.pmc.cite.BadParamException;
import gov.ncbi.pmc.cite.ItemSource;
import gov.ncbi.pmc.cite.TransformEngine;
import gov.ncbi.pmc.ids.RequestId;

/**
 * Test the "cases" defined in test-cases.json
 */
public class CasesTest {
    protected App app;
    private Logger log;

    @Rule
    public TestName name = new TestName();


    /**
     * Run all the test cases
     */
    @Test
    public void testCases() throws Exception
    {
        log = TestUtils.setup(name);

        // Read the test-cases json file
        ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        URL testCasesUrl = getClass().getClassLoader()
            .getResource("test-cases.json");
        List<TestCaseDescriptor> testCaseList =
            mapper.readValue(testCasesUrl.openStream(),
                new TypeReference<List<TestCaseDescriptor>>() {});

        ItemSource itemSource = App.getItemSource();
        TransformEngine engine = App.getTransformEngine();
        Document srcDoc = null;


        log.info("Number of test cases: " + testCaseList.size());

        Iterator<TestCaseDescriptor> i = testCaseList.iterator();
        while (i.hasNext()) {
            TestCaseDescriptor testCase = i.next();
            String description = testCase.description;
            log.info("Running transform test '" + description + "'");

            String id = testCase.id;
            String inReport = testCase.inreport;
            String transform = testCase.transform;

            // Get the input
            RequestId rid = new RequestId(id);
            if (inReport.equals("nxml")) {
                srcDoc = itemSource.retrieveItemNxml(rid);
            }
            else if (inReport.equals("pub1")) {
            	srcDoc = itemSource.retrieveItemPubOne(rid);
            }
            else {
            	throw new BadParamException("Bad value in test-cases for 'inreport': " + inReport);
            }
            log.trace("Source document:");
            log.trace(serializeXml(srcDoc));

            // Do the transformation
            Object result = engine.doTransform(srcDoc, transform);
            Document resultDocument = null;
            log.trace("Transform result:");
            if (result instanceof String) {
            	// result must be a json document
            	String resultString = (String) result;
            	log.trace(resultString);

            	// use Jackson to convert it to an XML Document
            	ObjectMapper jsonMapper = new ObjectMapper();
            	JsonNode resultJson = jsonMapper.readTree(resultString);
            	XmlMapper xmlMapper = new XmlMapper();
            	String xmlString = xmlMapper.writeValueAsString(resultJson);
            	log.trace("XML:\n" + xmlString);

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        	    DocumentBuilder builder = factory.newDocumentBuilder();
        	    InputSource is = new InputSource(new StringReader(xmlString));
        	    resultDocument = builder.parse(is);
            }
            else {
            	resultDocument = (Document) result;
                log.trace(serializeXml(resultDocument));
            }

            URL schematronUrl = getClass().getClassLoader()
                .getResource(testCase.validator + ".sch");
            assertTrue("Failed schematron validation",
                validateXml(schematronUrl, resultDocument));
        }
    }

    /**
     * Test whether an XML file conforms to a given Schematron
     * @param sstr
     * @param xml
     * @return
     * @throws IllegalArgumentException
     */
    static boolean validateXml(
            @Nonnull final URL schematronUrl,
            @Nonnull final Document xml)
        throws Exception
    {
        final ISchematronResource s =
            SchematronResourcePure.fromURL(schematronUrl);
        if (!s.isValidSchematron())
            throw new IllegalArgumentException("Invalid Schematron!");
        return s.getSchematronValidity(
            new DOMInputStreamProvider(xml)).isValid();
    }


}
