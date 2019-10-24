/*
 * The MIT License
 *
 * Copyright 2018 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.driver.chrome;

import com.intuit.karate.FileUtils;
import com.intuit.karate.Http;
import com.intuit.karate.LogAppender;
import com.intuit.karate.core.ScenarioContext;
import com.intuit.karate.shell.Command;
import com.intuit.karate.driver.DevToolsDriver;
import com.intuit.karate.driver.DriverOptions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 *
 * chrome devtools protocol - the "preferred" driver:
 * https://chromedevtools.github.io/devtools-protocol/
 *
 * @author pthomas3
 */
public class Chrome extends DevToolsDriver {

    public static final String DEFAULT_PATH_MAC = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
    public static final String DEFAULT_PATH_WIN = "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe";
    public static final String DEFAULT_PATH_LINUX = "/usr/bin/google-chrome";


    public Chrome(DriverOptions options, Command command, String webSocketUrl) {
        super(options, command, webSocketUrl);
    }    

    public static Chrome start(ScenarioContext context, Map<String, Object> map, LogAppender appender) {
        DriverOptions options = new DriverOptions(context, map, appender, 9222, 
                FileUtils.isOsWindows() ? DEFAULT_PATH_WIN : FileUtils.isOsMacOsX()?DEFAULT_PATH_MAC:DEFAULT_PATH_LINUX);
        options.arg("--remote-debugging-port=" + options.port);
        options.arg("--no-first-run");
        options.arg("--user-data-dir=" + options.workingDirPath);
        options.arg("--disable-popup-blocking");
        if (options.headless) {
            options.arg("--headless");
        }
        Command command = options.startProcess();
        String url = "http://" + options.host + ":" + options.port;
        Http http = Http.forUrl(options.driverLogger.getLogAppender(), url);
        Http.Response res = http.path("json").get();
        if (res.body().asList().isEmpty()) {
            if (command != null) {
                command.close(true);    
            }            
            throw new RuntimeException("chrome server returned empty list from " + url);
        }
        String webSocketUrl = res.jsonPath("get[0] $[?(@.type=='page')].webSocketDebuggerUrl").asString();        
        Chrome chrome = new Chrome(options, command, webSocketUrl);
        chrome.activate();
        chrome.enablePageEvents();
        chrome.enableRuntimeEvents();
        chrome.enableTargetEvents();
        if (!options.headless) {
            chrome.initWindowIdAndState();
        }
        return chrome;
    }
    
    public static Chrome start(String chromeExecutablePath, boolean headless) { 
        Map<String, Object> options = new HashMap();
        options.put("executable", chromeExecutablePath);
        options.put("headless", headless);
        return Chrome.start(null, options, null);
    }   
    
    public static Chrome start(Map<String, Object> options) {
        if (options == null) {
            options = new HashMap();
        }
        return Chrome.start(null, options, null);         
    }
    
    public static Chrome start() {
        return start(null);
    }
    
    public static Chrome startHeadless() {
        return start(Collections.singletonMap("headless", true));
    }

}
