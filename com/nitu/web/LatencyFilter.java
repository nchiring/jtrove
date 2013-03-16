package com.nitu.web;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author nchiring
 */
@WebFilter(filterName = "LatencyFilter", urlPatterns = {"/*"})
public class LatencyFilter implements Filter {
    
    private static final boolean debug = true;
    // The filter configuration object we are associated with.  If
    // this value is null, this filter instance is not currently
    // configured. 
    private FilterConfig filterConfig = null;
    
    public LatencyFilter() {
    }    
    
    
    /**
     *
     * @param request The servlet request we are processing
     * @param response The servlet response we are creating
     * @param chain The filter chain we are processing
     *
     * @exception IOException if an input/output error occurs
     * @exception ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

           try{
               if (debug) {                
                   log("LatencyFilter:doFilter()--starting watch");
               }
               LatencyWatchdog.start(Thread.currentThread(), ((HttpServletRequest)request), filterConfig.getServletContext() );
               //keep moving the chain
               chain.doFilter(request, response);
           }finally{
               if (debug) {                
                   log("LatencyFilter:doFilter()--ending watch");
               }
               LatencyWatchdog.end(Thread.currentThread(), ((HttpServletRequest)request));
           }
       
    }

   
    /**
     * Destroy method for this filter
     */
    @Override
    public void destroy() {        
    }

    /**
     * Init method for this filter
     */
    @Override
    public void init(FilterConfig filterConfig) {        
        this.filterConfig = filterConfig;
        if (filterConfig != null) {
            if (debug) {                
                log("LatencyFilter:Initializing filter");
            }
        }
    }
    
    public void log(String msg) {
        filterConfig.getServletContext().log(msg);        
    }
}
