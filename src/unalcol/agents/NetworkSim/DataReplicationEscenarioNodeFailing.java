/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package unalcol.agents.NetworkSim;

import edu.uci.ics.jung.algorithms.layout.ISOMLayout;
import edu.uci.ics.jung.algorithms.layout.Layout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.visualization.BasicVisualizationServer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Observer;
// unalcol.agent.networkSim.reports.GraphicReportHealingObserver;
import unalcol.agents.Agent;
import unalcol.agents.AgentProgram;
import unalcol.agents.simulate.util.SimpleLanguage;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.apache.commons.collections15.Transformer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import unalcol.agents.NetworkSim.environment.NetworkEnvironmentPheromoneReplicationNodeFailing;
import unalcol.agents.NetworkSim.environment.NetworkEnvironmentPheromoneReplicationNodeFailingAllInfo;
import unalcol.agents.NetworkSim.environment.NetworkEnvironmentPheromoneReplicationNodeFailingBroadcast;
import unalcol.agents.NetworkSim.environment.NetworkEnvironmentPheromoneReplicationNodeFailingChain;
import unalcol.agents.NetworkSim.environment.NetworkEnvironmentReplication;
import unalcol.agents.NetworkSim.environment.NetworkMessageBuffer;
import unalcol.agents.NetworkSim.environment.NetworkNodeMessageBuffer;
import unalcol.agents.NetworkSim.environment.ObjectSerializer;
import unalcol.agents.NetworkSim.programs.NodeFailingProgram;
import unalcol.agents.NetworkSim.util.DataReplicationNodeFailingObserver;
import unalcol.agents.NetworkSim.util.GraphComparator;
import unalcol.agents.NetworkSim.util.GraphStats;
//import unalcol.agents.NetworkSim.util.GraphStatistics;
import unalcol.agents.NetworkSim.util.StringSerializer;

/**
 * Creates a simulation without graphic interface
 *
 * @author arles.rodriguez
 */
public class DataReplicationEscenarioNodeFailing implements Runnable, ActionListener {

    private NetworkEnvironmentReplication world;
    public boolean renderAnts = true;
    public boolean renderSeeking = true;
    public boolean renderCarrying = true;
    int modo = 0;
//    GraphicReportHealingObserver greport;
    int executions = 0;
    int population = 100;
    int vertexNumber = 100;
    int channelNumber = 100;
    float probFailure = (float) 0.1;
    Hashtable<String, Object> positions;
    int width;
    int height;
    private Observer graphVisualization;
    ArrayList<GraphElements.MyVertex> locations;
    HashMap<String, Long> networkDelays;
    int indexLoc;
    boolean added = false;
    JFrame frame;
    JFrame frame2;
    private boolean isDrawing = false;
    XYSeries agentsLive;
    XYSeries nodesLive;
    XYSeries neighborMatchingSim;
    XYSeriesCollection juegoDatos = new XYSeriesCollection();
    FrameGraphUpdater fgup = null;
    private final JPanel networkPanel;
    private final JPanel bPanel;
    private final JButton redraw;
    Graph<GraphElements.MyVertex, String> initialNetwork;
    HashMap<Integer, Double> similarity;

