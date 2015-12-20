package gov.ncbi.pmc.cite.test;

import static org.hamcrest.text.MatchesPattern.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mock;
import org.slf4j.Logger;

import gov.ncbi.pmc.cite.App;
import gov.ncbi.pmc.cite.Request;

/**
 * Unit test for simple App.
 */
public class RequestTest {
    protected App app;
    private Logger log;

    @Rule
    public TestName name = new TestName();

    @Mock
    HttpServletRequest req;

    @Mock
    HttpServletResponse resp;

    @Before
    public void setup()  throws Exception
    {
        log = TestUtils.setup(name);
        initMocks(this);
    }

    /**
     * Test the request object.
     */
    @Test
    public void testRequest() throws Exception
    {
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

}
