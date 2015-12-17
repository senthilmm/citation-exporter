package gov.ncbi.pmc.cite.test;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.w3c.dom.Document;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.ncbi.pmc.cite.App;
import gov.ncbi.pmc.cite.ItemSource;
import gov.ncbi.pmc.cite.TransformEngine;
import gov.ncbi.pmc.ids.Identifier;
import gov.ncbi.pmc.ids.RequestId;

public class TransformTest {
    protected App app;
    private Logger log;

    @Rule
    public TestName name = new TestName();

    /**
     * Test the transformations
     */
    @Test
    public void testTransforms() throws Exception
    {
        log = TestSetup.setup(name);
        ItemSource itemSource = App.getItemSource();

        Document nxml_31 = null;
        RequestId requestId = new RequestId("31", new Identifier("aiid", "31"));
        nxml_31 = itemSource.retrieveItemNxml(requestId);
        assertEquals(nxml_31.getDocumentElement().getTagName(), "article");
    }

    /**
     * Run all the test cases
     */
    @Test
    public void testCases() throws Exception
    {
        log = TestSetup.setup(name);

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
            log.trace("Source document:");
            log.trace(serializeXml(srcDoc));


            // Do the transformation
            Object result = engine.doTransform(srcDoc, transform);
            log.trace("Transform result:");
            log.trace(serializeXml((Document) result));

        }
    }

    public String serializeXml(Document doc)    {
        try
        {
           DOMSource domSource = new DOMSource(doc);
           StringWriter writer = new StringWriter();
           StreamResult result = new StreamResult(writer);
           TransformerFactory tf = TransformerFactory.newInstance();
           Transformer transformer = tf.newTransformer();
           transformer.transform(domSource, result);
           writer.flush();
           return writer.toString();
        }
        catch(TransformerException ex)
        {
           ex.printStackTrace();
           return null;
        }
    }
}

