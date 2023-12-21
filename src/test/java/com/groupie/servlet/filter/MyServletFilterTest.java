package com.groupie.servlet.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class MyServletFilterTest {

    HttpServletRequest request;
    HttpServletResponse response;

    @BeforeEach
    public void setup() {
        request = mock(HttpServletRequestWrapper.class);
        response = mock(HttpServletResponse.class);
    }

    @AfterEach
    public void tearDown() {

    }

//    @Test
//    public void testSomething() throws ServletException, IOException {
//        when(request.getParameter(MyServletFilter.SPACE_KEY)).thenReturn("mySpaceKey");
//        when(request.getParameter(MyServletFilter.TITLE)).thenReturn("myTitle");
//        when(request.getParameterMap()).thenReturn(Map.of(
//                MyServletFilter.SPACE_KEY, "mySpaceKey",
//                MyServletFilter.TITLE, "myTitle"
//        ));
//        PageManager pageManager = mock(PageManager.class);
//        SpaceManager spaceManager = mock(SpaceManager.class);
//
//        MyServletFilter filter = new MyServletFilter(pageManager, spaceManager, null, groupResolver, null, null, null );
//
//        FilterChain chain = mock(FilterChain.class);
////        filter.doFilter(request, response, chain);
//
//        verifyZeroInteractions(chain);
//        //verify(chain, times(0)).doFilter();
//    }
}
