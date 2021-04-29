package DHT;

import peersim.edsim.*;
import peersim.core.*;
import java.util.Random;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import peersim.config.*;

public class DHTNode implements EDProtocol {
	    
    //identifiant de la couche transport
    private int transportPid;

    //objet couche transport
    private HWTransport transport;

    //identifiant de la couche courante (la couche applicative)
    private int mypid;

    //le numero de noeud
    private int nodeId;

    //prefixe de la couche (nom de la variable de protocole du fichier de config)
    private String prefix;

    private int randomId;

    private DHTNode leftNeighbor = this;
    private DHTNode rightNeighbor = this;
    private ArrayList<HashMap<Message, Integer>> data = new ArrayList<HashMap<Message, Integer>>();

    private DHTNode longNeighbor = this;

    private Integer lastUpdate = 1;

    public DHTNode(String prefix) {
    	this.prefix = prefix;
    	//initialisation des identifiants a partir du fichier de configuration
    	this.transportPid = Configuration.getPid(prefix + ".transport");
    	this.mypid = Configuration.getPid(prefix + ".myself");
    	this.transport = null; 
      this.randomId = new Random().nextInt() & Integer.MAX_VALUE;
    }

    public DHTNode getLeftNeighbor(){ return this.leftNeighbor; }
    public DHTNode getRightNeighbor(){ return this.rightNeighbor; }
    public void setLeftNeighbor(DHTNode left){ this.leftNeighbor = left; }
    public void setRightNeighbor(DHTNode right){ this.rightNeighbor = right; }

    public int getId(){ return this.nodeId; }

    public int getRandomId() { return this.randomId; }
    
    public void test(){
      DHTNode temp_node;
      Node net_node;

      //Test DHTNode consistency
      for (int i=0; i <= Network.size()-1; i++){
        net_node = Network.get(i);

        if (!net_node.equals(null)){
          temp_node = (DHTNode) net_node.getProtocol(this.mypid);
          System.out.println("\nLeft node : " + temp_node.getLeftNeighbor().getRandomId() + " Current node : " + temp_node.getRandomId() + " Right Node : " + temp_node.getRightNeighbor().getRandomId());
          System.out.println("Node " + temp_node.getRandomId() + " is correct : " + (temp_node.getLeftNeighbor().getRandomId() < temp_node.getRandomId()));
        }
      }

      //Test leave node
      DHTNode nodeToLeave = (DHTNode) Network.get(2).getProtocol(this.mypid);
      nodeToLeave.putData(new Message(new Random().nextInt() & Integer.MAX_VALUE, "DHT"));
      nodeToLeave.leave();

      //Test send message to node
      // DHTNode emitter = (DHTNode) Network.get(0).getProtocol(this.mypid);
      // DHTNode receiverTest = (DHTNode) Network.get(5).getProtocol(this.mypid);
      // emitter.sendToNode(receiverTest.getRandomId(), "Bonjour");

      //Test put data
      this.putData(new Message(new Random().nextInt() & Integer.MAX_VALUE, "DHT"));
    }

    public int generateRandom(int except, int max){
      int random =(int)Math.floor(Math.random()*(max)) ;
      while (random == except){
        random =(int)Math.floor(Math.random()*(max)) ;
      }
      return random;
    }

    public ArrayList<DHTNode> findMinMax(DHTNode entrypointNode){
      // Find the min randomId and the max randomId of the DHT and return minNode, maxNode

      DHTNode tempNode;
      DHTNode minNode = entrypointNode;
      DHTNode maxNode = entrypointNode;
      Node net_node;

      for (int i = 0; i <= Network.size()-1; i++){
        net_node = Network.get(i);

        if (!net_node.equals(null)){
          tempNode = (DHTNode) net_node.getProtocol(this.mypid);
          if (tempNode.getRandomId() > maxNode.getRandomId() && !tempNode.equals(this)) {
            maxNode = tempNode;
          }
          if (tempNode.getRandomId() < minNode.getRandomId() && !tempNode.equals(this)) {
            minNode = tempNode;
          }
        }
      }
      return new ArrayList(Arrays.asList(minNode, maxNode));
    }

