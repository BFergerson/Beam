/*
 * Copyright © 2014-2015 CodeBrig, LLC.
 * http://www.codebrig.com/
 *
 * Beam - Client/Server & P2P Networking Library
 *
 * ====
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * ====
 */
package com.codebrig.beam.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

/**
 * @author Brandon Fergerson <brandon.fergerson@codebrig.com>
 */
public class SystemPing
{

    public static boolean ping (String host) throws IOException, InterruptedException {
        boolean isWindows = System.getProperty ("os.name").toLowerCase ().contains ("win");

        ProcessBuilder processBuilder = new ProcessBuilder ("ping", isWindows ? "-n" : "-c", "2", host);
        Process proc = processBuilder.start ();

        int returnVal = proc.waitFor ();
        return returnVal == 0;
    }

    public static String pingForFastestHost (final String... hostArg) throws InterruptedException {
        final Set<String> hostMap = new HashSet<> ();
        final Set<String> finishedMap = new HashSet<> ();
        final List<Thread> pingThreadList = new ArrayList<> ();

        for (final String host : hostArg) {
            Thread pingThread = new Thread (new Runnable ()
            {

                @Override
                public void run () {
                    try {
                        if (ping (host)) {
                            synchronized (hostMap) {
                                hostMap.add (host);
                                hostMap.notifyAll ();
                            }
                        }
                        finishedMap.add (host);
                        if (finishedMap.size () >= hostArg.length) {
                            synchronized (hostMap) {
                                hostMap.notifyAll ();
                            }
                        }
                    } catch (IOException | InterruptedException ex) {
                        ex.printStackTrace ();
                    }
                }
            });
            pingThreadList.add (pingThread);
        }

        synchronized (hostMap) {
            for (Thread thread : pingThreadList) {
                thread.start ();
            }
            hostMap.wait ();
        }

        if (hostMap.isEmpty ()) {
            return null;
        }
        return (String) hostMap.toArray ()[hostMap.size () - 1];
    }

    public static List<String> pingOrderByFastestHost (final String... hostArg) throws InterruptedException {
        final TreeMap<Long, String> finishedMap = new TreeMap<> ();
        final List<Thread> pingThreadList = new ArrayList<> ();

        for (final String host : hostArg) {
            Thread pingThread = new Thread (new Runnable ()
            {

                @Override
                public void run () {
                    try {
                        long startTime = System.currentTimeMillis ();
                        ping (host);
                        long endTime = System.currentTimeMillis ();
                        long cost = endTime - startTime;

                        finishedMap.put (cost, host);
                        if (finishedMap.size () >= hostArg.length) {
                            synchronized (finishedMap) {
                                finishedMap.notifyAll ();
                            }
                        }
                    } catch (IOException | InterruptedException ex) {
                        ex.printStackTrace ();
                    }
                }
            });
            pingThreadList.add (pingThread);
        }

        synchronized (finishedMap) {
            for (Thread thread : pingThreadList) {
                thread.start ();
            }
            finishedMap.wait ();
        }

        List<String> hostList = new ArrayList<> (finishedMap.values ());
        return hostList;
    }

}
