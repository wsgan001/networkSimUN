package unalcol.agents.NetworkSim.environment;

import java.util.Hashtable;

import unalcol.agents.simulate.util.*;
import unalcol.agents.*;
import unalcol.agents.simulate.*;

import java.util.Vector;

import edu.uci.ics.jung.graph.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import unalcol.agents.NetworkSim.ActionParameters;
import unalcol.agents.NetworkSim.GraphElements;
import unalcol.agents.NetworkSim.MobileAgent;
import unalcol.agents.NetworkSim.Node;
import unalcol.agents.NetworkSim.util.StatsCreation;

public abstract class NetworkEnvironmentReplication extends Environment {

    public static String msg = null;
    public static float percentageSuccess = -1.0f;
    public int[][] structure = null;
    public SimpleLanguage language = null;
    Date date;
    //private final Graph<GraphElements.MyVertex, String> topology;
    //TopologySingleton topology;
    GraphElements.MyVertex currentNode = null;
    String currentEdge = null;
    String lastactionlog;
    public List<GraphElements.MyVertex> visitedNodes = Collections.synchronizedList(new ArrayList());
    public HashMap<MobileAgent, GraphElements.MyVertex> locationAgents = null;
    private HashMap<GraphElements.MyVertex, ArrayList<Agent>> nodesAgents = null;
    //private static final HashMap<Integer, ConcurrentLinkedQueue> mbuffer = new HashMap<>();
    private int roundComplete = -1;
    private int idBest = -1;
    private boolean finished = false;
    private AtomicInteger age = new AtomicInteger(0);
    public static int agentsDie = 0;
    private static int totalAgents = 0;
    private static HashMap<String, Long> networkDelays;
    

    /**
     * @return the totalAgents
     */
    public int getTotalAgents() {
        totalAgents = 0;
        Vector<Agent> agents1 = (Vector<Agent>) this.getAgents().clone();
        for (Agent a : agents1) {
            if (a instanceof MobileAgent) {
                totalAgents++;
            }
        }
        return totalAgents;
    }

    /**
     * @param aTotalAgents the totalAgents to set
     */
    public static void setTotalAgents(int aTotalAgents) {
        totalAgents = aTotalAgents;
    }

    /**
     * @return the idBest
     */
    public int getIdBest() {
        return idBest;
    }

    /**
     * @param aIdBest the idBest to set
     */
    public void setIdBest(int aIdBest) {
        idBest = aIdBest;
    }

    public int getRowsNumber() {
        return structure.length;
    }

    public int getColumnsNumber() {
        return structure[0].length;
    }

    public int getCompletionPercentage() {
        int completed = 0;

        for (GraphElements.MyVertex v : getTopology().getVertices()) {
            if (v.getData().size() == getTopology().getVertices().size()) {
                completed++;
            }
        }
        return completed;
    }

    public boolean nodesComplete() {
        int completed = 0;
        for (GraphElements.MyVertex v : getTopology().getVertices()) {
            if (v.getData().size() == getTopology().getVertices().size()) {
                completed++;
            }
        }

        if (percentageSuccess != (float) completed / (float) getTopology().getVertices().size() * 100) {
            percentageSuccess = (float) completed / (float) getTopology().getVertices().size() * 100;
            System.out.println(percentageSuccess + "%");
        }

        return completed == getTopology().getVertices().size();
    }

