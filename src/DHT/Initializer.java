package DHT;

import peersim.edsim.*;
import peersim.core.*;
import peersim.config.*;

/*
  Module d'initialisation de DHTNode: 
  Fonctionnement:
    pour chaque noeud, le module fait le lien entre la couche transport et la couche applicative
    ensuite, il fait envoyer au noeud 0 un message "Hello" a tous les autres noeuds
 */
public class Initializer implements peersim.core.Control {
    
    private int DHTNodePid;

    public Initializer(String prefix) {
	//recuperation du pid de la couche applicative
	this.DHTNodePid = Configuration.getPid(prefix + ".DHTProtocolPid");
    }

    public boolean execute() {
		int nodeNb;
		DHTNode emitter, current;
		Node dest;
		Message helloMsg;
	
		//recuperation de la taille du reseau
		nodeNb = Network.size();
		//creation du message
		helloMsg = new Message(Message.HELLOWORLD,"Hello!!");
		if (nodeNb < 1) {
		    System.err.println("Network size is not positive");
		    System.exit(1);
		}

		//recuperation de la couche applicative de l'emetteur (le noeud 0)
		emitter = (DHTNode)Network.get(0).getProtocol(this.DHTNodePid);
		emitter.setTransportLayer(0);

		//Number of nodes to join
		for (int i = 1; i < 10; i++){
			dest = new GeneralNode("protocol.applicative DHT.DHTNode");
			Network.add(dest);
			current = (DHTNode) dest.getProtocol(this.DHTNodePid);
			current.setTransportLayer(i);
			current.join();
		}

		emitter.test(); //Run all tests in DHTNode
		System.out.println("Initialization completed");
		return false;
    }
}