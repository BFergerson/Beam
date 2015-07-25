package com.codebrig.beam.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Pinger
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
        final Set<String> finishedMap = new HashSet<> ();
        final List<Thread> pingThreadList = new ArrayList<> ();

        for (final String host : hostArg) {
            Thread pingThread = new Thread (new Runnable ()
            {

                @Override
                public void run () {
                    try {
                        ping (host);
                        finishedMap.add (host);
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

        List<String> hostList = Arrays.asList (finishedMap.toArray (new String[finishedMap.size ()]));
        Collections.reverse (hostList);
        return hostList;
    }

}
