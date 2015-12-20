package gov.ncbi.pmc.cite.test;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import gov.ncbi.pmc.cite.App;
import gov.ncbi.pmc.cite.Request;

/**
 * Unit test for Requests. This uses Mockito to mock HttpServletRequest and
 * HttpServletResponse objects. It reads test cases from request-tests.json
 * into a list of RequestTestCase objects.
 */
public class TestRequests {
    protected App app;
    private Logger log;

    @Rule
    public TestName name = new TestName();

    @Before
    public void setup()  throws Exception
    {
        log = Utils.setup(name);
    }

    /**
     * Test the request object. This is a simple manual test using Mockito,
     * to show how it works.
     */
    @Test
    public void testRequestSimple() throws Exception
    {
        HttpServletRequest req = mock(HttpServletRequest.class);
        HttpServletResponse resp = mock(HttpServletResponse.class);

        when(req.getParameter("id")).thenReturn("30");
        when(req.getParameter("idtype")).thenReturn("aiid");

        StringWriter page = new StringWriter();
        when(resp.getWriter()).thenReturn(new PrintWriter(page));

        Request r = new Request(req, resp);
        r.doGet();
        verify(resp).setContentType(eq("text/html;charset=UTF-8"));
        verify(resp).setCharacterEncoding(eq("UTF-8"));
        verify(resp).setStatus(eq(200));
        assertThat(page.toString(), matchesPattern(".*aiid:30.*"));
    }

    /**
     * Data-driven request tests; uses request-cases.json.
     */
    @Test
    public void testRequestTestCases() throws Exception
    {
        // Read the transform-tests.json file
        ObjectMapper mapper = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        URL testCasesUrl = getClass().getClassLoader()
            .getResource("request-tests.json");
        List<RequestTestCase> testCaseList =
            mapper.readValue(testCasesUrl.openStream(),
                new TypeReference<List<RequestTestCase>>() {});

        for (RequestTestCase testCase: testCaseList) {
            HttpServletRequest req = mock(HttpServletRequest.class);
            HttpServletResponse resp = mock(HttpServletResponse.class);

            // Set the values that the mock request object will
            // return for the query-string parameters
            for (Map.Entry<String, String> param :
                 testCase.requestParams.entrySet())
            {
                when(req.getParameter(param.getKey()))
                  .thenReturn(param.getValue());
            }

            // Create a StringWriter to hold the response content
            StringWriter page = new StringWriter();
            when(resp.getWriter()).thenReturn(new PrintWriter(page));

            // This is what we are testing
            Request r = new Request(req, resp);
            r.doGet();

            // Verify the results
            // FIXME: I'd like to figure out how to customize the mockito
            // failure messages. Until I do, this log message will have to
            // suffice
            log.info("Verifying request '" + testCase.description + "'");
            RequestTestCase.Expect expect = testCase.expect;
            verify(resp).setStatus(eq(expect.status));
            verify(resp).setContentType(eq(expect.contentType));
            verify(resp).setCharacterEncoding(eq(expect.characterEncoding));
            String content = page.toString();
            for (String pattern : expect.patterns) {
                Pattern p = Pattern.compile("^.*" + pattern + ".*$",
                        Pattern.DOTALL);
                assertThat(content, matchesPattern(p));
            }
        }
    }
}