    public boolean act(Agent agent, Action action) {
        boolean flag = (action != null);
        MobileAgent a = (MobileAgent) agent;
        ActionParameters ac = (ActionParameters) action;
        currentNode = a.getLocation();
        a.getLocation().setStatus("visited");
        //visitedNodes.add(currentNode);

        getLocationAgents().put(a, a.getLocation());
        /**
         * Local communication deleted
         *
         * //detect other agents in network ArrayList<Integer> agentNeighbors =
         * getAgentNeighbors(a); //System.out.println(a.getId() +
         * "agentNeigbors" + agentNeighbors);
         *
         * //serialize messages String[] message = new String[2]; //msg:
         * [from|msg] message[0] = String.valueOf(a.getId()); message[1] =
         * ObjectSerializer.serialize(a.getData());
         *
         * //for each neighbor send a message for (Integer idAgent :
         * agentNeighbors) {
         * NetworkMessageBuffer.getInstance().putMessage(idAgent, message);
         * a.incMsgSend(); }
         *
         * String[] inbox =
         * NetworkMessageBuffer.getInstance().getMessage(a.getId());
         *
         * //inbox: id | infi if (inbox != null) { a.incMsgRecv();
         * //System.out.println("my " + a.getData().size()); ArrayList senderInf
         * = (ArrayList) ObjectSerializer.deserialize(inbox[1]);
         * //System.out.println("received" + senderInf.size()); // Join
         * ArrayLists a.getData().removeAll(senderInf);
         * a.getData().addAll(senderInf); //System.out.println("joined" +
         * a.getData().size()); }
         *
         *
         *
         *
         */
        if (flag) {
            //Agents can be put to Sleep for some ms
            //sleep is good is graph interface is on
            agent.sleep(30);
            String act = action.getCode();
            String msg = null;

            /**
             * 0- "move"
             */
            /* @TODO: Detect Stop Conditions for the algorithm */
            switch (language.getActionIndex(act)) {
                case 0: // move
                    GraphElements.MyVertex v = (GraphElements.MyVertex) ac.getAttribute("location");
                    a.setLocation(v);
                    a.setRound(a.getRound() + 1);
                    boolean complete = false;
                    if (a.getData().size() == getTopology().getVertexCount()) {
                        complete = true;
                    }

                    //System.out.println("vertex" + v.getData().size() + ", data" + v.getData());
                    if (getRoundComplete() == -1 && complete) {
                        //System.out.println("complete! round" + a.getRound());
                        setRoundComplete(a.getRound());
                        setIdBest(a.getId());
                        updateWorldAge();
                    }
                    break;
                case 1: //die
                    a.die();
                    a.setLocation(null);
                    getLocationAgents().put(a, null);
                    increaseAgentsDie();
                    setChanged();
                    notifyObservers();
                    return false;
                default:
                    msg = "[Unknown action " + act
                            + ". Action not executed]";
                    System.out.println(msg);
                    break;
            }
        }
        updateWorldAge();
//        evaluateAgentCreation();
        setChanged();
        notifyObservers();
        return flag;
    }

    @Override
    public Percept sense(Agent agent) {
        Percept p = new Percept();

        if (agent instanceof MobileAgent) {
            MobileAgent a = (MobileAgent) agent;
            //System.out.println("sense - topology " + topology);
            //Load neighbors 
            if (a.status != Action.DIE && getTopology().containsVertex(a.getLocation())) {
                p.setAttribute("neighbors", getTopology().getNeighbors(a.getLocation()));
                //System.out.println("agent" + anAgent.getId() + "- neighbor: " +  getTopology().getNeighbors(anAgent.getLocation()));
                //Load data in Agent
                //clone ArrayList
                //getData from the node and put in the agent
                a.getData().removeAll(a.getLocation().getData());
                a.getData().addAll(a.getLocation().getData());

                //Stores agent time, agent time and agent id
                a.getLocation().saveAgentInfo(a.getData(), a.getId(), a.getRound(), getAge());
                //System.out.println("agent info size:" + anAgent.getData().size());
            } else {
                System.out.println("Agent is removed from node that failed before:" + a.getId() + " status is dead: " + (a.status == Action.DIE) + ", loc" + a.getLocation());
                p.setAttribute("nodedeath", a.getLocation());
            }
        }
        if (agent instanceof Node) {
            Node n = (Node) agent;
            //System.out.println("sense node: " + n.getVertex().getName());
            try {
                ArrayList<Agent> agentNode = new ArrayList<>();
                synchronized (NetworkEnvironmentReplication.class) {
                    ArrayList<Agent> agentsCopy = new ArrayList(getAgents());
                    for (Agent a : agentsCopy) {
                        if (a instanceof MobileAgent) {
                            MobileAgent ma = (MobileAgent) a;
                            if (ma.getLocation() != null && ma.getLocation().getName().equals(n.getVertex().getName())) {
                                agentNode.add(ma);
                            }
                        }
                    }
                }
                n.setCurrentAgents(agentNode);
            } catch (Exception e) {
                System.out.println("Exception loading agents in this location" + e.getMessage() + " node:" + n.getVertex().getName());
            }
            //p.setAttribute("neighbors", topology.getNeighbors(n.getVertex()));
            //p.setAttribute("agents", agentNode);
        }
        return p;
    }

    public NetworkEnvironmentReplication(Vector<Agent> _agents, SimpleLanguage _language, Graph gr) {
        super(_agents);
        //this.mbuffer = 
        int n = _agents.size();
        // locationAgents = new ArrayList<>();

        /*for (int i = 0; i < n; i++) {
            MobileAgent ag = (MobileAgent) _agents.get(i);
         //   locationAgents.add(new GraphElements.MyVertex("null"));
            //System.out.println("creating buffer id" + ag.getAttribute("ID"));
            //mbuffer.put(i, new ConcurrentLinkedQueue());
        }*/
        for (Agent a : this.getAgents()) {
            if (a instanceof MobileAgent) {
                totalAgents++;
            }
        }

        language = _language;
        date = new Date();
        TopologySingleton.getInstance().init(gr);
        locationAgents = new HashMap<>();
    }

