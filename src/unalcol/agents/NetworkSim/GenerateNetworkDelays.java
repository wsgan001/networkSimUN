/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unalcol.agents.NetworkSim;

import edu.uci.ics.jung.graph.Graph;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import unalcol.agents.NetworkSim.util.GraphSerialization;
import unalcol.agents.NetworkSim.util.StringSerializer;

/**
 *
 *
 * @author Arles Rodriguez
 */
public class GenerateNetworkDelays {

    public static String graphMode = "lattice";
    public static int popSize = 5;
    public static int channelNumber = 5;
    public static int vertexNumber = 5;
    public static float pf = 0.5f;
    public static float beta = 1f;
    public static int rows = 5;
    public static int columns = 5;
    public static String motionAlg = "random";
    public static String filename = "";
    public static int maxIter = -1;
    public static int clusters = 4;
    public static int startNodesScaleFree = 1;
    public static int edgesToAttachScaleFree = 2;
    public static int numSteps = 5;
    public static int degree = 2;
    public static String filenameLoc = "";

    public static GraphElements.MyVertex getLocation(Graph<GraphElements.MyVertex, ?> g) {
        int pos = (int) (Math.random() * g.getVertexCount());
        Collection E = g.getVertices();
        return (GraphElements.MyVertex) E.toArray()[pos];
    }

    // Perform simulation
    public static void main(String[] args) {
        long defaultDelay = 30;
        String dir = args[0];
        defaultDelay = Integer.valueOf(args[1]);
        HashMap<String, Long> networkDelays = new HashMap<>();

        if (args.length >= 1) {
            //Pop Size

            File f = new File(dir);
            String extension;
            File[] files = f.listFiles();

            for (File file : files) {
                extension = "";
                int i = file.getName().lastIndexOf('.');
                int p = Math.max(file.getName().lastIndexOf('/'), file.getName().lastIndexOf('\\'));
                if (i > p) {
                    extension = file.getName().substring(i + 1);
                }

                if (file.isFile() && extension.equals("graph")) {
                    ArrayList<GraphElements.MyVertex> locations = new ArrayList<>();
                    System.out.println("File:" + file.getName());
                    Graph<GraphElements.MyVertex, String> g = GraphSerialization.loadDeserializeGraph(file.getName());

                    System.out.println(file.getName());
                    System.out.println("get: " + file.getName());
                    String output = file.getName().replace(extension, "");
                    output += "ndelay";

                    for (GraphElements.MyVertex orig : g.getVertices()) {
                        for (GraphElements.MyVertex dest : g.getNeighbors(orig)) {
                            networkDelays.put(orig.getName() + dest.getName(), (long) (defaultDelay * Math.random()));
                        }
                    }
                    StringSerializer s = new StringSerializer();
                    s.saveSerializedObject(output, networkDelays);
                }
            }

        } else {
            System.out.println("Usage:");
            System.out.println("java -Xmx4200m -classpath NetworkSimulator.jar unalcol.agents.NetworkSim graphmode [smallworld|scalefree|lattice]");
            System.out.println("java -Xmx4200m -classpath NetworkSimulator.jar unalcol.agents.NetworkSim graphmode smallworld beta nodenumber agentsnumber pf motionAlg");
            System.out.println("java -Xmx4200m -classpath NetworkSimulator.jar unalcol.agents.NetworkSim graphmode scalefree nodenumber agentsnumber pf motionAlg");
            System.out.println("java -Xmx4200m -classpath NetworkSimulator.jar unalcol.agents.NetworkSim graphmode lattice rows columns agentsnumber pf motionAlg");
        }
    }
}
