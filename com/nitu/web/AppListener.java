
package com.nitu.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Web application lifecycle listener.
 *
 * @author nchiring
 */
@WebListener()
public class AppListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        //do nothing for now
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        try {
           LatencyWatchdog.shutDown();
        }catch(Exception e){
            //ignore; we are shutting down anyway
        }
    }
}
