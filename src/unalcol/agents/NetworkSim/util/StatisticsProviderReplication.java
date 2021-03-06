/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unalcol.agents.NetworkSim.util;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import unalcol.agents.NetworkSim.MobileAgent;
import unalcol.agents.NetworkSim.Node;
import unalcol.agents.NetworkSim.environment.NetworkEnvironmentPheromoneReplication;
import unalcol.agents.NetworkSim.environment.NetworkEnvironmentReplication;
import unalcol.agents.NetworkSim.environment.ObjectSerializer;

/**
 *
 * @author INVESTIGADOR
 */
class StatisticsProviderReplication {

    private String reportFile;
    private static final long MEGABYTE = 1024L * 1024L;

    public static long bytesToMegabytes(long bytes) {
        return bytes / MEGABYTE;
    }

    StatisticsProviderReplication(String filename) {
        reportFile = filename;
    }

    Hashtable getStatisticsInteger(NetworkEnvironmentReplication w) {
        Hashtable Statistics = new Hashtable();
        int right = 0;
        int wrong = 0;
        ConcurrentHashMap<String, ConcurrentHashMap> nodeTimeout = new ConcurrentHashMap<>();

        int n = w.getAgents().size();

        ArrayList<Double> data = new ArrayList<>();
        ArrayList<Double> msgin = new ArrayList<>();
        ArrayList<Double> msgout = new ArrayList<>();
        //ArrayList<Double> explTerrain = new ArrayList<>();

        for (int i = 0; i < n; i++) {
            int count = 0;
            if (w.getAgent(i) instanceof MobileAgent) {
                MobileAgent a = (MobileAgent) w.getAgent(i);
                count = a.getData().size();
                if (count == w.getTopology().getVertices().size()) {
                    right++;
                } else {
                    wrong++;
                }
                data.add((double) count);
                //System.out.println("a" + i + " msg sent:" + a.getnMsgRecv() + "msg recv" + a.getnMsgRecv());
                msgin.add((double) a.getnMsgRecv());
                msgout.add((double) a.getnMsgSend());
            } else if (w.getAgent(i) instanceof Node) {
                Node node = (Node) w.getAgent(i);
                nodeTimeout.put(node.getVertex().getName(), node.getNodeTimeouts(1));
                Iterator it = node.getNodeTimeouts(1).keySet().iterator();

                System.out.print("Node " + node.getVertex().getName() + ": ");
                while (it.hasNext()) {
                    System.out.print(it.next() + " ");
                }
                System.out.println("");
            }
            //explTerrain.add((double) a.getExploredTerrain());
        }

        String timeoutFile = "timeout+" + reportFile;
        timeoutFile = timeoutFile.replaceAll(".graph", "");
        timeoutFile = timeoutFile.replaceAll(".csv", "");
        timeoutFile += ".timeout";
        ObjectSerializer.saveSerializedObject(timeoutFile, nodeTimeout);

        StatisticsNormalDist stnd = new StatisticsNormalDist(data, data.size());
        StatisticsNormalDist stsend = new StatisticsNormalDist(msgout, msgout.size());
        StatisticsNormalDist strecv = new StatisticsNormalDist(msgin, msgin.size());
        //StatisticsNormalDist explT = new StatisticsNormalDist(explTerrain, explTerrain.size());
        Statistics.put("nvertex", w.getTopology().getVertexCount());
        Statistics.put("nedges", w.getTopology().getEdgeCount());
        Statistics.put("mean", stnd.getMean());
        Statistics.put("stddev", stnd.getStdDev());
        Statistics.put("right", w.nodesComplete()); //Changed to obtain if information is replicated in nodes
        Statistics.put("wrong", wrong);
        Statistics.put("round", w.getAge());
        Statistics.put("avgSend", stsend.getMean());
        Statistics.put("avgRecv", strecv.getMean());
        Statistics.put("stdDevSend", stsend.getStdDev());
        Statistics.put("stdDevRecv", strecv.getStdDev());
        System.out.println("stats: " + Statistics);
        return Statistics;
    }

    void printStatistics(NetworkEnvironmentReplication w) {
        Hashtable st = getStatisticsInteger(w);
        try {
            //int nr = w.getAgents().size() - ((Integer) st.get("right") + (Integer) st.get("wrong"));
            //filename = getFileName() + "ds.trace";
            PrintWriter escribir;
            escribir = new PrintWriter(new BufferedWriter(new FileWriter(reportFile, true)));

            Runtime runtime = Runtime.getRuntime();
            // Run the garbage collector
            runtime.gc();
            // Calculate the used memory
            long memory = runtime.totalMemory() - runtime.freeMemory();
            
            if (w instanceof NetworkEnvironmentPheromoneReplication) {
                escribir.println(st.get("right") + "," + st.get("wrong") + "," + st.get("nvertex") + "," + st.get("nedges") + "," + st.get("mean") + "," + st.get("stddev") + "," + st.get("avgSend")
                        + "," + st.get("stdDevSend") + "," + st.get("avgRecv") + "," + st.get("stdDevRecv") + "," + st.get("round") + "," + NetworkEnvironmentPheromoneReplication.getFalsePossitives() + "," + NetworkEnvironmentPheromoneReplication.getAgentMovements() + "," + NetworkEnvironmentPheromoneReplication.getACKAmount()+ "," + memory);
            } else {
                escribir.println(st.get("right") + "," + st.get("wrong") + "," + st.get("nvertex") + "," + st.get("nedges") + "," + st.get("mean") + "," + st.get("stddev") + "," + st.get("avgSend")
                        + "," + st.get("stdDevSend") + "," + st.get("avgRecv") + "," + st.get("stdDevRecv") + "," + st.get("round"));
            }
            escribir.close();
        } catch (IOException ex) {
            Logger.getLogger(StatisticsProviderReplication.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
