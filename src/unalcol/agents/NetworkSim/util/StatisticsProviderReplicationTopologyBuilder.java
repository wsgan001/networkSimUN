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
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;
import unalcol.agents.NetworkSim.MobileAgent;
import unalcol.agents.NetworkSim.environment.NetworkEnvironmentReplication;
import unalcol.agents.NetworkSim.environment.NetworkEnvironmentReplicationTopologyBuilder;

/**
 *
 * @author INVESTIGADOR
 */
class StatisticsProviderReplicationTopologyBuilder {

    private String reportFile;

    StatisticsProviderReplicationTopologyBuilder(String filename) {
        reportFile = filename;
    }

    Hashtable getStatisticsInteger(NetworkEnvironmentReplicationTopologyBuilder w) {
        Hashtable Statistics = new Hashtable();
        int right = 0;
        int wrong = 0;

        int n = w.getAgents().size();

        ArrayList<Double> data = new ArrayList<>();
        ArrayList<Double> msgin = new ArrayList<>();
        ArrayList<Double> msgout = new ArrayList<>();
        //ArrayList<Double> explTerrain = new ArrayList<>();
        
        for (int i = 0; i < n; i++) {
            int count = 0;
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
            //explTerrain.add((double) a.getExploredTerrain());
        }

        StatisticsNormalDist stnd = new StatisticsNormalDist(data, data.size());
        StatisticsNormalDist stsend = new StatisticsNormalDist(msgout, msgout.size());
        StatisticsNormalDist strecv = new StatisticsNormalDist(msgin, msgin.size());
        //StatisticsNormalDist explT = new StatisticsNormalDist(explTerrain, explTerrain.size());
        Statistics.put("nvertex", w.topology.getVertexCount());
        Statistics.put("nedges", w.topology.getEdgeCount());
        Statistics.put("mean", stnd.getMean());
        Statistics.put("stddev", stnd.getStdDev());
        Statistics.put("right", right);
        Statistics.put("wrong", wrong);
        Statistics.put("round", w.getAge());
        Statistics.put("avgSend", stsend.getMean());
        Statistics.put("avgRecv", strecv.getMean());
        Statistics.put("stdDevSend", stsend.getStdDev());
        Statistics.put("stdDevRecv", strecv.getStdDev());
        System.out.println("stats: " + Statistics);
        return Statistics;
    }

    
    void printStatistics(NetworkEnvironmentReplicationTopologyBuilder w) {
        Hashtable st = getStatisticsInteger(w);
        try {
            int nr = w.getAgents().size() - ((Integer) st.get("right") + (Integer) st.get("wrong"));
            //filename = getFileName() + "ds.trace";
            PrintWriter escribir;
            escribir = new PrintWriter(new BufferedWriter(new FileWriter(reportFile, true)));
            escribir.println(st.get("right") + "," + st.get("wrong") + ","  + st.get("nvertex") + "," + st.get("nedges")+ "," + st.get("mean") + "," + st.get("stddev") + "," + st.get("avgSend")
                    + "," + st.get("stdDevSend") + "," + st.get("avgRecv")  + "," + st.get("stdDevRecv") + "," + st.get("round"));
            escribir.close();
        } catch (IOException ex) {
            Logger.getLogger(StatisticsProviderReplicationTopologyBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

}