    public Vector<Action> actions() {
        Vector<Action> acts = new Vector<Action>();
        int n = language.getActionsNumber();
        for (int i = 0; i < n; i++) {
            acts.add(new Action(language.getAction(i)));
        }
        return acts;
    }

    /* @param agentsDie set the number of agents with failures
     */
    public void setAgentsDie(int agentsDie) {
        NetworkEnvironmentReplication.agentsDie = agentsDie;
    }

    /**
     * increases the number of agents with failures
     */
    public void increaseAgentsDie() {
        synchronized (NetworkEnvironmentReplication.class) {
            NetworkEnvironmentReplication.agentsDie++;
        }
    }

    public HashMap<String, Long> getNetworkDelays() {
        return networkDelays;
    }

    public void setNetworkDelays(HashMap<String, Long> in) {
        this.networkDelays = in;
    }

    /**
     * increases the number of agents with failures
     *
     * @return number of agents with failures
     */
    public int getAgentsDie() {
        synchronized (NetworkEnvironmentReplication.class) {
            return agentsDie;
        }
    }

    public int getAgentsLive() {
        int agentsLive = 0;
        synchronized (NetworkEnvironmentReplication.class) {
            Vector<Agent> agentsClone = (Vector) this.getAgents().clone();
            for (Agent a : agentsClone) {
                if (a instanceof MobileAgent) {
                    if (((MobileAgent) a).status != Action.DIE) {
                        agentsLive++;
                    }
                }
            }
        }
        return agentsLive;
    }

    @Override
    public void init(Agent agent) {
        MobileAgent sim_agent = (MobileAgent) agent;
        //@TODO: Any special initialization processs of the environment
    }

    public String getLog() {
        return lastactionlog;
    }

    public void updateLog(String event, String log) {
        Date datenow = new Date();
        long diff = datenow.getTime() - date.getTime();
        //long diffSeconds = diff / 1000 % 60;
        lastactionlog = event + (String.valueOf(diff / 1000)) + " " + log;
        setChanged();
        notifyObservers();
    }

    private void returnOutput(String pid, Hashtable out) {
        controlBoard.getInstance().addOutput(pid, out);
    }

    /**
     * @return the topology
     */
    public Graph<GraphElements.MyVertex, String> getTopology() {
        return TopologySingleton.getInstance().getTopology();
    }

    /**
     * @param topology the topology to set
     */
    /*public void setTopology(Graph<GraphElements.MyVertex, String> topology) {
        this.topology = topology;
    }*/
    /**
     * @return the visitedNodes
     */
    public List<GraphElements.MyVertex> getVisitedNodes() {
        return visitedNodes;
    }

    /**
     * @param visitedNodes the visitedNodes to set
     */
    public void setVisitedNodes(ArrayList<GraphElements.MyVertex> visitedNodes) {
        this.visitedNodes = visitedNodes;
    }

    public void not() {
        setChanged();
        notifyObservers();
    }

    /**
     * @return the locationAgents
     */
    public HashMap<MobileAgent, GraphElements.MyVertex> getLocationAgents() {
        synchronized (NetworkEnvironmentReplication.class) {
            return locationAgents;
        }
    }

    /**
     * @param locationAgents the locationAgents to set
     */
    public void setLocationAgents(HashMap<MobileAgent, GraphElements.MyVertex> locationAgents) {
        this.locationAgents = locationAgents;
    }

    public ArrayList<Integer> getAgentNeighbors(MobileAgent x) {
        ArrayList n = new ArrayList();
        System.out.println("getLoc sizeeeeeeeeeee:" + getLocationAgents().size());
        for (int i = 0; i < getLocationAgents().size(); i++) {
            if (i != x.getId() && x.getLocation() != null && x.getLocation().equals(getLocationAgents().get(i))) {
                n.add(i);
            }
        }
        return n;
    }

    /**
     * @return the roundComplete
     */
    public int getRoundComplete() {
        return roundComplete;
    }

    /**
     * @param roundComplete the roundComplete to set
     */
    public void setRoundComplete(int roundComplete) {
        this.roundComplete = roundComplete;
    }

    /**
     * @return the finished
     */
    public boolean isFinished() {
        return finished;
    }

