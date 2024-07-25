package com.codesquad.cafe;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class E2ETestBase {
    private static final Logger logger = LoggerFactory.getLogger(E2ETestBase.class);

    private static Tomcat tomcat;

    protected static Context context;

    protected static int port;

    @BeforeAll
    public static synchronized void setUpClass() throws Exception {
        if (tomcat == null) {
            tomcat = startEmbeddedTomcat();
            port = tomcat.getConnector().getPort();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    stopEmbeddedTomcat();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
        }
    }

    private static Tomcat startEmbeddedTomcat() throws Exception {
        Tomcat tomcat = new Tomcat();

        port = getAvailablePort();
        tomcat.setPort(port);

        context = addWebApp(tomcat);
        addClassesToWebInfClasses(context);

        setErrorReportValue(tomcat);

        tomcat.start();

        logger.info("context initialized with {} servlet mappings, {} context listener",
                context.findApplicationListeners().length,
                context.findServletMappings().length);

        return tomcat;
    }

    private static int getAvailablePort() throws IOException, InterruptedException {
        ServerSocket socket = new ServerSocket(0);
        int localPort = socket.getLocalPort();
        socket.close();
        Thread.sleep(1000);
        return localPort;
    }

    private static Context addWebApp(Tomcat tomcat) {
        String webAppPath = new File("src/main/webapp").getAbsolutePath();
        return tomcat.addWebapp("", webAppPath);
    }

    private static void setErrorReportValue(Tomcat tomcat) {
        ErrorReportValve errorReportValve = new ErrorReportValve();
        errorReportValve.setShowReport(false);
        errorReportValve.setShowServerInfo(false);
        tomcat.getHost().getPipeline().addValve(errorReportValve);
    }

    private static void addClassesToWebInfClasses(Context context) {
        File additionWebInfClasses = new File("build/classes/java/main");
        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(
                new DirResourceSet(resources, "/WEB-INF/classes", additionWebInfClasses.getAbsolutePath(), "/"));
        context.setResources(resources);
    }


    private static void stopEmbeddedTomcat() throws LifecycleException {
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
            tomcat = null;
        }
    }

    // path should start with "/"
    protected SavedHttpResponse get(String path) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault();
             CloseableHttpResponse response = client.execute(new HttpGet("http://localhost:" + port + path));) {
            response.getStatusLine();
            response.getAllHeaders();
            response.getEntity();
            return new SavedHttpResponse(response.getStatusLine(), response.getAllHeaders(),
                    EntityUtils.toString(response.getEntity()));
        }
    }

    // path should start with "/"
    protected HttpResponse post(String path, String body) throws IOException {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost("http://localhost:" + port + path);
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");
            request.setEntity(new StringEntity(body));
            CloseableHttpResponse response = client.execute(request);
            return response;
        }
    }
}
