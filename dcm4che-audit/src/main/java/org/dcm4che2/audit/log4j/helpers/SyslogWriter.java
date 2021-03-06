/*
 * Copyright 1999-2005 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//provided by dcm4che-audit for use with versions before log4j-1.2.14
//package org.apache.log4j.helpers;
package org.dcm4che2.audit.log4j.helpers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import java.net.URL;
import java.net.MalformedURLException;


/**
   SyslogWriter is a wrapper around the java.net.DatagramSocket class
   so that it behaves like a java.io.Writer.

   @since 0.7.3
*/
public class SyslogWriter extends Writer {
  private static final int SYSLOG_PORT = 514;
  private InetAddress address;
  private final int port;
  private DatagramSocket ds;
  private String encoding;

  private Logger logger = LogManager.getLogger(SyslogWriter.class);
  private StringBuffer buf = new StringBuffer();
  
  /**
   *  Constructs a new instance of SyslogWriter.
   *  @param syslogHost host name, may not be null.  A port
   *  may be specified by following the name or IPv4 literal address with
   *  a colon and a decimal port number.  To specify a port with an IPv6
   *  address, enclose the IPv6 address in square brackets before appending
   *  the colon and decimal port number.
   */
  public SyslogWriter(final String syslogHost, String encoding) {
    if (syslogHost == null) {
        throw new NullPointerException("syslogHost");
    }
    
    String host = syslogHost;
    int urlPort = -1;
    
    //
    //  If not an unbracketed IPv6 address then
    //      parse as a URL
    //
    if (host.indexOf("[") != -1 || host.indexOf(':') == host.lastIndexOf(':')) {
        try {
            URL url = new URL("http://" + host);
            if (url.getHost() != null) {
                host = url.getHost();
                //   if host is a IPv6 literal, strip off the brackets
                if(host.startsWith("[") && host.charAt(host.length() - 1) == ']') {
                    host = host.substring(1, host.length() - 1);
                }
                urlPort = url.getPort();
            }
        } catch(MalformedURLException e) {
                logger.warn("Malformed URL: will attempt to interpret as InetAddress.", e);
        }
    }
    
    if (urlPort == -1) {
        urlPort = SYSLOG_PORT;
    }
    port = urlPort;

    try {
      this.address = InetAddress.getByName(host);
    } catch (UnknownHostException e) {
      logger.error(
        "Could not find " + host + ". All logging will FAIL.", e);
    }

    try {
      this.ds = new DatagramSocket();
    } catch (SocketException e) {
      e.printStackTrace();
      logger.error(
        "Could not instantiate DatagramSocket to " + host
        + ". All logging will FAIL.", e);
    }
    if (encoding != null) {
        try {
            encoding.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            logger.error(
                    "Unsupported encoding " + encoding
                    + ". Use default encoding.");
            encoding = null;
        }
    }
    this.encoding = encoding;
  }

  public void write(char c) {
    buf.append(c); 
  }
  
    @Override
    public void write(char[] charArray, int offset, int len) {
        buf.append(charArray, offset, len);
    }

    @Override
    public void write(String str) {
        buf.append(str);
    }

  /**
     * Sends the pending data.
     */
    @Override
    public void flush() throws IOException {
        if (buf.length() == 0)
            return;
        byte[] payload;
        if (encoding == null) {
            payload = buf.toString().getBytes();
        } else {
            payload = buf.toString().getBytes(encoding);
        }
        DatagramPacket packet = new DatagramPacket(payload, payload.length,
                address, port);

        ds.send(packet);

        // clean up for next time
        buf.setLength(0);
    }
  
  public void reset() {
    buf.setLength(0);
  }

  /**
     * Closes the datagram socket.
     */
    @Override
    public void close() {
        try {
            flush();
        } catch (IOException e) {
            // should throw it ... can't change method sig. though
        }
        ds.close();
    }
  
}