    /**
     * @param finished the finished to set
     */
    public void setFinished(boolean finished) {
        this.finished = finished;
    }

    /**
     * Function used to calculate the intersection of all the information that
     * agent have collected in a determined time.
     *
     * @return
     */
    /* public void calculateGlobalInfo() {
        if (!isCalculating) {
            isCalculating = true;
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    states[i][j].globalInfo = false;
                }
            }
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    for (int k = 0; k < this.getAgents().size(); k++) {
                        MobileAgent t = (MobileAgent)this.getAgent(k);
                        if (t.status != Action.DIE) {
                            String loc = i + "-" + j;
                            if (((Hashtable) t.getAttribute("inf_i")).containsKey(loc)) {
                                states[i][j].globalInfo = true;
                                break;
                            }
                        }
                    }
                }
            }
            isCalculating = false;
        } else {
            System.out.println("entra!");
        }
    }
    
     */
    public Double getAmountGlobalInfo() {
        synchronized (NetworkEnvironmentReplication.class) {
            Double amountGlobalInfo = 0.0;
            Iterator<GraphElements.MyVertex> itr = getTopology().getVertices().iterator();
            //List<GraphElements.MyVertex> vertex_t = new ArrayList<>();
            while (itr.hasNext()) {
                GraphElements.MyVertex v = itr.next();
                ArrayList<Object> vertex_info = new ArrayList<>(v.getData());
                //System.out.println("copy" + copy);
                Iterator<Object> it = vertex_info.iterator();
                while (it.hasNext()) {
                    Object x = it.next();
                    if (x == null) {
                        System.out.println("error 2!");
                    }
                    ArrayList<Agent> agentsTemp = new ArrayList(this.getAgents());
                    for (Agent m : agentsTemp) {
                        if (m instanceof MobileAgent) {
                            MobileAgent n = (MobileAgent) m;
                            if (n.status != Action.DIE) {
                                if (n.getData().contains(x)) {
                                    amountGlobalInfo++;
                                    break;
                                }
                            }
                        }
                    }

                }
            }
            return amountGlobalInfo / getTopology().getVertexCount();
        }
    }

    public synchronized void updateWorldAge() {
        age.incrementAndGet();
        /*int average = 0;
        int agentslive = 0;
        for (int k = 0; k < this.getAgents().size(); k++) {
            if ((this.getAgent(k)).status != Action.DIE) {
                if (this.getAgent(k) instanceof MobileAgent) {
                    average += ((MobileAgent) this.getAgent(k)).getRound();
                    agentslive++;
                }
            }
        }
        if (agentslive != 0) {
            average /= agentslive;
            //System.out.println("age:" + average);
            this.setAge(average);
        }*/
    }

    /**
     * @return the age
     */
    public int getAge() {
        //System.out.println("age:" + age);
        return age.get();
    }

    /**
     * @param age the age to set
     */
    /*  public void setAge(int age) {
        this.age = age;
    }
     */
    /**
     * @return the nodesAgents
     */
    public HashMap<GraphElements.MyVertex, ArrayList<Agent>> getNodesAgents() {
        return nodesAgents;
    }

    /**
     * @param nodesAgents the nodesAgents to set
     */
    public void setNodesAgents(HashMap<GraphElements.MyVertex, ArrayList<Agent>> nodesAgents) {
        this.nodesAgents = nodesAgents;
    }

    public boolean areAllAgentsDead() {
        synchronized (NetworkEnvironmentReplication.class) {
            Vector cloneAgents = (Vector) this.getAgents().clone();
            Iterator itr = cloneAgents.iterator();
            while (itr.hasNext()) {
                Agent a = (Agent) itr.next();
                if (a instanceof MobileAgent) {
                    if (((MobileAgent) a).status != Action.DIE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public abstract int getNodesAlive();

    public int getAgentsAlive() {
        int agentsAlive = 0;
        synchronized (NetworkEnvironmentReplication.class) {
            Vector cloneAgents = (Vector) this.getAgents().clone();
            Iterator itr = cloneAgents.iterator();

            while (itr.hasNext()) {
                Agent a = (Agent) itr.next();
                if (a instanceof MobileAgent) {
                    if (((MobileAgent) a).status != Action.DIE) {
                        agentsAlive++;
                    }
                }
            }
        }
        return agentsAlive;
    }

    public abstract boolean isOccuped(GraphElements.MyVertex v);

    public abstract void validateNodesAlive();

    public abstract List<Node> getNodes();
    
    public abstract void evaporatePheromone();
    
    public abstract StatsCreation getStatAgentCreation();
}