    /**
     * Creates a simulation without graphic interface
     *
     * @param pop
     * @param pf
     * @param width
     * @param height
     * @return
     */
    DataReplicationEscenarioNodeFailing(int pop, float pf) {
        population = pop;
        probFailure = pf;
        positions = new Hashtable<>();
        System.out.println("Pop: " + population);
        System.out.println("Pf: " + pf);
        System.out.println("Movement: " + SimulationParameters.motionAlg);
        indexLoc = 0;
        frame = new JFrame("Simple Graph View");
        //frame.setSize(1000, 1000);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame2 = new JFrame("Agent and Node Number");
        agentsLive = new XYSeries("agentsLive");
        nodesLive = new XYSeries("nodesLive");
        neighborMatchingSim = new XYSeries("Neighbour Sim");

        juegoDatos.addSeries(agentsLive);
        juegoDatos.addSeries(nodesLive);
        juegoDatos.addSeries(neighborMatchingSim);

        frame2.setLocation(650, 150);
        frame2.setSize(450, 450);
        frame.getContentPane().setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.Y_AXIS));
        bPanel = new JPanel();
        redraw = new JButton("Redraw Network");
        bPanel.add(redraw);
        networkPanel = new JPanel();
        frame.add(networkPanel);
        frame.add(bPanel);
        redraw.addActionListener(this);
        //frame2.show();
        frame.setSize(650, 650);
        frame.setVisible(true);
        frame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        similarity = new HashMap<>();
    }

    /**
     *
     * Initializes simulation.
     */
    public void init() {
        Vector<Agent> agents = new Vector();
        List<Node> nodes = new ArrayList<>();
        System.out.println("fp" + probFailure);

        //Language for Agents
        String[] _percepts = {"data", "neighbors"};
        String[] _actions = {"move", "die", "informfailure"};
        SimpleLanguage agentsLanguage = new SimpleLanguage(_percepts, _actions);

        //Language for nodes
        String[] nodePercepts = {"data", "neighbors"};
        String[] nodeActions = {"communicate", "die"};
        SimpleLanguage nodeLanguaje = new SimpleLanguage(nodePercepts, nodeActions);
        NodeFailingProgram np = new NodeFailingProgram(SimulationParameters.npf);
        // NodeFailingProgram np = new NodeFailingProgram((float)Math.random()*SimulationParameters.npf);

        //report = new reportHealingProgram(population, probFailure, this);
        //greport = new GraphicReportHealingObserver(probFailure);
        //Create graph
        Graph<GraphElements.MyVertex, String> g = graphSimpleFactory.createGraph(SimulationParameters.graphMode);
        StringSerializer s = new StringSerializer();
        String aCopy = s.serialize(g);
        initialNetwork = (Graph<GraphElements.MyVertex, String>) s.deserialize(aCopy);

        //maybe to fix: alldata must have getter
        System.out.println("All data" + SimulationParameters.globalData);
        System.out.println("All data size" + SimulationParameters.globalData.size());

        // System.out.println("Average Path Length: " + GraphStatistics.computeAveragePathLength(g));
        Map<GraphElements.MyVertex, Double> m = GraphStats.clusteringCoefficients(g);
        System.out.println("Clustering coeficients:" + m);
        System.out.println("Average Clustering Coefficient: " + GraphStats.averageCC(g));
        System.out.println("Average degree: " + GraphStats.averageDegree(g));

        String graphType = SimulationParameters.graphMode;
        graphType = graphType.replaceAll(".graph", "");

        String fileTimeout;

        if (SimulationParameters.nofailRounds == 0) {
            if (SimulationParameters.simMode.contains("chain")) {
                fileTimeout = "timeout+exp+ps+" + population + "+pf+" + SimulationParameters.npf + "+mode+" + SimulationParameters.motionAlg + "+maxIter+" + SimulationParameters.maxIter + "+e+" + g.getEdges().size() + "+v+" + g.getVertices().size() + "+" + graphType + "+" + SimulationParameters.activateReplication + "+" + SimulationParameters.nodeDelay + "+" + SimulationParameters.simMode + "+" + SimulationParameters.nhopsChain + "+wsize+" + SimulationParameters.wsize + ".timeout";
            } else {
                fileTimeout = "timeout+exp+ps+" + population + "+pf+" + SimulationParameters.npf + "+mode+" + SimulationParameters.motionAlg + "+maxIter+" + SimulationParameters.maxIter + "+e+" + g.getEdges().size() + "+v+" + g.getVertices().size() + "+" + graphType + "+" + SimulationParameters.activateReplication + "+" + SimulationParameters.nodeDelay + "+" + SimulationParameters.simMode + "+wsize+" + SimulationParameters.wsize + ".timeout";
            }
        } else {
            if (SimulationParameters.simMode.contains("chain")) {
                fileTimeout = "timeout+exp+ps+" + population + "+pf+" + SimulationParameters.npf + "+mode+" + SimulationParameters.motionAlg + "+maxIter+" + SimulationParameters.maxIter + "+e+" + g.getEdges().size() + "+v+" + g.getVertices().size() + "+" + graphType + "+" + SimulationParameters.activateReplication + "+" + SimulationParameters.nodeDelay + "+" + SimulationParameters.simMode + "+" + SimulationParameters.nhopsChain + "+wsize+" + SimulationParameters.wsize + "+nofailr+" + SimulationParameters.nofailRounds + ".timeout";
            } else {
                fileTimeout = "timeout+exp+ps+" + population + "+pf+" + SimulationParameters.npf + "+mode+" + SimulationParameters.motionAlg + "+maxIter+" + SimulationParameters.maxIter + "+e+" + g.getEdges().size() + "+v+" + g.getVertices().size() + "+" + graphType + "+" + SimulationParameters.activateReplication + "+" + SimulationParameters.nodeDelay + "+" + SimulationParameters.simMode + "+wsize+" + SimulationParameters.wsize + "+nofailr+" + SimulationParameters.nofailRounds + ".timeout";
            }
        }

        SimulationParameters.genericFilenameTimeouts = fileTimeout;

        ConcurrentHashMap<String, ConcurrentHashMap<Integer, ReplicationStrategyInterface>> nodeTimeout = null;
        //Here we use node pf instead agent pf.
        if (SimulationParameters.simMode.contains("chain")) {
            nodeTimeout = (ConcurrentHashMap) ObjectSerializer.loadDeserializedObject(fileTimeout);
        }

        for (GraphElements.MyVertex v : g.getVertices()) {
            v.setStatus("alive");
            Node n = null;
            if (SimulationParameters.simMode.contains("chain") && nodeTimeout != null && nodeTimeout.containsKey(v.getName())) {
                //System.out.println("load" + nodeTimeout.get(v.getName()));
                n = new Node(np, v, nodeTimeout.get(v.getName()));
            } else {
                n = new Node(np, v);
            }
            n.setPfCreate(0);
            NetworkNodeMessageBuffer.getInstance().createBuffer(v.getName());
            agents.add(n);
            nodes.add(n);
        }

        if (!SimulationParameters.simMode.equals("broadcast") && !SimulationParameters.simMode.equals("allinfo")) {
            if (SimulationParameters.filenameLoc.length() > 1) {
                loadLocations();
                loadNetworkDelays();
            }
            //Creates "Agents"
            for (int i = 0; i < population; i++) {
                AgentProgram program = MotionProgramSimpleFactory.createMotionProgram(SimulationParameters.pf, SimulationParameters.motionAlg);
                MobileAgent a = new MobileAgent(program, i);
                GraphElements.MyVertex tmp = getLocation(g);
                System.out.println("tmp" + tmp);
                a.setRound(-1);
                a.setLocation(tmp);
                a.setPrevLocation(tmp);

                a.setPrevPrevLocation(tmp);
                a.setProgram(program);
                a.setAttribute("infi", new ArrayList<>());
                NetworkMessageBuffer.getInstance().createBuffer(a.getId());
                agents.add(a);

                String[] msgnode = new String[3];
                msgnode[0] = "arrived";
                msgnode[1] = String.valueOf(a.getId());
                msgnode[2] = String.valueOf(a.getIdFather());
                NetworkNodeMessageBuffer.getInstance().putMessage(a.getLocation().getName(), msgnode);
                //Initialize implies arrival message from nodes!
            }
        }

        graphVisualization = new DataReplicationNodeFailingObserver(this);

        switch (SimulationParameters.simMode) {
            case "broadcast":
                world = new NetworkEnvironmentPheromoneReplicationNodeFailingBroadcast(agents, agentsLanguage, nodeLanguaje, g);
                ((NetworkEnvironmentPheromoneReplicationNodeFailingBroadcast) world).addNodes(nodes);
                break;
            case "chain":
            case "chainnoloop":
                world = new NetworkEnvironmentPheromoneReplicationNodeFailingChain(agents, agentsLanguage, nodeLanguaje, g);
                ((NetworkEnvironmentPheromoneReplicationNodeFailingChain) world).addNodes(nodes);
                break;
            case "allinfo":
                world = new NetworkEnvironmentPheromoneReplicationNodeFailingAllInfo(agents, nodeLanguaje, nodeLanguaje, g);
                ((NetworkEnvironmentPheromoneReplicationNodeFailingAllInfo) world).addNodes(nodes);
                for (Node n : world.getNodes()) {
                    //System.out.println("enerooooooooooooooooooo");
                    n.setNetworkdata(((NetworkEnvironmentPheromoneReplicationNodeFailingAllInfo) world).loadAllTopology());
                }
                break;
            default:
                world = new NetworkEnvironmentPheromoneReplicationNodeFailing(agents, agentsLanguage, nodeLanguaje, g);
                ((NetworkEnvironmentPheromoneReplicationNodeFailing) world).addNodes(nodes);
                break;
        }
        world.setNetworkDelays(networkDelays);
        world.addObserver(graphVisualization);

        world.not();
        world.run();
        executions++;
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        final JButton source = (JButton) ae.getSource();
        if (source.equals(redraw)) {
            redrawNetwork();
        }
    }

    private void redrawNetwork() {
        FrameGraphUpdaterOnce fgup2 = new FrameGraphUpdaterOnce(world.getTopology(), frame, world);
        fgup2.start();
    }

    public class FrameGraphUpdater extends Thread {

        Graph<GraphElements.MyVertex, String> g;
        JFrame frame;
        NetworkEnvironmentReplication n;

        public FrameGraphUpdater(Graph<GraphElements.MyVertex, String> g, JFrame frame, NetworkEnvironmentReplication ne) {
            this.g = g;
            this.frame = frame;
            this.n = ne;
        }

        public void run() {
            System.out.println("call runnn!!!");

            isDrawing = true;
            if (g.getVertexCount() == 0) {
                System.out.println("no nodes alive.");
            } else {
                try {

                    JFreeChart chart = ChartFactory.createXYLineChart(
                            "Nodes and Agent Number vs Round", "Round number", "Agents-Nodes",
                            juegoDatos, PlotOrientation.VERTICAL,
                            true, true, false);
                    ChartPanel chpanel = new ChartPanel(chart);

                    JPanel jPanel = new JPanel();
                    jPanel.setLayout(new BorderLayout());
                    jPanel.add(chpanel, BorderLayout.NORTH);
                    frame2.add(jPanel);
                    frame2.pack();
                    frame2.setVisible(true);

                    while (true) {

                        try {
                            //!world.isFinished()) {
                            Thread.sleep(10);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(DataReplicationEscenarioNodeFailing.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        //System.out.println("n visited nodes size" + n.visitedNodes.size());
                        // vv.getRenderContext().setVertexFillPaintTransformer(n.vertexColor);
                        // vv.getRenderContext().setEdgeDrawPaintTransformer(n.edgeColor);
                        //vv.repaint();
                        //}
                        int agentsAlive = n.getAgentsAlive();
                        int nodesAlive = n.getNodesAlive();
                        //System.out.println("n" + n.getAge() + "," + agentsAlive);
                        //System.out.println("n" + n.getAge() + "," + nodesAlive);
                        if (nodesAlive == 0) {
                            System.out.println("no nodes alive.");
                            break;
                        } else if (n != null) {

                            if (n.getAge() % 50 == 0) {
                                agentsLive.add(n.getAge(), agentsAlive);
                                nodesLive.add(n.getAge(), nodesAlive);
                                //call comparator here!
//                            GraphComparator gcmp = new GraphComparator();
                                // System.out.println("similarity" + gcmp.calculateSimilarity(initialNetwork, g));
//                            cosineSim.add(n.getAge(), gcmp.calculateSimilarity(initialNetwork, g));
                                GraphComparator gnm = new GraphComparator();
                                double sim = gnm.calculateSimilarity(initialNetwork, g);
                                neighborMatchingSim.add(n.getAge(), sim);
                                similarity.put(n.getAge(), sim);
                                frame2.repaint();
                            }
                        } // System.out.println("entra:" + n.getAge());

                        //frame2.getGraphics().drawImage(creaImagen(), 0, 0, null);
                    }
                } catch (NullPointerException ex) {
                    System.out.println("exception drawing graph: " + ex.getLocalizedMessage());
                    isDrawing = false;
                    fgup = null;
                } catch (ConcurrentModificationException ex) {
                    System.out.println("exception calculating similarity graph: " + ex.getLocalizedMessage());
                    isDrawing = false;
                    fgup = null;
                }
            }
        }
    }

    public class FrameGraphUpdaterOnce extends Thread {

        Graph<GraphElements.MyVertex, String> g;
        JFrame frame;
        NetworkEnvironmentReplication n;

        public FrameGraphUpdaterOnce(Graph<GraphElements.MyVertex, String> g, JFrame frame, NetworkEnvironmentReplication ne) {
            this.g = g;
            this.frame = frame;
            this.n = ne;
        }

        public void run() {
            System.out.println("call runnn!!!");
            isDrawing = true;
            if (g.getVertexCount() == 0) {
                System.out.println("no nodes alive.");
            } else {
                //GraphComparator gcmp = new GraphComparator();
                //nodesLive.add(n.getAge(), gcmp.calculateSimilarity(initialNetwork, g));
                try {
                    Layout<GraphElements.MyVertex, String> layout = null;

                    layout = new ISOMLayout<>(world.getTopology());

                    BasicVisualizationServer<GraphElements.MyVertex, String> vv = new BasicVisualizationServer<>(layout);
                    vv.setPreferredSize(new Dimension(600, 600)); //Sets the viewing area size
                    //vv.getRenderContext().setVertexLabelTransformer(new ToStringLabeller());
                    //vv.getRenderContext().setEdgeLabelTransformer(new ToStringLabeller());
                    //n.setVV(vv);
                    Transformer<GraphElements.MyVertex, Paint> vertexColor = new Transformer<GraphElements.MyVertex, Paint>() {
                        @Override
                        public Paint transform(GraphElements.MyVertex i) {
                            if (n.isOccuped(i)) {
                                return Color.YELLOW;
                            }

                            if (i.getStatus() != null && i.getStatus().equals("failed")) {
                                return Color.BLACK;
                            }

                            if (i.getStatus() != null && i.getStatus().equals("visited")) {
                                return Color.BLUE;
                            }

                            //if(i.getData().size() > 0){
                            //    System.out.println("i"+ i.getData().size());
                            //}
                            /*if (i.getData().size() == n.getTopology().getVertices().size()) {
                                return Color.GREEN;
                            }*/
                            return Color.RED;
                        }
                    };
                    vv.getRenderContext().setVertexFillPaintTransformer(vertexColor);
                    if (!added) {
                        networkPanel.add(vv);
                        added = true;
                        frame.pack();
                        frame.setVisible(true);
                    } else {
                        frame.repaint();
                    }
                } catch (NullPointerException ex) {
                    System.out.println("exception drawing graph: " + ex.getLocalizedMessage());
                    isDrawing = false;
                }
            }
        }
    }

    public HashMap<Integer, Double> getSimilarity() {
        return similarity;
    }

    public void setSimilarity(HashMap<Integer, Double> similarity) {
        this.similarity = similarity;
    }

    /**
     * Runs a simulation.
     *
     */
    @Override
    public void run() {
        //try {
        while (true) {
            try {
                //!world.isFinished()) {
                Thread.sleep(50);
            } catch (InterruptedException ex) {
                Logger.getLogger(DataReplicationEscenarioNodeFailing.class
                        .getName()).log(Level.SEVERE, null, ex);
            }
            if (fgup == null) {
                fgup = new FrameGraphUpdater(world.getTopology(), frame, world);
                fgup.start();
            }
            //System.out.println("go");
            //System.out.println("halo");
            /* world.updateSandC();
            world.calculateGlobalInfo();

            if (world.getAge() % 2 == 0 || world.getAgentsDie() == world.getAgents().size() || world.getRoundGetInfo() != -1) {
                world.nObservers();
            }
             */
 /*if (world instanceof NetworkEnvironmentPheromoneReplicationNodeFailing && SimulationParameters.motionAlg.equals("carriers")) {
                ((NetworkEnvironmentPheromoneReplicationNodeFailing) world).evaporatePheromone();
            }*/
            world.updateWorldAge();
            world.validateNodesAlive();

            /*
            if (world instanceof WorldTemperaturesOneStepOnePheromoneHybridLWEvaporationImpl) {
                ((WorldTemperaturesOneStepOnePheromoneHybridLWEvaporationImpl) world).evaporatePheromone();
            }

            if (world instanceof WorldTemperaturesOneStepOnePheromoneHybridLWEvaporationImpl2) {
                ((WorldTemperaturesOneStepOnePheromoneHybridLWEvaporationImpl2) world).evaporatePheromone();
            }

            if (world instanceof WorldLwphCLwEvapImpl) {
                ((WorldLwphCLwEvapImpl) world).evaporatePheromone();
            }*/
        }
        /*}catch (InterruptedException e) {
            System.out.println("interrupted!");
        } catch (NullPointerException e) {
            System.out.println("interrupted!");
        }*/
 /* System.out.println("End WorldThread");*/

    }

    public void loadLocations() {
        StringSerializer s = new StringSerializer();
        locations = (ArrayList<GraphElements.MyVertex>) s.loadDeserializeObject(SimulationParameters.filenameLoc);
    }

    public void loadNetworkDelays() {
        String output = SimulationParameters.filenameLoc.replace("loc", "");
        output += "ndelay";
        StringSerializer s = new StringSerializer();
        networkDelays = (HashMap<String, Long>) s.loadDeserializeObject(output);
        System.out.println("net Delays" + networkDelays);
    }

    private GraphElements.MyVertex getLocation(Graph<GraphElements.MyVertex, String> g) {
        if (SimulationParameters.filenameLoc.length() > 1) {
            GraphElements.MyVertex tmp = locations.get(indexLoc++);
            for (GraphElements.MyVertex v : g.getVertices()) {
                if (v.toString().equals(tmp.toString())) {
                    return v;
                }
            }
            //System.out.println("null???");
            return null;
        } else {
            int pos = (int) (Math.random() * g.getVertexCount());
            Collection E = g.getVertices();
            return (GraphElements.MyVertex) E.toArray()[pos];
        }
    }

    public void saveImage(String filename) {
        FileOutputStream output;
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Agents Live", "round number", "agents",
                juegoDatos, PlotOrientation.VERTICAL,
                true, true, false);

        try {
            output = new FileOutputStream(filename + ".jpg");
            ChartUtilities.writeChartAsJPEG(output, 1.0f, chart, 400, 400, null);

        } catch (FileNotFoundException ex) {
            Logger.getLogger(DataReplicationNodeFailingObserver.class
                    .getName()).log(Level.SEVERE, null, ex);

        } catch (IOException ex) {
            Logger.getLogger(DataReplicationNodeFailingObserver.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    public BufferedImage creaImagen() {
        JFreeChart chart = ChartFactory.createXYLineChart(
                "ssss", "Round number", "Agents",
                juegoDatos, PlotOrientation.VERTICAL,
                true, true, false);
        /*
         JFreeChart chart =
         ChartFactory.createTimeSeriesChart("Sesiones en Adictos al Trabajo"
         "Meses", "Sesiones", juegoDatos,
         false,
         false,
         true // Show legend
         );
         */
        BufferedImage image = chart.createBufferedImage(450, 450);
        return image;
    }

}