    public void join () {
      DHTNode entrypointNode = (DHTNode) Network.get(generateRandom(this.getId(), Network.size())).getProtocol(this.mypid);
      ArrayList<DHTNode> minMax = this.findMinMax(entrypointNode);
      DHTNode minNode = minMax.get(0);
      DHTNode maxNode = minMax.get(1);

      //Define max/min neighbors
      if (this.getRandomId() < minNode.getRandomId() || this.getRandomId() > maxNode.getRandomId()){
        this.setLeftNeighbor(maxNode);
        maxNode.setRightNeighbor(this);
        this.setRightNeighbor(minNode);
        minNode.setLeftNeighbor(this);
      }

      //If node to join is not a min or a max
      else if (this.getRandomId() > entrypointNode.getRandomId()){
        //ascendant
        while (this.getRandomId() > entrypointNode.getRandomId()){
          entrypointNode = (DHTNode) Network.get(entrypointNode.getRightNeighbor().getId()).getProtocol(this.mypid);
        }
        DHTNode leftNode = entrypointNode.getLeftNeighbor();
        this.setLeftNeighbor(leftNode);
        leftNode.setRightNeighbor(this);
        this.setRightNeighbor(entrypointNode);
        entrypointNode.setLeftNeighbor(this);
      }
      else {
        //descendant
        while (this.getRandomId() < entrypointNode.getRandomId()){
          entrypointNode = (DHTNode) Network.get(entrypointNode.getLeftNeighbor().getId()).getProtocol(this.mypid);
        }
        DHTNode rightNode = entrypointNode.getRightNeighbor();
        this.setLeftNeighbor(entrypointNode);
        entrypointNode.setRightNeighbor(this);
        this.setRightNeighbor(rightNode);
        rightNode.setLeftNeighbor(this);
      }
      
      //Get data from left neighbor
      for (HashMap element : this.getLeftNeighbor().getData()){
        if ((Integer)element.values().toArray()[0] == this.getLeftNeighbor().getRandomId()){
          this.addData((Message)element.keySet().toArray()[0], (Integer)element.values().toArray()[0]);
          this.getRightNeighbor().removeData(element);
        }
      }

      //Get data from right neighbor
      for (HashMap element : this.getRightNeighbor().getData()){
        if ((Integer)element.values().toArray()[0] == this.getRightNeighbor().getRandomId()){
          this.addData((Message)element.keySet().toArray()[0], (Integer)element.values().toArray()[0]);
          this.getLeftNeighbor().removeData(element);
        }
      }

      //Update long neighbor
      //To avoid updating at each join, we define a value (lastUpdate + 10%) at which execute updateLongNeighbors
      if (Network.size() <= 10){
        this.lastUpdate = Network.size();
        this.setLongNeighbor(this.getLeftNeighbor().getLongNeighbor().getRightNeighbor());
      }
      else if (Network.size() > 10 && (this.lastUpdate + (1/10)*this.lastUpdate == Network.size())){
        this.updateLongNeighbors();
        this.lastUpdate = Network.size();
      }
      else{
        this.setLongNeighbor(this.getLeftNeighbor().getLongNeighbor().getRightNeighbor());
      }
    }

    public void leave(){
      //Set neighbors
      this.getLeftNeighbor().setRightNeighbor(this.getRightNeighbor());
      this.getRightNeighbor().setLeftNeighbor(this.getLeftNeighbor());

      //Set neighbors data
      for (HashMap element : this.getLeftNeighbor().getData()){
        if ((Integer)element.values().toArray()[0] == this.getLeftNeighbor().getRandomId()){
          this.getRightNeighbor().addData((Message)element.keySet().toArray()[0], (Integer)element.values().toArray()[0]);
        }
      }

      for (HashMap element : this.getRightNeighbor().getData()){
        if ((Integer)element.values().toArray()[0] == this.getRightNeighbor().getRandomId()){
          this.getLeftNeighbor().addData((Message)element.keySet().toArray()[0], (Integer)element.values().toArray()[0]);
        }
      }

      //Notify neighbors that the node has left the DHT
      this.sendToNode(this.getLeftNeighbor().getRandomId(), "Node "+this.getRandomId()+ " left the network");
      this.sendToNode(this.getRightNeighbor().getRandomId(), "Node "+this.getRandomId()+" left the network");

      Network.remove(this.getId());

      this.updateLongNeighbors();
    }

