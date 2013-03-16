package com.nitu.web;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;

import javax.servlet.http.HttpServletRequest;

/**
 * Dumps thread and heap details if a request doesn't complete within an allowed time threshold, as
 * specified by jvm system property <code>latency.threshold</code>
 * 
 * @author nchiring
 */
public class LatencyWatchdog implements Runnable {
	
	
	private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);
	
	private static final Map<Thread, ScheduledFuture<?>> futures = new HashMap<Thread, ScheduledFuture<?>> ();
	

	public static void  start(Thread serviceThread, HttpServletRequest request, ServletContext context ){
		LatencyWatchdog monitor = new LatencyWatchdog(serviceThread, request, context );
		ScheduledFuture<?> future = scheduler.schedule(monitor, Integer.getInteger("latency.threshold", 1), TimeUnit.MINUTES );
		futures.put(serviceThread, future );
	}
	
        
	public static void end(Thread serviceThread, HttpServletRequest request ){
		ScheduledFuture<?> currentFuture = futures.get(serviceThread);
		if( currentFuture !=null ){
			try {
			   currentFuture.cancel(true);
			}catch(Exception e){
				
			}
			futures.remove(serviceThread);
		}
	}

        public static void shutDown(){
                           futures.clear();
			   scheduler.shutdownNow();
        }
	
		
    private Thread serviceThread = null;
    private HttpServletRequest request = null;
    private ServletContext context =null;
    
    
	private LatencyWatchdog(Thread serviceThread, HttpServletRequest request, ServletContext context ) {
		super();
		this.serviceThread = serviceThread;
		this.request = request;
                this.context =context;
	}


        @Override
	public void run() {

		     try {

                                        StringWriter sw = new StringWriter();
                                        PrintWriter writer = new PrintWriter(sw);
   
				        writer.println(".....Starting reporting.....");
        
                                        addRequestDetails( this.request, writer);
                                        
                                        addMemoryDetails( writer);
                                         
	
                                         writer.println("==============Current service thread details ============");  
                                         writer.println("thread id =" + serviceThread.getId() );
                                         writer.println(" & name=" + serviceThread.getName() ) ;
                                         
                                         writer.println("==============Thread dump ============"); 
                                         
				        //dump Live thread details
                                         StringBuilder dump = new StringBuilder();
				         ThreadInfo[]  threadInfos = ManagementFactory.getThreadMXBean().dumpAllThreads(true,true);
						
					int count = 0;
					for (ThreadInfo threadInfo : threadInfos) {
					    count++;
					    dump.append(threadInfo);
					   if( count % 5 == 0) {
					    	 writer.println( dump.toString() );
					    	 dump = new StringBuilder();
                                           }
						    
					}
					writer.println( dump.toString() );
                                        writer.println("==============Thread dump ends============"); 
                                        
                                        // Log the resulting string
                                        writer.flush();
                                        this.context.log(sw.toString());
   
		     }catch(Exception e){
		    	 this.context.log("error during run() ", e);
		     }finally{
		    	    ScheduledFuture<?> currentFuture = futures.get(this.serviceThread);
					if( currentFuture !=null ){
						futures.remove(this.serviceThread);
					}
		     }
				
	}
   

	 
         private static final int MB = 1024*1024;
	 private void addMemoryDetails( PrintWriter writer ) {

                         writer.println("============== Heap details [MB] ============");  
			//Getting the runtime reference from system
			Runtime runtime = Runtime.getRuntime();
			writer.println("Used Memory="+ ((runtime.totalMemory() - runtime.freeMemory()) / MB) );
			writer.println("Free Memory=" + ( runtime.freeMemory() / MB) );

			//Print total available memory
			writer.println("Total Memory=" + ( runtime.totalMemory() / MB) );
			//Print Maximum available memory
			writer.println("Max Memory=" + (runtime.maxMemory() / MB) );
	 }
         
         private void addRequestDetails( HttpServletRequest request, PrintWriter writer){
                writer.println("============== Request Details ============");  
                writer.println(" characterEncoding=" + request.getCharacterEncoding());
                writer.println("     contentLength=" + request.getContentLength());
                writer.println("       contentType=" + request.getContentType());
                writer.println("            locale=" + request.getLocale());
                writer.print("           locales=");
                Enumeration locales = request.getLocales();
                boolean first = true;
                while (locales.hasMoreElements()) {
                    Locale locale = (Locale) locales.nextElement();
                    if (first)
                        first = false;
                    else
                        writer.print(", ");
                    writer.print(locale.toString());
                }
                writer.println();
                Enumeration names = request.getParameterNames();
                while (names.hasMoreElements()) {
                    String name = (String) names.nextElement();
                    writer.print("         parameter=" + name + "=");
                    String values[] = request.getParameterValues(name);
                    for (int i = 0; i < values.length; i++) {
                        if (i > 0)
                            writer.print(", ");
                        writer.print(values[i]);
                    }
                    writer.println();
                }
                writer.println("          protocol=" + request.getProtocol());
                writer.println("        remoteAddr=" + request.getRemoteAddr());
                writer.println("        remoteHost=" + request.getRemoteHost());
                writer.println("            scheme=" + request.getScheme());
                writer.println("        serverName=" + request.getServerName());
                writer.println("        serverPort=" + request.getServerPort());
                writer.println("          isSecure=" + request.isSecure());

                // Render the HTTP servlet request properties
                if (request instanceof HttpServletRequest) {
                    writer.println("---------------------------------------------");
                    HttpServletRequest hrequest = (HttpServletRequest) request;
                    writer.println("       contextPath=" + hrequest.getContextPath());
                    Cookie cookies[] = hrequest.getCookies();
                         if (cookies == null)
                             cookies = new Cookie[0];
                    for (int i = 0; i < cookies.length; i++) {
                        writer.println("            cookie=" + cookies[i].getName() +
                             "=" + cookies[i].getValue());
                    }
                    names = hrequest.getHeaderNames();
                    while (names.hasMoreElements()) {
                        String name = (String) names.nextElement();
                   String value = hrequest.getHeader(name);
                        writer.println("            header=" + name + "=" + value);
                    }
                    writer.println("            method=" + hrequest.getMethod());
                    writer.println("          pathInfo=" + hrequest.getPathInfo());
                    writer.println("       queryString=" + hrequest.getQueryString());
                    writer.println("        remoteUser=" + hrequest.getRemoteUser());
                    writer.println("requestedSessionId=" +
                         hrequest.getRequestedSessionId());
                    writer.println("        requestURI=" + hrequest.getRequestURI());
                    writer.println("       servletPath=" + hrequest.getServletPath());
                }
                
         }
}