    private void addData(Message msg, Integer parentId){
      HashMap <Message, Integer> dataToInsert = new HashMap<Message, Integer>();
      dataToInsert.put(msg, parentId);
      this.data.add(dataToInsert);
    };

    public ArrayList<HashMap<Message, Integer>> getData(){
      return this.data;
    }

    private void removeData(HashMap element){
      this.data.remove(element);
    }

    public void putData(Message msg){
      DHTNode nearestNode = this.getRightNeighbor();
      Node networkNode;
      DHTNode tempNode;

      //Find nearest node of the data id
      for (int i = 0; i < Network.size(); i++){
        networkNode = Network.get(i);
        if (!networkNode.equals(null)){
          tempNode = (DHTNode) networkNode.getProtocol(this.mypid);
          if (Math.abs(tempNode.getRandomId() - msg.getType()) < nearestNode.getRandomId()){
            nearestNode = tempNode;
          }
        }
      }

      nearestNode.getLeftNeighbor().addData(msg, nearestNode.getRandomId());
      nearestNode.getRightNeighbor().addData(msg, nearestNode.getRandomId());
      nearestNode.addData(msg, nearestNode.getRandomId());

      LocalTime now = LocalTime.now();
      System.out.println(now + " data " + msg + " inserted in node " + nearestNode.getRandomId() + " and duplicated in nodes " + nearestNode.getLeftNeighbor().getRandomId() + ", " + nearestNode.getRightNeighbor().getRandomId() + "\n");
    }

    public void sendToNode(int receiver, String msg){
      DHTNode entrypointNode = this;

      // Find the receiver node
      while (entrypointNode.getRandomId() != receiver){
        entrypointNode = (DHTNode) Network.get(entrypointNode.getRightNeighbor().getId()).getProtocol(this.mypid);
      }

      this.send(new Message(this.getId(), msg), Network.get(entrypointNode.getId()));
    }

    public void setLongNeighbor(DHTNode node){
      this.longNeighbor = node;
    }

    public void findLongNeighbor(){
      int distance = (Integer) Network.size()/2 ;
      DHTNode current = this;

      for (int i = 0; i < distance; i ++){
        current = current.getRightNeighbor();
      }

      this.setLongNeighbor(current);

      System.out.println("Distance : "+distance+", Node "+this.getRandomId()+", long neighbor : "+this.getLeftNeighbor().getRandomId());
    }

    public void updateLongNeighbors(){
      for (int i=0; i < Network.size(); i ++){
        DHTNode node = (DHTNode) Network.get(i).getProtocol(this.mypid);
        node.findLongNeighbor();
      }
    }

    public DHTNode getLongNeighbor(){ return this.longNeighbor; }

    //methode appelee lorsqu'un message est recu par le protocole HelloWorld du noeud
    public void processEvent( Node node, int pid, Object event ) {
    	this.receive((Message)event);
    }

    //methode necessaire pour la creation du reseau (qui se fait par clonage d'un prototype)
    public Object clone() {

		DHTNode dolly = new DHTNode(this.prefix);
	
		return dolly;
    }

    //liaison entre un objet de la couche applicative et un 
    //objet de la couche transport situes sur le meme noeud
    public void setTransportLayer(int nodeId) {
    	this.nodeId = nodeId;
    	this.transport = (HWTransport) Network.get(this.nodeId).getProtocol(this.transportPid);
    }

    //envoi d'un message (l'envoi se fait via la couche transport)
    public void send(Message msg, Node dest) {
    	this.transport.send(getMyNode(), dest, msg, this.mypid);
    }
    
    public void sendToNextNode(Message msg) {
    	if (this.nodeId > 0 && this.nodeId+2 <= Network.size()) {
        	this.send(msg, Network.get(this.nodeId+1));
    	}
    	
    	else if (this.nodeId > 0 && this.nodeId+1 == Network.size()) {
    		this.send(msg, Network.get(0));
    	}
    }
    
    //affichage a la reception
    private void receive(Message msg) {
      LocalTime now = LocalTime.now();
      System.out.println("\n" + now + " " + this + ": Received " + msg.getContent());
    	//this.sendToNextNode(msg);
    }

    //retourne le noeud courant
    public Node getMyNode() {
    	return Network.get(this.nodeId);
    }

    public String toString() {
    	return "Node "+ this.nodeId;
    }
}
